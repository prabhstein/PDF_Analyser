CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    original_filename TEXT NOT NULL,
    stored_filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    byte_size BIGINT NOT NULL,
    page_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ
);

CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    char_count INTEGER NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, page_number, chunk_index)
);

CREATE INDEX document_chunks_document_id_idx ON document_chunks(document_id);
CREATE INDEX document_chunks_embedding_idx ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    not_answerable BOOLEAN NOT NULL DEFAULT false,
    citations_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX chat_messages_document_id_idx ON chat_messages(document_id, created_at);
