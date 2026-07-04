# 011 Telegram Publishing

## Goal

Publish target-language semantic event messages to Telegram channels and store publication history.

## Scope

- Route publication content to Telegram channels by target language.
- Build Telegram messages with source attribution.
- Send messages through Telegram Bot API client.
- Persist publication records.
- Enforce idempotency by semantic event, target language, and Telegram channel.

## Deliverables

- `TelegramRoutingService`.
- `TelegramMessageMapper`.
- `TelegramBotClient`.
- `PublicationService`.
- Publication repository methods.

## Tests

- Routing unit tests.
- Message mapper unit tests verifying source attribution.
- Publication idempotency tests.
- Telegram client tests with mock HTTP server.

## Done When

- Telegram messages are sent for selected target-language publication content.
- Duplicate publication to the same language/channel pair is prevented.
- Publication failures are persisted enough for troubleshooting.
