package com.example.switcher.handler

import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class HealthHandler {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun healthCheck(ctx: RoutingContext) {
    logger.debug(ctx.normalizedPath())

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(JsonObject()
        .put("status", "ok")
        .encode())
  }
}
