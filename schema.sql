-- Main table of guilds

create table public.guilds
(
    guild_id bigint not null
        constraint guilds_pk
            primary key
);


-- React roles

create table react_roles
(
    id         bigint generated always as identity (minvalue 1000),
    guild_id   bigint,
    channel_id bigint               not null,
    message_id bigint               not null,
    role_id    bigint,
    emoji_id   bigint               not null,
    enabled    boolean default true not null,
    constraint react_roles_pk
        primary key (id),
    constraint react_roles_guilds_guild_id_fk
        foreign key (guild_id) references guilds
            on update cascade on delete cascade
);

create index react_roles_emoji_id_index
    on react_roles (emoji_id);

create index react_roles_guild_id_index
    on react_roles (guild_id);

create unique index react_roles_message_id_uindex
    on react_roles (message_id);

comment on index react_roles_message_id_uindex is 'There can only be a single react role per message';

create index react_roles_role_id_index
    on react_roles (role_id);


-- React roles logging

create table react_roles_logging
(
    id         bigint generated always as identity (minvalue 1000),
    guild_id   bigint               not null,
    channel_id bigint               not null,
    mention    boolean default true not null,
    constraint react_roles_logging_pk
        primary key (id),
    constraint react_roles_logging_guilds_guild_id_fk
        foreign key (guild_id) references guilds
            on update cascade on delete cascade
);

create index react_roles_logging_channel_id_index
    on react_roles_logging (channel_id);

create unique index react_roles_logging_guild_id_uindex
    on react_roles_logging (guild_id);







