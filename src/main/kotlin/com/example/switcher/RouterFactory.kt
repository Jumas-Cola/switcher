package com.example.switcher

import com.example.switcher.config.JwtConfig
import com.example.switcher.handler.AuthHandler
import com.example.switcher.handler.HealthHandler
import com.example.switcher.handler.SwitchHandler
import com.example.switcher.handler.UserHandler
import com.example.switcher.service.JwtService
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.openapi.contract.OpenAPIContract

class RouterFactory(private val vertx: Vertx, jwtConfig: JwtConfig) {

  private val eventBus = vertx.eventBus()
  private val jwtService = JwtService(vertx, jwtConfig)
  private val healthHandler = HealthHandler()
  private val authHandler = AuthHandler(eventBus, jwtService)
  private val userHandler = UserHandler(eventBus)
  private val switchHandler = SwitchHandler(eventBus)

  fun create(): Future<Router> {
    return OpenAPIContract.from(vertx, "openapi.yaml")
      .compose { contract ->
        val routerBuilder = RouterBuilder.create(vertx, contract)

        // Health
        routerBuilder.getRoute("healthCheck")?.addHandler(healthHandler::healthCheck)

        // Auth
        routerBuilder.getRoute("register")?.addHandler(authHandler::register)
        routerBuilder.getRoute("login")?.addHandler(authHandler::login)

        // Users
        routerBuilder.getRoute("getAllUsers")?.addHandler(userHandler::getAll)
        routerBuilder.getRoute("getUserById")?.addHandler(userHandler::getById)
        routerBuilder.getRoute("createUser")?.addHandler(userHandler::create)

        // Switches
        routerBuilder.getRoute("getAllSwitches")?.addHandler(switchHandler::getAll)
        routerBuilder.getRoute("getSwitchById")?.addHandler(switchHandler::getById)
        routerBuilder.getRoute("getSwitchesByUser")?.addHandler(switchHandler::getByUser)
        routerBuilder.getRoute("createSwitch")?.addHandler(switchHandler::create)
        routerBuilder.getRoute("updateSwitch")?.addHandler(switchHandler::update)
        routerBuilder.getRoute("deleteSwitch")?.addHandler(switchHandler::delete)

        val router = routerBuilder.createRouter()

        // Swagger UI - serve from classpath
        router.route("/swagger-ui/*").handler(
          StaticHandler.create("webroot/swagger-ui").setCachingEnabled(false)
        )
        // Serve OpenAPI spec
        router.get("/openapi.yaml").handler { ctx ->
          ctx.vertx().fileSystem().readFile("openapi.yaml")
            .onSuccess { buffer ->
              ctx.response()
                .putHeader("content-type", "text/yaml")
                .putHeader("access-control-allow-origin", "*")
                .end(buffer)
            }
            .onFailure { ctx.fail(404) }
        }

        Future.succeededFuture(router)
      }
  }
}
