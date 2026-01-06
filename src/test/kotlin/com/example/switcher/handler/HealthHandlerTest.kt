package com.example.switcher.handler

import com.example.switcher.MainVerticle
import com.example.switcher.config.AppConfig
import com.example.switcher.config.ConfigLoader
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class HealthHandlerTest {

  private lateinit var webClient: WebClient
  private lateinit var config: AppConfig
  private lateinit var deploymentId: String

  @BeforeEach
  fun setUp(vertx: Vertx, testContext: VertxTestContext) {
    webClient = WebClient.create(vertx)

    ConfigLoader.load(vertx, "test-application.conf")
      .compose { loadedConfig ->
        config = loadedConfig
        vertx.deployVerticle(MainVerticle("test-application.conf"))
      }
      .onComplete(testContext.succeeding { id ->
        deploymentId = id
        testContext.completeNow()
      })
  }

  @AfterEach
  fun tearDown(vertx: Vertx, testContext: VertxTestContext) {
    webClient.close()
    if (::deploymentId.isInitialized) {
      vertx.undeploy(deploymentId)
        .onComplete(testContext.succeeding { testContext.completeNow() })
    } else {
      testContext.completeNow()
    }
  }

  @Test
  fun `healthCheck returns status ok`(vertx: Vertx, testContext: VertxTestContext) {
    webClient.get(config.http.port, config.http.host, "/api/health")
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
