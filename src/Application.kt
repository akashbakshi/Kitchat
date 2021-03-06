package com.akashbakshi

import com.google.gson.Gson
import com.mongodb.MongoClientURI
import io.ktor.application.*
import io.ktor.routing.*
import freemarker.cache.*
import io.ktor.auth.*
import io.ktor.freemarker.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.litote.kmongo.*
import java.util.*
import org.mindrot.jbcrypt.BCrypt

val connections =  arrayListOf<SocketSession>() // store all Websocket connections
val rooms = arrayListOf<RoomData>() // store all the rooms
val uri = MongoClientURI("mongodb://root:rootPassword@localhost:27017/")
val mongoClient = KMongo.createClient(uri = uri)

val db = mongoClient.getDatabase("kitchatDB")
val userCol = db.getCollection<User>()
val chatCol = db.getCollection<ChatData>()

fun main(args: Array<String>){
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }


    install(Sessions) {
        cookie<UserIdPrincipal>(
            // We set a cookie by this name upon login.
            "auth",
            // Stores session contents in memory...good for development only.
            storage = SessionStorageMemory()
        ) {
            cookie.path = "/"
            // CSRF protection in modern browsers. Make sure your important side-effect-y operations, like ordering,
            // uploads, and changing settings, use "unsafe" HTTP verbs like POST and PUT, not GET or HEAD.
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies#SameSite_cookies
            cookie.extensions["SameSite"] = "lax"
        }

    }



    install(WebSockets) {
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

        webSocket("/chat") {
            val client = SocketSession(UserSession(UUID.randomUUID().toString(),null),this) // generate random uuid for socketId and null username for not
            connections += client
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
                connections.removeIf { c-> c.user.id == client.user.id }
                removeFromRooms(client)
                println("${client.user.username} disconnect( ${client.user.id})")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

    }
}

suspend fun handleSocketMessage(msg:SocketMsg){
    if(msg.type == MsgType.HANDSHAKE.raw){ // if we're in the handshake phase (type 0)

        val handshakeData = Gson().fromJson(msg.data,HandshakeData::class.java) // parse our data field as UserSession class
        connections.forEach {
            if (it.user.id == handshakeData.id) { // loop through all WebSocketSessions and see if the SocketId sent from client matches any active WebSocketSession SocketId
                it.user.username = handshakeData.username // if we get a hit then we want to now set the username sent from client in the handshake phase to make things easier to search for
                addToRoom(handshakeData.room,it)

                roomBroadcast(handshakeData.room,createServerMsgString(MsgType.NEW_USER.raw,it.user))
            }
        }
        connections.forEach { it.webSocketSession.send(createServerMsgString(MsgType.NEW_USER.raw,handshakeData)) }

    }else if(msg.type == MsgType.CHAT_MESSAGE.raw){
        //we received a message from the chat box
        val chatData = Gson().fromJson(msg.data,ChatData::class.java) // parse the data object as chat data now instead
        chatData.msgTimestamp = Date()
        chatCol.insertOne(chatData)

        for(conn in connections){

            conn.webSocketSession.send(createServerMsgString(1,chatData))
        }
    }
}



//MsgType will be used when sending messages from client/server it will be used to identify which type of messages are sent
// so we can parse the data: object appropriately, for example in the type 0 HANDSHAKE phase client will send socketId & username
// So we parse it as UserSession class using GSON, but in type 1 CHAT_MESSAGE client sends different chat related data so we
// parse it was a ChatData class using GSON, it will also help us differentiate what methods and and logic to perform based on the
// action type so DIRECT (type 2) will be a direct message sent to a specific user's DM rather than broadcasted to all users in a room

enum class MsgType(val raw:Int){
    USER_LEFT(-2),
    NEW_USER(-1),
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

fun addToRoom(room:String,socketSession:SocketSession){
    val roomToFind = rooms.find { it.name == room }
    if (roomToFind == null) {
        val newRoom = RoomData(room, arrayListOf(socketSession))
        rooms.add(newRoom )  // add new room to our array

    }else{
        roomToFind.users.add(socketSession)
    }

}

fun removeFromRooms(socketSession:SocketSession){
    rooms.forEach { room->
        room.users.removeIf { u-> u.user.id == socketSession.user.id }
    }
}

data class SocketMsg(val type:Int,var data:String)
data class ChatData(val username:String, val content:String, val roomName: String, var msgTimestamp: Date )
data class RoomData(val name:String, var users: ArrayList<SocketSession>)


data class SignUpForm(val username:String,var password:String,var verifypassword:String)
data class LoginForm(val username:String,var password:String)

data class User(var username:String,var password:String)
data class UserSession(val id:String,var username:String?)
data class HandshakeData(val id:String,val username:String,val room:String)
data class SocketSession(val user:UserSession,val webSocketSession: WebSocketSession)