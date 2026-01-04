package com.example.switcher.handler

import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.openapi.validation.ValidatedRequest

class SwitchHandler(private val eventBus: EventBus) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun getAll(ctx: RoutingContext) {
    eventBus.request<Any>(DatabaseVerticle.ADDRESS_SWITCH_GET_ALL, JsonObject())
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().toString())
      }
      .onFailure { err ->
        logger.error("Failed to get switches", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun getById(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_ID, request)
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().encode())
      }
      .onFailure { err ->
        if (err.message?.contains("404") == true) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to get switch", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }

  fun getByUser(ctx: RoutingContext) {
    val userId = ctx.pathParam("userId")
    val request = JsonObject().put("user_id", userId)

    eventBus.request<Any>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_USER, request)
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().toString())
      }
      .onFailure { err ->
        logger.error("Failed to get switches for user", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun create(ctx: RoutingContext) {
    val validatedRequest = ctx.get<ValidatedRequest>("openApiValidatedRequest")
    val json = validatedRequest.body.jsonObject

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_CREATE, json)
      .onSuccess { reply ->
        ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(reply.body().encode())
      }
      .onFailure { err ->
        logger.error("Failed to create switch", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun update(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val validatedRequest = ctx.get<ValidatedRequest>("openApiValidatedRequest")
    val json = validatedRequest.body.jsonObject.put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_UPDATE, json)
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().encode())
      }
      .onFailure { err ->
        if (err.message?.contains("404") == true) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to update switch", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }

  fun delete(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_DELETE, request)
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().encode())
      }
      .onFailure { err ->
        logger.error("Failed to delete switch", err)
        ctx.response().setStatusCode(500).end()
      }
  }
}
