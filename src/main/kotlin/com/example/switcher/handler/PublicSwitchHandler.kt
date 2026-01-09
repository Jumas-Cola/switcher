package com.example.switcher.handler

import com.example.switcher.dto.response.switches.PublicSwitchResponse
import com.example.switcher.error.AppException
import com.example.switcher.error.NotFoundException
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
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
        when {
          err is ReplyException && err.failureCode() == 404 -> {
            ctx.fail(NotFoundException("Switch with public code '$code' not found"))
          }

          else -> {
            logger.error("Failed to get switch by public code", err)
            ctx.fail(
              AppException(
                "Failed to retrieve switch",
                statusCode = 500,
                errorCode = "PUBLIC_SWITCH_RETRIEVAL_FAILED",
                cause = err
              )
            )
          }
        }
      }
  }
}
