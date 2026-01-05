package com.example.switcher.config

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class ConfigLoaderTest {

  @Test
  fun `load config from test file`(vertx: Vertx, testContext: VertxTestContext) {
    ConfigLoader.load(vertx, "test-application.conf")
      .onComplete(testContext.succeeding { config ->
        testContext.verify {
          assertNotNull(config)
          assertEquals("localhost", config.http.host)
          assertEquals(8080, config.http.port)
          assertEquals("localhost", config.database.host)
          assertEquals(5432, config.database.port)
          assertEquals("test-secret-key-for-testing", config.jwt.secret)
        }
        testContext.completeNow()
      })
  }

  @Test
  fun `createTestConfig returns valid config`() {
    val config = ConfigLoader.createTestConfig(
      httpHost = "127.0.0.1",
      httpPort = 9999,
      dbName = "test_db"
    )

    assertEquals("127.0.0.1", config.http.host)
    assertEquals(9999, config.http.port)
    assertEquals("test_db", config.database.database)
    assertEquals("test-secret-key", config.jwt.secret)
  }
}
