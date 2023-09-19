-- -- ### Создание роли базы данных ###
CREATE ROLE dev WITH LOGIN PASSWORD 'dev';

-- -- ### Создание базы данных ###
CREATE DATABASE dev_mem OWNER dev;
