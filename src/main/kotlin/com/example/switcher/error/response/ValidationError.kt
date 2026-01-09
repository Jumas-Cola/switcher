package com.example.switcher.error.response

import io.vertx.core.json.JsonObject

/**
 * Represents a validation error for a specific field.
 *
 * @property field The name of the field that failed validation
 * @property message Human-readable error message
 * @property code Machine-readable error code for client-side handling
 */
data class ValidationError(
  val field: String,
  val message: String,
  val code: String
) {
  fun toJsonObject(): JsonObject = JsonObject()
    .put("field", field)
    .put("message", message)
    .put("code", code)
}
