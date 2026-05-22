-- =====================================================================
-- Bereza — base schema. PostgreSQL 14+.
-- Все таблицы используют BIGINT GENERATED ALWAYS AS IDENTITY и TIMESTAMPTZ.
-- =====================================================================

-- ------------------------ users ---------------------------------------
CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(16)  NOT NULL CHECK (role IN ('TOURIST', 'GUIDE', 'HOTEL', 'ADMIN')),
    phone           VARCHAR(32),
    avatar_url      VARCHAR(512),
    is_enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX ux_users_email_lower    ON users (lower(email));
CREATE INDEX ix_users_role                  ON users (role);

-- ------------------------ chats ---------------------------------------
CREATE TABLE chats (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type            VARCHAR(16)  NOT NULL CHECK (type IN ('PERSONAL', 'GROUP')),
    title           VARCHAR(120),
    description     VARCHAR(500),
    avatar_url      VARCHAR(512),
    owner_id        BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_chats_type      ON chats (type);
CREATE INDEX ix_chats_owner     ON chats (owner_id);

-- ------------------------ chat_members --------------------------------
CREATE TABLE chat_members (
    chat_id         BIGINT       NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_role     VARCHAR(16)  NOT NULL DEFAULT 'MEMBER' CHECK (member_role IN ('OWNER', 'ADMIN', 'MEMBER')),
    joined_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_read_message_id BIGINT,
    muted           BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (chat_id, user_id)
);
CREATE INDEX ix_chat_members_user ON chat_members (user_id);

-- ------------------------ messages ------------------------------------
CREATE TABLE messages (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id         BIGINT       NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id       BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    type            VARCHAR(16)  NOT NULL CHECK (type IN ('TEXT', 'FILE', 'GEO', 'SYSTEM', 'BOOKING')),
    content         TEXT,
    -- payload — для GEO/BOOKING/SYSTEM хранится как JSONB
    payload         JSONB,
    reply_to_id     BIGINT       REFERENCES messages(id) ON DELETE SET NULL,
    edited_at       TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_messages_chat_created ON messages (chat_id, created_at DESC);
CREATE INDEX ix_messages_sender       ON messages (sender_id);

-- ------------------------ message read receipts -----------------------
CREATE TABLE message_reads (
    message_id      BIGINT       NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, user_id)
);
CREATE INDEX ix_message_reads_user ON message_reads (user_id);

-- ------------------------ attachments ---------------------------------
CREATE TABLE attachments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message_id      BIGINT       REFERENCES messages(id) ON DELETE CASCADE,
    uploader_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    storage_key     VARCHAR(512) NOT NULL,     -- путь в файловом хранилище
    original_name   VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(120) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    checksum_sha256 VARCHAR(64),
    category        VARCHAR(32)  NOT NULL DEFAULT 'GENERIC'
        CHECK (category IN ('GENERIC', 'IMAGE', 'PDF', 'INSURANCE', 'VOUCHER', 'TICKET')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_attachments_message  ON attachments (message_id);
CREATE INDEX ix_attachments_uploader ON attachments (uploader_id);

-- ------------------------ hotels --------------------------------------
CREATE TABLE hotels (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id        BIGINT       REFERENCES users(id) ON DELETE SET NULL,   -- HOTEL role
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    city            VARCHAR(120) NOT NULL,
    address         VARCHAR(500) NOT NULL,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    stars           SMALLINT     CHECK (stars BETWEEN 1 AND 5),
    price_per_night NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'RUB',
    rooms_available INTEGER      NOT NULL DEFAULT 0,
    photos          TEXT[]       NOT NULL DEFAULT '{}',
    amenities       TEXT[]       NOT NULL DEFAULT '{}',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_hotels_city          ON hotels (city);
CREATE INDEX ix_hotels_owner         ON hotels (owner_id);
CREATE INDEX ix_hotels_price         ON hotels (price_per_night);
CREATE INDEX ix_hotels_active        ON hotels (is_active);

-- ------------------------ bookings ------------------------------------
CREATE TABLE bookings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id        BIGINT       NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    check_in        DATE         NOT NULL,
    check_out       DATE         NOT NULL,
    guests          SMALLINT     NOT NULL DEFAULT 1,
    rooms           SMALLINT     NOT NULL DEFAULT 1,
    total_price     NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'RUB',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    chat_id         BIGINT       REFERENCES chats(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_bookings_dates CHECK (check_out > check_in)
);
CREATE INDEX ix_bookings_user        ON bookings (user_id);
CREATE INDEX ix_bookings_hotel       ON bookings (hotel_id);
CREATE INDEX ix_bookings_status      ON bookings (status);
CREATE INDEX ix_bookings_check_in    ON bookings (check_in);

-- ------------------------ geolocation_points --------------------------
CREATE TABLE geo_points (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id         BIGINT       REFERENCES chats(id) ON DELETE CASCADE,
    author_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('USER_LOCATION', 'MEETING_POINT', 'ATTRACTION', 'EMERGENCY')),
    title           VARCHAR(150),
    description     VARCHAR(500),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    -- метка истекает (для USER_LOCATION) — после expires_at скрываем
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_geo_chat        ON geo_points (chat_id);
CREATE INDEX ix_geo_author      ON geo_points (author_id);
CREATE INDEX ix_geo_type        ON geo_points (type);

-- ------------------------ notifications -------------------------------
CREATE TABLE notifications (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(32)  NOT NULL,    -- NEW_MESSAGE, BOOKING_CONFIRMED, SYSTEM, GROUP_INVITE...
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(2000),
    payload         JSONB,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id, is_read, created_at DESC);
