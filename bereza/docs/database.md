# База данных

PostgreSQL 16. Все таблицы используют `BIGINT GENERATED ALWAYS AS IDENTITY`,
временные метки — `TIMESTAMPTZ`. Кодировка БД — `UTF8`, локаль `ru_RU.UTF-8`.

## ER-диаграмма

```mermaid
erDiagram
    USERS ||--o{ CHAT_MEMBERS : "состоит"
    USERS ||--o{ MESSAGES     : "отправляет"
    USERS ||--o{ ATTACHMENTS  : "загружает"
    USERS ||--o{ BOOKINGS     : "бронирует"
    USERS ||--o{ NOTIFICATIONS: "получает"
    USERS ||--o{ GEO_POINTS   : "публикует"
    USERS ||--o| HOTELS       : "владеет"

    CHATS ||--o{ CHAT_MEMBERS : "имеет"
    CHATS ||--o{ MESSAGES     : "содержит"
    CHATS ||--o{ GEO_POINTS   : "точки чата"
    CHATS ||--o{ BOOKINGS     : "обсуждается"

    MESSAGES ||--o{ ATTACHMENTS    : "имеет вложения"
    MESSAGES ||--o{ MESSAGE_READS  : "прочитано"
    MESSAGES ||--o| MESSAGES       : "reply_to"

    HOTELS ||--o{ BOOKINGS : "забронирован"

    USERS {
      bigint id PK
      varchar username "unique"
      varchar email "unique"
      varchar password_hash
      varchar display_name
      varchar role "TOURIST|GUIDE|HOTEL|ADMIN"
      varchar phone
      varchar avatar_url
      boolean is_enabled
      boolean is_locked
      timestamptz last_seen_at
      timestamptz created_at
      timestamptz updated_at
    }
    CHATS {
      bigint id PK
      varchar type "PERSONAL|GROUP"
      varchar title
      varchar description
      varchar avatar_url
      bigint owner_id FK
      timestamptz created_at
      timestamptz updated_at
    }
    CHAT_MEMBERS {
      bigint chat_id FK
      bigint user_id FK
      varchar member_role "OWNER|ADMIN|MEMBER"
      timestamptz joined_at
      bigint last_read_message_id
      boolean muted
    }
    MESSAGES {
      bigint id PK
      bigint chat_id FK
      bigint sender_id FK
      varchar type "TEXT|FILE|GEO|SYSTEM|BOOKING"
      text content
      jsonb payload
      bigint reply_to_id FK
      timestamptz edited_at
      timestamptz deleted_at
      timestamptz created_at
    }
    MESSAGE_READS {
      bigint message_id FK
      bigint user_id FK
      timestamptz read_at
    }
    ATTACHMENTS {
      bigint id PK
      bigint message_id FK
      bigint uploader_id FK
      varchar storage_key
      varchar original_name
      varchar mime_type
      bigint size_bytes
      varchar checksum_sha256
      varchar category "GENERIC|IMAGE|PDF|INSURANCE|VOUCHER|TICKET"
      timestamptz created_at
    }
    HOTELS {
      bigint id PK
      bigint owner_id FK
      varchar name
      text description
      varchar city
      varchar address
      double latitude
      double longitude
      smallint stars
      numeric price_per_night
      varchar currency
      int rooms_available
      text photos "TEXT[]"
      text amenities "TEXT[]"
      boolean is_active
    }
    BOOKINGS {
      bigint id PK
      bigint hotel_id FK
      bigint user_id FK
      date check_in
      date check_out
      smallint guests
      smallint rooms
      numeric total_price
      varchar currency
      varchar status "PENDING|CONFIRMED|CANCELLED|COMPLETED"
      bigint chat_id FK
      timestamptz created_at
    }
    GEO_POINTS {
      bigint id PK
      bigint chat_id FK
      bigint author_id FK
      varchar type "USER_LOCATION|MEETING_POINT|ATTRACTION|EMERGENCY"
      varchar title
      varchar description
      double latitude
      double longitude
      timestamptz expires_at
      timestamptz created_at
    }
    NOTIFICATIONS {
      bigint id PK
      bigint user_id FK
      varchar type
      varchar title
      varchar body
      jsonb payload
      boolean is_read
      timestamptz created_at
    }
```

## Индексы (важные)

| Индекс                                | Назначение                                                |
|---------------------------------------|-----------------------------------------------------------|
| `ux_users_username_lower`             | Уникальность логина без учёта регистра                    |
| `ux_users_email_lower`                | Уникальность email без учёта регистра                     |
| `ix_messages_chat_created (DESC)`     | Быстрая выгрузка истории чата (`Slice<Message>` history)   |
| `ix_notifications_user_unread`        | Композитный: счётчик непрочитанных, hot-path               |
| `ix_bookings_status, ix_bookings_check_in` | Поиск пересекающихся броней (overlap query)           |
| `ix_chat_members_user`                | "Все чаты пользователя"                                   |
| `ix_hotels_city`, `ix_hotels_price`   | Фильтрация в поиске отелей                                |

## Целостность и инварианты

- `users.role` — `CHECK IN (...)`, аналогично у `chats.type`, `messages.type`,
  `chat_members.member_role`, `attachments.category`, `bookings.status`,
  `geo_points.type`. Защищает от мусора, даже если приложение ошибётся.
- `bookings.check_out > check_in` — `CHECK`.
- FK с `ON DELETE` стратегией: `SET NULL` для авторов сообщений
  (сообщение остаётся после удаления юзера), `CASCADE` для `chat_members` и
  `attachments` к удалению сообщения.

## Миграции

| Версия | Файл                             | Содержимое                                |
|--------|----------------------------------|-------------------------------------------|
| V1     | `V1__init_schema.sql`            | Бизнес-схема                              |
| V2     | `V2__spring_session.sql`         | Таблицы `SPRING_SESSION` + индексы        |
| V3     | `V3__seed_demo_data.sql`         | Демо-пользователи и три отеля             |

Для прод-деплоя можно отключить демо-данные параметром `spring.flyway.target=2`.
