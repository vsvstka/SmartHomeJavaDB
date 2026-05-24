package smarthome.model;

import java.sql.Timestamp;

public class Scenario {
    private int id;
    private String name;
    private boolean isActive;
    private Timestamp createdAt;

    public Scenario(int id, String name, boolean isActive, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public Timestamp getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return name + (isActive ? " (Активен)" : " (Выключен)");
    }
}