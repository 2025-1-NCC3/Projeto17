create database UberProject;
use UberProject;

create table usuario(
id int primary key auto_increment,
nome varchar(255) not null,
senha varchar(50) not null,
email varchar(255) not null unique,
telefone varchar(50) not null unique
);

create table viagem(
id int primary key auto_increment,
embarque varchar(255) not null,
destino varchar(255) not null,
idUsuario int,
foreign key (idUsuario) references usuario(id),
idMotorista int,
foreign key (idMotorista) references motorista(id)
);

create table motorista(
id int primary key auto_increment,
nome varchar(255) not null,
senha varchar(50) not null,
email varchar(255) not null unique,
telefone varchar(50) not null unique
);

create table alerta(
id int primary key auto_increment,
tipoAlerta varchar(255) not null,
latitude varchar(25) not null,
longitude varchar(25) not null,
dataOcorrencia datetime default current_timestamp,
idUsuario int,
foreign key (idUsuario) references Usuario(id)
);

DELIMITER $$

CREATE EVENT delete_old_alerts
ON SCHEDULE EVERY 48 HOUR
DO
BEGIN
    DELETE FROM alerta
    WHERE dataOcorrencia < NOW() - INTERVAL 24 HOUR;
END $$

DELIMITER ;

create table logErros(
id int primary key auto_increment,
erro varchar(255) not null);