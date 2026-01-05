package com.example.switcher

import com.example.switcher.config.AppConfig
import com.example.switcher.config.ConfigLoader
import com.example.switcher.verticle.DatabaseVerticle
import com.example.switcher.verticle.HttpVerticle
import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.internal.logging.LoggerFactory

class MainVerticle(
  private val configPath: String = "application.conf"
) : VerticleBase() {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private var databaseVerticleId: String? = null
  private var httpVerticleId: String? = null

  override fun start(): Future<*> {
    return loadConfig().compose { config ->
      deployDatabaseVerticle(config).compose {
        deployHttpVerticle(config)
      }
    }
  }

  private fun loadConfig(): Future<AppConfig> {
    return ConfigLoader.load(vertx, configPath)
  }

  private fun deployDatabaseVerticle(config: AppConfig): Future<String> {
    return vertx
      .deployVerticle(DatabaseVerticle(config))
      .onSuccess { id ->
        databaseVerticleId = id
        logger.info("DatabaseVerticle deployed: $id")
      }
      .onFailure { error ->
        logger.error("Failed to deploy DatabaseVerticle", error)
      }
  }

  private fun deployHttpVerticle(config: AppConfig): Future<String> {
    return vertx
      .deployVerticle(HttpVerticle(config))
      .onSuccess { id ->
        httpVerticleId = id
        logger.info("HttpVerticle deployed: $id")
      }
      .onFailure { error ->
        logger.error("Failed to deploy HttpVerticle", error)
      }
  }

  override fun stop(): Future<Void> {
    val futures = mutableListOf<Future<Void>>()

    httpVerticleId?.let { futures.add(vertx.undeploy(it)) }
    databaseVerticleId?.let { futures.add(vertx.undeploy(it)) }

    if (futures.isEmpty()) {
      return Future.succeededFuture()
    }

    return Future.join(futures)
      .onSuccess { logger.info("All verticles undeployed") }
      .mapEmpty()
  }
}
