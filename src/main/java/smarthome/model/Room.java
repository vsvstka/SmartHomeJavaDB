package smarthome.model;

public class Room {
    private int id;
    private String name;
    private int floor;

    public Room(int id, String name, int floor) {
        this.id = id;
        this.name = name;
        this.floor = floor;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getFloor() { return floor; }

    @Override
    public String toString() {
        return name + " (" + floor + " этаж)";
    }
}