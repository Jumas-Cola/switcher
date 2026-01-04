package com.example.switcher.dto.request

import io.vertx.core.json.JsonObject

class RegisterDto(
  val email: String,
  val password: String,
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("email", email)
      .put("password", password)
  }
}
