package com.example.switcher.model

import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import java.time.LocalDateTime
import java.util.*

data class Switch(
  val id: UUID,
  val name: String,
  val type: String,
  val state: String,
  val publicCode: UUID,
  val userId: UUID,
  val toggledAt: LocalDateTime?,
  val createdAt: LocalDateTime,
) {
  fun toJson(): JsonObject = JsonObject()
    .put("id", id.toString())
    .put("name", name)
    .put("type", type)
    .put("state", state)
    .put("public_code", publicCode.toString())
    .put("user_id", userId.toString())
    .put("created_at", createdAt.toString())
    .put("toggled_at", toggledAt?.toString())

  companion object {
    fun fromRow(row: Row): Switch = Switch(
      id = row.getUUID("id"),
      name = row.getString("name"),
      type = row.getString("type"),
      state = row.getString("state"),
      publicCode = row.getUUID("public_code"),
      userId = row.getUUID("user_id"),
      toggledAt = if (row.getColumnName(row.getColumnIndex("toggled_at")) != null) {
        row.getLocalDateTime("toggled_at")
      } else {
        null
      },
      createdAt = row.getLocalDateTime("created_at"),
    )
  }
}
