package cn.edu.whut.sept.zuul.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Room
{
    private final String roomId;
    private String description;
    private String lockId;
    private boolean teleport;
    private RoomScene scene;
    private final Map<String, Room> exits;
    private final List<Item> items;

    public Room(String roomId, String description)
    {
        this.roomId = roomId;
        this.description = description;
        this.exits = new HashMap<>();
        this.items = new ArrayList<>();
    }

    public String getRoomId()
    {
        return roomId;
    }

    public String getDescription()
    {
        return description;
    }

    public String getLockId()
    {
        return lockId;
    }

    public void setLockId(String lockId)
    {
        this.lockId = lockId;
    }

    public boolean isTeleport()
    {
        return teleport;
    }

    public void setTeleport(boolean teleport)
    {
        this.teleport = teleport;
    }

    public RoomScene getScene()
    {
        return scene;
    }

    public void setScene(RoomScene scene)
    {
        this.scene = scene;
    }

    public void setExit(String direction, Room neighbor)
    {
        exits.put(direction, neighbor);
    }

    public Room getExit(String direction)
    {
        return exits.get(direction);
    }

    public Set<String> getExitDirections()
    {
        return exits.keySet();
    }

    public List<Item> getItems()
    {
        return items;
    }

    public void addItem(Item item)
    {
        items.add(item);
    }

    public Item removeItem(String itemId)
    {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItemId().equals(itemId)) {
                return items.remove(i);
            }
        }
        return null;
    }

    public int totalItemWeight()
    {
        int total = 0;
        for (Item item : items) {
            total += item.getWeight();
        }
        return total;
    }

    public String getLongDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(description).append(".\n");
        sb.append("Exits:");
        for (String exit : exits.keySet()) {
            sb.append(" ").append(exit);
        }
        if (!items.isEmpty()) {
            sb.append("\nItems here:");
            for (Item item : items) {
                sb.append(" ").append(item.getName());
            }
        }
        return sb.toString();
    }
}
