CREATE TABLE users (
	client_id 	varchar(255) primary key,
	client_key 	varchar(7) not null
);

insert into users ("client_id", "client_key") values ('alexey', 'abcdefg'), ('zhenya', '1234567');

CREATE TABLE tgs_users (
	client_id 	varchar(255) primary key,
	client_key 	varchar(7) not null
);

insert into tgs_users ("client_id", "client_key") values ('alexey', 'gfedcba'), ('zhenya', '7654321');

