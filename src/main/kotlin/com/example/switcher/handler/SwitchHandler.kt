package com.example.switcher.handler

import com.example.switcher.dto.request.switches.CreateSwitchDto
import com.example.switcher.dto.response.switches.SwitchResponse
import com.example.switcher.model.enums.SwitchState
import com.example.switcher.model.enums.SwitchType
import com.example.switcher.verticle.DatabaseVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.openapi.validation.ValidatedRequest
import java.util.*

class SwitchHandler(private val eventBus: EventBus) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun getById(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_ID, request)
      .onSuccess { reply ->
        val body = reply.body()
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(
            SwitchResponse(
              id = body.getString("id"),
              name = body.getString("name"),
              type = body.getString("type"),
              state = body.getString("state"),
              publicCode = body.getString("public_code"),
              userId = body.getString("user_id"),
              toggledAt = body.getString("toggled_at"),
              createdAt = body.getString("created_at"),
            ).toResponse()
          )
      }
      .onFailure { err ->
        if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to get switch", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }

  fun getByUser(ctx: RoutingContext) {
    val userId = ctx.get<String>("userId")
    val request = JsonObject().put("userId", userId)

    eventBus.request<Any>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_USER, request)
      .onSuccess { reply ->
        val switches = reply.body() as io.vertx.core.json.JsonArray
        val responses = switches.map { switchObj ->
          val obj = switchObj as JsonObject
          SwitchResponse(
            id = obj.getString("id"),
            name = obj.getString("name"),
            type = obj.getString("type"),
            state = obj.getString("state"),
            publicCode = obj.getString("public_code"),
            userId = obj.getString("user_id"),
            toggledAt = obj.getString("toggled_at"),
            createdAt = obj.getString("created_at"),
          )
        }
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(io.vertx.core.json.JsonArray(responses.map { JsonObject.mapFrom(it) }).encode())
      }
      .onFailure { err ->
        logger.error("Failed to get switches for user", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun create(ctx: RoutingContext) {
    val validatedRequest = ctx.get<ValidatedRequest>("openApiValidatedRequest")
    val json = validatedRequest.body.jsonObject

    val dto = CreateSwitchDto(
      name = json.getString("name"),
      type = SwitchType.valueOf(json.getString("type")),
      state = SwitchState.OFF,
      userId = ctx.get("userId"),
      publicCode = UUID.randomUUID(),
    )

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_CREATE, dto.toJsonObject())
      .onSuccess { reply ->
        val body = reply.body()
        ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(
            SwitchResponse(
              id = body.getString("id"),
              name = body.getString("name"),
              type = body.getString("type"),
              state = body.getString("state"),
              publicCode = body.getString("public_code"),
              userId = body.getString("user_id"),
              toggledAt = body.getString("toggled_at"),
              createdAt = body.getString("created_at"),
            ).toResponse()
          )
      }
      .onFailure { err ->
        logger.error("Failed to create switch", err)
        ctx.response().setStatusCode(500).end()
      }
  }

  fun toggle(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_GET_BY_ID, request)
      .onSuccess { reply ->
        val type = SwitchType.valueOf(reply.body().getString("type"))
        val state = SwitchState.valueOf(reply.body().getString("state"))

        if (state == SwitchState.OFF || type == SwitchType.SWITCH) {
          val json = JsonObject()
            .put("id", id)
            .put("state", if (state == SwitchState.OFF) SwitchState.ON else SwitchState.OFF)

          eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_UPDATE, json)
            .onSuccess { reply ->
              val body = reply.body()
              ctx.response()
                .putHeader("content-type", "application/json")
                .end(
                  SwitchResponse(
                    id = body.getString("id"),
                    name = body.getString("name"),
                    type = body.getString("type"),
                    state = body.getString("state"),
                    publicCode = body.getString("public_code"),
                    userId = body.getString("user_id"),
                    toggledAt = body.getString("toggled_at"),
                    createdAt = body.getString("created_at"),
                  ).toResponse()
                )
            }
            .onFailure { err ->
              if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
                ctx.response().setStatusCode(404).end()
              } else {
                logger.error("Failed to toggle switch state", err)
                ctx.response().setStatusCode(500).end()
              }
            }
        }
      }
      .onFailure { err ->
        if (err is io.vertx.core.eventbus.ReplyException && err.failureCode() == 404) {
          ctx.response().setStatusCode(404).end()
        } else {
          logger.error("Failed to get switch", err)
          ctx.response().setStatusCode(500).end()
        }
      }
  }

  fun delete(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    val request = JsonObject().put("id", id)

    eventBus.request<JsonObject>(DatabaseVerticle.ADDRESS_SWITCH_DELETE, request)
      .onSuccess { reply ->
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(reply.body().encode())
      }
      .onFailure { err ->
        logger.error("Failed to delete switch", err)
        ctx.response().setStatusCode(500).end()
      }
  }
}
