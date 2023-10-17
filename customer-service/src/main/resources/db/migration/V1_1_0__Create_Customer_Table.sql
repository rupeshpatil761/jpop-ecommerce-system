CREATE TABLE IF NOT EXISTS CUSTOMER (
id serial PRIMARY KEY,
first_name varchar(100),
email varchar(50) UNIQUE NOT NULL
);