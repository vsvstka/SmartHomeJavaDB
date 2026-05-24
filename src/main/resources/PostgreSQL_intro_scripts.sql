SET search_path TO svistunova_va;

-- Удаляем таблицы, если они уже были созданы ранее (для чистого перезапуска)
DROP TABLE IF EXISTS scenarios_devices CASCADE;
DROP TABLE IF EXISTS scenarios CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS devices CASCADE;
DROP TABLE IF EXISTS device_types CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Создание таблиц 
CREATE TABLE users (
id SERIAL PRIMARY KEY,
username VARCHAR(50) UNIQUE NOT NULL,
password VARCHAR(255) NOT NULL,
role VARCHAR(20) NOT NULL CHECK (role IN ('Admin', 'User', 'Guest'))
);

CREATE TABLE rooms (
id SERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
floor INT NOT NULL DEFAULT 1
);

CREATE TABLE device_types (
id SERIAL PRIMARY KEY,
type_name VARCHAR(50) NOT NULL,
manufacturer VARCHAR(100),
protocol VARCHAR(30)
);

CREATE TABLE devices (
id SERIAL PRIMARY KEY,
room_id INT REFERENCES rooms(id) ON DELETE SET NULL,
type_id INT REFERENCES device_types(id) ON DELETE RESTRICT,
name VARCHAR(100) NOT NULL,
status VARCHAR(50) NOT NULL,
ip_address VARCHAR(15) UNIQUE
);

CREATE TABLE events (
id SERIAL PRIMARY KEY,
device_id INT REFERENCES devices(id) ON DELETE CASCADE,
timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
event_type VARCHAR(50) NOT NULL,
description TEXT
);

CREATE TABLE scenarios (
id SERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scenarios_devices (
scenario_id INT REFERENCES scenarios(id) ON DELETE CASCADE,
device_id INT REFERENCES devices(id) ON DELETE CASCADE,
target_state VARCHAR(50) NOT NULL,
PRIMARY KEY (scenario_id, device_id)
);

-- Наполнение тестовыми данными
INSERT INTO users (username, password, role) VALUES
('admin', 'admin', 'Admin'),
('maria_user', 'maria', 'User'),
('guest_one', 'guest', 'Guest'),
('kid_user', 'kid', 'User'),
('service', 'admin', 'Admin');

INSERT INTO rooms (name, floor) VALUES
('Гостиная', 1),
('Кухня', 1),
('Спальня', 2),
('Ванная', 1),
('Прихожая', 1);

INSERT INTO device_types (type_name, manufacturer, protocol) VALUES
('Датчик движения', 'Manuf1', 'Zigbee'),
('Умная лампа', 'Manuf2', 'Zigbee'),
('Термостат', 'Manuf1', 'Wi-Fi'),
('Умная розетка', 'Manuf3', 'Wi-Fi'),
('Датчик протечки', 'Xiaomi', 'Zigbee');

INSERT INTO devices (room_id, type_id, name, status, ip_address) VALUES
(1, 2, 'Люстра Гостиная', 'Online', '192.168.1.15'),
(2, 4, 'Чайник Кухня', 'Offline', '192.168.1.16'),
(3, 3, 'Климат Спальня', 'Online', '192.168.1.17'),
(4, 5, 'Датчик Ванная', 'Online', '192.168.1.18'),
(5, 1, 'Движение Прихожая', 'Online', '192.168.1.19');

INSERT INTO events (device_id, event_type, description) VALUES
(5, 'Motion Detected', 'Зафиксировано движение в коридоре'),
(1, 'State Changed', 'Включен свет на 50% яркости'),
(3, 'Temp Report', 'Текущая температура 22.5 C'),
(4, 'Status Check', 'Датчик протечки активен, сухо'),
(2, 'Disconnect', 'Устройство потеряло сеть');

INSERT INTO scenarios (name, is_active) VALUES
('Я ушел', TRUE),
('Вечерний просмотр кино', TRUE),
('Доброе утро', TRUE),
('Тревога: Протечка', TRUE),
('Энергосбережение ночь', FALSE);

INSERT INTO scenarios_devices (scenario_id, device_id, target_state) VALUES
(1, 1, 'Turn Off'),
(1, 2, 'Turn Off'),
(2, 1, 'Dim to 10%'),
(3, 4, 'Activate'),
(4, 4, 'Activate Valve Shutoff'),
(5, 3, 'Set Temp 18C');