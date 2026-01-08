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
│    ├─ SwitchHandler ────┼──EventBus──▶ handlers            │
│    └─ PublicSwitchHandler                                   │
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
│   ├── AuthHandler.kt           # POST /api/register, POST /api/login
│   ├── SwitchHandler.kt         # CRUD switches (EventBus → DatabaseVerticle)
│   └── PublicSwitchHandler.kt   # GET /public/:code
├── repository/
│   ├── UserRepository.kt        # SQL запросы для users
│   └── SwitchRepository.kt      # SQL запросы для switches
├── model/
│   ├── User.kt                  # data class User
│   └── Switch.kt                # data class Switch
├── dto/
│   ├── request/                 # DTO для входящих запросов
│   └── response/                # DTO для ответов
├── middleware/
│   ├── JwtAuthMiddleware.kt     # Проверка JWT токена
│   └── CheckSwitchOwnerMiddleware.kt # Проверка владельца switch
├── service/
│   └── JwtService.kt            # Генерация и валидация JWT
└── util/
    └── PasswordHasher.kt        # Хеширование паролей (Argon2)

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
- [x] Создать Handlers:
  - [x] HealthHandler — GET /api/health
  - [x] AuthHandler — регистрация и авторизация
  - [x] SwitchHandler — CRUD switches (EventBus)
  - [x] PublicSwitchHandler — публичный доступ
- [x] Создать Repositories (SQL):
  - [x] UserRepository — create, findByEmail
  - [x] SwitchRepository — create, getById, getByUserId, getByCode, toggle, delete
- [x] Создать модели:
  - [x] User — data class (toJson, fromRow)
  - [x] Switch — data class (toJson, fromRow)

## Этап 2: Авторизация и JWT ✅

- [x] Добавить зависимости — vertx-auth-jwt, argon2-jvm
- [x] Реализовать PasswordHasher — хеширование Argon2
- [x] Создать JwtService — генерация и валидация токенов
- [x] Создать JwtAuthMiddleware — проверка токена
- [x] Реализовать AuthHandler — регистрация и логин
- [x] Расширить UserRepository — findByEmail
- [x] Защитить приватные маршруты

## Этап 3: Switches и публичный доступ ✅

- [x] Реализовать SwitchHandler — CRUD операции
- [x] Реализовать SwitchRepository — findByCode, toggle
- [x] Добавить CheckSwitchOwnerMiddleware
- [x] Создать PublicSwitchHandler — GET /public/:code

## Этап 4: Документация ✅

- [x] Написать OpenAPI спецификацию — openapi.yaml (contract-first)
- [x] Подключить Swagger UI — http://localhost:8080/swagger-ui/index.html
- [x] Интегрировать vertx-web-openapi-router для валидации запросов

## Этап 5: Обработка ошибок

- [ ] Создать ApiException — кастомные исключения
- [ ] Создать ErrorHandler — глобальный обработчик ошибок
- [ ] Улучшить валидацию входящих данных

## Этап 6: Тестирование

- [ ] Unit тесты — repositories, handlers
- [ ] Integration тесты — API endpoints с TestContainers

## API Endpoints

| Method | Endpoint                      | Handler              | Auth | Description              |
|--------|-------------------------------|----------------------|------|--------------------------|
| GET    | `/api/health`                 | HealthHandler        | -    | Health check             |
| POST   | `/api/register`               | AuthHandler          | -    | Регистрация пользователя |
| POST   | `/api/login`                  | AuthHandler          | -    | Авторизация              |
| GET    | `/api/switches`               | SwitchHandler        | JWT  | Список switches юзера    |
| GET    | `/api/switches/:id`           | SwitchHandler        | JWT  | Получить switch по ID    |
| POST   | `/api/switches`               | SwitchHandler        | JWT  | Создать switch           |
| PATCH  | `/api/switches/:id/toggle`    | SwitchHandler        | JWT  | Переключить состояние    |
| DELETE | `/api/switches/:id`           | SwitchHandler        | JWT  | Удалить switch           |
| GET    | `/public/:code`               | PublicSwitchHandler  | -    | Публичный доступ         |

## EventBus Addresses

| Address               | Description               |
|-----------------------|---------------------------|
| `db.user.create`      | Создать пользователя      |
| `db.user.findByEmail` | Найти user по email       |
| `db.switch.create`    | Создать switch            |
| `db.switch.getById`   | Получить switch по ID     |
| `db.switch.getByUser` | Получить switches юзера   |
| `db.switch.getByCode` | Получить switch по коду   |
| `db.switch.toggle`    | Переключить состояние     |
| `db.switch.delete`    | Удалить switch            |

## Поток данных

```
HTTP Request
    ↓
OpenAPI Router → Handler (protected by JWT/Owner middleware)
    ↓
EventBus.request(address, JsonObject)
    ↓
DatabaseVerticle → Repository → PostgreSQL
    ↓
EventBus reply (JsonObject)
    ↓
Handler → HTTP Response (JSON)
```
