package com.example.switcher.handler

import com.example.switcher.dto.request.switches.CreateSwitchDto
import com.example.switcher.model.enums.SwitchState
import com.example.switcher.model.enums.SwitchType
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.openapi.validation.ValidatedRequest
import java.util.*

class SwitchHandler(private val eventBus: EventBus) {

  private val logger = LoggerFactory.getLogger(this::class.java)

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
        if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to get switch", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }

  fun getByUser(ctx: RoutingContext) {
    val userId = ctx.get<String>("userId")
    val request = JsonObject().put("userId", userId)

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

    val dto = CreateSwitchDto(
      name = json.getString("name"),
      type = SwitchType.valueOf(json.getString("type")),
      state = SwitchState.OFF,
      userId = ctx.get("userId"),
      publicCode = UUID.randomUUID(),
    )

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_CREATE, dto.toJsonObject())
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

  fun toggle(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_ID, request)
      .onSuccess { reply ->
        val type = SwitchType.valueOf(reply.body().getString("type"))
        val state = SwitchState.valueOf(reply.body().getString("state"))

        if (state == SwitchState.OFF || type == SwitchType.SWITCH) {
          val json = JsonObject()
            .put("id", id)
            .put("state", if (state == SwitchState.OFF) SwitchState.ON else SwitchState.OFF)

          eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_UPDATE, json)
            .onSuccess { reply ->
              ctx.response()
                .putHeader("content-type", "application/json")
                .end(reply.body().encode())
            }
            .onFailure { err ->
              if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
                ctx.response().setStatusCode(404).end()
              } else {
                logger.error("Failed to toggle switch state", err)
                ctx.response().setStatusCode(500).end()
              }
            }
        }
      }
      .onFailure { err ->
        if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to get switch", err)
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
