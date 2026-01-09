package com.example.switcher.error

/**
 * Exception thrown when a request is malformed or contains invalid data.
 *
 * @property message The error message
 */
class BadRequestException(
  message: String = "Bad request"
) : AppException(
  message = message,
  statusCode = 400,
  errorCode = "BAD_REQUEST"
) {
  companion object {
    fun invalidUuid(field: String = "id") = BadRequestException("Invalid UUID format for field '$field'")
    fun missingParameter(param: String) = BadRequestException("Required parameter '$param' is missing")
  }
}
