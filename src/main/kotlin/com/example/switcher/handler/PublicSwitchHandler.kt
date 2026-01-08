package com.example.switcher.handler

import com.example.switcher.dto.response.switches.PublicSwitchResponse
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class PublicSwitchHandler(private val eventBus: EventBus) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun getByPublicCode(ctx: RoutingContext) {
    val code = ctx.pathParam("code")
    val request = JsonObject().put("code", code)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_PUBLIC_CODE, request)
      .onSuccess { reply ->
        val body = reply.body()
        val response = PublicSwitchResponse(
          state = body.getString("state"),
          publicCode = body.getString("public_code"),
          toggledAt = body.getString("toggled_at"),
        )

        ctx.response()
          .putHeader("content-type", "application/json")
          .end(response.toJsonObject().encode())
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
}
