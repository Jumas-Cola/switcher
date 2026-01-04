package com.example.switcher.handler

import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.openapi.validation.ValidatedRequest

class UserHandler(private val eventBus: EventBus) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun getAll(ctx: RoutingContext) {
    eventBus.request<Any>(DatabaseVerticle.ADDRESS_USER_GET_ALL, JsonObject())
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().toString())
      }
      .onFailure { err ->
        logger.error("Failed to get users", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun getById(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_USER_GET_BY_ID, request)
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().encode())
      }
      .onFailure { err ->
        if (err.message?.contains("404") == true) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to get user", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }

  fun create(ctx: RoutingContext) {
    val validatedRequest = ctx.get<ValidatedRequest>("openApiValidatedRequest")
    val json = validatedRequest.body.jsonObject

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_USER_CREATE, json)
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
  }
}
