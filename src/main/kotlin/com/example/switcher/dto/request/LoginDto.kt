package com.example.switcher.dto.request

import io.vertx.core.json.JsonObject

class LoginDto(
  val email: String,
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("email", email)
  }
}
