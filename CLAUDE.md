# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew clean assemble    # Build the project
./gradlew clean test        # Run all tests
./gradlew clean run         # Run the application
./gradlew test --tests "com.example.switcher.TestMainVerticle.testSomething"  # Run single test
```

The build produces a shadow JAR (fat JAR) with all dependencies at `build/libs/switcher-<version>-fat.jar`.

## Architecture

This is a Kotlin/Vert.x 5.0.6 reactive web API server targeting JVM 21.

**Core Pattern**: Single-verticle event-driven architecture using `VerticleBase`. The application uses Vert.x's
Future-based asynchronous composition.

**Main Components**:

- `MainVerticle` - HTTP server with Router-based endpoint handling
- Configuration via HOCON (`application.conf`) with environment variable overrides
- PostgreSQL client configured but not yet implemented in routes

**Key Dependencies**:

- Vert.x Web for HTTP routing
- Vert.x PostgreSQL client with SQL client templates
- Vert.x Kotlin coroutines support for suspend functions
- Logback/SLF4J for logging

## Configuration

Environment variables override `application.conf` defaults. See `.env.example` for available options:

- `HTTP_HOST`/`HTTP_PORT` - Server binding (default: 0.0.0.0:8080)
- `DB_*` - PostgreSQL connection settings
- `JWT_SECRET`/`JWT_EXPIRATION_TIME` - JWT auth config
- `LOG_LEVEL` - Logging verbosity

## Local Development

Start PostgreSQL:

```bash
docker-compose up -d
```

Database: `switchapi` on localhost:5432 (credentials: switchapi/switchapi)

## Code Style

- 2-space indentation
- UTF-8 encoding
- LF line endings
- Trailing newline required
