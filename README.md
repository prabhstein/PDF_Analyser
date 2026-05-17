# PDF Analyser

Single-user AI web app for uploading PDFs and chatting with their contents using Spring Boot, Thymeleaf, HTMX, PostgreSQL, pgvector, and OpenAI.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker
- OpenAI API key

## Run Locally

Start PostgreSQL with pgvector:

```bash
docker compose up -d db
```

Run the app:

```bash
export OPENAI_API_KEY=your_key_here
mvn spring-boot:run
```

Open http://localhost:8080.

## Test

```bash
mvn test
```

## What v1 Does

- Upload a PDF.
- Extract text page by page.
- Chunk and embed the text with OpenAI embeddings.
- Store chunks in PostgreSQL using pgvector.
- Ask questions against the PDF.
- Generate grounded answers with page citations.
