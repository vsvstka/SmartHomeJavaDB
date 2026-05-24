package smarthome.model;

public class Device {
    private int id;
    private int roomId;
    private int typeId;
    private String name;
    private String status;
    private String ipAddress;
    private String roomName;
    private String typeName;

    public Device(int id, int roomId, int typeId, String name, String status, String ipAddress, String roomName, String typeName) {
        this.id = id;
        this.roomId = roomId;
        this.typeId = typeId;
        this.name = name;
        this.status = status;
        this.ipAddress = ipAddress;
        this.roomName = roomName;
        this.typeName = typeName;
    }

    public int getId() { return id; }
    public int getRoomId() { return roomId; }
    public int getTypeId() { return typeId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getIpAddress() { return ipAddress; }
    public String getRoomName() { return roomName; }
    public String getTypeName() { return typeName; }

    @Override
    public String toString() {
        return name + " [" + status + "]";
    }
}