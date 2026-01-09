package com.example.switcher.error

/**
 * Exception thrown when a user is authenticated but doesn't have permission to access a resource.
 *
 * @property message The error message
 */
class ForbiddenException(
  message: String = "Access denied"
) : AppException(
  message = message,
  statusCode = 403,
  errorCode = "FORBIDDEN"
) {
  companion object {
    fun resourceOwnershipRequired() = ForbiddenException("You don't have permission to access this resource")
  }
}
