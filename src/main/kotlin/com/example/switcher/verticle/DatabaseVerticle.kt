package com.example.switcher.verticle

import com.example.switcher.config.AppConfig
import com.example.switcher.repository.SwitchRepository
import com.example.switcher.repository.UserRepository
import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.eventbus.Message
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import java.util.*

class DatabaseVerticle(private val config: AppConfig) : VerticleBase() {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private lateinit var dbPool: Pool
  private lateinit var userRepository: UserRepository
  private lateinit var switchRepository: SwitchRepository

  companion object {
    const val ADDRESS_USER_CREATE = "db.user.create"
    const val ADDRESS_USER_GET_BY_ID = "db.user.getById"
    const val ADDRESS_USER_GET_BY_EMAIL = "db.user.getByEmail"

    const val ADDRESS_SWITCH_CREATE = "db.switch.create"
    const val ADDRESS_SWITCH_GET_BY_ID = "db.switch.getById"
    const val ADDRESS_SWITCH_GET_BY_USER = "db.switch.getByUser"
    const val ADDRESS_SWITCH_UPDATE = "db.switch.update"
    const val ADDRESS_SWITCH_DELETE = "db.switch.delete"
  }

  override fun start(): Future<*> {
    dbPool = createDbPool()
    userRepository = UserRepository(dbPool)
    switchRepository = SwitchRepository(dbPool)

    registerEventBusHandlers()
    logger.info("DatabaseVerticle started")
    return Future.succeededFuture<Void>()
  }

  private fun createDbPool(): Pool {
    val connectOptions = PgConnectOptions()
      .setPort(config.database.port)
      .setHost(config.database.host)
      .setDatabase(config.database.database)
      .setUser(config.database.user)
      .setPassword(config.database.password)
      .setReconnectAttempts(5)
      .setReconnectInterval(1000)

    val poolOptions = PoolOptions()
      .setMaxSize(config.database.maxPoolSize)

    return PgBuilder
      .pool()
      .with(poolOptions)
      .connectingTo(connectOptions)
      .using(vertx)
      .build()
  }

  private fun registerEventBusHandlers() {
    val eventBus = vertx.eventBus()

    eventBus.consumer(ADDRESS_USER_CREATE, ::handleUserCreate)
    eventBus.consumer(ADDRESS_USER_GET_BY_ID, ::handleUserGetById)
    eventBus.consumer(ADDRESS_USER_GET_BY_EMAIL, ::handleUserGetByEmail)

    eventBus.consumer(ADDRESS_SWITCH_CREATE, ::handleSwitchCreate)
    eventBus.consumer(ADDRESS_SWITCH_GET_BY_ID, ::handleSwitchGetById)
    eventBus.consumer(ADDRESS_SWITCH_GET_BY_USER, ::handleSwitchGetByUser)
    eventBus.consumer(ADDRESS_SWITCH_UPDATE, ::handleSwitchUpdate)
    eventBus.consumer(ADDRESS_SWITCH_DELETE, ::handleSwitchDelete)
  }

  // User handlers

  private fun handleUserCreate(message: Message<JsonObject>) {
    val email = message.body().getString("email")
    val password = message.body().getString("password")

    userRepository.create(email, password)
      .onSuccess { user -> message.reply(user.toJson()) }
      .onFailure { err ->
        logger.error("Failed to create user", err)
        message.fail(500, err.message)
      }
  }

  private fun handleUserGetById(message: Message<JsonObject>) {
    val id = UUID.fromString(message.body().getString("id"))

    userRepository.getById(id)
      .onSuccess { user ->
        if (user == null) {
          message.fail(404, "User not found")
        } else {
          message.reply(user.toJson())
        }
      }
      .onFailure { err ->
        logger.error("Failed to get user", err)
        message.fail(500, err.message)
      }
  }

  private fun handleUserGetByEmail(message: Message<JsonObject>) {
    val email = message.body().getString("email")

    userRepository.getByEmail(email)
      .onSuccess { user ->
        if (user == null) {
          message.fail(404, "User not found")
        } else {
          message.reply(user.toJson())
        }
      }
      .onFailure { err ->
        logger.error("Failed to get user", err)
        message.fail(500, err.message)
      }
  }

  // Switch handlers

  private fun handleSwitchCreate(message: Message<JsonObject>) {
    val body = message.body()
    val name = body.getString("name")
    val type = body.getString("type")
    val state = body.getString("state")
    val userId = UUID.fromString(body.getString("userId"))
    val publicCode = UUID.fromString(body.getString("publicCode"))

    switchRepository.create(name, type, state, userId, publicCode)
      .onSuccess { switch -> message.reply(switch.toJson()) }
      .onFailure { err ->
        logger.error("Failed to create switch", err)
        message.fail(500, err.message)
      }
  }

  private fun handleSwitchGetById(message: Message<JsonObject>) {
    val id = UUID.fromString(message.body().getString("id"))

    switchRepository.getById(id)
      .onSuccess { switch ->
        if (switch == null) {
          message.fail(404, "Switch not found")
        } else {
          message.reply(switch.toJson())
        }
      }
      .onFailure { err ->
        logger.error("Failed to get switch", err)
        message.fail(500, err.message)
      }
  }

  private fun handleSwitchGetByUser(message: Message<JsonObject>) {
    val userId = UUID.fromString(message.body().getString("userId"))

    switchRepository.getByUserId(userId)
      .onSuccess { switches ->
        val json = JsonArray(switches.map { it.toJson() })
        message.reply(json)
      }
      .onFailure { err ->
        logger.error("Failed to get switches for user", err)
        message.fail(500, err.message)
      }
  }

  private fun handleSwitchUpdate(message: Message<JsonObject>) {
    val body = message.body()
    val id = UUID.fromString(body.getString("id"))
    val state = body.getString("state")

    switchRepository.update(id, state)
      .onSuccess { switch ->
        if (switch == null) {
          message.fail(404, "Switch not found")
        } else {
          message.reply(switch.toJson())
        }
      }
      .onFailure { err ->
        logger.error("Failed to update switch", err)
        message.fail(500, err.message)
      }
  }

  private fun handleSwitchDelete(message: Message<JsonObject>) {
    val id = UUID.fromString(message.body().getString("id"))

    switchRepository.delete(id)
      .onSuccess { deleted ->
        message.reply(JsonObject().put("deleted", deleted))
      }
      .onFailure { err ->
        logger.error("Failed to delete switch", err)
        message.fail(500, err.message)
      }
  }

  override fun stop(): Future<Void> {
    if (::dbPool.isInitialized) {
      return dbPool.close()
        .onSuccess { logger.info("DatabaseVerticle pool closed") }
    }
    return Future.succeededFuture()
  }
}
