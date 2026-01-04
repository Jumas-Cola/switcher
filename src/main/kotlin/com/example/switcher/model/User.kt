package com.example.switcher.model

import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import java.time.LocalDateTime
import java.util.UUID

data class User(
  val id: UUID,
  val email: String,
  val password: String? = null,
  val createdAt: LocalDateTime
) {
  fun toJson(): JsonObject = JsonObject()
    .put("id", id.toString())
    .put("email", email)
    .put("password", password)
    .put("created_at", createdAt.toString())

  companion object {
    fun fromRow(row: Row): User = User(
      id = row.getUUID("id"),
      email = row.getString("email"),
      password = row.getString("password"),
      createdAt = row.getLocalDateTime("created_at")
    )
  }
}
