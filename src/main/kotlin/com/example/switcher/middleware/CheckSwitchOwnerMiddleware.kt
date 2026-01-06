package com.example.switcher.middleware

import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.util.*

class CheckSwitchOwnerMiddleware(
  private val eventBus: EventBus
) : Handler<RoutingContext> {

  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun handle(ctx: RoutingContext) {
    val switchId = ctx.pathParam("id")
    val currentUserId = ctx.get<String>(JwtAuthMiddleware.USER_ID_KEY)

    if (switchId == null) {
      logger.warn("Switch ID is missing in path parameters")
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end("""{"error":"Switch ID is required"}""")
      return
    }

    if (currentUserId == null) {
      logger.warn("User ID is missing in context - JwtAuthMiddleware should run before CheckSwitchOwnerMiddleware")
      ctx.response()
        .setStatusCode(401)
        .putHeader("content-type", "application/json")
        .end("""{"error":"Authentication required"}""")
      return
    }

    // Validate UUID format
    try {
      UUID.fromString(switchId)
    } catch (e: IllegalArgumentException) {
      logger.warn("Invalid switch ID format: $switchId")
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end("""{"error":"Invalid switch ID format"}""")
      return
    }

    val request = JsonObject().put("id", switchId)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_ID, request)
      .onSuccess { reply ->
        val switchData = reply.body()
        val switchUserId = switchData.getString("user_id")

        if (switchUserId == currentUserId) {
          logger.debug("Access granted: Switch $switchId belongs to user $currentUserId")
          ctx.next()
        } else {
          logger.warn("Access denied: Switch $switchId belongs to user $switchUserId, not $currentUserId")
          ctx.response()
            .setStatusCode(403)
            .putHeader("content-type", "application/json")
            .end("""{"error":"You do not have permission to access this switch"}""")
        }
      }
      .onFailure { err ->
        if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
          logger.warn("Switch not found: $switchId")
          ctx.response()
            .setStatusCode(404)
            .putHeader("content-type", "application/json")
            .end("""{"error":"Switch not found"}""")
        } else {
          logger.error("Failed to check switch ownership", err)
          ctx.response()
            .setStatusCode(500)
            .putHeader("content-type", "application/json")
            .end("""{"error":"Internal server error"}""")
        }
      }
  }
}
