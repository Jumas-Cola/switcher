package com.example.switcher

import com.example.switcher.config.AppConfig
import com.example.switcher.verticle.DatabaseVerticle
import com.example.switcher.verticle.HttpVerticle
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject

class MainVerticle : VerticleBase() {

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
    val hoconStore = ConfigStoreOptions()
      .setType("file")
      .setFormat("hocon")
      .setConfig(JsonObject().put("path", "application.conf"))

    val options = ConfigRetrieverOptions().addStore(hoconStore)
    val retriever = ConfigRetriever.create(vertx, options)

    return retriever.config.map { json -> AppConfig.fromJson(json) }
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
