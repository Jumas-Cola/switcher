package com.example.switcher.handler

import com.example.switcher.MainVerticle
import com.example.switcher.config.AppConfig
import com.example.switcher.config.ConfigLoader
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class PublicSwitchHandlerTest {

  private lateinit var webClient: WebClient
  private lateinit var config: AppConfig
  private val testEmail = "test-public-${System.currentTimeMillis()}@example.com"
  private val testPassword = "password123"
  private lateinit var authToken: String

  @BeforeEach
  fun setUp(vertx: Vertx, testContext: VertxTestContext) {
    webClient = WebClient.create(vertx)

    ConfigLoader.load(vertx, "test-application.conf")
      .compose { loadedConfig ->
        config = loadedConfig
        vertx.deployVerticle(MainVerticle("test-application.conf"))
      }
      .compose {
        val registerRequest = JsonObject()
          .put("email", testEmail)
          .put("password", testPassword)

        webClient.post(config.http.port, config.http.host, "/api/register")
          .putHeader("content-type", "application/json")
          .sendJsonObject(registerRequest)
      }
      .compose { registerResponse ->
        val loginRequest = JsonObject()
          .put("email", testEmail)
          .put("password", testPassword)

        webClient.post(config.http.port, config.http.host, "/api/login")
          .putHeader("content-type", "application/json")
          .sendJsonObject(loginRequest)
      }
      .onComplete(testContext.succeeding { loginResponse ->
        authToken = loginResponse.bodyAsJsonObject().getString("token")
        testContext.completeNow()
      })
  }

  @AfterEach
  fun tearDown(vertx: Vertx) {
    webClient.close()
  }

  @Test
  fun `get public switch by code returns switch data`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Public Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val publicCode = createResponse.bodyAsJsonObject().getString("public_code")

        // Делаем запрос БЕЗ аутентификации к публичному эндпоинту
        webClient.get(config.http.port, config.http.host, "/api/public/$publicCode")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))

          val body = response.bodyAsJsonObject()
          assertNotNull(body.getString("publicCode"))
          assertNotNull(body.getString("state"))
          assertEquals("OFF", body.getString("state")) // Новый свитч создается в состоянии OFF
          // toggledAt может быть null для нового свитча
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get public switch by code does not require authentication`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Public Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val publicCode = createResponse.bodyAsJsonObject().getString("public_code")

        // Явно НЕ передаем Authorization header
        webClient.get(config.http.port, config.http.host, "/api/public/$publicCode")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get public switch by non-existent code returns 404`(vertx: Vertx, testContext: VertxTestContext) {
    val nonExistentCode = "00000000-0000-0000-0000-000000000000"

    webClient.get(config.http.port, config.http.host, "/api/public/$nonExistentCode")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(404, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get public switch reflects current state after toggle`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Toggle Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")
        val publicCode = createResponse.bodyAsJsonObject().getString("public_code")

        // Переключаем свитч
        webClient.put(config.http.port, config.http.host, "/api/switches/$switchId/toggle")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
          .map(publicCode) // Передаем publicCode дальше
      }
      .compose { publicCode ->
        // Проверяем публичный эндпоинт
        webClient.get(config.http.port, config.http.host, "/api/public/$publicCode")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals("ON", body.getString("state"))
          assertNotNull(body.getString("toggledAt")) // После toggle должна быть дата
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get public switch returns toggled_at timestamp`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Timestamp Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")
        val publicCode = createResponse.bodyAsJsonObject().getString("public_code")

        webClient.put(config.http.port, config.http.host, "/api/switches/$switchId/toggle")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
          .map(publicCode)
      }
      .compose { publicCode ->
        webClient.get(config.http.port, config.http.host, "/api/public/$publicCode")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          val body = response.bodyAsJsonObject()

          // Проверяем наличие всех полей
          assertNotNull(body.getString("state"))
          assertNotNull(body.getString("publicCode"))
          assertNotNull(body.getString("toggledAt"))
        }
        testContext.completeNow()
      })
  }
}
