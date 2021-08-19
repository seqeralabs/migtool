create table if not exists MIGTOOL_HISTORY
(
    id serial,
    rank int not null,
    script varchar(250) not null,
    checksum varchar(64) not null,
    created_on timestamp not null,
    execution_time int,
    primary key (id)
);
