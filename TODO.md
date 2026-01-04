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
│    ├─ AuthHandler ──────┼──EventBus──▶ handlers            │
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
│   ├── AuthHandler.kt           # POST /api/register (EventBus → DatabaseVerticle)
│   ├── UserHandler.kt           # CRUD users (EventBus → DatabaseVerticle)
│   ├── SwitchHandler.kt         # CRUD switches (EventBus → DatabaseVerticle)
│   └── ErrorHandler.kt          # (TODO) Глобальная обработка ошибок
├── repository/
│   ├── UserRepository.kt        # SQL запросы для users
│   └── SwitchRepository.kt      # SQL запросы для switches
├── model/
│   ├── User.kt                  # data class User (toJson, fromRow)
│   ├── Switch.kt                # data class Switch (toJson, fromRow)
│   └── SwitchType.kt            # (TODO) enum: SWITCH, BUTTON
├── dto/
│   └── request/
│       └── RegisterRequest.kt   # DTO для регистрации
├── middleware/
│   └── JwtAuthMiddleware.kt     # (TODO) Проверка JWT токена
├── service/
│   └── JwtService.kt            # (TODO) Генерация и валидация JWT
├── exception/
│   └── ApiException.kt          # (TODO) Кастомные исключения
└── util/
    ├── PasswordHasher.kt        # (TODO) Хеширование паролей
    └── ShortCodeGenerator.kt    # (TODO) Генерация коротких кодов

src/main/resources/
├── application.conf             # HOCON конфигурация
├── openapi.yaml                 # OpenAPI 3.1 спецификация
├── webroot/
│   └── swagger-ui/
│       └── index.html           # Swagger UI (CDN)
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
- [x] Создать dto/RegisterRequest — валидация email
- [x] Создать AuthHandler — обработка регистрации через EventBus
- [x] Добавить маршрут POST /api/register в RouterFactory
- [x] Создать dto/LoginRequest — валидация входных данных
- [x] Реализовать PasswordHasher — хеширование Argon2 и проверка паролей
- [x] Интегрировать PasswordHasher в AuthHandler (хеширование при регистрации)
- [x] Расширить UserRepository — findByEmail
- [ ] Реализовать JwtService — generateToken, getUserFromToken
- [ ] Создать JwtAuthMiddleware — проверка токена в заголовке Authorization
- [x] Добавить маршрут POST /api/login в RouterFactory
- [ ] Защитить приватные маршруты через JwtAuthMiddleware

## Этап 3: Расширение переключателей

- [ ] Создать SwitchType enum — SWITCH, BUTTON
- [ ] Создать dto/CreateSwitchRequest — валидация входных данных
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

## Этап 6: Документация ✅

- [x] Написать OpenAPI спецификацию — openapi.yaml (contract-first)
- [x] Подключить Swagger UI — http://localhost:8080/swagger-ui/index.html
- [x] Интегрировать vertx-web-openapi-router для валидации запросов

## Этап 7: Тестирование

- [ ] Unit тесты — repositories, handlers
- [ ] Integration тесты — API endpoints с TestContainers

## API Endpoints

| Method | Endpoint                     | Handler       | Auth | Description          |
|--------|------------------------------|---------------|------|----------------------|
| GET    | `/api/health`                | HealthHandler | -    | Health check         |
| POST   | `/api/register`              | AuthHandler   | -    | Регистрация          |
| POST   | `/api/login`                 | AuthHandler   | -    | (TODO) Авторизация   |
| GET    | `/api/users`                 | UserHandler   | JWT  | List users           |
| GET    | `/api/users/:id`             | UserHandler   | JWT  | Get user by ID       |
| POST   | `/api/users`                 | UserHandler   | JWT  | Create user          |
| GET    | `/api/switches`              | SwitchHandler | JWT  | List switches        |
| GET    | `/api/switches/:id`          | SwitchHandler | JWT  | Get switch by ID     |
| GET    | `/api/users/:userId/switches`| SwitchHandler | JWT  | Get user's switches  |
| POST   | `/api/switches`              | SwitchHandler | JWT  | Create switch        |
| PUT    | `/api/switches/:id`          | SwitchHandler | JWT  | Update switch        |
| DELETE | `/api/switches/:id`          | SwitchHandler | JWT  | Delete switch        |
| GET    | `/public/:code`              | PublicHandler | -    | (TODO) Public access |

## EventBus Addresses

| Address              | Description              |
|----------------------|--------------------------|
| `db.user.create`     | Create user              |
| `db.user.getAll`     | Get all users            |
| `db.user.getById`    | Get user by ID           |
| `db.user.findByEmail`| (TODO) Find user by email|
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
