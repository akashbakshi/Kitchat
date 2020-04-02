package com.akashbakshi

import io.ktor.auth.UserIdPrincipal
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.sessions.*
import org.litote.kmongo.eq
import io.ktor.freemarker.FreeMarkerContent

fun Application.routingModule(){
    routing{
        routes()
    }
}

fun Routing.routes(){
    get("/") {
        val principal = call.sessions.get<UserIdPrincipal>()
        call.respond(
            io.ktor.freemarker.FreeMarkerContent( "index.ftl",  mapOf("user" to principal),
                ""
            )
        )

    }

    post("/enterchat") {
        val formData = call.receiveParameters()


        call.respondRedirect("/chatroom?roomName=" + formData["roomName"])
    }

    // using global route naming to reduce redundancy and add syntax sugar
    route("signup"){
        get {
            val principal = call.sessions.get<UserIdPrincipal>()
            call.respond(
                io.ktor.freemarker.FreeMarkerContent(
                    "signup.ftl",
                    kotlin.collections.mapOf("user" to principal),
                    "e"
                )
            )
        }
        post{
            val rawData = call.receiveParameters()
            val signUpInfo =
                rawData["username"]?.let { it1 ->
                    rawData["password"]?.let { it2 ->
                        rawData["verifypassword"]?.let { it3 ->
                            SignUpForm(it1, it2, it3)
                        }
                    }
                }

            val user = signUpInfo?.let {
                User(
                    signUpInfo.username,
                    org.mindrot.jbcrypt.BCrypt.hashpw(signUpInfo.password, org.mindrot.jbcrypt.BCrypt.gensalt(12))
                ) // create new user with hashed password
            }

            user?.let { userCol.insertOne(it) } // insert user if it's not null

            call.respondRedirect("/login")
        }
    }

    // add route for /login to handle post and get request
    route("login"){
        get{
            val error = call.request.queryParameters["error"]
            val principal = call.sessions.get<UserIdPrincipal>()

            if (call.authentication.principal<UserIdPrincipal>() != null) {
                kotlin.io.println("authenticated")
            }
            call.respond(
                io.ktor.freemarker.FreeMarkerContent(
                    "login.ftl",
                    kotlin.collections.mapOf("user" to principal, "error" to error),
                    "e"
                )
            )
        }

        // authenticate using our Form authentication method defined below
        authenticate("form") {
            // our POST request of /login from the form
            post {
                // this part is called once the user has successfully been authenticated
                val principal = call.principal<UserIdPrincipal>()!!  // get the principal from the authed users
                call.sessions.set(principal) // set the session with the principal
                call.respondRedirect("/") // redirect to default 'login successful' page or in this case back to homepage
            }
        }
    }



    get("logout") {
        call.sessions.clear<UserIdPrincipal>()
        call.respondRedirect("/")
    }



    // If they user if authenticated their auth info will be stored in the session,
    // so we must use authenticate(session) for any routes where we require the user to be logged in
    authenticate("session") {
        get("profile") {
            val principal = call.principal<UserIdPrincipal>()!! // get the principal from the session
            println(principal)
        }

        get("/chatroom") {
            val roomName = call.request.queryParameters["roomName"] // get room name from queryString

            val userSession = call.principal<UserIdPrincipal>()!! // will be used to pass the username down
            val chatMsgs = chatCol.find(ChatData::roomName eq roomName)
                .into(kotlin.collections.mutableListOf<ChatData>()) // find all the chat logs from this specific room
            call.respond(FreeMarkerContent("chat.ftl", mapOf(   "name" to roomName, "username" to userSession.name,  "chat" to chatMsgs ), ""  )
            ) // render the FTL template with the required data
        }
    }

}