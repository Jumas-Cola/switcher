package com.example.switcher.handler

import com.example.switcher.MainVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class HealthHandlerTest {

  private lateinit var webClient: WebClient

  @BeforeEach
  fun setUp(vertx: Vertx, testContext: VertxTestContext) {
    webClient = WebClient.create(vertx)
    vertx.deployVerticle(MainVerticle())
      .onComplete(testContext.succeeding { testContext.completeNow() })
  }

  @Test
  fun `healthCheck returns status ok`(vertx: Vertx, testContext: VertxTestContext) {
    webClient.get(8080, "localhost", "/api/health")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))
          val body = response.bodyAsJsonObject()
          assertEquals("ok", body.getString("status"))
        }
        testContext.completeNow()
      })
  }
}
