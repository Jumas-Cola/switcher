package com.example.switcher.error

/**
 * Base exception class for all application-specific exceptions.
 *
 * @property message The error message
 * @property statusCode HTTP status code associated with this exception
 * @property errorCode Application-specific error code for client handling
 * @property cause The underlying cause of this exception, if any
 */
open class AppException(
  override val message: String,
  val statusCode: Int = 500,
  val errorCode: String = "INTERNAL_ERROR",
  override val cause: Throwable? = null
) : RuntimeException(message, cause)
