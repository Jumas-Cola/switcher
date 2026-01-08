package com.example.switcher.dto.response.switches

import io.vertx.core.json.JsonObject

data class PublicSwitchResponse(
  val state: String?,
  val publicCode: String?,
  val toggledAt: String?,
) {
  fun toJsonObject() = JsonObject()
    .put("state", state)
    .put("publicCode", publicCode)
    .put("toggledAt", toggledAt)
}
