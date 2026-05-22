# REST API — `/api/**`

Все ответы — JSON (если не указано иное). Ошибки оформлены в формате [RFC 7807 `application/problem+json`](https://datatracker.ietf.org/doc/html/rfc7807) (`type/title/status/detail/instance`).

Аутентификация — **HTTP session cookie** `BEREZA_SESSION` (httpOnly). Для всех мутирующих запросов нужен заголовок `X-XSRF-TOKEN`, равный значению cookie `XSRF-TOKEN` (double-submit). CSRF выключен только для `/ws/**`.

Роли: `TOURIST` (путник), `GUIDE` (путевождь), `HOTEL` (гостинный двор), `ADMIN` (воевода).

Пагинация унифицирована: `?page=0&size=20`, ответ — `PageResponse<T>`:

```json
{ "items": [ ... ], "page": 0, "size": 20, "totalElements": 137, "totalPages": 7 }
```

---

## Auth — `/api/auth`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | `permitAll` | Выдать пару `XSRF-TOKEN` cookie + JSON `{headerName, parameterName, token}`. Дёргается фронтом при старте и перед логином. |
| `POST` | `/api/auth/register` | `permitAll` | Регистрация. Тело `RegisterRequest`. **201 Created** + `AuthResponse`. |
| `POST` | `/api/auth/login` | `permitAll` | Логин обрабатывается Spring Security (`UsernamePasswordAuthenticationFilter`). Формат `application/x-www-form-urlencoded`: `username`, `password`. На успех — 200 + `AuthResponse`, сессия закрепляется в cookie. На провал — 401. |
| `POST` | `/api/auth/logout` | authenticated | Выход. Инвалидирует HTTP-сессию, удаляет cookie `BEREZA_SESSION` и `XSRF-TOKEN`. |
| `GET` | `/api/auth/me` | `permitAll` | Текущий пользователь либо `null` для гостя. Используется при бутстрапе SPA. |

### `RegisterRequest`
```json
{
  "username": "string (3-50, [A-Za-z0-9._-])",
  "email": "string (RFC email, ≤254)",
  "password": "string (8-100)",
  "displayName": "string (≤100)",
  "role": "TOURIST | GUIDE | HOTEL  (null ⇒ TOURIST)",
  "phone": "string (≤32, optional)"
}
```

### `AuthResponse`
```json
{ "id": 1, "username": "tourist", "displayName": "Иван", "email": "...", "role": "TOURIST", "avatarUrl": null }
```

---

## Users — `/api/users`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `GET` | `/api/users/me` | authenticated | Профиль текущего пользователя (`UserResponse`). |
| `PUT` | `/api/users/me` | authenticated | Обновить профиль. Тело `UpdateUserRequest` (`displayName`, `phone`, `avatarUrl`). |
| `GET` | `/api/users/{id}` | authenticated | Получить пользователя по id. |
| `GET` | `/api/users?q=&page=&size=` | authenticated | Поиск по `username` / `displayName` / `email` (ILIKE). При пустом `q` возвращает всех с пагинацией. Сортировка по `displayName`. |

### `UserResponse`
```json
{
  "id": 1, "username": "tourist", "displayName": "Иван", "email": "...",
  "role": "TOURIST", "phone": "+7...", "avatarUrl": null,
  "enabled": true, "lastSeenAt": "2026-05-21T08:30:00Z"
}
```

---

## Hotels — `/api/hotels`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `GET` | `/api/hotels?city=&minPrice=&maxPrice=&minStars=&page=&size=` | `permitAll` | Поиск активных дворов; сортировка по `pricePerNight`. |
| `GET` | `/api/hotels/my` | `HOTEL`, `ADMIN` | Список дворов, принадлежащих текущему пользователю. |
| `GET` | `/api/hotels/{id}` | `permitAll` | Карточка двора. |
| `POST` | `/api/hotels` | `HOTEL`, `ADMIN` | Создать двор. Тело `HotelUpsertRequest`. |
| `PUT` | `/api/hotels/{id}` | `HOTEL`, `ADMIN` | Обновить двор (только владелец или ADMIN). |

### `HotelUpsertRequest`
```json
{
  "name": "string (≤200, required)",
  "description": "string (optional)",
  "city": "string (≤120, required)",
  "address": "string (≤500, required)",
  "latitude": -90..90, "longitude": -180..180,
  "stars": 1..5 (optional),
  "pricePerNight": "BigDecimal ≥ 0 (required)",
  "currency": "ISO-4217 (3 char, default RUB)",
  "roomsAvailable": "int ≥ 0",
  "photos": ["/api/files/123", ...],
  "amenities": ["Wi-Fi", "Завтрак", ...],
  "active": true
}
```

### `HotelResponse`
Все поля выше + `id`, `ownerId`.

---

## Bookings — `/api/bookings`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `POST` | `/api/bookings` | `TOURIST`, `GUIDE`, `ADMIN` | Создать бронь. Стартует чат «гость↔владелец двора», шлёт в него BOOKING-сообщение, уведомление владельцу. |
| `GET` | `/api/bookings/my?page=&size=` | authenticated | Брони текущего гостя. JOIN FETCH на отеле и пользователе. Сортировка по `createdAt DESC`. |
| `GET` | `/api/bookings/incoming?page=&size=` | `HOTEL`, `ADMIN` | Брони, принятые во дворы пользователя. |
| `GET` | `/api/bookings/{id}` | гость / владелец / ADMIN | Карточка брони. |
| `POST` | `/api/bookings/{id}/status` | authenticated | Сменить статус. Гость может только `CANCELLED`. Владелец — любое. В чат улетает SYSTEM-сообщение + уведомление другой стороне. |

### `CreateBookingRequest`
```json
{
  "hotelId": 1,
  "checkIn": "2026-06-01",   // @FutureOrPresent
  "checkOut": "2026-06-05",  // @Future
  "guests": 2,               // 1..20
  "rooms": 1                 // 1..10
}
```

### Бизнес-правила
- `checkOut > checkIn` — иначе `400 BadRequest`.
- У гостя нет активной (`PENDING|CONFIRMED`) брони этого двора, пересекающейся по датам — иначе `409 Conflict` («У вас уже есть бронь этого отеля на эти даты»).
- Количество пересекающихся броней по двору `< roomsAvailable` — иначе `409 Conflict`.

### `BookingResponse`
```json
{
  "id": 5, "hotelId": 1, "hotelName": "Отель «Берёзка»",
  "userId": 2, "userDisplayName": "Иван",
  "checkIn": "2026-06-01", "checkOut": "2026-06-05",
  "guests": 2, "rooms": 1,
  "totalPrice": "26000.00", "currency": "RUB",
  "status": "PENDING", "chatId": 7, "createdAt": "..."
}
```

---

## Chats — `/api/chats`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `GET` | `/api/chats?page=&size=` | authenticated | Все чаты пользователя; `ChatResponse.unreadCount` высчитывается по `last_read_message_id` участника. |
| `GET` | `/api/chats/{id}` | участник | Получить чат + участников + unread. |
| `POST` | `/api/chats` | `PERSONAL` — все; `GROUP` — `GUIDE`, `ADMIN` | Создать чат. `CreateChatRequest`. Если `type=PERSONAL` или 1 участник — личный, иначе группа. |
| `POST` | `/api/chats/{id}/members` | OWNER/ADMIN чата | Добавить участника. Тело `{userId}`. |
| `DELETE` | `/api/chats/{id}/members/{userId}` | OWNER/ADMIN чата (или сам участник) | Удалить участника. Владельца удалить нельзя. |

### `CreateChatRequest`
```json
{
  "type": "PERSONAL | GROUP | null",
  "title": "string (≤120, обязательно для GROUP)",
  "description": "string (≤500)",
  "memberIds": [2, 3, 4]
}
```

---

## Messages — `/api/chats/{chatId}/messages`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `GET` | `/api/chats/{chatId}/messages?before=&size=` | участник | История сообщений. `before` — id, до которого скролить вверх. `size` ≤ 100, дефолт 50. Возвращает Slice (порядок DESC по `createdAt`). |
| `POST` | `/api/chats/{chatId}/messages` | участник | Отправить сообщение. Тело `SendMessageRequest`. После сохранения публикует в `/topic/chats.{chatId}` через `SimpMessagingTemplate`. |
| `POST` | `/api/chats/{chatId}/messages/{messageId}/read` | участник | Отметить прочитанным. Публикует событие в `/topic/chats.{chatId}.reads`. |
| `DELETE` | `/api/chats/{chatId}/messages/{messageId}` | автор сообщения | Мягкое удаление (`deleted_at`, `content=null`). |

### `SendMessageRequest`
```json
{
  "type": "TEXT | FILE | GEO | BOOKING | SYSTEM (null ⇒ TEXT)",
  "content": "string ≤8000",
  "payload": { "any": "json" },
  "replyToId": 12,
  "attachmentIds": [42, 43]
}
```

### `MessageResponse`
```json
{
  "id": 100, "chatId": 7, "senderId": 2, "senderDisplayName": "Иван",
  "type": "TEXT", "content": "Привет",
  "payload": null, "replyToId": null,
  "attachments": [ {"id":42, "originalName":"...", "downloadUrl":"/api/files/42"} ],
  "createdAt": "...", "editedAt": null, "deleted": false,
  "readBy": [3, 5]
}
```

---

## Files — `/api/files`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `POST` | `/api/files` | authenticated | `multipart/form-data`: `file` (обязательно), `category` (опц., `GENERIC|IMAGE|PDF|INSURANCE|VOUCHER|TICKET`). Возвращает `AttachmentView`. Файл живёт «висящим», пока не привязан к сообщению через `attachmentIds` в `SendMessageRequest`. |
| `GET` | `/api/files/{id}` | authenticated | Скачать/получить контент. `Content-Disposition: inline; filename*=UTF-8''...`. |

### `AttachmentView`
```json
{ "id": 42, "originalName": "voucher.pdf", "mimeType": "application/pdf",
  "sizeBytes": 184320, "category": "PDF", "downloadUrl": "/api/files/42" }
```

Лимиты: 25 MB на файл (см. `bereza.storage.max-file-size-mb`), MIME-whitelist — `image/{png,jpeg,webp,gif}`, `application/pdf`.

---

## Geo — `/api/geo`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `POST` | `/api/geo/points` | `GUIDE`, `HOTEL`, `ADMIN` | Создать гео-метку (точка сбора, достопримечательность, локация). Tело `CreateGeoPointRequest`. |
| `GET` | `/api/geo/chats/{chatId}` | участник чата | Активные (не истёкшие) метки чата. |

### `CreateGeoPointRequest`
```json
{
  "chatId": 7,
  "type": "USER_LOCATION | MEETING_POINT | ATTRACTION | EMERGENCY",
  "title": "string ≤150",
  "description": "string ≤500",
  "latitude": 55.75, "longitude": 37.61,
  "expiresAt": "2026-05-22T08:00:00Z"
}
```

---

## Notifications — `/api/notifications`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `GET` | `/api/notifications?page=&size=` | authenticated | Лента уведомлений. |
| `GET` | `/api/notifications/unread/count` | authenticated | `{ "count": 3 }`. |
| `POST` | `/api/notifications/{id}/read` | владелец уведомления | Отметить прочитанным. |

Уведомления (`NEW_MESSAGE`, `BOOKING_NEW`, `BOOKING_STATUS`, `GROUP_INVITE`, …) также пушатся клиенту через WebSocket: `/user/queue/notifications` (см. [websocket-endpoints.md](./websocket-endpoints.md)).

---

## Коды ошибок (RFC 7807)

| HTTP | type | Когда |
|---|---|---|
| `400` | `.../errors/bad-request` | Валидация полей, нарушение бизнес-правил формы. |
| `401` | `.../errors/unauthorized` | Не авторизован (логин обязателен). |
| `403` | `.../errors/forbidden` | Прав на действие нет (роль / не владелец). |
| `404` | `.../errors/not-found` | Сущность не найдена. |
| `409` | `.../errors/conflict` | Конфликт: дубликат, пересечение броней, отсутствие свободных номеров. |
| `413` | стандарт Spring | Превышен размер файла. |
| `500` | `.../errors/internal` | Непредвиденная ошибка. |

## CORS

`bereza.security.allowed-origins` (по умолчанию `http://localhost:5173,http://localhost`). При production — задаётся через `ALLOWED_ORIGINS`.
