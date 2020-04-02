package com.akashbakshi

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.response.respondRedirect
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.mindrot.jbcrypt.BCrypt


fun Application.authModule(){
    install(Authentication){
        configureSessionAuth()
        configureFormAuth()
    }
}

private fun Authentication.Configuration.configureFormAuth() {

    form("form") {

        userParamName = "username" // name of username field in form
        passwordParamName = "password" //name of password field in form
        challenge {

            val errors:  List<AuthenticationFailedCause> = call.authentication.allFailures
            when (errors.singleOrNull()) {
                AuthenticationFailedCause.InvalidCredentials ->
                    call.respondRedirect("/login?error=invalid") // if invalid credentials redirect with invalid so we have appropriate err msg

                AuthenticationFailedCause.NoCredentials ->
                    call.respondRedirect("/login?error=no") // no credentials, throw with no credentials

                else ->
                    call.respondRedirect("/login") // any other errors just redirect
            }
        }
        validate { cred: UserPasswordCredential ->
            // actual validation login

            // we retrieve the username from our database User collection and if not found return with null which will throw AuthenticationFailedCause.InvalidCredentials shown above

            val userDbInfo = userCol.findOne(User::username eq cred.name) ?: return@validate null

            if (BCrypt.checkpw(cred.password,userDbInfo.password)) // use BCrypt to check plain password (cred.password) from the form against the hashed password from the database
                UserIdPrincipal(cred.name) // store username in the UserIdPrincipal if they match
            else
                null // if they don't match null will throw AuthenticationFailedCause.InvalidCredentials
        }
    }
}

private fun Authentication.Configuration.configureSessionAuth() {
    session<UserIdPrincipal>("session") {
        challenge {
            // redirect if the user isn't logged in
            call.respondRedirect("/login")
        }
        validate { session: UserIdPrincipal ->
            // If you need to do additional validation on session data, you can do so here.
            session
        }
    }
}