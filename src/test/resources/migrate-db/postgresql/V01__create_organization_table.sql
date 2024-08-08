CREATE TABLE organization
(
    id           varchar(25)  NOT NULL,
    company      varchar(125) NOT NULL,
    contact      varchar(125) NOT NULL,
    email        varchar(125) NOT NULL,
    address      varchar(255)          DEFAULT NULL,
    zip          varchar(25)           DEFAULT NULL,
    country      varchar(125)          DEFAULT NULL,
    deleted      boolean      NOT NULL DEFAULT false,
    date_created timestamp    NOT NULL,
    last_updated timestamp    NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE license
(
    id              varchar(25) NOT NULL,
    features        varchar(255) NOT NULL,
    product         varchar(75) NOT NULL,
    contact         varchar(125) NOT NULL,
    email           varchar(125) NOT NULL,
    secret          bytea        NOT NULL,
    organization_id varchar(25)  NOT NULL,
    activation      timestamp    NOT NULL,
    expiration      timestamp    NOT NULL,
    date_created    timestamp    NOT NULL,
    last_updated    timestamp    NOT NULL,
    last_accessed   timestamp,
    last_access_ip  varchar(255)          DEFAULT NULL,
    access_count    int          NOT NULL DEFAULT 0,
    suspended       boolean      NOT NULL DEFAULT false,
    deleted         boolean      NOT NULL DEFAULT false,
    expired         boolean      NOT NULL DEFAULT false,
    PRIMARY KEY (id),
    FOREIGN KEY (organization_id) REFERENCES organization(id)
);
