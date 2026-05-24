CREATE DATABASE smarthome;
GO
USE smarthome;
GO

-- Создание таблиц
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) UNIQUE NOT NULL,
    password NVARCHAR(255) NOT NULL,
    role NVARCHAR(20) NOT NULL CHECK (role IN ('Admin', 'User', 'Guest'))
);

CREATE TABLE rooms (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    floor INT NOT NULL DEFAULT 1 CHECK (floor > 0 AND floor <= 10) 
);

CREATE TABLE device_types (
    id INT IDENTITY(1,1) PRIMARY KEY,
    type_name NVARCHAR(50) NOT NULL,
    manufacturer NVARCHAR(100),
    protocol NVARCHAR(30)
);

CREATE TABLE devices (
    id INT IDENTITY(1,1) PRIMARY KEY,
    room_id INT FOREIGN KEY REFERENCES rooms(id) ON DELETE SET NULL,
    type_id INT FOREIGN KEY REFERENCES device_types(id) ON DELETE NO ACTION,
    name NVARCHAR(100) NOT NULL,
    status NVARCHAR(50) NOT NULL,
    ip_address NVARCHAR(15) UNIQUE,
   
    CONSTRAINT chk_ip_format CHECK (
        ip_address NOT LIKE '%[^0-9.]%' AND 
        ip_address NOT LIKE '%[0-9][0-9][0-9][0-9]%' AND 
        PARSENAME(ip_address, 4) IS NOT NULL
    )
);

CREATE TABLE events (
    id INT IDENTITY(1,1) PRIMARY KEY,
    device_id INT FOREIGN KEY REFERENCES devices(id) ON DELETE CASCADE,
    timestamp DATETIME2 NOT NULL DEFAULT GETDATE(),
    event_type NVARCHAR(50) NOT NULL,
    description NVARCHAR(MAX)
);

CREATE TABLE scenarios (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE TABLE scenarios_devices (
    scenario_id INT FOREIGN KEY REFERENCES scenarios(id) ON DELETE CASCADE,
    device_id INT FOREIGN KEY REFERENCES devices(id) ON DELETE CASCADE,
    target_state NVARCHAR(50) NOT NULL,
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
(N'Гостиная', 1), 
(N'Кухня', 1), 
(N'Спальня', 2), 
(N'Ванная', 1), 
(N'Прихожая', 1);

INSERT INTO device_types (type_name, manufacturer, protocol) VALUES 
(N'Датчик движения', 'Manuf1', 'Zigbee'), 
(N'Умная лампа', 'Manuf2', 'Zigbee'), 
(N'Термостат', 'Manuf1', 'Wi-Fi'), 
(N'Умная розетка', 'Manuf3', 'Wi-Fi'), 
(N'Датчик протечки', 'Xiaomi', 'Zigbee');

INSERT INTO devices (room_id, type_id, name, status, ip_address) VALUES 
(1, 2, N'Люстра Гостиная', 'Online', '192.168.1.15'), 
(2, 4, N'Чайник Кухня', 'Offline', '192.168.1.16'), 
(3, 3, N'Климат Спальня', 'Online', '192.168.1.17'), 
(4, 5, N'Датчик Ванная', 'Online', '192.168.1.18'), 
(5, 1, N'Движение Прихожая', 'Online', '192.168.1.19');

INSERT INTO events (device_id, event_type, description) VALUES 
(5, 'Motion Detected', N'Зафиксировано движение в коридоре'), 
(1, 'State Changed', N'Включен свет на 50% яркости'), 
(3, 'Temp Report', N'Текущая температура 22.5 C'), 
(4, 'Status Check', N'Датчик протечки активен, сухо'), 
(2, 'Disconnect', N'Устройство потеряло сеть');

INSERT INTO scenarios (name, is_active) VALUES 
(N'Я ушел', 1), 
(N'Вечерний просмотр кино', 1), 
(N'Доброе утро', 1), 
(N'Тревога: Протечка', 1), 
(N'Энергосбережение ночь', 0);

INSERT INTO scenarios_devices (scenario_id, device_id, target_state) VALUES 
(1, 1, 'Turn Off'), 
(1, 2, 'Turn Off'), 
(2, 1, 'Dim to 10%'), 
(3, 4, 'Activate'), 
(4, 4, 'Activate Valve Shutoff'), 
(5, 3, 'Set Temp 18C');