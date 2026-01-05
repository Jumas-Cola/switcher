package com.example.switcher.dto.request.switches

import com.example.switcher.model.enums.SwitchState
import com.example.switcher.model.enums.SwitchType
import io.vertx.core.json.JsonObject
import java.util.*

class CreateSwitchDto(
  val name: String,
  val type: SwitchType,
  val state: SwitchState,
  val userId: String,
  val publicCode: UUID
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("name", name)
      .put("type", type)
      .put("state", state)
      .put("userId", userId)
      .put("publicCode", publicCode.toString())
  }
}
