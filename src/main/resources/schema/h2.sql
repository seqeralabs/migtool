create table MIGTOOL_HISTORY
(
    `id` int not null auto_increment,
    `rank` int not null,
    `script` varchar(250) not null,
    `checksum` varchar(64) not null,
    `created_on` timestamp not null,
    `execution_time` int,
    primary key (id)
);
