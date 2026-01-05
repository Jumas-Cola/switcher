package com.example.switcher.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

object ConfigLoader {
  fun load(
    vertx: Vertx,
    configPath: String = "application.conf",
    clientHost: String? = null
  ): Future<AppConfig> {
    val hoconStore = ConfigStoreOptions()
      .setType("file")
      .setFormat("hocon")
      .setConfig(JsonObject().put("path", configPath))

    val options = ConfigRetrieverOptions().addStore(hoconStore)
    val retriever = ConfigRetriever.create(vertx, options)

    return retriever.config.map { json ->
      val config = AppConfig.fromJson(json)
      if (clientHost != null) {
        config.copy(http = config.http.copy(host = clientHost))
      } else {
        config
      }
    }
  }

  fun createTestConfig(
    httpHost: String = "localhost",
    httpPort: Int = 8080,
    dbHost: String = "localhost",
    dbPort: Int = 5432,
    dbName: String = "switchapi_test",
    dbUser: String = "switchapi",
    dbPassword: String = "switchapi",
    jwtSecret: String = "test-secret-key",
    jwtExpirationMs: Long = 3600000L
  ): AppConfig {
    return AppConfig(
      http = HttpConfig(httpHost, httpPort),
      database = DatabaseConfig(dbHost, dbPort, dbName, dbUser, dbPassword, 10),
      jwt = JwtConfig(jwtSecret, jwtExpirationMs)
    )
  }
}
