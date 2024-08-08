create table if not exists MIGTOOL_HISTORY
(
    id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rank           INTEGER                  NOT NULL,
    script         VARCHAR(250)             NOT NULL,
    checksum       VARCHAR(64)              NOT NULL,
    created_on     timestamp                NOT NULL,
    execution_time INTEGER
);
