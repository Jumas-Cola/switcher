package com.example.switcher.config

import io.vertx.core.json.JsonObject

data class HttpConfig(
  val host: String,
  val port: Int
) {
  companion object {
    fun fromJson(json: JsonObject?): HttpConfig {
      return HttpConfig(
        host = json?.getString("host") ?: "0.0.0.0",
        port = json?.getInteger("port") ?: 8080
      )
    }
  }
}

data class DatabaseConfig(
  val host: String,
  val port: Int,
  val database: String,
  val user: String,
  val password: String,
  val maxPoolSize: Int
) {
  companion object {
    fun fromJson(json: JsonObject?): DatabaseConfig {
      return DatabaseConfig(
        host = json?.getString("host") ?: "localhost",
        port = json?.getInteger("port") ?: 5432,
        database = json?.getString("database") ?: "switchapi",
        user = json?.getString("user") ?: "switchapi",
        password = json?.getString("password") ?: "switchapi",
        maxPoolSize = json?.getInteger("maxPoolSize") ?: 10
      )
    }
  }
}

data class JwtConfig(
  val secret: String,
  val expirationMs: Long
) {
  companion object {
    fun fromJson(json: JsonObject?): JwtConfig {
      return JwtConfig(
        secret = json?.getString("secret") ?: "change-this-secret-in-production",
        expirationMs = json?.getLong("expirationMs") ?: 604800000L
      )
    }
  }
}

data class AppConfig(
  val http: HttpConfig,
  val database: DatabaseConfig,
  val jwt: JwtConfig
) {
  companion object {
    fun fromJson(json: JsonObject): AppConfig {
      return AppConfig(
        http = HttpConfig.fromJson(json.getJsonObject("http")),
        database = DatabaseConfig.fromJson(json.getJsonObject("database")),
        jwt = JwtConfig.fromJson(json.getJsonObject("jwt"))
      )
    }
  }
}