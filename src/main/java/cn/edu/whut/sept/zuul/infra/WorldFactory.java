package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * 集中构建游戏世界。当前 15 房，含完整拓扑、物品放置、cookie 随机注入。
 */
public final class WorldFactory
{
    private static final Logger LOG = GameLogger.get();
    private static final Map<String, Room> ROOMS = new HashMap<>();
    private static final Random RANDOM = new Random();

    /** cookie 候选房间 */
    private static final List<String> COOKIE_CANDIDATES =
        Arrays.asList("cellar", "library", "hidden-shrine");

    private WorldFactory()
    {
    }

    public static Room build(String playerName)
    {
        ROOMS.clear();
        buildWorld();
        placeMagicCookie();
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

    /** 随机传送禁止落入的房间（进入后可能无法离开）。 */
    private static final List<String> TELEPORT_EXCLUDED =
        Collections.singletonList("throne-hall");

    public static Room randomRoomExcept(String excludeRoomId)
    {
        List<Room> candidates = new ArrayList<>();
        for (Room room : ROOMS.values()) {
            if (!room.getRoomId().equals(excludeRoomId)
                && !TELEPORT_EXCLUDED.contains(room.getRoomId())
                && room.getLockId() == null) {
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

    // ======================== 世界构建 ========================

    private static void buildWorld()
    {
        // -- outside: 主门外广场 --
        Room outside = createRoom("outside", "outside the main entrance of the university");
        setSpawns(outside, 15,8, 15,3, 15,13, 27,8, 3,8);
        outside.addItem(createItem("welcome-note", "欢迎纸条",
            "一张皱巴巴的欢迎纸条。", 1, null));

        // -- theatre: 讲堂 --
        Room theatre = createRoom("theatre", "in a lecture theater");
        setSpawns(theatre, 15,8, 15,2, 15,13, 27,8, 3,8);
        theatre.addItem(createItem("torch", "火把",
            "一支摇曳的火把。", 3, "light"));

        // -- pub: 酒馆 --
        Room pub = createRoom("pub", "in the campus pub");
        setSpawns(pub, 15,8, 15,2, 15,13, 27,8, 3,8);
        pub.addItem(createItem("ale-mug", "半杯酒",
            "半杯残酒。", 2, null));

        // -- lab: 机房 --
        Room lab = createRoom("lab", "in a computing lab");
        setSpawns(lab, 15,8, 15,3, 15,14, 27,8, 3,8);
        lab.addItem(createItem("key-vault", "锈钥匙",
            "一把锈迹斑斑的钥匙，刻着「金库」。", 1, "unlock:vault-door"));

        // -- office: 行政办公室 --
        Room office = createRoom("office", "in the computing admin office");
        setSpawns(office, 15,8, 15,8, 15,8, 15,8, 3,8);
        office.addItem(createItem("key-guard", "铁钥匙",
            "一把沉重的铁钥匙，标着「守卫之门」。", 2, "unlock:guard-gate"));

        // -- library: 图书馆 --
        Room library = createRoom("library", "in an ancient library");
        setSpawns(library, 15,8, 15,3, 15,14, 27,8, 3,8);
        library.addItem(createItem("ancient-tome", "古书",
            "一本厚重古书，皮面已经龟裂。", 15, "lore"));

        // -- cellar: 地窖 --
        Room cellar = createRoom("cellar", "in a dark, damp cellar");
        setSpawns(cellar, 15,8, 15,3, 15,13, 27,8, 3,8);
        cellar.addItem(createItem("old-barrel", "旧木桶",
            "一个腐烂的木桶。说不定里面藏着什么。", 20, null));

        // -- vault: 金库（上锁） --
        Room vault = createRoom("vault", "in a gleaming vault");
        vault.setLockId("vault-door");
        setSpawns(vault, 15,8, 27,8, 27,8, 27,8, 4,8);
        vault.addItem(createItem("gem-light", "光之宝石",
            "散发着纯净光芒的宝石。", 5, "light:full"));
        vault.addItem(createItem("gold-coins", "金币",
            "一小堆金币。", 10, null));

        // -- hidden-shrine: 隐士神龛 --
        Room hiddenShrine = createRoom("hidden-shrine", "in a hidden shrine");
        setSpawns(hiddenShrine, 15,8, 15,3, 15,13, 27,8, 3,8);
        hiddenShrine.addItem(createItem("crystal-shard", "水晶碎片",
            "一块微微颤动的水晶碎片。", 3, "reputation:+5"));

        // -- garden: 庭院 --
        Room garden = createRoom("garden", "in a serene garden");
        setSpawns(garden, 15,8, 15,3, 15,13, 27,8, 3,8);
        garden.addItem(createItem("healing-herb", "治愈草药",
            "一株散发着清香的草药。", 2, "heal:20"));

        // -- guard-room: 守卫哨站 --
        Room guardRoom = createRoom("guard-room", "in a guard station");
        guardRoom.setLockId("guard-gate");
        setSpawns(guardRoom, 15,8, 15,3, 15,13, 27,8, 3,8);

        // -- armory: 军械库 --
        Room armory = createRoom("armory", "in the armory");
        setSpawns(armory, 15,8, 15,3, 15,13, 27,8, 3,8);
        armory.addItem(createItem("sword-rusty", "锈剑",
            "一把旧剑，仍然锋利。", 25, null));
        armory.addItem(createItem("shield-wooden", "木盾",
            "一面破旧的木盾。", 18, null));

        // -- forge: 铁匠铺 --
        Room forge = createRoom("forge", "in a blazing forge");
        setSpawns(forge, 15,8, 15,3, 15,13, 27,8, 3,8);

        // -- teleport-alcove: 传送密室 --
        Room teleportAlcove = createRoom("teleport-alcove", "in an unstable teleport alcove");
        teleportAlcove.setTeleport(true);
        setSpawns(teleportAlcove, 15,8, 27,8, 27,8, 27,8, 4,8);
        teleportAlcove.addItem(createItem("warp-dust", "Warp Dust",
            "Fine dust that sparkles with teleport energy.", 2, null));

        // -- throne-hall: 王座大厅（结局） --
        Room throneHall = createRoom("throne-hall", "in the great throne hall");
        setSpawns(throneHall, 15,8, 15,3, 15,13, 27,8, 3,8);

        // =================== 拓扑连接（双向成对，与 assets/maps 出口一致） ===================
        linkBidirectional(outside, "north", theatre, "south");
        linkBidirectional(outside, "east", pub, "west");
        linkBidirectional(outside, "south", lab, "north");
        linkBidirectional(outside, "west", office, "east");

        linkBidirectional(theatre, "east", library, "west");
        linkBidirectional(pub, "south", cellar, "north");
        linkBidirectional(pub, "east", garden, "west");

        linkBidirectional(lab, "south", vault, "north");

        linkBidirectional(library, "north", hiddenShrine, "south");
        linkBidirectional(library, "east", teleportAlcove, "west");

        linkBidirectional(garden, "south", guardRoom, "north");
        linkBidirectional(garden, "east", armory, "west");

        linkBidirectional(guardRoom, "south", throneHall, "north");

        linkBidirectional(armory, "south", forge, "north");
    }

    /** 随机 cookie 放置：从 cellar/library/hidden-shrine 中随机选 1 间 */
    private static void placeMagicCookie()
    {
        String picked = COOKIE_CANDIDATES.get(RANDOM.nextInt(COOKIE_CANDIDATES.size()));
        Room room = ROOMS.get(picked);
        if (room != null) {
            room.addItem(new Item("magic-cookie", "Magic Cookie",
                "A glowing cookie. Eating it makes you feel stronger.", 0.5, "maxWeight:+20"));
            LOG.info("WorldFactory: magic-cookie placed in [" + picked + "]");
        }
    }

    // ======================== 工具方法 ========================

    private static Item createItem(String id, String name, String desc, double weight, String effect)
    {
        return new Item(id, name, desc, weight, effect);
    }

    /**
     * 按 default / north / south / east / west 顺序设置 spawn 坐标（格子索引）。
     */
    private static void setSpawns(Room room,
        float dx, float dy, float nx, float ny, float sx, float sy,
        float ex, float ey, float wx, float wy)
    {
        RoomScene scene = room.getScene();
        scene.addSpawn(Direction.DEFAULT, dx, dy);
        scene.addSpawn(Direction.NORTH, nx, ny);
        scene.addSpawn(Direction.SOUTH, sx, sy);
        scene.addSpawn(Direction.EAST, ex, ey);
        scene.addSpawn(Direction.WEST, wx, wy);
    }

    private static Room createRoom(String roomId, String description)
    {
        Room room = new Room(roomId, description);
        RoomScene scene = new RoomScene("maps/" + roomId + ".tmx");
        room.setScene(scene);
        ROOMS.put(roomId, room);
        return room;
    }

    private static void link(Room from, String direction, Room to)
    {
        from.setExit(direction, to);
    }

    private static void linkBidirectional(Room fromA, String dirA, Room roomB, String dirB)
    {
        link(fromA, dirA, roomB);
        link(roomB, dirB, fromA);
    }
}
