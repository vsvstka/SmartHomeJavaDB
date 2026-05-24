package smarthome.model;

import java.sql.Timestamp;

public class Event {
    private int id;
    private Integer deviceId; // Может быть null, если событие не связано с конкретным девайсом напрямую
    private String deviceName;
    private Timestamp timestamp;
    private String eventType;
    private String description;

    public Event(int id, Integer deviceId, String deviceName, Timestamp timestamp, String eventType, String description) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.description = description;
    }

    public int getId() { return id; }
    public Integer getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName == null ? "Система/Пользователь" : deviceName; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
}