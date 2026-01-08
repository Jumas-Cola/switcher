package com.example.switcher

import com.example.switcher.config.JwtConfig
import com.example.switcher.handler.AuthHandler
import com.example.switcher.handler.HealthHandler
import com.example.switcher.handler.PublicSwitchHandler
import com.example.switcher.handler.SwitchHandler
import com.example.switcher.middleware.CheckSwitchOwnerMiddleware
import com.example.switcher.middleware.JwtAuthMiddleware
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
  private val jwtAuthMiddleware = JwtAuthMiddleware(jwtService, eventBus)
  private val checkSwitchOwnerMiddleware = CheckSwitchOwnerMiddleware(eventBus)
  private val healthHandler = HealthHandler()
  private val authHandler = AuthHandler(eventBus, jwtService)
  private val switchHandler = SwitchHandler(eventBus)
  private val publicSwitchHandler = PublicSwitchHandler(eventBus)

  fun create(): Future<Router> {
    return OpenAPIContract.from(vertx, "openapi.yaml")
      .compose { contract ->
        val routerBuilder = RouterBuilder.create(vertx, contract)

        // Health
        routerBuilder.getRoute("healthCheck")?.addHandler(healthHandler::healthCheck)

        // Auth (public routes)
        routerBuilder.getRoute("register")?.addHandler(authHandler::register)
        routerBuilder.getRoute("login")?.addHandler(authHandler::login)

        // Switches (protected routes)
        routerBuilder.getRoute("getSwitchById") // Получение switch по ID
          ?.addHandler(jwtAuthMiddleware)
          ?.addHandler(switchHandler::getById)
        routerBuilder.getRoute("getSwitchesByUser") // Получение всех switch текущего пользователя
          ?.addHandler(jwtAuthMiddleware)
          ?.addHandler(switchHandler::getByUser)
        routerBuilder.getRoute("createSwitch") // Создание switch
          ?.addHandler(jwtAuthMiddleware)
          ?.addHandler(switchHandler::create)
        routerBuilder.getRoute("toggleSwitch") // Публикация switch
          ?.addHandler(jwtAuthMiddleware)
          ?.addHandler(checkSwitchOwnerMiddleware)
          ?.addHandler(switchHandler::toggle)
        routerBuilder.getRoute("deleteSwitch")
          ?.addHandler(jwtAuthMiddleware)
          ?.addHandler(checkSwitchOwnerMiddleware)
          ?.addHandler(switchHandler::delete)

        // Public routes
        routerBuilder.getRoute("getPublicSwitch")
          ?.addHandler(publicSwitchHandler::getByPublicCode)

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
