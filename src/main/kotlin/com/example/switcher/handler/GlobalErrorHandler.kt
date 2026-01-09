package com.example.switcher.handler

import com.example.switcher.error.*
import com.example.switcher.error.response.ErrorResponse
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

/**
 * Global error handler that catches all exceptions and returns standardized error responses.
 *
 * This handler should be registered as the last handler in the router chain using:
 * `router.errorHandler(500, GlobalErrorHandler())`
 */
class GlobalErrorHandler : Handler<RoutingContext> {

  private val logger = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

  override fun handle(ctx: RoutingContext) {
    val failure = ctx.failure()
    val statusCode = ctx.statusCode()
    val path = ctx.request().path()

    logger.debug("Handling error: statusCode=$statusCode, failure=${failure?.message}", failure)

    val errorResponse = when {
      // Application-specific exceptions
      failure is AppException -> handleAppException(failure, path)

      // HTTP status code based errors (includes OpenAPI validation errors)
      statusCode in 400..599 -> handleHttpStatusError(statusCode, failure?.message, path)

      // Unexpected errors
      else -> handleUnexpectedError(failure, path)
    }

    // Log errors based on severity
    when (errorResponse.statusCode) {
      in 500..599 -> logger.error("Server error: ${errorResponse.message}", failure)
      in 400..499 -> logger.warn("Client error: ${errorResponse.message}")
      else -> logger.debug("Error: ${errorResponse.message}")
    }

    // Send response
    ctx.response()
      .setStatusCode(errorResponse.statusCode)
      .putHeader("Content-Type", "application/json; charset=utf-8")
      .end(errorResponse.toJsonObject().encode())
  }

  /**
   * Handles application-specific exceptions (AppException and subclasses).
   */
  private fun handleAppException(exception: AppException, path: String?): ErrorResponse {
    return when (exception) {
      is ValidationException -> ErrorResponse(
        error = "Validation Error",
        message = exception.message,
        errorCode = exception.errorCode,
        statusCode = exception.statusCode,
        validationErrors = exception.validationErrors,
        path = path
      )

      is NotFoundException -> ErrorResponse(
        error = "Not Found",
        message = exception.message,
        errorCode = exception.errorCode,
        statusCode = exception.statusCode,
        path = path
      )

      is UnauthorizedException -> ErrorResponse(
        error = "Unauthorized",
        message = exception.message,
        errorCode = exception.errorCode,
        statusCode = exception.statusCode,
        path = path
      )

      is ForbiddenException -> ErrorResponse(
        error = "Forbidden",
        message = exception.message,
        errorCode = exception.errorCode,
        statusCode = exception.statusCode,
        path = path
      )

      is BadRequestException -> ErrorResponse(
        error = "Bad Request",
        message = exception.message,
        errorCode = exception.errorCode,
        statusCode = exception.statusCode,
        path = path
      )

      else -> ErrorResponse(
        error = exception::class.simpleName ?: "Application Error",
        message = exception.message,
        errorCode = exception.errorCode,
        statusCode = exception.statusCode,
        path = path
      )
    }
  }

  /**
   * Handles errors based on HTTP status codes.
   */
  private fun handleHttpStatusError(
    statusCode: Int,
    message: String?,
    path: String?
  ): ErrorResponse {
    val (error, defaultMessage, errorCode) = when (statusCode) {
      400 -> Triple("Bad Request", "The request is invalid", "BAD_REQUEST")
      401 -> Triple("Unauthorized", "Authentication required", "UNAUTHORIZED")
      403 -> Triple("Forbidden", "Access denied", "FORBIDDEN")
      404 -> Triple("Not Found", "Resource not found", "NOT_FOUND")
      405 -> Triple("Method Not Allowed", "HTTP method not allowed", "METHOD_NOT_ALLOWED")
      408 -> Triple("Request Timeout", "Request timed out", "REQUEST_TIMEOUT")
      409 -> Triple("Conflict", "Resource conflict", "CONFLICT")
      415 -> Triple("Unsupported Media Type", "Content type not supported", "UNSUPPORTED_MEDIA_TYPE")
      422 -> Triple("Unprocessable Entity", "Request data is invalid", "UNPROCESSABLE_ENTITY")
      429 -> Triple("Too Many Requests", "Rate limit exceeded", "TOO_MANY_REQUESTS")
      500 -> Triple("Internal Server Error", "An unexpected error occurred", "INTERNAL_ERROR")
      502 -> Triple("Bad Gateway", "Upstream service error", "BAD_GATEWAY")
      503 -> Triple("Service Unavailable", "Service temporarily unavailable", "SERVICE_UNAVAILABLE")
      504 -> Triple("Gateway Timeout", "Upstream service timeout", "GATEWAY_TIMEOUT")
      else -> Triple("Error", "An error occurred", "UNKNOWN_ERROR")
    }

    return ErrorResponse(
      error = error,
      message = message ?: defaultMessage,
      errorCode = errorCode,
      statusCode = statusCode,
      path = path
    )
  }

  /**
   * Handles unexpected errors that don't fit into other categories.
   */
  private fun handleUnexpectedError(failure: Throwable?, path: String?): ErrorResponse {
    logger.error("Unexpected error occurred", failure)

    return ErrorResponse(
      error = "Internal Server Error",
      message = if (failure != null) {
        "An unexpected error occurred: ${failure.message}"
      } else {
        "An unexpected error occurred"
      },
      errorCode = "INTERNAL_ERROR",
      statusCode = 500,
      path = path
    )
  }
}
