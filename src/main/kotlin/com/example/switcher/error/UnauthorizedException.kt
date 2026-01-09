package com.example.switcher.error

/**
 * Exception thrown when authentication is required but not provided or invalid.
 *
 * @property message The error message
 */
class UnauthorizedException(
  message: String = "Authentication required"
) : AppException(
  message = message,
  statusCode = 401,
  errorCode = "UNAUTHORIZED"
) {
  companion object {
    fun invalidCredentials() = UnauthorizedException("Invalid credentials")
    fun invalidToken() = UnauthorizedException("Invalid or expired token")
    fun missingToken() = UnauthorizedException("Authorization token required")
  }
}
