package cn.edu.whut.sept.zuul.client.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界地图拓扑数据 —— 房间位置、连接关系、锁信息、中文标签。
 */
public class WorldMapTopology
{
    public final Map<String, int[]> roomPos;
    public final List<String[]> connections;
    public final Map<String, String> roomLockIds;

    public WorldMapTopology()
    {
        roomPos = new LinkedHashMap<>();
        // 严格正交布局：南北连接=同列，东西连接=同行
        // col 0    1         2         3         4         5
        //r0                           [神龛]
        //r1               [讲堂]──[图书馆]──[传送室]
        //r2 [办公室]←──[广场]──[酒馆]──[庭院]──[军械库]
        //r3              [机房]          [哨站]  [铁匠铺]
        //r4              [金库]  [地窖]  [王座厅]
        roomPos.put("hidden-shrine", new int[]{3, 0});    // 同col3 library北
        roomPos.put("theatre", new int[]{2, 1});           // 同col2 outside北
        roomPos.put("library", new int[]{3, 1});           // 同row1 theatre东
        roomPos.put("teleport-alcove", new int[]{4, 1});   // 同row1 library东
        roomPos.put("office", new int[]{1, 2});            // 同row2 outside西
        roomPos.put("outside", new int[]{2, 2});           // 中心
        roomPos.put("pub", new int[]{3, 2});               // 同row2 outside东
        roomPos.put("garden", new int[]{4, 2});            // 同row2 pub东
        roomPos.put("armory", new int[]{5, 2});            // 同row2 garden东
        roomPos.put("lab", new int[]{2, 3});               // 同col2 outside南
        roomPos.put("vault", new int[]{2, 4});             // 同col2 lab南
        roomPos.put("guard-room", new int[]{4, 3});        // 同col4 garden南
        roomPos.put("forge", new int[]{5, 3});             // 同col5 armory南
        roomPos.put("cellar", new int[]{3, 4});            // 同col3 pub南
        roomPos.put("throne-hall", new int[]{4, 4});       // 同col4 guardRoom南

        connections = new ArrayList<>();
        connections.add(new String[]{"outside", "theatre"});
        connections.add(new String[]{"outside", "pub"});
        connections.add(new String[]{"outside", "lab"});
        connections.add(new String[]{"outside", "office"});
        connections.add(new String[]{"theatre", "library"});
        connections.add(new String[]{"pub", "cellar"});
        connections.add(new String[]{"pub", "garden"});
        connections.add(new String[]{"lab", "vault"});
        connections.add(new String[]{"library", "hidden-shrine"});
        connections.add(new String[]{"library", "teleport-alcove"});
        connections.add(new String[]{"garden", "guard-room"});
        connections.add(new String[]{"garden", "armory"});
        connections.add(new String[]{"guard-room", "throne-hall"});
        connections.add(new String[]{"armory", "forge"});

        roomLockIds = new LinkedHashMap<>();
        roomLockIds.put("vault", "vault-door");
        roomLockIds.put("guard-room", "guard-gate");
    }

    public static String roomLabel(String roomId)
    {
        switch (roomId) {
            case "outside": return "广场";
            case "theatre": return "讲堂";
            case "pub": return "酒馆";
            case "lab": return "机房";
            case "office": return "办公室";
            case "library": return "图书馆";
            case "cellar": return "地窖";
            case "vault": return "金库";
            case "hidden-shrine": return "神龛";
            case "garden": return "庭院";
            case "guard-room": return "哨站";
            case "armory": return "军械库";
            case "forge": return "铁匠铺";
            case "teleport-alcove": return "传送室";
            case "throne-hall": return "王座厅";
            default: return roomId;
        }
    }
}
