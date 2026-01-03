# TODO: SwitchAPI

## Целевая структура проекта

```
src/main/kotlin/com/example/switcher/
├── MainVerticle.kt              # Точка входа, конфигурация, запуск HTTP сервера
├── config/
│   └── AppConfig.kt             # Data class для типизированной конфигурации
├── router/
│   ├── RouterFactory.kt         # Создание главного Router
│   ├── AuthRouter.kt            # POST /auth/register, POST /auth/login
│   ├── SwitchRouter.kt          # CRUD для переключателей (защищённые)
│   └── PublicRouter.kt          # GET /public/{code} (открытый)
├── handler/
│   ├── AuthHandler.kt           # Логика регистрации/авторизации
│   ├── SwitchHandler.kt         # Логика работы с переключателями
│   └── ErrorHandler.kt          # Глобальная обработка ошибок
├── middleware/
│   └── JwtAuthMiddleware.kt     # Проверка JWT токена
├── service/
│   ├── UserService.kt           # Бизнес-логика пользователей
│   ├── SwitchService.kt         # Бизнес-логика переключателей
│   └── JwtService.kt            # Генерация и валидация JWT
├── repository/
│   ├── UserRepository.kt        # SQL запросы для users
│   └── SwitchRepository.kt      # SQL запросы для switches
├── model/
│   ├── User.kt                  # data class User
│   ├── Switch.kt                # data class Switch
│   └── SwitchType.kt            # enum: SWITCH, BUTTON
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.kt
│   │   ├── LoginRequest.kt
│   │   └── CreateSwitchRequest.kt
│   └── response/
│       ├── AuthResponse.kt
│       ├── SwitchResponse.kt
│       └── PublicSwitchResponse.kt
├── exception/
│   └── ApiException.kt          # Кастомные исключения
└── util/
    ├── PasswordHasher.kt        # Хеширование паролей
    └── ShortCodeGenerator.kt    # Генерация коротких кодов

src/main/resources/
├── application.conf             # HOCON конфигурация
├── db/migration/
│   ├── V1__create_users.sql
│   └── V2__create_switches.sql
└── openapi.yaml                 # OpenAPI спецификация
```

## Этап 1: Инфраструктура

- [x] Настроить структуру пакетов
- [x] Создать AppConfig — типизированная обёртка над HOCON
- [x] Добавить зависимости — vertx-auth-jwt, argon2-jvm
- [x] Написать SQL миграции — таблицы users и switches
- [x] Настроить PostgreSQL клиент — пул соединений в MainVerticle

## Этап 2: Пользователи и авторизация

- [ ] Создать модели — User, dto/RegisterRequest, dto/LoginRequest
- [ ] Реализовать PasswordHasher — хеширование и проверка паролей
- [ ] Реализовать UserRepository — findByEmail, create
- [ ] Реализовать JwtService — generateToken, validateToken
- [ ] Реализовать UserService — register, authenticate
- [ ] Создать AuthHandler — обработка HTTP запросов
- [ ] Создать JwtAuthMiddleware — проверка токена в заголовке Authorization
- [ ] Создать AuthRouter — POST /auth/register, POST /auth/login

## Этап 3: Переключатели

- [ ] Создать модели — Switch, SwitchType, dto/CreateSwitchRequest
- [ ] Реализовать ShortCodeGenerator — генерация уникальных кодов
- [ ] Реализовать SwitchRepository — CRUD операции
- [ ] Реализовать SwitchService — бизнес-логика (проверка типа BUTTON)
- [ ] Создать SwitchHandler — обработка HTTP запросов
- [ ] Создать SwitchRouter:
  - [ ] GET /switches — список своих переключателей
  - [ ] POST /switches — создать
  - [ ] GET /switches/{id} — получить по id
  - [ ] PATCH /switches/{id}/toggle — переключить состояние
  - [ ] POST /switches/{id}/publish — опубликовать
  - [ ] DELETE /switches/{id} — удалить

## Этап 4: Публичный доступ

- [ ] Создать PublicSwitchResponse — DTO без приватных полей
- [ ] Создать PublicRouter — GET /public/{code}

## Этап 5: Обработка ошибок и валидация

- [ ] Создать ApiException — кастомные исключения
- [ ] Создать ErrorHandler — глобальный обработчик ошибок
- [ ] Добавить валидацию входящих данных

## Этап 6: Документация

- [ ] Написать OpenAPI спецификацию — openapi.yaml
- [ ] Подключить Swagger UI (опционально)

## Этап 7: Тестирование

- [ ] Unit тесты — сервисы, утилиты
- [ ] Integration тесты — API endpoints с TestContainers
