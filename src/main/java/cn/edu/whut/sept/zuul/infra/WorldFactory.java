package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 集中构建游戏世界。第一步：5 个原版房间 + 场景元数据骨架。
 */
public final class WorldFactory
{
    private static final Map<String, Room> ROOMS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private WorldFactory()
    {
    }

    public static Room build(String playerName)
    {
        ROOMS.clear();
        buildPrototypeWorld();
        return ROOMS.get("outside");
    }

    /** 兼容旧调用。 */
    public static Room build()
    {
        return build("编年史者");
    }

    public static Room getRoom(String roomId)
    {
        return ROOMS.get(roomId);
    }

    public static Room randomRoomExcept(String excludeRoomId)
    {
        List<Room> candidates = new ArrayList<>();
        for (Room room : ROOMS.values()) {
            if (!room.getRoomId().equals(excludeRoomId)) {
                candidates.add(room);
            }
        }
        if (candidates.isEmpty()) {
            return ROOMS.get(excludeRoomId);
        }
        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    public static Map<String, Room> getAllRooms()
    {
        return Collections.unmodifiableMap(ROOMS);
    }

    private static void buildPrototypeWorld()
    {
        Room outside = createRoom("outside", "outside the main entrance of the university");
        Room theatre = createRoom("theatre", "in a lecture theater");
        Room pub = createRoom("pub", "in the campus pub");
        Room lab = createRoom("lab", "in a computing lab");
        Room office = createRoom("office", "in the computing admin office");

        link(outside, "east", theatre);
        link(outside, "south", lab);
        link(outside, "west", pub);

        link(theatre, "west", outside);
        link(pub, "east", outside);
        link(lab, "north", outside);
        link(lab, "east", office);
        link(office, "west", lab);

        outside.addItem(new Item("welcome-note", "note", "A crumpled welcome note.", 1, null));
    }

    private static Room createRoom(String roomId, String description)
    {
        Room room = new Room(roomId, description);
        RoomScene scene = new RoomScene("maps/" + roomId + ".tmx");
        scene.addSpawn(Direction.DEFAULT, 2f, 2f);
        scene.addSpawn(Direction.NORTH, 2f, 1f);
        scene.addSpawn(Direction.SOUTH, 2f, 4f);
        scene.addSpawn(Direction.EAST, 1f, 2f);
        scene.addSpawn(Direction.WEST, 4f, 2f);
        room.setScene(scene);
        ROOMS.put(roomId, room);
        return room;
    }

    private static void link(Room from, String direction, Room to)
    {
        from.setExit(direction, to);
    }
}
