package com.example.switcher.dto.response.users

import io.vertx.core.json.JsonObject

data class UserResponse(
  val id: String?,
  val email: String?,
  val createdAt: String?,
) {
  fun toResponse(): String = JsonObject.mapFrom(this).encode()
}
