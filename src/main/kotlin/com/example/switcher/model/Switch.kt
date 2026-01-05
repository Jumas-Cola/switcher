package com.example.switcher.model

import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import java.time.LocalDateTime
import java.util.*

data class Switch(
  val id: UUID,
  val name: String,
  val enabled: Boolean,
  val userId: UUID,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime
) {
  fun toJson(): JsonObject = JsonObject()
    .put("id", id.toString())
    .put("name", name)
    .put("enabled", enabled)
    .put("user_id", userId.toString())
    .put("created_at", createdAt.toString())
    .put("updated_at", updatedAt.toString())

  companion object {
    fun fromRow(row: Row): Switch = Switch(
      id = row.getUUID("id"),
      name = row.getString("name"),
      enabled = row.getBoolean("enabled"),
      userId = row.getUUID("user_id"),
      createdAt = row.getLocalDateTime("created_at"),
      updatedAt = row.getLocalDateTime("updated_at")
    )
  }
}
