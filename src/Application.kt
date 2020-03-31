package com.akashbakshi

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import freemarker.cache.*
import io.ktor.freemarker.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.util.toZonedDateTime
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.io.File
import java.net.Socket
import java.nio.channels.ClosedChannelException
import java.util.*
import kotlin.collections.LinkedHashSet

val connections =  Collections.synchronizedSet(LinkedHashSet<SocketSession>()) // store all Websocket connections
val rooms = arrayListOf<RoomData>() // store all the rooms

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    install(Sessions) {
        cookie<UserSession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        gson {
        }
    }


    routing {
        static("/static"){
            resources("static")
        }
        get("/") {
            call.respond(FreeMarkerContent("index.ftl",null,""))
        }
        get("/chatroom"){
            val roomName = call.request.queryParameters["roomName"] // get room name from queryString

            var newRoom:RoomData? = null

            if (rooms.find { it.name == roomName } == null) {
                newRoom = roomName?.let { it1 -> RoomData(it1, arrayListOf()) } // create a new, let check to ensure roomName isn't null
                newRoom?.let { room-> rooms.add(room) }  // add new room to our array
            }


            newRoom?.let{r-> call.respond(FreeMarkerContent("chat.ftl",mapOf("room" to r),"")) }

            call.respond(HttpStatusCode.NotFound,"Room Cannot be found")
        }

        post("/enterchat"){
            val formData = call.receiveParameters()
            println(formData)

            call.sessions.set(formData["username"]?.let { it1 -> UserSession(UUID.randomUUID().toString(),it1) })
            call.respondRedirect("/chatroom?roomName="+formData["roomName"])
        }
        webSocket("/chat") {
            val client = SocketSession(UserSession(UUID.randomUUID().toString(),null),this) // generate random uuid for socketId and null username for not
            connections += client
            println("connected user: ${client.user.id}")
            client.webSocketSession.send(createServerMsgString(0,client.user)) // send the type 0 for initial handshake to client and the socketId down
            // to the user so they can send their username in the future to associate with their unique socketId
            try {
                while (true) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        val data = frame.readText()

                        //GSON won't parse SocketMsg because our data contains another json object and it won't parse it as a string
                        // it'll try and parse it as an object so we have to trim it temporarily parse the SocketMsg as an object and then
                        // set the data field back to the original string value to get around this
                        val lastBracketPos = data.lastIndexOf("{")
                        var typeJson = data.substring(0,lastBracketPos) // trim upto last { so we can insert an blank string to temporarily parse this
                        typeJson += "\" \"}" // insert blank string and close bracket
                        val incomingData = Gson().fromJson(typeJson, SocketMsg::class.java)
                        incomingData.data = data.substring(lastBracketPos,data.length-1) // parse from the point we chopped off to the end -1 (to remove extra '}' ) to get the original data back
                        handleSocketMessage(incomingData)

                    }
                }
            }catch(e:ClosedReceiveChannelException) {
                connections -= client
                println("${client.user.username} disconnect( ${client.user.id})")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }


    }
}

suspend fun handleSocketMessage(msg:SocketMsg){
    if(msg.type == MsgType.HANDSHAKE.raw){ // if we're in the handshake phase (type 0)

        val handshakeData = Gson().fromJson(msg.data,UserSession::class.java) // parse our data field as UserSession class
        connections.forEach {
            if(it.user.id == handshakeData.id) // loop through all WebSocketSessions and see if the SocketId sent from client matches any active WebSocketSession SocketId
                it.user.username = handshakeData.username // if we get a hit then we want to now set the username sent from client in the handshake phase to make things easier to search for
        }

        connections.forEach { it.webSocketSession.send(createServerMsgString(-1,handshakeData)) }

    }else if(msg.type == MsgType.CHAT_MESSAGE.raw){
        //we received a message from the chat box
        val chatData = Gson().fromJson(msg.data,ChatData::class.java) // parse the data object as chat data now instead
        chatData.msgTimestamp = Date()
        println(msg)
        for(conn in connections){

            conn.webSocketSession.send("{\"type\":0,\"data\":${Gson().toJson(chatData)}")
        }
    }
}

//MsgType will be used when sending messages from client/server it will be used to identify which type of messages are sent
// so we can parse the data: object appropriately, for example in the type 0 HANDSHAKE phase client will send socketId & username
// So we parse it as UserSession class using GSON, but in type 1 CHAT_MESSAGE client sends different chat related data so we
// parse it was a ChatData class using GSON, it will also help us differentiate what methods and and logic to perform based on the
// action type so DIRECT (type 2) will be a direct message sent to a specific user's DM rather than broadcasted to all users in a room

enum class MsgType(val raw:Int){
    HANDSHAKE(0),
    CHAT_MESSAGE(1),
    DIRECT(2)
}

fun createServerMsgString(type:Int,data:Any):String =  "{\"type\":$type,\"data\":${Gson().toJson(data)}}" // function used to dynamically create JSON string based on type and data and return it

suspend fun globalBroadcast(msg:String) = connections.forEach { it.webSocketSession.send(msg) } // function used to broadcast to ALL websocket users
suspend fun roomBroadcast(room:String, msg:String) {
    // This function will be used to broadcast to all users in a given room

    //loop through all our connections
    val roomToBroadcast = rooms.find {r-> r.name == room } // search for the room provided in the argument
    roomToBroadcast?.let{
        //if the room exists loop through all users in that room
        for(roomUser in it.users){
            // broadcast the message to the user
            roomUser.webSocketSession.send(msg)
        }
    }

}

data class SocketMsg(val type:Int,var data:String)
data class ChatData(val username:String, val content:String, val roomName: String, var msgTimestamp: Date )
data class RoomData(val name:String, var users: ArrayList<SocketSession>)
data class UserSession(val id:String,var username:String?)

data class SocketSession(val user:UserSession,val webSocketSession: WebSocketSession)