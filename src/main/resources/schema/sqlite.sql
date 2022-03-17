create table MIGTOOL_HISTORY
(
    `id` integer primary key autoincrement,
    `rank` INTEGER not null,
    `script` varchar(250) not null,
    `checksum` varchar(64) not null,
    `created_on` INTEGER not null,
    `execution_time` INTEGER

);
