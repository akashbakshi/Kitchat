package com.akashbakshi

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import freemarker.cache.*
import io.ktor.freemarker.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.client.*
import kotlin.test.*
import io.ktor.server.testing.*
import org.mindrot.jbcrypt.BCrypt

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun validUserTest(){
        val user = User("akash227", BCrypt.hashpw("test123",BCrypt.gensalt(12)))

        val loginCred = LoginForm("akash227","test123")

        assertTrue(BCrypt.checkpw(loginCred.password,user.password))
    }

    @Test
    fun invalidUserTest(){
        val user = User("akash227", BCrypt.hashpw("test123",BCrypt.gensalt(12)))

        val loginCred = LoginForm("akash227","test12322222")

        assertFalse(BCrypt.checkpw(loginCred.password,user.password))
    }
}
