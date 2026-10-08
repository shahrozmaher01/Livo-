-- ============================================================================
-- LIVO SOCIAL VOICE CHAT PLATFORM
-- Production PostgreSQL Database Schema
-- Scalable relational schema with full foreign keys, checks, indexes, and audit logs.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Roles & Permissions
CREATE TABLE IF NOT EXISTS roles (
    role_key VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
    permission_key VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_key VARCHAR(50) REFERENCES roles(role_key) ON DELETE CASCADE,
    permission_key VARCHAR(100) REFERENCES permissions(permission_key) ON DELETE CASCADE,
    PRIMARY KEY (role_key, permission_key)
);

-- 2. Users & Authentication
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    livo_id BIGINT UNIQUE NOT NULL, -- 8-digit unique public Livo ID
    email VARCHAR(255) UNIQUE,
    google_sub VARCHAR(255) UNIQUE,
    phone VARCHAR(50) UNIQUE,
    nickname VARCHAR(60) NOT NULL,
    avatar_url TEXT,
    bio VARCHAR(255) DEFAULT '',
    gender VARCHAR(10) DEFAULT 'unspecified',
    birthday DATE,
    country_code VARCHAR(10) DEFAULT 'US',
    primary_role VARCHAR(50) NOT NULL DEFAULT 'User' REFERENCES roles(role_key),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_livo_id ON users(livo_id);
CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 3. User Profiles & Stats
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    level INT NOT NULL DEFAULT 1 CHECK (level >= 1),
    experience_points BIGINT NOT NULL DEFAULT 0,
    wealth_score BIGINT NOT NULL DEFAULT 0, -- Total coins spent
    charm_score BIGINT NOT NULL DEFAULT 0,  -- Total gift value received
    followers_count INT NOT NULL DEFAULT 0,
    following_count INT NOT NULL DEFAULT 0,
    visitors_count INT NOT NULL DEFAULT 0,
    equipped_frame_id VARCHAR(50),
    equipped_entrance_effect_id VARCHAR(50),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    current_room_id UUID,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. User Sessions
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token TEXT NOT NULL,
    device_id VARCHAR(255),
    device_model VARCHAR(255),
    client_ip VARCHAR(50),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user ON user_sessions(user_id);

-- 5. Follow / Following Relationships
CREATE TABLE IF NOT EXISTS follows (
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_follower ON follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following ON follows(following_id);

-- 6. Agencies & Agency Members
CREATE TABLE IF NOT EXISTS agencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agency_code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    logo_url TEXT,
    description TEXT,
    commission_rate NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agencies_code ON agencies(agency_code);

CREATE TABLE IF NOT EXISTS agency_members (
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'Host', -- 'Manager', 'Host', 'Member'
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (agency_id, user_id)
);

-- 7. Voice Rooms
CREATE TABLE IF NOT EXISTS rooms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_number BIGINT UNIQUE NOT NULL, -- 6 to 8 digit public Room ID
    title VARCHAR(100) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cover_url TEXT,
    background_url TEXT,
    announcement VARCHAR(500) DEFAULT 'Welcome to Livo Voice Room! Please follow community guidelines.',
    category VARCHAR(50) NOT NULL DEFAULT 'Chat',
    seat_count INT NOT NULL DEFAULT 8 CHECK (seat_count IN (8, 9, 10, 12)),
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    current_listeners_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rooms_number ON rooms(room_number);
CREATE INDEX IF NOT EXISTS idx_rooms_owner ON rooms(owner_id);
CREATE INDEX IF NOT EXISTS idx_rooms_active ON rooms(is_active);

-- 8. Room Members & Admins
CREATE TABLE IF NOT EXISTS room_members (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'Listener', -- 'Owner', 'Admin', 'Speaker', 'Listener'
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE IF NOT EXISTS room_admins (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
);

-- 9. Room Seats
CREATE TABLE IF NOT EXISTS room_seats (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    seat_index INT NOT NULL CHECK (seat_index >= 0 AND seat_index < 16),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, seat_index)
);

-- 10. Room Messages (Chat)
CREATE TABLE IF NOT EXISTS room_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES users(id) ON DELETE SET NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- 'TEXT', 'GIFT', 'SYSTEM', 'JOIN', 'LEAVE'
    content TEXT NOT NULL,
    payload_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_room_messages_room ON room_messages(room_id, created_at DESC);

-- 11. Private Conversations & Messages
CREATE TABLE IF NOT EXISTS private_conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_message_text TEXT,
    last_message_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    unread_user1_count INT NOT NULL DEFAULT 0,
    unread_user2_count INT NOT NULL DEFAULT 0,
    CONSTRAINT unique_conversation_pair UNIQUE (user1_id, user2_id),
    CONSTRAINT check_distinct_users CHECK (user1_id <> user2_id)
);

CREATE TABLE IF NOT EXISTS private_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES private_conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_private_messages_conv ON private_messages(conversation_id, created_at DESC);

-- 12. Coin Wallets & Transactions
-- Rule: $1 = 28,000 coins
CREATE TABLE IF NOT EXISTS coin_wallets (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    diamond_balance BIGINT NOT NULL DEFAULT 0 CHECK (diamond_balance >= 0), -- Earned from received gifts, withdrawable
    total_recharged BIGINT NOT NULL DEFAULT 0,
    total_spent BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1, -- Optimistic concurrency lock
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coin_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(64) UNIQUE NOT NULL, -- Cryptographic transaction idempotency key
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount BIGINT NOT NULL, -- Positive for recharge/reward, negative for spending
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL, -- 'RECHARGE', 'GIFT_SEND', 'REWARD', 'ADMIN_ADJUST', 'REFUND'
    reference_id VARCHAR(64),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coin_transactions_user ON coin_transactions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_coin_transactions_txid ON coin_transactions(transaction_id);

-- 13. Virtual Gifts & Gift Transactions
CREATE TABLE IF NOT EXISTS gifts (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    coin_price BIGINT NOT NULL CHECK (coin_price > 0),
    icon_name VARCHAR(100) NOT NULL,
    animation_type VARCHAR(50) DEFAULT 'NONE',
    category VARCHAR(50) DEFAULT 'Popular',
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS gift_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(id) ON DELETE SET NULL,
    gift_id VARCHAR(50) NOT NULL REFERENCES gifts(id),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    total_coins BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gift_transactions_sender ON gift_transactions(sender_id);
CREATE INDEX IF NOT EXISTS idx_gift_transactions_receiver ON gift_transactions(receiver_id);
CREATE INDEX IF NOT EXISTS idx_gift_transactions_room ON gift_transactions(room_id);

-- 14. Rankings & Leaderboards
CREATE TABLE IF NOT EXISTS rankings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ranking_type VARCHAR(30) NOT NULL, -- 'WEALTH', 'CHARM', 'ROOM'
    period VARCHAR(20) NOT NULL, -- 'DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME'
    period_key VARCHAR(30) NOT NULL, -- e.g. '2026-10-08', '2026-W41', '2026-10'
    entity_id UUID NOT NULL, -- user_id or room_id
    score BIGINT NOT NULL DEFAULT 0,
    rank_position INT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_ranking_entry UNIQUE (ranking_type, period, period_key, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_rankings_lookup ON rankings(ranking_type, period, period_key, rank_position);

-- 15. Level Progression Configuration
CREATE TABLE IF NOT EXISTS levels (
    level_number INT PRIMARY KEY,
    required_exp BIGINT NOT NULL,
    title VARCHAR(50) NOT NULL,
    badge_icon VARCHAR(100),
    privilege_perks TEXT
);

-- 16. Inventory & Backpack (Frames, Entrance Effects)
CREATE TABLE IF NOT EXISTS inventory_items (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL, -- 'AVATAR_FRAME', 'ROOM_FRAME', 'ENTRANCE_EFFECT', 'BADGE'
    rarity VARCHAR(20) NOT NULL DEFAULT 'COMMON', -- 'COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'ROYAL'
    preview_url TEXT,
    description TEXT,
    coin_price BIGINT DEFAULT 0,
    is_obtainable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id VARCHAR(50) NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE, -- NULL for permanent
    acquired_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_item UNIQUE (user_id, item_id)
);

-- 17. Room Frames
CREATE TABLE IF NOT EXISTS room_frames (
    id VARCHAR(50) PRIMARY KEY,
    role_category VARCHAR(50) NOT NULL, -- 'Official', 'Super Admin', 'Admin', 'Admin Leader', 'Manager', 'BD', 'BD Leader', 'Agency', 'Agency Leader', 'Host', 'Coin Reseller', 'Super Coin Reseller', "C's", "C's Leader"
    name VARCHAR(100) NOT NULL,
    border_color_hex VARCHAR(10) NOT NULL,
    glow_color_hex VARCHAR(10) NOT NULL,
    description TEXT
);

-- 18. Events
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    banner_url TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    rules_text TEXT,
    rewards_json JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 19. Reports & Moderation
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL, -- 'USER', 'ROOM', 'MESSAGE'
    target_id VARCHAR(64) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'REVIEWED', 'ACTION_TAKEN', 'DISMISSED'
    reviewer_id UUID REFERENCES users(id),
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS blocks (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, blocked_user_id)
);

-- 20. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(30) NOT NULL, -- 'GIFT', 'FOLLOW', 'INVITE', 'SYSTEM', 'COIN', 'ADMIN'
    payload_json JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read, created_at DESC);

-- 21. Audit Logs (System & Administrative actions)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(64),
    payload_json JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
