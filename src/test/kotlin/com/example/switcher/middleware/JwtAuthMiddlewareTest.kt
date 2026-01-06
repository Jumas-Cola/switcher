package com.example.switcher.middleware

import com.example.switcher.config.JwtConfig
import com.example.switcher.service.JwtService
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class JwtAuthMiddlewareTest {

  private lateinit var webClient: WebClient
  private lateinit var jwtService: JwtService
  private lateinit var server: io.vertx.core.http.HttpServer
  private val port = 8888

  @BeforeEach
  fun setUp(vertx: Vertx, testContext: VertxTestContext) {
    webClient = WebClient.create(vertx)

    val jwtConfig = JwtConfig(
      secret = "test-secret-for-middleware-test",
      expirationMs = 3600000L
    )
    jwtService = JwtService(vertx, jwtConfig)

    val router = Router.router(vertx)
    val eventBus = vertx.eventBus()
    val middleware = JwtAuthMiddleware(jwtService, eventBus)

    // Mock EventBus handler для получения пользователя
    eventBus.consumer<io.vertx.core.json.JsonObject>("db.user.getById") { message ->
      val userId = message.body().getString("id")
      val createdAt = java.time.LocalDateTime.now().toString() // ISO 8601 формат
      val response = io.vertx.core.json.JsonObject()
        .put("id", userId)
        .put("email", "test@example.com")
        .put("created_at", createdAt)
      message.reply(response)
    }

    // Protected route
    router.get("/protected").handler(middleware).handler { ctx ->
      val userId = ctx.get<String>(JwtAuthMiddleware.USER_ID_KEY)
      val email = ctx.get<String>(JwtAuthMiddleware.USER_EMAIL_KEY)
      ctx.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json")
        .end("""{"userId":"$userId","email":"$email"}""")
    }

    // Public route
    router.get("/public").handler { ctx ->
      ctx.response().setStatusCode(200).end("OK")
    }

    // Небольшая задержка для регистрации EventBus consumer
    vertx.setTimer(100) {
      vertx.createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onComplete(testContext.succeeding { httpServer ->
          server = httpServer
          testContext.completeNow()
        })
    }
  }

  @AfterEach
  fun tearDown(vertx: Vertx, testContext: VertxTestContext) {
    webClient.close()
    if (::server.isInitialized) {
      server.close()
        .onComplete(testContext.succeeding { testContext.completeNow() })
    } else {
      testContext.completeNow()
    }
  }

  @Test
  fun `access protected route without token returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    webClient.get(port, "localhost", "/protected")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `access protected route with invalid token returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    webClient.get(port, "localhost", "/protected")
      .putHeader("Authorization", "Bearer invalid-token")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `access protected route with valid token returns 200`(vertx: Vertx, testContext: VertxTestContext) {
    val userId = "test-user-123"
    val email = "test@example.com"
    val token = jwtService.generateToken(userId, email)

    webClient.get(port, "localhost", "/protected")
      .putHeader("Authorization", "Bearer $token")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertEquals(userId, body.getString("userId"))
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `access public route without token returns 200`(vertx: Vertx, testContext: VertxTestContext) {
    webClient.get(port, "localhost", "/public")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `missing Bearer prefix returns 401`(vertx: Vertx, testContext: VertxTestContext) {
    val token = jwtService.generateToken("user-123", "test@example.com")

    webClient.get(port, "localhost", "/protected")
      .putHeader("Authorization", token)
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(401, response.statusCode())
        }
        testContext.completeNow()
      })
  }
}
