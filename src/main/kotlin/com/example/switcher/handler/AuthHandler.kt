package com.example.switcher.handler

import com.example.switcher.dto.request.LoginDto
import com.example.switcher.dto.request.RegisterDto
import com.example.switcher.dto.response.users.UserResponse
import com.example.switcher.service.JwtService
import com.example.switcher.util.PasswordHasher
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.openapi.validation.ValidatedRequest

class AuthHandler(private val eventBus: EventBus, private val jwtService: JwtService) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun register(ctx: RoutingContext) {
    val validatedRequest = ctx.get<ValidatedRequest>("openApiValidatedRequest")
    val json = validatedRequest.body.jsonObject

    val email = json.getString("email")
    val password = json.getString("password")
    val hashedPassword = PasswordHasher.hash(password)

    val dto = RegisterDto(
      email = email,
      password = hashedPassword,
    )

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_USER_CREATE, dto.toJsonObject())
      .onSuccess { reply ->
        val body = reply.body()
        ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(
            UserResponse(
              id = body.getString("id"),
              email = body.getString("email"),
              createdAt = body.getString("created_at"),
            ).toResponse()
          )
      }
      .onFailure { err ->
        logger.error("Failed to create user", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun login(ctx: RoutingContext) {
    val validatedRequest = ctx.get<ValidatedRequest>("openApiValidatedRequest")
    val body = validatedRequest.body.jsonObject

    val email = body.getString("email")
    val password = body.getString("password")

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_USER_GET_BY_EMAIL, LoginDto(email).toJsonObject())
      .onSuccess { reply ->
        val user = reply.body()
        val storedHash = user.getString("password")

        if (PasswordHasher.verify(password, storedHash)) {
          val userId = user.getString("id")
          val token = jwtService.generateToken(userId, email)

          val response = JsonObject()
            .put("token", token)
            .put(
              "user", UserResponse(
                id = userId,
                email = email,
                createdAt = user.getString("created_at"),
              )
            )

          ctx.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(response.encode())
        } else {
          ctx.response().setStatusCode(401).end()
        }
      }
      .onFailure { err ->
        if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
          ctx.response().setStatusCode(401).end()
        } else {
          logger.error("Failed to login", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }
}
