package com.example.switcher.middleware

import com.example.switcher.service.JwtService
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.web.RoutingContext

class JwtAuthMiddleware(
  private val jwtService: JwtService,
  private val eventBus: EventBus
) : Handler<RoutingContext> {

  private val logger = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val USER_KEY = "user"
    const val USER_ID_KEY = "userId"
    const val USER_EMAIL_KEY = "userEmail"
  }

  override fun handle(ctx: RoutingContext) {
    val authHeader = ctx.request().getHeader("Authorization")

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      logger.warn("Missing or invalid Authorization header")
      ctx.response()
        .setStatusCode(401)
        .putHeader("content-type", "application/json")
        .end("""{"error":"Missing or invalid Authorization header"}""")
      return
    }

    val token = authHeader.substring(7)

    jwtService.getAuthProvider().authenticate(TokenCredentials(token))
      .compose { jwtUser ->
        val principal = jwtUser.principal()
        val userId = principal.getString("sub")

        logger.debug("JWT token validated, fetching user from DB: $userId")

        val request = JsonObject().put("id", userId)
        eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_USER_GET_BY_ID, request)
      }
      .onSuccess { reply ->
        try {
          val userJson = reply.body()
          val userId = userJson.getString("id")

          logger.debug("User fetched from DB: $userId")

          ctx.put(USER_KEY, userJson)
          ctx.put(USER_ID_KEY, userId)

          logger.debug("Authenticated user: $userId")
          ctx.next()
        } catch (e: Exception) {
          logger.error("Failed to process user data", e)
          ctx.response()
            .setStatusCode(500)
            .putHeader("content-type", "application/json")
            .end("""{"error":"Internal server error"}""")
        }
      }
      .onFailure { err ->
        logger.warn("JWT authentication failed: ${err.message}", err)
        ctx.response()
          .setStatusCode(401)
          .putHeader("content-type", "application/json")
          .end("""{"error":"Invalid or expired token"}""")
      }
  }

  /**
   * Вспомогательный метод для извлечения данных пользователя из контекста
   */
  fun getUserJson(ctx: RoutingContext): JsonObject? {
    return ctx.get(USER_KEY)
  }

  /**
   * Вспомогательный метод для извлечения ID пользователя из контекста
   */
  fun getUserId(ctx: RoutingContext): String? {
    return ctx.get(USER_ID_KEY)
  }

  /**
   * Вспомогательный метод для извлечения email пользователя из контекста
   */
  fun getUserEmail(ctx: RoutingContext): String? {
    return ctx.get(USER_EMAIL_KEY)
  }
}
