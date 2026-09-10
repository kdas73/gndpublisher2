# Security

This project handles credentials for external systems, including Telegram and hosted LLM providers. Secrets must never be committed to the repository.

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
- OpenAI API key, required only when a use case targets the `openai` provider. An Ollama-only deployment needs no LLM credential at all.
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

## Self-Hosted LLM Endpoints

The Ollama provider has no authentication: anyone who can reach `gnd.llm.ollama.base-url` can use the
model and read whatever is sent to it. The base URL is therefore security-relevant configuration, not
just a connection detail.

- `gnd.llm.ollama.base-url` must point at an internal host: `localhost` for local development, or a
  cluster-internal service address in a cloud deployment. Never a public endpoint.
- Because the endpoint carries no credential, network policy is the only access control. Restrict it
  at the network or Kubernetes NetworkPolicy level rather than relying on the URL being obscure.
- Do not add an `Authorization` header for Ollama. Sending a bearer token to an unauthenticated
  endpoint leaks the secret without gaining anything.
- Article text, generated summaries, and classification decisions are sent to whatever this URL
  resolves to. Treat a misconfigured base URL as a data exfiltration path, not just a broken feature.

## Logging Rules

- Do not log tokens, API keys, full database credentials, or secret file contents.
- Be careful when logging Telegram API errors because request payloads may contain channel identifiers or message content.
- Be careful when logging LLM requests and responses because they may contain news text, generated summaries, translated content, or category decisions. This applies to every provider, including a local one.
- Log enough context for troubleshooting without exposing credentials.
- See [`observability.md`](observability.md) for the enforced logging/error-handling conventions and the tests that guard against secret/payload leakage.

## Publishing Safety

- Keep separate local/test Telegram channels when possible.
- Make the active Spring profile visible in logs at startup.
- Protect against accidental duplicate publication by storing publication records in the database.
