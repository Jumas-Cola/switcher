package com.example.switcher.handler

import com.example.switcher.MainVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class AuthHandlerTest {

  private lateinit var webClient: WebClient
  private val testEmail = "test-${System.currentTimeMillis()}@example.com"
  private val testPassword = "password123"

  @BeforeEach
  fun setUp(vertx: Vertx, testContext: VertxTestContext) {
    webClient = WebClient.create(vertx)
    vertx.deployVerticle(MainVerticle())
      .onComplete(testContext.succeeding { testContext.completeNow() })
  }

  @AfterEach
  fun tearDown(vertx: Vertx) {
    webClient.close()
  }

  @Test
  fun `register creates new user and returns 201`(vertx: Vertx, testContext: VertxTestContext) {
    val registerRequest = JsonObject()
      .put("email", testEmail)
      .put("password", testPassword)

    webClient.post(8080, "localhost", "/api/register")
      .putHeader("content-type", "application/json")
      .sendJsonObject(registerRequest)
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(201, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))

          val body = response.bodyAsJsonObject()
          assertNotNull(body.getString("id"))
          assertEquals(testEmail, body.getString("email"))
          assertNotNull(body.getString("password"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `login with valid credentials returns token`(vertx: Vertx, testContext: VertxTestContext) {
    val registerRequest = JsonObject()
      .put("email", testEmail)
      .put("password", testPassword)

    webClient.post(8080, "localhost", "/api/register")
      .putHeader("content-type", "application/json")
      .sendJsonObject(registerRequest)
      .compose { registerResponse ->
        testContext.verify {
          assertEquals(201, registerResponse.statusCode())
        }

        val loginRequest = JsonObject()
          .put("email", testEmail)
          .put("password", testPassword)

        webClient.post(8080, "localhost", "/api/login")
          .putHeader("content-type", "application/json")
          .sendJsonObject(loginRequest)
      }
      .onComplete(testContext.succeeding { loginResponse ->
        testContext.verify {
          assertEquals(200, loginResponse.statusCode())
          assertEquals("application/json", loginResponse.getHeader("content-type"))

          val body = loginResponse.bodyAsJsonObject()
          assertNotNull(body.getString("token"))

          val user = body.getJsonObject("user")
          assertNotNull(user)
          assertNotNull(user.getString("id"))
          assertEquals(testEmail, user.getString("email"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `login with invalid password returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val registerRequest = JsonObject()
      .put("email", testEmail)
      .put("password", testPassword)

    webClient.post(8080, "localhost", "/api/register")
      .putHeader("content-type", "application/json")
      .sendJsonObject(registerRequest)
      .compose { registerResponse ->
        testContext.verify {
          assertEquals(201, registerResponse.statusCode())
        }

        val loginRequest = JsonObject()
          .put("email", testEmail)
          .put("password", "wrongpassword")

        webClient.post(8080, "localhost", "/api/login")
          .putHeader("content-type", "application/json")
          .sendJsonObject(loginRequest)
      }
      .onComplete(testContext.succeeding { loginResponse ->
        testContext.verify {
          assertEquals(401, loginResponse.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `login with non-existent user returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val loginRequest = JsonObject()
      .put("email", "nonexistent@example.com")
      .put("password", testPassword)

    webClient.post(8080, "localhost", "/api/login")
      .putHeader("content-type", "application/json")
      .sendJsonObject(loginRequest)
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }
}
