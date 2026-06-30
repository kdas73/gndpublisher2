# 010 Summary Generation

## Goal

Generate or improve summaries for selected publishable candidates when needed.

## Scope

- Detect missing, too short, or unsuitable summaries.
- Call GPT-5.5 summary flow only for selected candidates.
- Persist generated summaries.
- Avoid adding unsupported facts.

## Deliverables

- `SummaryService`.
- `OpenAiSummarizer`.
- `news_summaries` persistence flow.
- Summary prompt usage.

## Tests

- Unit tests for summary-needed decision.
- Service tests with mocked OpenAI summarizer.
- Fixture tests for summary JSON.

## Done When

- Selected candidates have usable summaries.
- Summary generation is skipped when not needed.
- Summary output is persisted.
