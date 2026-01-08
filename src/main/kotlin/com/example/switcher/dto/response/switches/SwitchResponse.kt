package com.example.switcher.dto.response.switches

import io.vertx.core.json.JsonObject

data class SwitchResponse(
  val id: String?,
  val name: String?,
  val type: String?,
  val state: String?,
  val publicCode: String?,
  val userId: String?,
  val toggledAt: String?,
  val createdAt: String?,
) {
  fun toResponse(): String = JsonObject.mapFrom(this).encode()
}
