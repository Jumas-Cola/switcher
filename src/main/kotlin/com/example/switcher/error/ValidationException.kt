package com.example.switcher.error

import com.example.switcher.error.response.ValidationError

/**
 * Exception thrown when request validation fails.
 *
 * @property message The general validation error message
 * @property validationErrors List of specific field validation errors
 */
class ValidationException(
  message: String = "Validation failed",
  val validationErrors: List<ValidationError> = emptyList()
) : AppException(
  message = message,
  statusCode = 400,
  errorCode = "VALIDATION_ERROR"
) {
  constructor(fieldError: ValidationError) : this(
    message = "Validation failed",
    validationErrors = listOf(fieldError)
  )

  constructor(field: String, fieldMessage: String, code: String = "INVALID_VALUE") : this(
    ValidationError(field, fieldMessage, code)
  )
}
