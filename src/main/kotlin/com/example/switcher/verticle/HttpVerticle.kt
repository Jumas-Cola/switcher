package com.example.switcher.verticle

import com.example.switcher.RouterFactory
import com.example.switcher.config.AppConfig
import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.http.HttpServer
import io.vertx.core.internal.logging.LoggerFactory

class HttpVerticle(private val config: AppConfig) : VerticleBase() {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private lateinit var httpServer: HttpServer

  override fun start(): Future<*> {
    return RouterFactory(vertx, config.jwt).create()
      .compose { router ->
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

  override fun stop(): Future<Void> {
    if (::httpServer.isInitialized) {
      return httpServer.close()
        .onSuccess { logger.info("HttpVerticle stopped") }
    }
    return Future.succeededFuture()
  }
}
