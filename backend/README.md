# Livo Voice Chat & Social Platform - Backend Architecture

## System Architecture

The Livo platform backend provides high-performance, low-latency audio signaling and social graph operations using:
1. **PostgreSQL**: Relational persistence for accounts, ledgers, rooms, agencies, rankings, and audit logs.
2. **WebSockets (`/ws/room/{roomId}` & `/ws/notifications`)**: Real-time room state broadcasts, seat updates, text chats, gift animations, and voice signaling.
3. **REST API (`/api/v1`)**: Atomic wallet debits, Google OAuth verification, user profiles, room search, and administration.

## Environment Variables (`.env`)
```bash
DATABASE_URL=postgresql://livo_user:secure_password@localhost:5432/livo_db
REDIS_URL=redis://localhost:6379/0
JWT_SECRET=production_jwt_signing_secret_key
COIN_EXCHANGE_RATE=28000 # $1.00 USD = 28,000 Livo Coins
PORT=8080
```

## REST API Endpoints
- `POST /api/v1/auth/google`: Verifies Google ID tokens and returns session JWT.
- `GET /api/v1/users/me`: Current user profile, level, coins, badges.
- `GET /api/v1/rooms`: Public active voice rooms list.
- `POST /api/v1/rooms`: Create a new voice room.
- `POST /api/v1/wallet/recharge`: Process verified recharge package.
- `POST /api/v1/wallet/send-gift`: Atomic server-side gift transaction.
- `GET /api/v1/rankings`: Top Wealth, Top Charm, and Top Room leaderboards.
- `GET /api/v1/admin/overview`: Super admin metrics, user moderation, coin audit.

## WebSocket Real-time Protocol
- `JOIN_ROOM`: Payload `{ roomId, userId, seatIndex }`
- `SEAT_UPDATE`: Payload `{ seatIndex, userId, isMuted, isSpeaking }`
- `ROOM_MESSAGE`: Payload `{ messageId, senderId, text, timestamp }`
- `GIFT_EVENT`: Broadcast `{ giftId, senderName, receiverName, count, animation }`
- `VOICE_AUDIO_CHUNK`: Binary Opus/PCM audio packets transmitted between active seated speakers.
