package com.example.switcher

import com.example.switcher.handler.AuthHandler
import com.example.switcher.handler.HealthHandler
import com.example.switcher.handler.SwitchHandler
import com.example.switcher.handler.UserHandler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

class RouterFactory(private val vertx: Vertx) {

  private val eventBus = vertx.eventBus()
  private val healthHandler = HealthHandler()
  private val authHandler = AuthHandler(eventBus)
  private val userHandler = UserHandler(eventBus)
  private val switchHandler = SwitchHandler(eventBus)

  fun create(): Router {
    val router = Router.router(vertx)

    router.get("/api/health").handler(healthHandler::healthCheck)
    setupAuthRoutes(router)
    setupUserRoutes(router)
    setupSwitchRoutes(router)

    return router
  }

  private fun setupAuthRoutes(router: Router) {
    router.post("/api/register").handler(authHandler::register)
  }

  private fun setupUserRoutes(router: Router) {
    router.get("/api/users").handler(userHandler::getAll)
    router.get("/api/users/:id").handler(userHandler::getById)
    router.post("/api/users").handler(userHandler::create)
  }

  private fun setupSwitchRoutes(router: Router) {
    router.get("/api/switches").handler(switchHandler::getAll)
    router.get("/api/switches/:id").handler(switchHandler::getById)
    router.get("/api/users/:userId/switches").handler(switchHandler::getByUser)
    router.post("/api/switches").handler(switchHandler::create)
    router.put("/api/switches/:id").handler(switchHandler::update)
    router.delete("/api/switches/:id").handler(switchHandler::delete)
  }
}
