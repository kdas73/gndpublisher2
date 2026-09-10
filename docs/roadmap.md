# Implementation Roadmap

This roadmap breaks the implementation into small, independently reviewable tasks. Each task should keep changes narrow, include focused tests, and update documentation when behavior or schema changes.

## Tasks

1. [Project Scaffold](tasks/001-project-scaffold.md)
2. [Testing Foundation](tasks/002-testing-foundation.md)
3. [Runtime Configuration](tasks/003-runtime-configuration.md)
4. [Database Baseline](tasks/004-database-baseline.md)
5. [RSS Ingestion](tasks/005-rss-ingestion.md)
6. [Source Deduplication](tasks/006-source-deduplication.md)
7. [OpenAI Integration Foundation](tasks/007-openai-integration-foundation.md)
8. [Classification And Semantic Events](tasks/008-classification-semantic-events.md)
9. [Source Quota Selection](tasks/009-source-quota-selection.md)
10. [Publication Content Generation](tasks/010-publication-content-generation.md)
11. [Telegram Publishing](tasks/011-telegram-publishing.md)
12. [Important News Digest](tasks/012-important-news-digest.md)
13. [Cleanup Service](tasks/013-cleanup-service.md)
14. [Docker And Deployment Skeleton](tasks/014-docker-deployment-skeleton.md)
15. [Observability And Error Handling](tasks/015-observability-error-handling.md)
16. [Null Pointer Safety Review](tasks/016-null-pointer-safety-review.md)
17. [LLM Provider Abstraction](tasks/017-llm-provider-abstraction.md)

## Suggested Order

Start with tasks 1-2 to create the project and testing foundation. Then implement configuration and database baseline in tasks 3-4. Implement the ingestion path through task 9 before adding publication content generation and Telegram publishing. Task 15 can be expanded as soon as the first application slices exist. Tasks 16-17 are cross-cutting reviews and refactors that run over the finished slices rather than adding a new slice.

Task 7 built the integration layer against OpenAI directly. Task 17 replaced that with a provider-neutral LLM abstraction, so `docs/llm.md` and `docs/architecture.md` are the current reference; task documents 2-16 are kept unchanged as historical records of what was built at the time.
