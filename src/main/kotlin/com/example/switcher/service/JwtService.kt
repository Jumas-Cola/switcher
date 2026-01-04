package com.example.switcher.service

import com.example.switcher.config.JwtConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.PubSecKeyOptions

class JwtService(vertx: Vertx, private val config: JwtConfig) {

  private val jwtAuth: JWTAuth

  init {
    val jwtAuthOptions = JWTAuthOptions()
      .addPubSecKey(
        PubSecKeyOptions()
          .setAlgorithm("HS256")
          .setBuffer(config.secret)
      )

    jwtAuth = JWTAuth.create(vertx, jwtAuthOptions)
  }

  fun generateToken(userId: String, email: String): String {
    val claims = JsonObject()
      .put("sub", userId)
      .put("email", email)

    val options = JWTOptions()
      .setExpiresInSeconds((config.expirationMs / 1000).toInt())
      .setAlgorithm("HS256")

    return jwtAuth.generateToken(claims, options)
  }

  fun getAuthProvider(): JWTAuth = jwtAuth
}
