package smarthome.database;

import smarthome.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SmartHomeDAO {

    // 1. Авторизация пользователей по имени и паролю
    public String authenticate(String username, String password) throws SQLException {
        String query = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        }
        return null;
    }

    // 2. Получение списка комнат
    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String query = "SELECT id, name, floor FROM rooms ORDER BY floor, name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new Room(rs.getInt("id"), rs.getString("name"), rs.getInt("floor")));
            }
        }
        return rooms;
    }

    // Добавление комнаты с проверкой на дубликат имени и этажа + логирование
    public void addRoom(String name, int floor, String initiator) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM rooms WHERE name = ? AND floor = ?";
        String insertQuery = "INSERT INTO rooms (name, floor) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // ШАГ 1: Проверяем, существует ли уже такая комната на данном этаже
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, name);
                    checkStmt.setInt(2, floor);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Если комната найдена, выбрасываем ошибку, которая отобразится в интерфейсе
                            throw new SQLException("Комната с названием '" + name + "' уже существует на " + floor + " этаже!");
                        }
                    }
                }

                // ШАГ 2: Если проверки пройдены, добавляем комнату
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setString(1, name);
                    stmt.setInt(2, floor);
                    stmt.executeUpdate();
                }

                // Логируем действие в журнал событий
                logSystemEvent(conn, null, "Room Created",
                        "Комната '" + name + "' (этаж " + floor + ") успешно добавлена. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // Удаление комнаты с логированием
    public void deleteRoom(int roomId, String roomName, String initiator) throws SQLException {
        String query = "DELETE FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, roomId);
                    stmt.executeUpdate();
                }
                // Логируем удаление
                logSystemEvent(conn, null, "Room Deleted",
                        "Комната '" + roomName + "' (ID: " + roomId + ") удалена. Все устройства в ней переведены в статус 'Без комнаты'. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // 3. Получение списка девайсов с комнатами и типами
    public List<Device> getAllDevices() throws SQLException {
        List<Device> devices = new ArrayList<>();
        String query = "SELECT d.id, d.room_id, d.type_id, d.name, d.status, d.ip_address, " +
                "r.name AS room_name, t.type_name AS type_name " +
                "FROM devices d " +
                "LEFT JOIN rooms r ON d.room_id = r.id " +
                "LEFT JOIN device_types t ON d.type_id = t.id " +
                "ORDER BY d.name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                devices.add(new Device(
                        rs.getInt("id"),
                        rs.getInt("room_id"),
                        rs.getInt("type_id"),
                        rs.getString("name"),
                        rs.getString("status"),
                        rs.getString("ip_address"),
                        rs.getString("room_name"),
                        rs.getString("type_name")
                ));
            }
        }
        return devices;
    }

    // Добавление девайса с логированием
    public void addDevice(String name, int roomId, int typeId, String status, String ip, String initiator) throws SQLException {
        String query = "INSERT INTO devices (name, room_id, type_id, status, ip_address) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deviceId = -1;
                try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name);
                    stmt.setInt(2, roomId);
                    stmt.setInt(3, typeId);
                    stmt.setString(4, status);
                    if (ip == null || ip.trim().isEmpty()) {
                        stmt.setNull(5, Types.VARCHAR);
                    } else {
                        stmt.setString(5, ip);
                    }
                    stmt.executeUpdate();

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            deviceId = generatedKeys.getInt(1);
                        }
                    }
                }

                // Логируем в журнал
                logSystemEvent(conn, deviceId > 0 ? deviceId : null, "Device Registered",
                        "Устройство '" + name + "' зарегистрировано в системе. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // Удаление девайса с логированием
    public void deleteDevice(int deviceId, String deviceName, String initiator) throws SQLException {
        String query = "DELETE FROM devices WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, deviceId);
                    stmt.executeUpdate();
                }
                logSystemEvent(conn, null, "Device Removed",
                        "Устройство '" + deviceName + "' (ID: " + deviceId + ") навсегда удалено из конфигурации дома. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // Получение списка типов девайсов
    public List<String> getDeviceTypesList() throws SQLException {
        List<String> list = new ArrayList<>();
        String query = "SELECT id, type_name FROM device_types";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getInt("id") + " - " + rs.getString("type_name"));
            }
        }
        return list;
    }

    // Добавление нового типа устройства
    public void addDeviceType(String typeName, String manufacturer, String protocol, String initiator) throws SQLException {
        String query = "INSERT INTO device_types (type_name, manufacturer, protocol) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, typeName);
                    stmt.setString(2, manufacturer);
                    stmt.setString(3, protocol);
                    stmt.executeUpdate();
                }
                logSystemEvent(conn, null, "Type Created",
                        "Добавлен новый тип устройства: '" + typeName + "'. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // 4. Работа со сценариями
    public List<Scenario> getAllScenarios() throws SQLException {
        List<Scenario> scenarios = new ArrayList<>();
        String query = "SELECT id, name, is_active, created_at FROM scenarios ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                scenarios.add(new Scenario(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBoolean("is_active"),
                        rs.getTimestamp("created_at")
                ));
            }
        }
        return scenarios;
    }

    // Добавление сценария
    public void addScenario(String name, boolean isActive, String initiator) throws SQLException {
        String query = "INSERT INTO scenarios (name, is_active) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, name);
                    if (DatabaseManager.getDbType().equals("sqlserver")) {
                        stmt.setInt(2, isActive ? 1 : 0);
                    } else {
                        stmt.setBoolean(2, isActive);
                    }
                    stmt.executeUpdate();
                }
                logSystemEvent(conn, null, "Scenario Created",
                        "Сценарий '" + name + "' успешно создан (Активен: " + isActive + "). Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // Переключение активности сценария с автоматическим логом действий пользователя
    public void toggleScenarioActive(int scenarioId, String scenarioName, boolean isActive, String initiator) throws SQLException {
        String query = "UPDATE scenarios SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    if (DatabaseManager.getDbType().equals("sqlserver")) {
                        stmt.setInt(1, isActive ? 1 : 0);
                    } else {
                        stmt.setBoolean(1, isActive);
                    }
                    stmt.setInt(2, scenarioId);
                    stmt.executeUpdate();
                }
                logSystemEvent(conn, null, "Scenario Toggled",
                        "Статус сценария '" + scenarioName + "' изменен на " + (isActive ? "АКТИВЕН" : "ДЕАКТИВИРОВАН") + ". Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // Удаление сценария
    public void deleteScenario(int scenarioId, String scenarioName, String initiator) throws SQLException {
        String query = "DELETE FROM scenarios WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, scenarioId);
                    stmt.executeUpdate();
                }
                logSystemEvent(conn, null, "Scenario Deleted",
                        "Сценарий '" + scenarioName + "' удален из автоматизации. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // 5. Связывание сценария и устройства (scenarios_devices)
    public void addDeviceToScenario(int scenarioId, int deviceId, String targetState, String initiator) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM scenarios_devices WHERE scenario_id = ? AND device_id = ?";
        String insertQuery = "INSERT INTO scenarios_devices (scenario_id, device_id, target_state) VALUES (?, ?, ?)";
        String updateQuery = "UPDATE scenarios_devices SET target_state = ? WHERE scenario_id = ? AND device_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean exists = false;
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, scenarioId);
                    checkStmt.setInt(2, deviceId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            exists = true;
                        }
                    }
                }

                if (exists) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, targetState);
                        updateStmt.setInt(2, scenarioId);
                        updateStmt.setInt(3, deviceId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, scenarioId);
                        insertStmt.setInt(2, deviceId);
                        insertStmt.setString(3, targetState);
                        insertStmt.executeUpdate();
                    }
                }

                String sName = "ID: " + scenarioId;
                String dName = "ID: " + deviceId;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM scenarios WHERE id = ?")) {
                    stmt.setInt(1, scenarioId);
                    try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) sName = rs.getString("name"); }
                }
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM devices WHERE id = ?")) {
                    stmt.setInt(1, deviceId);
                    try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) dName = rs.getString("name"); }
                }

                logSystemEvent(conn, deviceId, "Scenario Link",
                        "Устройство '" + dName + "' добавлено в сценарий '" + sName + "' с целевым действием: '" + targetState + "'. Инициатор: " + initiator);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // Получение связей сценария и устройств для отображения в таблице/списке
    public List<String> getDevicesInScenario(int scenarioId) throws SQLException {
        List<String> list = new ArrayList<>();
        String query = "SELECT d.name, sd.target_state FROM scenarios_devices sd " +
                "JOIN devices d ON sd.device_id = d.id " +
                "WHERE sd.scenario_id = ? ORDER BY d.name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, scenarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("name") + " -> Действие: " + rs.getString("target_state"));
                }
            }
        }
        return list;
    }

    // 6. Журнал событий
    public List<Event> getAllEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String query = "SELECT e.id, e.device_id, d.name AS device_name, e.timestamp, e.event_type, e.description " +
                "FROM events e " +
                "LEFT JOIN devices d ON e.device_id = d.id " +
                "ORDER BY e.timestamp DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int devId = rs.getInt("device_id");
                Integer nullableDevId = rs.wasNull() ? null : devId;
                events.add(new Event(
                        rs.getInt("id"),
                        nullableDevId,
                        rs.getString("device_name"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("event_type"),
                        rs.getString("description")
                ));
            }
        }
        return events;
    }

    // Вспомогательный метод записи логов (внутри существующих транзакций)
    private void logSystemEvent(Connection conn, Integer deviceId, String eventType, String description) throws SQLException {
        String query = "INSERT INTO events (device_id, event_type, description) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            if (deviceId == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, deviceId);
            }
            stmt.setString(2, eventType);
            stmt.setString(3, description);
            stmt.executeUpdate();
        }
    }
}