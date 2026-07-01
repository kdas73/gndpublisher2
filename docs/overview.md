# Project Overview

GND Publisher collects news from Greek RSS feeds, stores them in a database, selects relevant items by category, translates selected news into configured languages, and publishes the translated messages to Telegram channels.

## Product Goal

The application should automate multilingual news publishing while preserving the original source of each news item.

## Main Workflow

1. Read configured RSS feeds on a schedule.
2. Normalize feed items into a common internal news format.
3. Store source-level new items in the database.
4. Use OpenAI GPT5.5-mini to assign each item to an existing or new semantic event key and match it against configured categories.
5. Use OpenAI GPT-5.5 to create a summary when needed and translate selected publishable events into configured target languages.
6. Publish translated event messages to Telegram channels mapped by language.
7. Include the original source in every Telegram message.

## Initial Domain Concepts

- Feed source: a configured Greek news RSS feed.
- News item: a normalized article or feed entry stored in the database.
- Semantic event: a real-world news event represented by a short semantic key and linked to one or more source news items.
- Category: a project-defined grouping used to decide whether a news item should be processed.
- Categorizer: OpenAI GPT5.5-mini model used to assign news items to project categories and semantic event keys.
- Language: a target publication language.
- Translation and summary provider: OpenAI GPT-5.5 model used for target-language translation and summary generation when needed.
- Telegram channel: a destination channel for one language or language/category combination.
- Publication: a record that a translated news item was sent to a Telegram channel.

## Non-Goals For The First Version

- Manual editorial UI.
- Full article scraping beyond RSS data unless explicitly required.
- Rewriting source content without attribution.
- Publishing messages without durable publication tracking.

## Operational Expectations

- The application runs continuously as a backend service.
- Scheduling should be configurable.
- RSS source list should be managed through the database; category rules, languages, and Telegram channel mapping should be configurable without code changes where practical.
- Local development uses SQLite 3.
- Cloud runtime uses PostgreSQL.
- Cloud deployment uses Docker and a Kubernetes service on AWS.
