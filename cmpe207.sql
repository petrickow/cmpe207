USE cmpe207;
DROP TABLE IF EXISTS messages;

CREATE TABLE messages(
	msg_id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
	uname char(20) NOT NULL, 
	sender char(20) NOT NULL, 
	message TEXT NOT NULL);

DROP TABLE IF EXISTS users;

CREATE TABLE users(
	uname char(20) unique NOT NULL);