package com.example.switcher.repository

import com.example.switcher.model.User
import io.vertx.core.Future
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import java.time.LocalDateTime
import java.util.UUID

class UserRepository(private val pool: Pool) {

  fun create(email: String, password: String): Future<User> {
    val id = UUID.randomUUID()
    val createdAt = LocalDateTime.now()

    return pool
      .preparedQuery("INSERT INTO users (id, email, password, created_at) VALUES ($1, $2, $3, $4) RETURNING *")
      .execute(Tuple.of(id, email, password,createdAt))
      .map { rows -> User.fromRow(rows.first()) }
  }

  fun getAll(): Future<List<User>> {
    return pool
      .query("SELECT id, email, created_at FROM users")
      .execute()
      .map { rows -> rows.map { User.fromRow(it) } }
  }

  fun getById(id: UUID): Future<User?> {
    return pool
      .preparedQuery("SELECT id, email, created_at FROM users WHERE id = $1")
      .execute(Tuple.of(id))
      .map { rows ->
        if (rows.size() == 0) null else User.fromRow(rows.first())
      }
  }

  fun delete(id: UUID): Future<Boolean> {
    return pool
      .preparedQuery("DELETE FROM users WHERE id = $1")
      .execute(Tuple.of(id))
      .map { rows -> rows.rowCount() > 0 }
  }
}
