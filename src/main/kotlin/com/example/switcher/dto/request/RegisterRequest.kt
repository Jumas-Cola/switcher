package com.example.switcher.dto.request

import io.vertx.core.json.JsonObject

class RegisterRequest(
  val email: String,
  val password: String,
) {
  init {
    require(email.matches(Regex("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}\$"))) {
      "Invalid email format"
    }
  }

  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("email", email)
      .put("password", password)
  }
}
