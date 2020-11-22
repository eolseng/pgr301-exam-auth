create table authorities
(
    username  varchar(255) not null,
    authority varchar(255)
);
create table kegs
(
    id             serial       not null,
    capacity       int4         not null check (capacity <= 600 AND capacity >= 100),
    current_volume int4         not null check (current_volume >= 0 AND current_volume <= 600),
    owner_username varchar(255) not null,
    primary key (id)
);
create table mug
(
    id             serial       not null,
    capacity       int4         not null,
    current_volume int4         not null check (current_volume <= 6 AND current_volume >= 0),
    owner_username varchar(255) not null,
    primary key (id)
);
create table users
(
    username varchar(255) not null,
    enabled  boolean      not null,
    password varchar(255),
    primary key (username)
);
alter table if exists authorities
    add constraint FKhjuy9y4fd8v5m3klig05ktofg foreign key (username) references users;
alter table if exists kegs
    add constraint FKrgatvjap4mjtm9bgah38wcjp2 foreign key (owner_username) references users;
alter table if exists mug
    add constraint FK9i588y0kkx7yek6wd4955407n foreign key (owner_username) references users;
