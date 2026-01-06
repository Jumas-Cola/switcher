package com.example.switcher.repository

import com.example.switcher.model.Switch
import io.vertx.core.Future
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import java.time.LocalDateTime
import java.util.*

class SwitchRepository(private val pool: Pool) {

  fun create(name: String, type: String, state: String, userId: UUID, publicCode: UUID): Future<Switch> {
    val id = UUID.randomUUID()
    val now = LocalDateTime.now()

    return pool
      .preparedQuery(
        """
        INSERT INTO switches (id, name, type, state, user_id, public_code, created_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *
      """.trimIndent()
      )
      .execute(Tuple.of(id, name, type, state, userId, publicCode, now))
      .map { rows -> Switch.fromRow(rows.first()) }
  }

  fun getAll(): Future<List<Switch>> {
    return pool
      .query("SELECT * FROM switches")
      .execute()
      .map { rows -> rows.map { Switch.fromRow(it) } }
  }

  fun getById(id: UUID): Future<Switch?> {
    return pool
      .preparedQuery("SELECT * FROM switches WHERE id = $1")
      .execute(Tuple.of(id))
      .map { rows ->
        if (rows.size() == 0) null else Switch.fromRow(rows.first())
      }
  }

  fun getByPublicCode(publicCode: UUID): Future<Switch?> {
    return pool
      .preparedQuery("SELECT * FROM switches WHERE public_code = $1")
      .execute(Tuple.of(publicCode))
      .map { rows ->
        if (rows.size() == 0) null else Switch.fromRow(rows.first())
      }
  }

  fun getByUserId(userId: UUID): Future<List<Switch>> {
    return pool
      .preparedQuery("SELECT * FROM switches WHERE user_id = $1")
      .execute(Tuple.of(userId))
      .map { rows -> rows.map { Switch.fromRow(it) } }
  }

  fun update(id: UUID, state: String): Future<Switch?> {
    val toggledAt = LocalDateTime.now()

    return pool
      .preparedQuery(
        """
        UPDATE switches SET state = $1, toggled_at = $2
        WHERE id = $3 RETURNING *
      """.trimIndent()
      )
      .execute(Tuple.of(state, toggledAt, id))
      .map { rows ->
        if (rows.size() == 0) null else Switch.fromRow(rows.first())
      }
  }

  fun delete(id: UUID): Future<Boolean> {
    return pool
      .preparedQuery("DELETE FROM switches WHERE id = $1")
      .execute(Tuple.of(id))
      .map { rows -> rows.rowCount() > 0 }
  }
}
