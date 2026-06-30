# Security

This project handles credentials for external systems, including Telegram and OpenAI services. Secrets must never be committed to the repository.

## Secret Storage

All local secrets must be stored in a local configuration file that is excluded by `.gitignore`.

Recommended local file name:

```text
application-secrets.local.yml
```

Recommended `.gitignore` entries:

```gitignore
application-secrets.local.yml
*.local.yml
*.local.properties
*.db
*.sqlite
*.sqlite3
```

The Spring Boot application should import this file only for local development, for example through profile-specific configuration or `spring.config.import`.

## Secret Types

Expected secrets include:

- Telegram bot token.
- Telegram channel IDs if they are considered private.
- OpenAI API key.
- Cloud PostgreSQL credentials.
- Any future provider credentials.

## Runtime Environments

### Local

- Use a local secrets file excluded by `.gitignore`.
- Use SQLite 3 in a local database file.
- Do not use production Telegram channels unless explicitly testing production publishing.

### Cloud

- Use AWS or Kubernetes secret management for cloud secrets.
- Do not bake secrets into container images, application packages, or committed configuration files.
- Use PostgreSQL credentials provided by the deployment environment.
- Do not store production secrets directly in Kubernetes manifests committed to the repository.

## Logging Rules

- Do not log tokens, API keys, full database credentials, or secret file contents.
- Be careful when logging Telegram API errors because request payloads may contain channel identifiers or message content.
- Be careful when logging OpenAI requests and responses because they may contain news text, generated summaries, translated content, or category decisions.
- Log enough context for troubleshooting without exposing credentials.

## Publishing Safety

- Keep separate local/test Telegram channels when possible.
- Make the active Spring profile visible in logs at startup.
- Protect against accidental duplicate publication by storing publication records in the database.
