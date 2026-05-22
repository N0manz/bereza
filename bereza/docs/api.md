# REST API + WebSocket события

> Все эндпоинты — относительные к `/api`. Все защищённые методы требуют
> session-cookie `BEREZA_SESSION` и заголовка `X-XSRF-TOKEN` (для не-GET).
> Ответы об ошибках — RFC 7807 `application/problem+json`.

## 1. Аутентификация

### GET `/api/auth/csrf`
Получает CSRF-токен. Cookie `XSRF-TOKEN` выставится автоматически.

**200 OK**
```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "a9c2e6c5-...."
}
```

### POST `/api/auth/register`
Регистрация. **Тело JSON**:
```json
{
  "username": "ivan",
  "email": "ivan@example.ru",
  "password": "verystrong1",
  "displayName": "Иван Иванов",
  "role": "TOURIST",
  "phone": "+79001234567"
}
```

**201 Created** → `AuthResponse`. `409 Conflict` если логин/email заняты.

### POST `/api/auth/login`
`application/x-www-form-urlencoded`, параметры `username`, `password`.
На успех — `200 OK` + JSON пользователя, `Set-Cookie: BEREZA_SESSION=...`.
На неуспех — `401 Unauthorized`.

### POST `/api/auth/logout`
Инвалидирует сессию, удаляет cookie.

### GET `/api/auth/me`
Возвращает текущего пользователя либо `null` (если гость).

## 2. Пользователи

| Метод | URL              | Описание                                  |
|-------|------------------|-------------------------------------------|
| GET   | `/users/me`      | Профиль авторизованного                   |
| PUT   | `/users/me`      | Изменить `displayName`, `phone`, `avatarUrl` |
| GET   | `/users/{id}`    | Профиль по id                             |
| GET   | `/users?q=...&page=0&size=20` | Поиск (по username/email/displayName) |

## 3. Чаты

| Метод | URL                                | Описание                                  |
|-------|------------------------------------|-------------------------------------------|
| GET   | `/chats?page=0&size=30`            | Список чатов пользователя                 |
| GET   | `/chats/{id}`                      | Получить чат                              |
| POST  | `/chats`                           | Создать чат (личный/групповой)            |
| POST  | `/chats/{id}/members`              | Добавить участника (только админ)         |
| DELETE| `/chats/{id}/members/{userId}`     | Удалить участника / выйти                 |

`POST /chats` тело:
```json
{ "type": "GROUP", "title": "Алтай 2026", "description": "...", "memberIds": [3,4,5] }
```
или для личного:
```json
{ "type": "PERSONAL", "memberIds": [7] }
```

## 4. Сообщения

| Метод | URL                                      | Описание                                |
|-------|------------------------------------------|-----------------------------------------|
| GET   | `/chats/{chatId}/messages?before=&size=` | История с пагинацией по id (`<before`)  |
| POST  | `/chats/{chatId}/messages`               | Отправить сообщение                     |
| POST  | `/chats/{chatId}/messages/{id}/read`     | Отметить прочитанным                    |
| DELETE| `/chats/{chatId}/messages/{id}`          | Soft-delete своего сообщения            |

Тело `POST /messages`:
```json
{
  "type": "TEXT",
  "content": "Привет!",
  "replyToId": null,
  "attachmentIds": [42, 43]
}
```

## 5. Файлы

| Метод | URL                                          | Описание                                |
|-------|----------------------------------------------|-----------------------------------------|
| POST  | `/files` (multipart, поле `file`, `category`)| Загрузка. Лимит 25 МБ, whitelist MIME   |
| GET   | `/files/{id}`                                | Скачать. `Content-Disposition: inline`  |

## 6. Отели

| Метод | URL                                          | Доступ                  |
|-------|----------------------------------------------|-------------------------|
| GET   | `/hotels?city=&minPrice=&maxPrice=&minStars=`| Публичный               |
| GET   | `/hotels/{id}`                               | Публичный               |
| POST  | `/hotels`                                    | `HOTEL`, `ADMIN`        |
| PUT   | `/hotels/{id}`                               | владелец / `ADMIN`      |

## 7. Бронирования

| Метод | URL                            | Доступ                                |
|-------|--------------------------------|---------------------------------------|
| POST  | `/bookings`                    | `TOURIST`, `GUIDE`, `ADMIN`           |
| GET   | `/bookings/my`                 | любой авторизованный                  |
| GET   | `/bookings/incoming`           | `HOTEL`, `ADMIN`                      |
| GET   | `/bookings/{id}`               | гость / владелец отеля / `ADMIN`      |
| POST  | `/bookings/{id}/status`        | гость (только CANCELLED) или владелец |

## 8. Геолокация

| Метод | URL                                | Описание                                 |
|-------|------------------------------------|------------------------------------------|
| POST  | `/geo/points`                      | Создать метку                            |
| GET   | `/geo/chats/{chatId}`              | Активные метки чата                      |

## 9. Уведомления

| Метод | URL                              | Описание                            |
|-------|----------------------------------|-------------------------------------|
| GET   | `/notifications?page=&size=`     | Список                              |
| GET   | `/notifications/unread/count`    | Счётчик непрочитанных               |
| POST  | `/notifications/{id}/read`       | Пометить прочитанным                |

## 10. WebSocket / STOMP

**Endpoint:** `wss://host/ws` (SockJS-fallback). Аутентификация — через HTTP-сессию
(cookie `BEREZA_SESSION` присылается при SockJS handshake).

### Подписки (server → client)

| Назначение            | Канал                                  | Payload                        |
|-----------------------|----------------------------------------|--------------------------------|
| Новые сообщения чата  | `/topic/chats.{chatId}`                | `MessageResponse`              |
| Прочтения сообщений   | `/topic/chats.{chatId}.reads`          | `{messageId, userId}`          |
| Печатает...           | `/topic/chats.{chatId}.typing`         | `{userId, displayName}`        |
| Personal notifications| `/user/queue/notifications`            | `NotificationResponse`         |
| Online/offline        | `/topic/presence`                      | `{userId, online}`             |

### Публикации (client → server)

| Назначение               | Destination                            | Payload                                |
|--------------------------|----------------------------------------|----------------------------------------|
| Отправить сообщение по WS| `/app/chats/{chatId}/send`             | `SendMessageRequest`                   |
| «Печатает…»              | `/app/chats/{chatId}/typing`           | `{}` (любой)                           |
| Отметка прочтения        | `/app/chats/{chatId}/read`             | `{messageId}`                          |

> REST-метод `POST /chats/{id}/messages` и STOMP `/app/chats/{id}/send` идемпотентно
> создают одно и то же сообщение и публикуют событие через `ChatEventPublisher`,
> поэтому фронт может пользоваться любым из путей.

## 11. Health/Info

- `GET /actuator/health/liveness` — для k8s/docker healthcheck.
- `GET /actuator/health/readiness` — для трафик-роутера (готов ли принимать запросы).
