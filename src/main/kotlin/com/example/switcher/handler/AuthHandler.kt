package com.example.switcher.handler

import com.example.switcher.dto.request.RegisterRequest
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class AuthHandler(private val eventBus: EventBus) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun register(ctx: RoutingContext) {
    ctx.request().bodyHandler { body ->
      val json = body.toJsonObject()
      try {
        val dto = RegisterRequest(
          email = json.getString("email"),
          password = json.getString("password")
        )

        eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_USER_CREATE, dto.toJsonObject())
          .onSuccess { reply ->
            ctx.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json")
              .end(reply.body().encode())
          }
          .onFailure { err ->
            logger.error("Failed to create user", err)
            ctx.response().setStatusCode(500).end()
          }
      } catch (e: Exception) {
        logger.error(e.message, e)
        ctx.response().setStatusCode(400).end()
      }
    }
  }
}
