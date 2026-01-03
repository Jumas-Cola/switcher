# TODO: SwitchAPI

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                      MainVerticle                           │
│              (загрузка конфига, деплой verticles)           │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────┐
│      HttpVerticle       │     │     DatabaseVerticle        │
│  (HTTP server, routing) │     │  (EventBus handlers, repos) │
│                         │     │                             │
│  RouterFactory          │     │  UserRepository             │
│    ├─ HealthHandler     │     │  SwitchRepository           │
│    ├─ UserHandler ──────┼──EventBus──▶ handlers            │
│    └─ SwitchHandler ────┼──EventBus──▶ handlers            │
└─────────────────────────┘     └─────────────────────────────┘
```

## Структура проекта

```
src/main/kotlin/com/example/switcher/
├── MainVerticle.kt              # Загрузка конфига, деплой verticles
├── RouterFactory.kt             # Регистрация маршрутов, создание handlers
├── config/
│   └── AppConfig.kt             # Data class для типизированной конфигурации
├── verticle/
│   ├── HttpVerticle.kt          # HTTP сервер
│   └── DatabaseVerticle.kt      # EventBus handlers, репозитории
├── handler/
│   ├── HealthHandler.kt         # GET /api/health
│   ├── UserHandler.kt           # CRUD users (EventBus → DatabaseVerticle)
│   ├── SwitchHandler.kt         # CRUD switches (EventBus → DatabaseVerticle)
│   ├── AuthHandler.kt           # (TODO) Регистрация/авторизация
│   └── ErrorHandler.kt          # (TODO) Глобальная обработка ошибок
├── repository/
│   ├── UserRepository.kt        # SQL запросы для users
│   └── SwitchRepository.kt      # SQL запросы для switches
├── model/
│   ├── User.kt                  # data class User (toJson, fromRow)
│   ├── Switch.kt                # data class Switch (toJson, fromRow)
│   └── SwitchType.kt            # (TODO) enum: SWITCH, BUTTON
├── middleware/
│   └── JwtAuthMiddleware.kt     # (TODO) Проверка JWT токена
├── service/
│   └── JwtService.kt            # (TODO) Генерация и валидация JWT
├── dto/
│   ├── request/                 # (TODO) DTO для входящих данных
│   └── response/                # (TODO) DTO для ответов
├── exception/
│   └── ApiException.kt          # (TODO) Кастомные исключения
└── util/
    ├── PasswordHasher.kt        # (TODO) Хеширование паролей
    └── ShortCodeGenerator.kt    # (TODO) Генерация коротких кодов

src/main/resources/
├── application.conf             # HOCON конфигурация
└── db/migration/
    ├── V1__create_users.sql
    └── V2__create_switches.sql
```

## Этап 1: Инфраструктура ✅

- [x] Настроить структуру пакетов
- [x] Создать AppConfig — типизированная обёртка над HOCON
- [x] Настроить PostgreSQL клиент — пул соединений
- [x] Реализовать Verticle архитектуру:
  - [x] MainVerticle — загрузка конфига, деплой других verticles
  - [x] HttpVerticle — HTTP сервер
  - [x] DatabaseVerticle — EventBus handlers + репозитории
- [x] Создать RouterFactory — регистрация маршрутов
- [x] Создать Handlers (EventBus клиенты):
  - [x] HealthHandler — GET /api/health
  - [x] UserHandler — CRUD users
  - [x] SwitchHandler — CRUD switches
- [x] Создать Repositories (SQL):
  - [x] UserRepository — create, getAll, getById, delete
  - [x] SwitchRepository — create, getAll, getById, getByUserId, update, delete
- [x] Создать модели:
  - [x] User — data class (toJson, fromRow)
  - [x] Switch — data class (toJson, fromRow)

## Этап 2: Пользователи и авторизация

- [x] Добавить зависимости — vertx-auth-jwt, argon2-jvm
- [ ] Создать dto/RegisterRequest, dto/LoginRequest
- [ ] Реализовать PasswordHasher — хеширование и проверка паролей
- [ ] Расширить UserRepository — findByEmail
- [ ] Реализовать JwtService — generateToken, validateToken
- [ ] Создать AuthHandler — обработка регистрации/логина через EventBus
- [ ] Создать JwtAuthMiddleware — проверка токена в заголовке Authorization
- [ ] Добавить маршруты в RouterFactory — POST /auth/register, POST /auth/login

## Этап 3: Расширение переключателей

- [ ] Создать SwitchType enum — SWITCH, BUTTON
- [ ] Создать dto/CreateSwitchRequest
- [ ] Реализовать ShortCodeGenerator — генерация уникальных кодов
- [ ] Расширить SwitchRepository — findByCode
- [ ] Расширить SwitchHandler:
  - [ ] PATCH /api/switches/{id}/toggle — переключить состояние
  - [ ] POST /api/switches/{id}/publish — опубликовать

## Этап 4: Публичный доступ

- [ ] Создать PublicHandler — GET /public/{code}
- [ ] Добавить маршрут в RouterFactory

## Этап 5: Обработка ошибок и валидация

- [ ] Создать ApiException — кастомные исключения
- [ ] Создать ErrorHandler — глобальный обработчик ошибок
- [ ] Добавить валидацию входящих данных

## Этап 6: Документация

- [ ] Написать OpenAPI спецификацию — openapi.yaml
- [ ] Подключить Swagger UI (опционально)

## Этап 7: Тестирование

- [ ] Unit тесты — repositories, handlers
- [ ] Integration тесты — API endpoints с TestContainers

## API Endpoints

| Method | Endpoint                     | Handler       | Description          |
|--------|------------------------------|---------------|----------------------|
| GET    | `/api/health`                | HealthHandler | Health check         |
| GET    | `/api/users`                 | UserHandler   | List users           |
| GET    | `/api/users/:id`             | UserHandler   | Get user by ID       |
| POST   | `/api/users`                 | UserHandler   | Create user          |
| GET    | `/api/switches`              | SwitchHandler | List switches        |
| GET    | `/api/switches/:id`          | SwitchHandler | Get switch by ID     |
| GET    | `/api/users/:userId/switches`| SwitchHandler | Get user's switches  |
| POST   | `/api/switches`              | SwitchHandler | Create switch        |
| PUT    | `/api/switches/:id`          | SwitchHandler | Update switch        |
| DELETE | `/api/switches/:id`          | SwitchHandler | Delete switch        |

## EventBus Addresses

| Address              | Description              |
|----------------------|--------------------------|
| `db.user.create`     | Create user              |
| `db.user.getAll`     | Get all users            |
| `db.user.getById`    | Get user by ID           |
| `db.switch.create`   | Create switch            |
| `db.switch.getAll`   | Get all switches         |
| `db.switch.getById`  | Get switch by ID         |
| `db.switch.getByUser`| Get switches by user ID  |
| `db.switch.update`   | Update switch            |
| `db.switch.delete`   | Delete switch            |

## Поток данных

```
HTTP Request
    ↓
RouterFactory → Handler.method(ctx)
    ↓
EventBus.request(ADDRESS, JsonObject)
    ↓
DatabaseVerticle.handler(Message)
    ↓
Repository.method() → SQL
    ↓
Model.fromRow() → Model.toJson()
    ↓
EventBus reply (JsonObject/JsonArray)
    ↓
Handler → ctx.response().end(json)
    ↓
HTTP Response
```
