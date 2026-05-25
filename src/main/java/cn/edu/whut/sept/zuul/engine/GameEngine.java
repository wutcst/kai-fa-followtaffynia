package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.WorldFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 游戏核心引擎。GUI 唯一调用入口（见 docs/04 §10）。
 */
public class GameEngine
{
    private static final Logger LOG = GameLogger.get();

    private final Player player;
    private Room currentRoom;
    private Direction entryDirection;
    private final Deque<String> roomHistory;
    private final Set<String> exploredRoomIds;
    private final Set<String> unlockedLocks;

    public GameEngine(String playerName)
    {
        WorldFactory.build(playerName);
        this.player = new Player(playerName);
        this.currentRoom = WorldFactory.getRoom("outside");
        this.entryDirection = Direction.DEFAULT;
        this.roomHistory = new ArrayDeque<>();
        this.exploredRoomIds = new HashSet<>();
        this.unlockedLocks = new HashSet<>();
        exploredRoomIds.add(currentRoom.getRoomId());

        LOG.info("=== GAME START ===");
        LOG.info("Player: " + playerName + " | MaxWeight: " + player.getMaxWeight()
            + " | HP: " + player.getHp());
        LOG.info("Start room: " + currentRoom.getRoomId()
            + " | Exits: " + currentRoom.getExitDirections());
        logWorldSummary();
    }

    private void logWorldSummary()
    {
        int totalItems = 0;
        for (Room r : WorldFactory.getAllRooms().values()) {
            totalItems += r.getItems().size();
        }
        LOG.info("World: " + WorldFactory.getAllRooms().size() + " rooms, "
            + totalItems + " total items");
        // 列出所有物品归属
        for (Room r : WorldFactory.getAllRooms().values()) {
            if (!r.getItems().isEmpty()) {
                String items = r.getItems().stream()
                    .map(i -> i.getItemId() + "(" + i.getWeight() + " wt)")
                    .collect(Collectors.joining(", "));
                LOG.info("  [" + r.getRoomId() + "]: " + items
                    + (r.isTeleport() ? " [TELEPORT]" : "")
                    + (r.getLockId() != null ? " [LOCKED:" + r.getLockId() + "]" : ""));
            }
        }
    }

    public Player getPlayer()
    {
        return player;
    }

    public Room getCurrentRoom()
    {
        return currentRoom;
    }

    public Direction getEntryDirection()
    {
        return entryDirection;
    }

    public boolean movePlayer(Direction direction)
    {
        if (direction == null || direction == Direction.DEFAULT) {
            LOG.warning("movePlayer: null/default direction rejected");
            return false;
        }

        String fromId = currentRoom.getRoomId();
        Room next = currentRoom.getExit(direction.toExitKey());
        if (next == null) {
            LOG.info("movePlayer: " + fromId + " -> " + direction.toExitKey()
                + " = BLOCKED (no exit)");
            return false;
        }

        // 检查目标房间是否上锁
        if (next.getLockId() != null && !unlockedLocks.contains(next.getLockId())) {
            LOG.info("movePlayer: " + fromId + " -> " + next.getRoomId()
                + " = LOCKED [" + next.getLockId() + "]");
            return false;
        }

        roomHistory.push(currentRoom.getRoomId());
        entryDirection = direction;
        currentRoom = next;
        String toIdBeforeTeleport = currentRoom.getRoomId();
        resolveTeleportIfNeeded();
        exploredRoomIds.add(currentRoom.getRoomId());

        if (!toIdBeforeTeleport.equals(currentRoom.getRoomId())) {
            LOG.info("movePlayer: " + fromId + " --[" + direction.toExitKey() + "]--> "
                + toIdBeforeTeleport + " [TELEPORT!] --> " + currentRoom.getRoomId()
                + " | explored: " + exploredRoomIds.size());
        } else {
            LOG.info("movePlayer: " + fromId + " --[" + direction.toExitKey() + "]--> "
                + currentRoom.getRoomId()
                + " | explored: " + exploredRoomIds.size());
        }
        return true;
    }

    private void resolveTeleportIfNeeded()
    {
        if (!currentRoom.isTeleport()) {
            return;
        }
        String fromId = currentRoom.getRoomId();
        currentRoom = WorldFactory.randomRoomExcept(fromId);
        entryDirection = Direction.DEFAULT;
        exploredRoomIds.add(currentRoom.getRoomId());
    }

    public boolean takeItem(String itemId)
    {
        Room room = currentRoom;
        Item item = room.removeItem(itemId);
        if (item == null) {
            LOG.info("takeItem: " + itemId + " = FAIL (not in " + room.getRoomId() + ")");
            return false;
        }
        int currentWt = player.totalWeight();
        if (currentWt + item.getWeight() > player.getMaxWeight()) {
            room.addItem(item);
            LOG.info("takeItem: " + itemId + " = FAIL overweight ("
                + (currentWt + item.getWeight()) + " > " + player.getMaxWeight() + ")");
            return false;
        }
        player.addItem(item);
        LOG.info("takeItem: " + itemId + " (" + item.getWeight() + " wt) from "
            + room.getRoomId() + " | backpack: " + player.totalWeight()
            + "/" + player.getMaxWeight() + " (" + player.getInventory().size() + " items)");
        return true;
    }

    public boolean dropItem(String itemId)
    {
        Item item = player.removeItem(itemId);
        if (item == null) {
            LOG.info("dropItem: " + itemId + " = FAIL (not in backpack)");
            return false;
        }
        currentRoom.addItem(item);
        LOG.info("dropItem: " + itemId + " (" + item.getWeight() + " wt) -> "
            + currentRoom.getRoomId() + " | backpack: " + player.totalWeight()
            + "/" + player.getMaxWeight());
        return true;
    }

    public void dropAllItems()
    {
        List<Item> copy = new ArrayList<>(player.getInventory());
        for (Item item : copy) {
            player.removeItem(item.getItemId());
            currentRoom.addItem(item);
        }
        LOG.info("dropAllItems: " + copy.size() + " items dropped in "
            + currentRoom.getRoomId());
    }

    public String useItem(String itemId)
    {
        Item item = player.getInventory().stream()
            .filter(i -> i.getItemId().equals(itemId))
            .findFirst().orElse(null);
        if (item == null) {
            LOG.info("useItem: " + itemId + " = FAIL (not in backpack)");
            return "背包中没有 " + itemId;
        }
        String effect = item.getEffect();
        if (effect == null || effect.isEmpty()) {
            LOG.info("useItem: " + itemId + " has no effect");
            return item.getName() + " 没有特殊效果";
        }

        String[] parts = effect.split(":", 2);
        String key = parts[0];
        String value = parts.length > 1 ? parts[1] : "";

        switch (key) {
            case "unlock":
                unlockedLocks.add(value);
                player.removeItem(itemId);
                LOG.info("useItem: " + itemId + " -> UNLOCK " + value + " (consumed)");
                return "使用了 " + item.getName() + "，" + value + " 已解锁！";
            case "heal":
                int healAmt = Integer.parseInt(value);
                player.setHp(Math.min(player.getHp() + healAmt, player.getMaxHp()));
                player.removeItem(itemId);
                LOG.info("useItem: " + itemId + " -> HEAL " + healAmt
                    + " | HP: " + player.getHp());
                return "使用了 " + item.getName() + "，恢复了 " + healAmt + " HP！";
            case "light":
                player.removeItem(itemId);
                LOG.info("useItem: " + itemId + " -> LIGHT (consumed)");
                return "使用了 " + item.getName() + "，周围亮了起来！";
            case "reputation":
                int repDelta = Integer.parseInt(value);
                player.setReputation(player.getReputation() + repDelta);
                player.removeItem(itemId);
                LOG.info("useItem: " + itemId + " -> REPUTATION " + repDelta
                    + " | now: " + player.getReputation());
                return "使用了 " + item.getName() + "，声望 "
                    + (repDelta > 0 ? "+" : "") + repDelta + "！";
            default:
                LOG.info("useItem: " + itemId + " effect=" + effect + " (unhandled)");
                return item.getName() + " 暂时无法使用";
        }
    }

    public void eatItem(String itemId)
    {
        Item item = player.removeItem(itemId);
        if (item != null && item.isMagicCookie()) {
            int before = player.getMaxWeight();
            player.setMaxWeight(before + Item.COOKIE_WEIGHT_BOOST);
            LOG.info("eatItem: magic-cookie! MaxWeight " + before + " -> "
                + player.getMaxWeight());
        } else if (item != null) {
            player.addItem(item);
            LOG.info("eatItem: " + itemId + " is not edible, returned to backpack");
        } else {
            LOG.info("eatItem: " + itemId + " = FAIL (not in backpack)");
        }
    }

    public Dialogue talkNpc(String npcId)
    {
        LOG.info("talkNpc: " + npcId + " (placeholder dialogue)");
        return new Dialogue(npcId, "（对话系统将在后续步骤实现）", new ArrayList<>());
    }

    public String look()
    {
        String desc = currentRoom.getLongDescription();
        LOG.info("look: " + currentRoom.getRoomId()
            + " | items in room: " + currentRoom.getItems().size());
        return desc;
    }

    public boolean moveBack()
    {
        if (roomHistory.isEmpty()) {
            LOG.info("moveBack: FAIL (history empty)");
            return false;
        }
        String fromId = currentRoom.getRoomId();
        String previousRoomId = roomHistory.pop();
        Room previous = WorldFactory.getRoom(previousRoomId);
        if (previous == null) {
            LOG.warning("moveBack: FAIL (room not found: " + previousRoomId + ")");
            return false;
        }
        entryDirection = entryDirection.opposite();
        currentRoom = previous;
        LOG.info("moveBack: " + fromId + " -> " + currentRoom.getRoomId()
            + " | history depth: " + roomHistory.size());
        return true;
    }

    public String getRoomItemsWithWeight()
    {
        String items = currentRoom.getItems().stream()
            .map(i -> i.getName() + " (" + i.getWeight() + ")")
            .collect(Collectors.joining(", "));
        if (items.isEmpty()) {
            items = "无";
        }
        return items + " | 总重量: " + currentRoom.totalItemWeight();
    }

    public String getPlayerItemsWithWeight()
    {
        String items = player.getInventory().stream()
            .map(i -> i.getName() + " (" + i.getWeight() + ")")
            .collect(Collectors.joining(", "));
        if (items.isEmpty()) {
            items = "无";
        }
        return items + " | 总重量: " + player.totalWeight() + " / " + player.getMaxWeight();
    }

    public RoomScene.SpawnPoint resolveCurrentSpawn()
    {
        if (currentRoom.getScene() == null) {
            return new RoomScene.SpawnPoint(2f, 2f);
        }
        return currentRoom.getScene().resolveSpawn(entryDirection);
    }

    public GameState captureState()
    {
        GameState state = new GameState();
        state.setPlayerName(player.getName());
        state.setCurrentRoomId(currentRoom.getRoomId());
        state.setEntryDirection(entryDirection.toExitKey());
        state.setHp(player.getHp());
        state.setMaxHp(player.getMaxHp());
        state.setMaxWeight(player.getMaxWeight());
        state.setReputation(player.getReputation());
        state.getInventory().addAll(player.getInventory().stream()
            .map(Item::getItemId)
            .collect(Collectors.toList()));
        state.getUnlockedLocks().addAll(unlockedLocks);
        state.getExploredRoomIds().addAll(exploredRoomIds);
        state.getRoomHistory().addAll(roomHistory);

        RoomScene.SpawnPoint spawn = resolveCurrentSpawn();
        state.setPlayerX(spawn.tileX);
        state.setPlayerY(spawn.tileY);

        LOG.info("captureState: room=" + state.getCurrentRoomId()
            + " | inv=" + state.getInventory().size() + " items"
            + " | explored=" + state.getExploredRoomIds().size()
            + " | maxWeight=" + state.getMaxWeight());
        return state;
    }

    public void restoreState(GameState state)
    {
        if (state == null) {
            LOG.warning("restoreState: null state, skipped");
            return;
        }
        player.setHp(state.getHp());
        player.setMaxWeight(state.getMaxWeight());
        player.setReputation(state.getReputation());
        player.clearInventory();
        for (String itemId : state.getInventory()) {
            Item item = findItemTemplate(itemId);
            if (item != null) {
                player.addItem(item);
            }
        }
        unlockedLocks.clear();
        unlockedLocks.addAll(state.getUnlockedLocks());
        exploredRoomIds.clear();
        exploredRoomIds.addAll(state.getExploredRoomIds());
        roomHistory.clear();
        roomHistory.addAll(state.getRoomHistory());
        entryDirection = Direction.fromExitKey(state.getEntryDirection());
        currentRoom = WorldFactory.getRoom(state.getCurrentRoomId());
        if (currentRoom == null) {
            currentRoom = WorldFactory.getRoom("outside");
        }
        LOG.info("restoreState: room=" + state.getCurrentRoomId()
            + " | inv=" + state.getInventory().size()
            + " | explored=" + state.getExploredRoomIds().size());
    }

    private Item findItemTemplate(String itemId)
    {
        for (Room room : WorldFactory.getAllRooms().values()) {
            for (Item item : room.getItems()) {
                if (item.getItemId().equals(itemId)) {
                    return cloneItem(item);
                }
            }
        }
        if ("welcome-note".equals(itemId)) {
            return new Item("welcome-note", "note", "A crumpled welcome note.", 1, null);
        }
        return null;
    }

    private Item cloneItem(Item source)
    {
        return new Item(
            source.getItemId(),
            source.getName(),
            source.getDescription(),
            source.getWeight(),
            source.getEffect()
        );
    }
}
