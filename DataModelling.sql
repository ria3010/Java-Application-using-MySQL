create database assignment_1_TDM;
use assignment_1_TDM;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

create table Users(username VARCHAR(100), password VARCHAR(100) NOT NULL, first_name VARCHAR(100), last_name VARCHAR(100), 
PRIMARY KEY(username));

create table Products(product_id int NOT NULL AUTO_INCREMENT , product_name VARCHAR(100), description VARCHAR(500), price FLOAT, numItems int, PRIMARY KEY (product_id));
ALTER TABLE Products AUTO_INCREMENT = 1000; # the first 4-digit product number

create table Orders(order_id int NOT NULL AUTO_INCREMENT , quantity int, order_date DATE, product_id VARCHAR(10) references 
Products(product_id),purchasing_user VARCHAR(100) references Users(username),PRIMARY KEY(order_id));

create table Reviews(description VARCHAR(500), rating ENUM('1','2','3','4','5') , review_date DATE, username_fk VARCHAR(100) , product_id int NOT NULL references Products(product_id), foreign key(username_fk) references Users(username),primary key(username_fk,product_id));

