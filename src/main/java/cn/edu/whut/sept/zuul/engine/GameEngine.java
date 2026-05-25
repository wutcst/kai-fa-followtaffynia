package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.WorldFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 游戏核心引擎。GUI 唯一调用入口（见 docs/04 §10）。
 */
public class GameEngine
{
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
            return false;
        }

        Room next = currentRoom.getExit(direction.toExitKey());
        if (next == null) {
            return false;
        }

        roomHistory.push(currentRoom.getRoomId());
        entryDirection = direction;
        currentRoom = next;
        resolveTeleportIfNeeded();
        exploredRoomIds.add(currentRoom.getRoomId());
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
        Item item = currentRoom.removeItem(itemId);
        if (item == null) {
            return false;
        }
        if (player.totalWeight() + item.getWeight() > player.getMaxWeight()) {
            currentRoom.addItem(item);
            return false;
        }
        player.addItem(item);
        return true;
    }

    public boolean dropItem(String itemId)
    {
        Item item = player.removeItem(itemId);
        if (item == null) {
            return false;
        }
        currentRoom.addItem(item);
        return true;
    }

    public void dropAllItems()
    {
        List<Item> copy = new ArrayList<>(player.getInventory());
        for (Item item : copy) {
            player.removeItem(item.getItemId());
            currentRoom.addItem(item);
        }
    }

    public boolean useItem(String itemId)
    {
        // 第二步实现 UseEffect
        return player.getInventory().stream().anyMatch(i -> i.getItemId().equals(itemId));
    }

    public void eatItem(String itemId)
    {
        Item item = player.removeItem(itemId);
        if (item != null && item.isMagicCookie()) {
            player.setMaxWeight(player.getMaxWeight() + Item.COOKIE_WEIGHT_BOOST);
        } else if (item != null) {
            player.addItem(item);
        }
    }

    public Dialogue talkNpc(String npcId)
    {
        return new Dialogue(npcId, "（对话系统将在后续步骤实现）", new ArrayList<>());
    }

    public String look()
    {
        return currentRoom.getLongDescription();
    }

    public boolean moveBack()
    {
        if (roomHistory.isEmpty()) {
            return false;
        }
        String previousRoomId = roomHistory.pop();
        Room previous = WorldFactory.getRoom(previousRoomId);
        if (previous == null) {
            return false;
        }
        entryDirection = entryDirection.opposite();
        currentRoom = previous;
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
        return state;
    }

    public void restoreState(GameState state)
    {
        if (state == null) {
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
