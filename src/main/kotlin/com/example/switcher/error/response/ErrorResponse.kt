package com.example.switcher.error.response

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.Instant

/**
 * Standardized error response structure for all API errors.
 *
 * @property error Error type/category (e.g., "Validation Error", "Not Found")
 * @property message Human-readable error description
 * @property errorCode Machine-readable error code for client-side handling
 * @property statusCode HTTP status code
 * @property validationErrors List of field-specific validation errors (optional)
 * @property timestamp ISO-8601 timestamp when the error occurred
 * @property path Request path where the error occurred (optional)
 */
data class ErrorResponse(
  val error: String,
  val message: String,
  val errorCode: String,
  val statusCode: Int,
  val validationErrors: List<ValidationError>? = null,
  val timestamp: String = Instant.now().toString(),
  val path: String? = null
) {
  fun toJsonObject(): JsonObject {
    val json = JsonObject()
      .put("error", error)
      .put("message", message)
      .put("errorCode", errorCode)
      .put("statusCode", statusCode)
      .put("timestamp", timestamp)

    path?.let { json.put("path", it) }

    validationErrors?.let { errors ->
      val errorsArray = JsonArray()
      errors.forEach { errorsArray.add(it.toJsonObject()) }
      json.put("validationErrors", errorsArray)
    }

    return json
  }

  companion object {
    /**
     * Creates an ErrorResponse from a Throwable.
     */
    fun fromThrowable(
      throwable: Throwable,
      statusCode: Int = 500,
      errorCode: String = "INTERNAL_ERROR",
      path: String? = null
    ): ErrorResponse {
      return ErrorResponse(
        error = throwable::class.simpleName ?: "Unknown Error",
        message = throwable.message ?: "An unexpected error occurred",
        errorCode = errorCode,
        statusCode = statusCode,
        path = path
      )
    }
  }
}
