package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 15 房拓扑：双向出口、主地图可达、无孤立死胡同（上锁房除外）。
 */
class WorldTopologyTest
{
    @BeforeEach
    void buildWorld()
    {
        WorldFactory.build("拓扑测试");
    }

    @Test
    void everyExitHasReverseLink()
    {
        for (Room room : WorldFactory.getAllRooms().values()) {
            for (String dir : room.getExitDirections()) {
                Room neighbor = room.getExit(dir);
                assertNotNull(neighbor, room.getRoomId() + " -> " + dir + " is null");
                String opposite = oppositeDirection(dir);
                Room back = neighbor.getExit(opposite);
                assertNotNull(back,
                    neighbor.getRoomId() + " missing reverse exit " + opposite
                        + " to " + room.getRoomId());
                assertEquals(room.getRoomId(), back.getRoomId(),
                    "reverse link from " + neighbor.getRoomId() + " points to wrong room");
            }
        }
    }

    @Test
    void allRoomsReachableFromOutside()
    {
        Set<String> reachable = bfs("outside");
        assertEquals(15, reachable.size(),
            "unreachable: " + diff(allRoomIds(), reachable));
    }

    @Test
    void allRoomsCanReturnToOutside()
    {
        for (String roomId : allRoomIds()) {
            Set<String> reachable = bfs(roomId);
            assertTrue(reachable.contains("outside"),
                "cannot return to outside from " + roomId);
        }
    }

    @Test
    void outsideNorthConnectsTheatre()
    {
        assertEquals("theatre",
            WorldFactory.getRoom("outside").getExit("north").getRoomId());
        assertEquals("outside",
            WorldFactory.getRoom("theatre").getExit("south").getRoomId());
    }

    @Test
    void outsideSouthConnectsLab()
    {
        assertEquals("lab",
            WorldFactory.getRoom("outside").getExit("south").getRoomId());
        assertEquals("outside",
            WorldFactory.getRoom("lab").getExit("north").getRoomId());
    }

    @Test
    void gardenReachableViaPub()
    {
        Set<String> reachable = bfs("outside");
        assertTrue(reachable.contains("garden"));
    }

    @Test
    void armoryReachableWithoutGuardKey()
    {
        Set<String> reachable = bfs("outside");
        assertTrue(reachable.contains("armory"));
        assertTrue(reachable.contains("forge"));
    }

    private static Set<String> bfs(String startId)
    {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(startId);
        visited.add(startId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            Room room = WorldFactory.getRoom(id);
            for (String dir : room.getExitDirections()) {
                Room next = room.getExit(dir);
                if (next != null && visited.add(next.getRoomId())) {
                    queue.add(next.getRoomId());
                }
            }
        }
        return visited;
    }

    private static List<String> allRoomIds()
    {
        return new ArrayList<>(WorldFactory.getAllRooms().keySet());
    }

    private static Set<String> diff(List<String> all, Set<String> subset)
    {
        Set<String> missing = new HashSet<>(all);
        missing.removeAll(subset);
        return missing;
    }

    private static String oppositeDirection(String dir)
    {
        Map<String, String> opposites = new HashMap<>();
        opposites.put("north", "south");
        opposites.put("south", "north");
        opposites.put("east", "west");
        opposites.put("west", "east");
        return opposites.get(dir);
    }
}
