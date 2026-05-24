package smarthome.database;

import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final Properties properties = new Properties();
    private static String dbType;

    static {
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Не удалось найти файл конфигурации config.properties");
            }
            properties.load(input);
            dbType = properties.getProperty("db.type");
            String driverClass = properties.getProperty(dbType + ".driver");
            Class.forName(driverClass);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Ошибка инициализации подключения к базе данных", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = properties.getProperty(dbType + ".url");
        String user = properties.getProperty(dbType + ".user");
        String pass = properties.getProperty(dbType + ".password");
        return DriverManager.getConnection(url, user, pass);
    }

    public static String getDbType() {
        return dbType;
    }
}