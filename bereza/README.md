# Бёreza — мессенджер внутреннего туризма (РФ)

Цифровой ассистент туриста, гида, отеля и туроператора:
- личные и групповые чаты (WebSocket/STOMP, online-обновления, прочтения, печатает…);
- обмен файлами (фото, PDF, ваучеры, страховки, билеты);
- геолокация — отправка координат, точки сбора, метки достопримечательностей;
- поиск и **бронирование отелей** с автоматически создающимся чатом «турист ↔ отель»;
- уведомления (в приложении и Web Push API).

Соответствует требованиям ФЗ-152 (хранение ПДн на территории РФ — Postgres + локальное файловое хранилище), session-based аутентификация без JWT и без хранения токенов в браузере.

## Стек
- **Backend:** Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Session JDBC, WebSocket+STOMP, PostgreSQL 16, Flyway, Apache Tika, BCrypt.
- **Frontend:** React 18 + Vite, React Router, Axios, @stomp/stompjs + sockjs-client.
- **Инфра:** Docker, docker-compose, Nginx (reverse-proxy + SSL).
- **Архитектура:** монолит с чистой слойностью (`web → service → repository → domain`), легко выносится в микросервисы.

## Структура репозитория

```
bereza/
├── pom.xml
├── Dockerfile                 # backend
├── docker-compose.yml         # PG + backend + frontend + nginx
├── docker-compose.dev.yml     # только PG (для локальной dev-разработки)
├── nginx/nginx.conf           # reverse-proxy + SSL шаблон
├── src/main/java/messenger/bereza/
│   ├── BerezaApplication.java
│   ├── config/                # SecurityConfig, WebSocketConfig, SessionConfig…
│   ├── security/              # UserDetails, CSRF/auth handlers, CurrentUserProvider
│   ├── domain/                # JPA-сущности и enum-ы
│   ├── repository/            # Spring Data репозитории
│   ├── service/               # бизнес-логика: AuthService, ChatService, MessageService…
│   ├── web/
│   │   ├── controller/        # REST API
│   │   ├── dto/               # request/response модели
│   │   ├── mapper/            # entity ↔ DTO
│   │   └── ws/                # STOMP-контроллеры и presence
│   └── exception/             # GlobalExceptionHandler + типизированные исключения
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   └── db/migration/          # Flyway-миграции
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── nginx.frontend.conf    # отдача SPA
│   ├── Dockerfile
│   └── src/
│       ├── api/               # axios-клиенты по доменам
│       ├── components/        # AppLayout, MessageList, MessageInput…
│       ├── context/           # AuthContext, NotificationContext
│       ├── pages/             # Login, Register, Chats, ChatRoom, Hotels, Bookings…
│       ├── ws/stomp.js        # STOMP-клиент
│       └── styles/global.css
└── docs/
    ├── architecture.md
    ├── database.md
    ├── api.md
    ├── sequence-diagrams.md
    ├── development-plan.md
    ├── cicd.md
    └── deployment-ru.md
```

## Быстрый старт

### Вариант 1: всё в Docker

```bash
cp .env.example .env
# отредактируйте .env (как минимум REMEMBER_ME_KEY и пароли)
docker compose up -d --build
# UI:        http://localhost
# Actuator:  http://localhost/actuator/health  (доступ только из приватной сети)
```

После старта Flyway применит миграции `V1__init_schema.sql`, `V2__spring_session.sql`
и `V3__seed_demo_data.sql` (демо-пользователи: `admin / tourist / guide / hotel`,
пароль для всех — `demo1234`).

### Вариант 2: dev-режим (бэк/фронт локально)

```bash
# 1. Поднимаем только Postgres
docker compose -f docker-compose.dev.yml up -d

# 2. Backend
./mvnw spring-boot:run

# 3. Frontend
cd frontend
npm install
npm run dev    # http://localhost:5173 (проксирует /api и /ws → 8080)
```

## Demo-аккаунты

| Логин   | Роль     | Назначение                                |
|---------|----------|-------------------------------------------|
| admin   | ADMIN    | Полный доступ                             |
| tourist | TOURIST  | Турист (бронирует, общается)              |
| guide   | GUIDE    | Гид (создаёт группы, шлёт точки сбора)    |
| hotel   | HOTEL    | Владелец отеля (отвечает на брони)        |

Пароль для всех: **`demo1234`**.

## Ключевые архитектурные решения

- **Аутентификация — session-based, без JWT.** Логин выдаёт серверную HTTP-сессию,
  cookie `BEREZA_SESSION` — HttpOnly, Secure (в проде), SameSite=Lax. Сессии хранятся
  в таблице `SPRING_SESSION` (Spring Session JDBC). Это позволяет легко горизонтально
  масштабировать backend и инвалидировать сессии на logout без отдельных denylist'ов.
- **CSRF — double-submit cookie.** `XSRF-TOKEN` отдаётся JS-доступным cookie, фронт
  читает его и шлёт в заголовке `X-XSRF-TOKEN`. Сессионный cookie остаётся HttpOnly.
- **Защита от Session Fixation.** `ChangeSessionIdAuthenticationStrategy` меняет id
  сессии после успешного логина.
- **WebSocket поверх HTTP-сессии.** SockJS handshake протаскивает session-cookie,
  STOMP-фреймы выполняются от имени аутентифицированного principal.
- **Файлы — Apache Tika.** Реальный MIME определяется по содержимому, а не по
  заголовку клиента; whitelist-разрешённые типы заданы в `bereza.storage.allowed-mime`.
- **Геолокация.** Координаты хранятся как `DOUBLE PRECISION`. USER_LOCATION имеет
  `expires_at` — фоновая задача `GeoPointCleanupJob` чистит протухшие метки.

## Документация

- [`docs/architecture.md`](docs/architecture.md) — слои, диаграмма компонентов, security.
- [`docs/database.md`](docs/database.md) — ER-диаграмма, описание таблиц, индексы.
- [`docs/api.md`](docs/api.md) — REST endpoints, WebSocket события.
- [`docs/sequence-diagrams.md`](docs/sequence-diagrams.md) — sequence-диаграммы (логин, отправка сообщения, бронирование).
- [`docs/development-plan.md`](docs/development-plan.md) — поэтапный план разработки.
- [`docs/cicd.md`](docs/cicd.md) — CI/CD конвейер.
- [`docs/deployment-ru.md`](docs/deployment-ru.md) — деплой на серверы РФ, ФЗ-152.

## Тестирование

```bash
./mvnw test
```

Подключены `spring-security-test` и `testcontainers/postgresql` — интеграционные тесты
поднимают настоящий PostgreSQL в Docker.

## Лицензия

Internal / Proprietary.
