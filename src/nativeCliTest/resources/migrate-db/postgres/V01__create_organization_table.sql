/**
 * Schema migration example
 */
CREATE TABLE organization
(
    id           VARCHAR(25) NOT NULL PRIMARY KEY,
    company      VARCHAR(125) NOT NULL,
    contact      VARCHAR(125) NOT NULL,
    email        VARCHAR(125) NOT NULL,
    address      VARCHAR(255) DEFAULT NULL,
    zip          VARCHAR(25) DEFAULT NULL,
    country      VARCHAR(125) DEFAULT NULL,
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL
);

CREATE TABLE license
(
    id              VARCHAR(25) NOT NULL PRIMARY KEY,
    features        VARCHAR(255) NOT NULL,
    product         VARCHAR(75) NOT NULL,
    contact         VARCHAR(125) NOT NULL,
    email           VARCHAR(125) NOT NULL,
    secret          BYTEA NOT NULL,
    organization_id VARCHAR(25) NOT NULL,
    activation      timestamp with time zone NOT NULL,
    expiration      timestamp with time zone NOT NULL,
    date_created    timestamp with time zone NOT NULL,
    last_updated    timestamp with time zone NOT NULL,
    last_accessed   timestamp with time zone,
    last_access_ip  VARCHAR(255) DEFAULT NULL,
    access_count    INTEGER NOT NULL DEFAULT 0,
    suspended       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    expired         BOOLEAN NOT NULL DEFAULT FALSE,
    constraint fk_license_organization_id
        foreign key (organization_id) references organization(id)
            on delete cascade
);
