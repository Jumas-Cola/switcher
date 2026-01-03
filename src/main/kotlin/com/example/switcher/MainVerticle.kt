package com.example.switcher

import com.example.switcher.config.AppConfig
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.http.HttpServer
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions

class MainVerticle : VerticleBase() {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private lateinit var httpServer: HttpServer
  private lateinit var config: AppConfig
  private lateinit var pgPool: Pool

  override fun start(): Future<*> {
    return loadConfig().compose { appConfig ->
      config = appConfig

      pgPool = createPgPool(config)

      val router = createRouter()

      vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(config.http.port, config.http.host)
        .onSuccess { server ->
          httpServer = server
          logger.info("HTTP server started on ${config.http.host}:${config.http.port}")
        }
        .onFailure { error ->
          logger.error("Failed to start HTTP server", error)
        }
    }
  }

  private fun loadConfig(): Future<AppConfig> {
    val hoconStore = ConfigStoreOptions()
      .setType("file")
      .setFormat("hocon")
      .setConfig(JsonObject().put("path", "application.conf"))

    val options = ConfigRetrieverOptions().addStore(hoconStore)
    val retriever = ConfigRetriever.create(vertx, options)

    return retriever.config.map { json -> AppConfig.fromJson(json) }
  }

  private fun createRouter(): Router {
    val router = Router.router(vertx)

    router.get("/health").handler { ctx ->
      logger.debug(ctx.normalizedPath())

      ctx.response()
        .putHeader("content-type", "application/json")
        .end(JsonObject().put("status", "ok").encode())
    }

    router.get("/api/test").handler { ctx ->
      logger.debug(ctx.normalizedPath())

      pgPool
        .query("SELECT id, email FROM users")
        .execute()
        .onSuccess { rows ->
          val users = rows.map { row ->
            JsonObject()
              .put("id", row.getUUID("id"))
              .put("email", row.getString("email"))
          }
          ctx.response()
            .putHeader("content-type", "application/json")
            .end(JsonArray(users).encode())
        }
        .onFailure { err ->
          logger.error("DB query failed", err)
          ctx.response().setStatusCode(500).end()
        }
    }

    return router
  }

  private fun createPgPool(config: AppConfig): Pool {
    val connectOptions = PgConnectOptions()
      .setPort(config.database.port)
      .setHost(config.database.host)
      .setDatabase(config.database.database)
      .setUser(config.database.user)
      .setPassword(config.database.password)

    val poolOptions = PoolOptions()
      .setMaxSize(config.database.maxPoolSize)

    return PgBuilder
      .pool()
      .with(poolOptions)
      .connectingTo(connectOptions)
      .using(vertx)
      .build()
  }

  override fun stop(): Future<Void> {
    val futures = mutableListOf<Future<Void>>()

    if (::pgPool.isInitialized) {
      futures.add(pgPool.close())
    }

    if (::httpServer.isInitialized) {
      futures.add(httpServer.close())
    }

    if (futures.isEmpty()) {
      return Future.succeededFuture()
    }

    return Future.join(futures)
      .onSuccess { logger.info("All resources closed") }
      .mapEmpty()
  }
}
