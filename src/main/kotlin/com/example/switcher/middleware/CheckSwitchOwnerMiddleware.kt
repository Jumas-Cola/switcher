package com.example.switcher.middleware

import com.example.switcher.error.AppException
import com.example.switcher.error.BadRequestException
import com.example.switcher.error.ForbiddenException
import com.example.switcher.error.NotFoundException
import com.example.switcher.error.UnauthorizedException
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
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
      ctx.fail(BadRequestException.missingParameter("id"))
      return
    }

    if (currentUserId == null) {
      logger.warn("User ID is missing in context - JwtAuthMiddleware should run before CheckSwitchOwnerMiddleware")
      ctx.fail(UnauthorizedException("Authentication required"))
      return
    }

    // Validate UUID format
    try {
      UUID.fromString(switchId)
    } catch (e: IllegalArgumentException) {
      logger.warn("Invalid switch ID format: $switchId")
      ctx.fail(BadRequestException.invalidUuid("id"))
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
          ctx.fail(ForbiddenException.resourceOwnershipRequired())
        }
      }
      .onFailure { err ->
        when {
          err is ReplyException && err.failureCode() == 404 -> {
            logger.warn("Switch not found: $switchId")
            ctx.fail(NotFoundException("Switch", switchId))
          }
          else -> {
            logger.error("Failed to check switch ownership", err)
            ctx.fail(AppException("Failed to verify switch ownership", statusCode = 500, errorCode = "OWNERSHIP_CHECK_FAILED", cause = err))
          }
        }
      }
  }
}
