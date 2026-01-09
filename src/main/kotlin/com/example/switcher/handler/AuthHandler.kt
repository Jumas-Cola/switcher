package com.example.switcher.handler

import com.example.switcher.dto.request.LoginDto
import com.example.switcher.dto.request.RegisterDto
import com.example.switcher.dto.response.users.UserResponse
import com.example.switcher.error.AppException
import com.example.switcher.error.UnauthorizedException
import com.example.switcher.error.ValidationException
import com.example.switcher.error.response.ValidationError
import com.example.switcher.service.JwtService
import com.example.switcher.util.PasswordHasher
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
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

        when (err) {
          is ReplyException -> {
            // Check if it's a duplicate email error (PostgreSQL unique constraint violation)
            if (err.message?.contains("duplicate key", ignoreCase = true) == true ||
              err.message?.contains("users_email_key", ignoreCase = true) == true
            ) {
              ctx.fail(
                ValidationException(
                  ValidationError("email", "Email already exists", "EMAIL_EXISTS")
                )
              )
            } else {
              ctx.fail(
                AppException(
                  "Failed to register user",
                  statusCode = 500,
                  errorCode = "REGISTRATION_FAILED",
                  cause = err
                )
              )
            }
          }

          else -> ctx.fail(
            AppException(
              "Failed to register user",
              statusCode = 500,
              errorCode = "REGISTRATION_FAILED",
              cause = err
            )
          )
        }
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
          ctx.fail(UnauthorizedException.invalidCredentials())
        }
      }
      .onFailure { err ->
        when {
          err is ReplyException && err.failureCode() == 404 -> {
            // User not found - return same error as invalid password for security
            ctx.fail(UnauthorizedException.invalidCredentials())
          }

          else -> {
            logger.error("Failed to login", err)
            ctx.fail(AppException("Login failed", statusCode = 500, errorCode = "LOGIN_FAILED", cause = err))
          }
        }
      }
  }
}
