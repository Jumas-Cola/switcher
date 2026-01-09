package com.example.switcher.error

/**
 * Exception thrown when a requested resource is not found.
 *
 * @property message The error message describing what was not found
 */
class NotFoundException(
  message: String = "Resource not found"
) : AppException(
  message = message,
  statusCode = 404,
  errorCode = "NOT_FOUND"
) {
  constructor(resourceType: String, id: String) : this(
    "$resourceType with id '$id' not found"
  )
}
