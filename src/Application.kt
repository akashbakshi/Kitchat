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
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.util.toZonedDateTime
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashSet

val connections =  Collections.synchronizedSet(LinkedHashSet<SocketSession>())

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
            val roomName = call.request.queryParameters["roomName"]

            val roomData = call.sessions.get<UserSession>()?.username?.let { it1 ->

                    RoomData(it1,roomName)

            }


            call.respond(FreeMarkerContent("chat.ftl",mapOf("room" to roomData),""))
        }

        post("/enterchat"){
            val formData = call.receiveParameters()
            println(formData)

            call.sessions.set(formData["username"]?.let { it1 -> UserSession(it1) })
            call.respondRedirect("/chatroom?roomName="+formData["roomName"])
        }
        webSocket("/chat") {
            val client = SocketSession(UUID.randomUUID().toString(),this)
            connections += client
            println("connected user: ")
            try {
                while (true) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        val data = frame.readText()
                        println(data)
                        val incomingData = Gson().fromJson(frame.readText(), ChatData::class.java)
                        incomingData.msgTimestamp = Date()
                        println(incomingData)
                        for(conn in connections){

                            conn.webSocketSession.send(Gson().toJson(incomingData))
                        }
                    }
                }
            }finally {
                connections -= client
            }
        }


    }
}

data class ChatData(val username:String, val content:String, val roomName: String, var msgTimestamp: Date )
data class RoomData(val username:String, val name: String?)
data class UserSession(val username:String)

data class SocketSession(val id:String,val webSocketSession: WebSocketSession)