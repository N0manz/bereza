# Use-Case диаграмма

Четыре актора: **Путник (TOURIST)**, **Путевождь (GUIDE)**, **Гостинный двор (HOTEL)**, **Воевода (ADMIN)**.
Mermaid не умеет нативный UML use-case, поэтому диаграмма дана в виде графа: акторы — слева/справа, прецеденты — в середине, `<<include>>` и `<<extend>>` подписаны на рёбрах.

```mermaid
flowchart LR
  classDef actor fill:#e1ebde,stroke:#2f5d3a,color:#1f4128,font-weight:bold
  classDef uc fill:#faf6ee,stroke:#cfc6ad,color:#1f1b15

  T(["👤 Путник<br/>(TOURIST)"]):::actor
  G(["🧭 Путевождь<br/>(GUIDE)"]):::actor
  H(["🏨 Гостинный двор<br/>(HOTEL)"]):::actor
  A(["⚔ Воевода<br/>(ADMIN)"]):::actor

  %% Аутентификация — общие
  UC_REG([Зарегистрироваться]):::uc
  UC_LOG([Войти в систему]):::uc
  UC_PROF([Править светлицу<br/>профиля]):::uc

  %% Дворы / каталог
  UC_BROWSE([Искать дворы]):::uc
  UC_VIEW([Смотреть карточку двора]):::uc

  %% Бронирование
  UC_BOOK([Записать заездную грамоту]):::uc
  UC_MYBK([Мои грамоты]):::uc
  UC_CANCEL([Отозвать грамоту]):::uc

  %% Hotel-only
  UC_CREATE_HOTEL([Возвести двор]):::uc
  UC_EDIT_HOTEL([Переписать двор]):::uc
  UC_INCOMING([Грамоты, принятые во двор]):::uc
  UC_APPROVE([Утвердить / отвергнуть<br/>грамоту]):::uc

  %% Чаты
  UC_CHAT_LIST([Список бесед]):::uc
  UC_CHAT_PERS([Завести личную беседу]):::uc
  UC_CHAT_GROUP([Собрать дружину<br/>—групповой чат—]):::uc
  UC_SEND_MSG([Послать сообщение]):::uc
  UC_SEND_FILE([Приложить грамоту/файл]):::uc
  UC_GEO([Указать место<br/>на карте]):::uc
  UC_READ([Прочесть весточку]):::uc

  %% Уведомления
  UC_NOTIF([Смотреть вести]):::uc

  %% Admin-only
  UC_ADMIN([Управлять<br/>пользователями]):::uc

  %% --- Путник ---
  T --- UC_REG
  T --- UC_LOG
  T --- UC_PROF
  T --- UC_BROWSE
  T --- UC_VIEW
  T --- UC_BOOK
  T --- UC_MYBK
  T --- UC_CANCEL
  T --- UC_CHAT_LIST
  T --- UC_CHAT_PERS
  T --- UC_SEND_MSG
  T --- UC_SEND_FILE
  T --- UC_READ
  T --- UC_NOTIF

  %% --- Путевождь --- (всё что путник + группы + гео)
  G --- UC_REG
  G --- UC_LOG
  G --- UC_PROF
  G --- UC_BROWSE
  G --- UC_VIEW
  G --- UC_BOOK
  G --- UC_MYBK
  G --- UC_CANCEL
  G --- UC_CHAT_LIST
  G --- UC_CHAT_PERS
  G --- UC_CHAT_GROUP
  G --- UC_SEND_MSG
  G --- UC_SEND_FILE
  G --- UC_GEO
  G --- UC_READ
  G --- UC_NOTIF

  %% --- Гостинный двор --- (управление дворами + входящие брони)
  H --- UC_REG
  H --- UC_LOG
  H --- UC_PROF
  H --- UC_BROWSE
  H --- UC_VIEW
  H --- UC_CREATE_HOTEL
  H --- UC_EDIT_HOTEL
  H --- UC_INCOMING
  H --- UC_APPROVE
  H --- UC_CHAT_LIST
  H --- UC_CHAT_PERS
  H --- UC_SEND_MSG
  H --- UC_SEND_FILE
  H --- UC_GEO
  H --- UC_READ
  H --- UC_NOTIF

  %% --- Воевода ---
  A --- UC_ADMIN
  A --- UC_LOG
  A --- UC_PROF
  A --- UC_INCOMING
  A --- UC_APPROVE
  A --- UC_CREATE_HOTEL
  A --- UC_EDIT_HOTEL
  A --- UC_CHAT_GROUP

  %% include/extend связи между прецедентами
  UC_BOOK -. "&lt;&lt;include&gt;&gt;" .-> UC_CHAT_PERS
  UC_APPROVE -. "&lt;&lt;include&gt;&gt;" .-> UC_SEND_MSG
  UC_BOOK -. "&lt;&lt;include&gt;&gt;" .-> UC_NOTIF
  UC_SEND_MSG -. "&lt;&lt;extend&gt;&gt;<br/>при типе FILE" .-> UC_SEND_FILE
  UC_SEND_MSG -. "&lt;&lt;extend&gt;&gt;<br/>при типе GEO" .-> UC_GEO
  UC_CREATE_HOTEL -. "&lt;&lt;include&gt;&gt;<br/>загрузка фото" .-> UC_SEND_FILE
```

## Роли и ключевые сценарии

| Прецедент | TOURIST | GUIDE | HOTEL | ADMIN | Реализация |
|---|---|---|---|---|---|
| Регистрация / логин | ✅ | ✅ | ✅ | ✅ | `POST /api/auth/register`, `POST /api/auth/login` |
| Поиск дворов | ✅ | ✅ | ✅ | ✅ | `GET /api/hotels` |
| Запись заездной грамоты | ✅ | ✅ | — | ✅ | `POST /api/bookings` (`@PreAuthorize hasAnyRole('TOURIST','GUIDE','ADMIN')`) |
| Отзыв своей грамоты | ✅ | ✅ | — | ✅ | `POST /api/bookings/{id}/status status=CANCELLED` |
| Возвести двор | — | — | ✅ | ✅ | `POST /api/hotels` (`hasAnyRole('HOTEL','ADMIN')`) |
| Утвердить/отвергнуть грамоту | — | — | ✅ (свои) | ✅ | `POST /api/bookings/{id}/status` |
| Личная беседа | ✅ | ✅ | ✅ | ✅ | `POST /api/chats` `type=PERSONAL` |
| Групповой чат | — | ✅ | — | ✅ | `POST /api/chats` `type=GROUP` (`ChatService.createGroup` проверяет роль) |
| Послать гео-метку | — | ✅ | ✅ | ✅ | `POST /api/geo/points` (`hasAnyRole('GUIDE','HOTEL','ADMIN')`) |
| Вести (уведомления) | ✅ | ✅ | ✅ | ✅ | `GET /api/notifications` + WS `/user/queue/notifications` |

## Бизнес-инварианты, проверяемые на сервере

1. У путника не может быть **двух активных грамот** на тот же двор с пересекающимися датами (`BookingRepository.countOverlappingForUser`).
2. Сумма пересекающихся броней по двору не превышает `roomsAvailable`.
3. Изменение `is_active` отеля видно сразу в поиске (фильтр `WHERE h.active = true`).
4. Удалить отель, по которому есть брони, невозможно (`ON DELETE RESTRICT`).
5. Удалить владельца дворов — отель остаётся «бесхозным» (`ON DELETE SET NULL`).
