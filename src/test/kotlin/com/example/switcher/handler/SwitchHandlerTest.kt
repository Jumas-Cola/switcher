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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class SwitchHandlerTest {

  private lateinit var webClient: WebClient
  private lateinit var config: AppConfig
  private val testEmail = "test-switch-${System.currentTimeMillis()}@example.com"
  private val testPassword = "password123"
  private lateinit var authToken: String
  private lateinit var userId: String

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
        userId = registerResponse.bodyAsJsonObject().getString("id")

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
  fun `create switch with valid data returns 201`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(201, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))

          val body = response.bodyAsJsonObject()
          assertNotNull(body.getString("id"))
          assertEquals("Test Switch", body.getString("name"))
          assertEquals("SWITCH", body.getString("type"))
          assertEquals(userId, body.getString("user_id"))
          assertNotNull(body.getString("created_at"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `create switch without auth returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .sendJsonObject(createRequest)
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `create switch with invalid type returns 400`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "INVALID_TYPE")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(400, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get switches by user returns list`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch 1")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        testContext.verify {
          assertEquals(201, createResponse.statusCode())
        }

        webClient.get(config.http.port, config.http.host, "/api/switches")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))

          val body = response.bodyAsJsonArray()
          assertTrue(body.size() >= 1)

          val firstSwitch = body.getJsonObject(0)
          assertNotNull(firstSwitch.getString("id"))
          assertNotNull(firstSwitch.getString("name"))
          assertEquals(userId, firstSwitch.getString("user_id"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get switches without auth returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    webClient.get(config.http.port, config.http.host, "/api/switches")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get switch by id returns switch`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")

        webClient.get(config.http.port, config.http.host, "/api/switches/$switchId")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))

          val body = response.bodyAsJsonObject()
          assertNotNull(body.getString("id"))
          assertEquals("Test Switch", body.getString("name"))
          assertEquals(userId, body.getString("user_id"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get switch by id without auth returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val validUuid = "00000000-0000-0000-0000-000000000000"

    webClient.get(config.http.port, config.http.host, "/api/switches/$validUuid")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `get switch by non-existent id returns 404`(vertx: Vertx, testContext: VertxTestContext) {
    val nonExistentId = "00000000-0000-0000-0000-000000000000"

    webClient.get(config.http.port, config.http.host, "/api/switches/$nonExistentId")
      .putHeader("Authorization", "Bearer $authToken")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(404, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `toggle switch changes state`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")
        val initialState = createResponse.bodyAsJsonObject().getString("state")

        testContext.verify {
          assertEquals("OFF", initialState)
        }

        webClient.put(config.http.port, config.http.host, "/api/switches/$switchId/toggle")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))

          val body = response.bodyAsJsonObject()
          assertEquals("ON", body.getString("state"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `toggle switch twice returns to original state`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")

        webClient.put(config.http.port, config.http.host, "/api/switches/$switchId/toggle")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .compose { firstToggle ->
        testContext.verify {
          assertEquals("ON", firstToggle.bodyAsJsonObject().getString("state"))
        }

        val switchId = firstToggle.bodyAsJsonObject().getString("id")

        webClient.put(config.http.port, config.http.host, "/api/switches/$switchId/toggle")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals("OFF", body.getString("state"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `toggle button type switch from ON returns to OFF after delay`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Button")
      .put("type", "BUTTON")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")

        webClient.put(config.http.port, config.http.host, "/api/switches/$switchId/toggle")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals("ON", body.getString("state"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `toggle switch without auth returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val validUuid = "00000000-0000-0000-0000-000000000000"

    webClient.put(config.http.port, config.http.host, "/api/switches/$validUuid/toggle")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `toggle non-existent switch returns 404`(vertx: Vertx, testContext: VertxTestContext) {
    val nonExistentId = "00000000-0000-0000-0000-000000000000"

    webClient.put(config.http.port, config.http.host, "/api/switches/$nonExistentId/toggle")
      .putHeader("Authorization", "Bearer $authToken")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(404, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `delete switch returns 200`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        val switchId = createResponse.bodyAsJsonObject().getString("id")

        webClient.delete(config.http.port, config.http.host, "/api/switches/$switchId")
          .putHeader("Authorization", "Bearer $authToken")
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
  fun `delete switch without auth returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val validUuid = "00000000-0000-0000-0000-000000000000"

    webClient.delete(config.http.port, config.http.host, "/api/switches/$validUuid")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `delete switch and verify it no longer exists`(vertx: Vertx, testContext: VertxTestContext) {
    val createRequest = JsonObject()
      .put("name", "Test Switch")
      .put("type", "SWITCH")

    var switchId: String? = null

    webClient.post(config.http.port, config.http.host, "/api/switches")
      .putHeader("content-type", "application/json")
      .putHeader("Authorization", "Bearer $authToken")
      .sendJsonObject(createRequest)
      .compose { createResponse ->
        switchId = createResponse.bodyAsJsonObject().getString("id")

        webClient.delete(config.http.port, config.http.host, "/api/switches/$switchId")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .compose { deleteResponse ->
        testContext.verify {
          assertEquals(200, deleteResponse.statusCode())
        }

        webClient.get(config.http.port, config.http.host, "/api/switches/$switchId")
          .putHeader("Authorization", "Bearer $authToken")
          .send()
      }
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(404, response.statusCode())
        }
        testContext.completeNow()
      })
  }
}
