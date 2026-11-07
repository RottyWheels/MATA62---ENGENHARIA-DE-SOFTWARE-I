-- Tabelas de infraestrutura exigidas pelas dependências do Spring.

-- Spring Modulith: registro de publicação de eventos (spring-modulith-starter-jpa)
CREATE TABLE event_publication (
	id UUID NOT NULL,
	listener_id TEXT NOT NULL,
	event_type TEXT NOT NULL,
	serialized_event TEXT NOT NULL,
	publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
	completion_date TIMESTAMP WITH TIME ZONE,
	last_resubmission_date TIMESTAMP WITH TIME ZONE,
	status TEXT,
	completion_attempts INT NOT NULL DEFAULT 0,
	CONSTRAINT event_publication_pk PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);

CREATE TABLE event_publication_archive (
	id UUID NOT NULL,
	listener_id TEXT NOT NULL,
	event_type TEXT NOT NULL,
	serialized_event TEXT NOT NULL,
	publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
	completion_date TIMESTAMP WITH TIME ZONE,
	last_resubmission_date TIMESTAMP WITH TIME ZONE,
	status TEXT,
	completion_attempts INT NOT NULL DEFAULT 0,
	CONSTRAINT event_publication_archive_pk PRIMARY KEY (id)
);

CREATE INDEX event_publication_archive_serialized_event_hash_idx ON event_publication_archive USING hash (serialized_event);
CREATE INDEX event_publication_archive_by_completion_date_idx ON event_publication_archive (completion_date);

-- Spring Session JDBC (schema-postgresql.sql do spring-session-jdbc)
CREATE TABLE SPRING_SESSION (
	PRIMARY_ID CHAR(36) NOT NULL,
	SESSION_ID CHAR(36) NOT NULL,
	CREATION_TIME BIGINT NOT NULL,
	LAST_ACCESS_TIME BIGINT NOT NULL,
	MAX_INACTIVE_INTERVAL INT NOT NULL,
	EXPIRY_TIME BIGINT NOT NULL,
	PRINCIPAL_NAME VARCHAR(100),
	CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
	SESSION_PRIMARY_ID CHAR(36) NOT NULL,
	ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
	ATTRIBUTE_BYTES BYTEA NOT NULL,
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
