package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.effect.UseEffectRegistry;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.WorldFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final Set<String> defeatedNpcs;
    private final Set<String> playerFlags;
    /** 非持久——进入vault时若golem未死则自动触发战斗 */
    private String pendingAutoCombat;
    private final UseEffectRegistry useEffectRegistry;
    private final CombatActionRegistry combatActionRegistry;
    private final QuestManager questManager;
    private final EndingEvaluator endingEvaluator;
    private final DialogueActionExecutor dialogueActionExecutor;
    private final DialogueManager dialogueManager;
    private final CombatManager combatManager;
    private final ItemManager itemManager;
    private String lastMessage;
    /** true 表示刚 moveBack，落点贴在返回的那扇门一侧 */
    private boolean spawnAtDoorSide;
    private EndingType currentEnding;

    public GameEngine(String playerName)
    {
        WorldFactory.build(playerName);
        this.player = new Player(playerName);
        this.currentRoom = WorldFactory.getRoom("outside");
        this.entryDirection = Direction.DEFAULT;
        this.roomHistory = new ArrayDeque<>();
        this.exploredRoomIds = new HashSet<>();
        this.unlockedLocks = new HashSet<>();
        this.defeatedNpcs = new HashSet<>();
        this.playerFlags = new HashSet<>();
        this.useEffectRegistry = new UseEffectRegistry();
        this.combatActionRegistry = new CombatActionRegistry();
        this.questManager = new QuestManager();
        this.endingEvaluator = new EndingEvaluator();
        this.dialogueActionExecutor = new DialogueActionExecutor(this);
        this.dialogueManager = new DialogueManager(dialogueActionExecutor,
            msg -> this.lastMessage = msg);
        this.combatManager = new CombatManager(player, defeatedNpcs, unlockedLocks,
            combatActionRegistry, questManager,
            msg -> this.lastMessage = msg);
        this.itemManager = new ItemManager(this, unlockedLocks, questManager,
            msg -> this.lastMessage = msg);
        this.currentEnding = EndingType.NONE;
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

    public QuestManager getQuestManager()
    {
        return questManager;
    }

    public EndingType getCurrentEnding()
    {
        return currentEnding;
    }

    public boolean isInDialogue()
    {
        return dialogueManager.isInDialogue();
    }

    public boolean isInCombat()
    {
        return combatManager.isInCombat();
    }

    public CombatOutcome getLastCombatOutcome()
    {
        return combatManager.getLastCombatOutcome();
    }

    public Set<String> getDefeatedNpcs()
    {
        return Collections.unmodifiableSet(defeatedNpcs);
    }

    public Set<String> getPlayerFlags()
    {
        return Collections.unmodifiableSet(playerFlags);
    }

    public void setFlag(String flag)
    {
        if (flag != null && !flag.isEmpty()) {
            playerFlags.add(flag);
            LOG.info("playerFlags: set " + flag);
        }
    }

    public boolean hasFlag(String flag)
    {
        return playerFlags.contains(flag);
    }

    public String getPendingAutoCombat()
    {
        return pendingAutoCombat;
    }

    public void clearPendingAutoCombat()
    {
        pendingAutoCombat = null;
    }

    /** 通过对话/战斗给玩家物品，直接加入背包。 */
    public Item giveItem(String itemId)
    {
        Item item = createKnownItem(itemId);
        if (item != null) {
            player.addItem(cloneItem(item));
            lastMessage = "获得了 " + item.getName() + "。";
        }
        return item;
    }

    public Set<String> getExploredRoomIds()
    {
        return Collections.unmodifiableSet(exploredRoomIds);
    }

    public boolean isPlayerDead()
    {
        return player.getHp() <= 0;
    }

    public EncounterMenu startNpcEncounter(String npcId)
    {
        return combatManager.startNpcEncounter(npcId);
    }

    public void leaveEncounter()
    {
        combatManager.leaveEncounter();
    }

    public CombatSnapshot startCombat(String npcId)
    {
        return startCombat(npcId, CombatMode.TURN_BASED);
    }

    /**
     * 按指定模式开战。mode=TURN_BASED 用传统回合制，UNDERTALE 用弹幕系统。
     */
    public CombatSnapshot startCombat(String npcId, CombatMode mode)
    {
        endDialogue();
        return combatManager.startCombat(npcId, mode);
    }

    /** 获取当前战斗系统，UI 可强转为 UndertaleCombatEngine 访问 UT 专用 API。 */
    public CombatSystem getCombatSystem()
    {
        return combatManager.getCombatSystem();
    }

    public boolean isUndertaleCombat()
    {
        return combatManager.isUndertaleCombat();
    }

    public CombatSnapshot combatAction(CombatAction action, String itemIdOrNull)
    {
        return combatManager.combatAction(action, itemIdOrNull);
    }

    public void applyCombatOutcome()
    {
        combatManager.applyCombatOutcome();
        // 战斗胜利后掉落物品到当前房间
        NpcCombatDef def = combatManager.getLastDef();
        if (def != null && def.onDefeatSpawnItem != null
            && !def.onDefeatSpawnItem.isEmpty()
            && getLastCombatOutcome() == CombatOutcome.VICTORY)
        {
            Item spawn = createKnownItem(def.onDefeatSpawnItem);
            if (spawn != null) {
                currentRoom.addItem(spawn);
                LOG.info("Combat loot: " + spawn.getItemId()
                    + " spawned in " + currentRoom.getRoomId());
            }
        }
    }

    public String getLastMessage()
    {
        return lastMessage;
    }

    public boolean isLockUnlocked(String lockId)
    {
        return unlockedLocks.contains(lockId);
    }

    public void unlockLock(String lockId)
    {
        if (lockId != null && !lockId.isEmpty()) {
            unlockedLocks.add(lockId);
            questManager.onLockUnlocked(lockId);
        }
    }

    public boolean movePlayer(Direction direction)
    {
        if (direction == null || direction == Direction.DEFAULT) {
            LOG.warning("movePlayer: null/default direction rejected");
            lastMessage = "无法朝该方向移动。";
            return false;
        }

        String fromId = currentRoom.getRoomId();
        Room next = currentRoom.getExit(direction.toExitKey());
        if (next == null) {
            LOG.info("movePlayer: " + fromId + " -> " + direction.toExitKey()
                + " = BLOCKED (no exit)");
            lastMessage = "那边没有路。";
            return false;
        }

        if (next.getLockId() != null && !unlockedLocks.contains(next.getLockId())) {
            LOG.info("movePlayer: " + fromId + " -> " + next.getRoomId()
                + " = LOCKED [" + next.getLockId() + "]");
            if ("guard-gate".equals(next.getLockId())) {
                lastMessage = "守卫之门紧锁。可在 office 拾取铁钥匙后于门前使用（U），"
                    + "或在 garden 南侧门口与守卫对话（E）请求放行。";
            } else {
                lastMessage = "门被锁住了（" + next.getLockId() + "）。";
            }
            return false;
        }

        roomHistory.push(currentRoom.getRoomId());
        entryDirection = direction;
        spawnAtDoorSide = false;
        this.pendingAutoCombat = null;
        currentRoom = next;
        String toIdBeforeTeleport = currentRoom.getRoomId();
        resolveTeleportIfNeeded();
        exploredRoomIds.add(currentRoom.getRoomId());
        questManager.onRoomEntered(currentRoom, exploredRoomIds);

        // 金库魔像自动遭遇：进入vault且golem未死时触发
        if ("vault".equals(currentRoom.getRoomId()) && !defeatedNpcs.contains("golem")) {
            pendingAutoCombat = "golem";
        }

        if ("throne-hall".equals(currentRoom.getRoomId())) {
            currentEnding = endingEvaluator.evaluate(player, defeatedNpcs);
            LOG.info("Ending triggered: " + currentEnding);
        }

        if (!toIdBeforeTeleport.equals(currentRoom.getRoomId())) {
            LOG.info("movePlayer: " + fromId + " --[" + direction.toExitKey() + "]--> "
                + toIdBeforeTeleport + " [TELEPORT!] --> " + currentRoom.getRoomId()
                + " | explored: " + exploredRoomIds.size());
            lastMessage = "你被传送到了 " + currentRoom.getRoomId() + "！";
        } else {
            LOG.info("movePlayer: " + fromId + " --[" + direction.toExitKey() + "]--> "
                + currentRoom.getRoomId()
                + " | explored: " + exploredRoomIds.size());
            lastMessage = "进入了 " + currentRoom.getRoomId() + "。";
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

    public boolean takeItem(String itemId) { return itemManager.takeItem(itemId); }
    public boolean dropItem(String itemId) { return itemManager.dropItem(itemId); }
    public void dropAllItems() { itemManager.dropAllItems(); }
    public ItemUseCheck checkItemUse(String itemId) { return itemManager.checkItemUse(itemId); }
    public String tryUseItem(String itemId) { return itemManager.tryUseItem(itemId); }
    public void eatItem(String itemId) { itemManager.eatItem(itemId); }

    /** 以物易物：用全部破烂换草药。每个破烂换 1 个草药。 */
    public String barterJunkForHerbs()
    {
        List<String> junkIds = java.util.Arrays.asList(
            "gold-coins", "ale-mug", "old-barrel", "ancient-tome",
            "warp-dust", "welcome-note");
        int traded = 0;
        for (String id : junkIds) {
            while (player.getInventory().stream().anyMatch(i -> i.getItemId().equals(id))) {
                player.removeItem(id);
                traded++;
            }
        }
        if (traded == 0) {
            return "商人翻了翻你的背包……\"你身上没有我感兴趣的东西。\"";
        }
        // 每件破烂换 1 个草药
        for (int i = 0; i < traded; i++) {
            player.addItem(cloneItem(createKnownItem("healing-herb")));
        }
        return "商人眼睛一亮！收下了你 " + traded + " 件杂物，给了你 " + traded + " 株草药。\n\"下次再带些有趣的东西来。\"";
    }

    public String useItem(String itemId)
    {
        Item item = player.getInventory().stream()
            .filter(i -> i.getItemId().equals(itemId)).findFirst().orElse(null);
        if (item == null) { lastMessage = "背包中没有 " + itemId; return lastMessage; }
        UseResult result = useEffectRegistry.apply(this, item);
        if (result.isSuccess()) player.removeItem(itemId);
        lastMessage = result.getMessage();
        return lastMessage;
    }

    public Dialogue talkNpc(String npcId)
    {
        // 学者条件对话：同时拒绝祭司和信徒后揭示中立之路
        if ("scholar".equals(npcId)
            && hasFlag("refused-priest")
            && hasFlag("refused-follower"))
        {
            return dialogueManager.talkNpc("scholar_neutral");
        }
        return dialogueManager.talkNpc(npcId);
    }

    /** MERCY 退出后自动对话——前缀拼接在正常对话前 */
    public Dialogue talkNpcWithPrefix(String npcId, String prefix)
    {
        lastMessage = prefix;
        return dialogueManager.talkNpc(npcId);
    }

    public Dialogue chooseDialogueOption(int optionIndex)
    {
        return dialogueManager.chooseDialogueOption(optionIndex);
    }

    public void endDialogue()
    {
        dialogueManager.endDialogue();
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
        spawnAtDoorSide = true;
        currentRoom = previous;
        lastMessage = "返回到了 " + currentRoom.getRoomId() + "。";
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
        Direction spawnSide;
        if (entryDirection == Direction.DEFAULT) {
            spawnSide = Direction.DEFAULT;
        } else if (spawnAtDoorSide) {
            // moveBack 后 entryDirection 已取反，再取反回到原来的门侧
            spawnSide = entryDirection.opposite();
        } else {
            // 从入口方向进来，落在门对侧（靠近来时的门，方便回去）
            spawnSide = entryDirection.opposite();
        }
        return currentRoom.getScene().getSpawnAt(spawnSide);
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
        state.getQuestStates().clear();
        state.getQuestStates().putAll(questManager.getQuestStates());
        state.getDefeatedNpcs().addAll(defeatedNpcs);
        state.getPlayerFlags().addAll(playerFlags);
        state.getRoomItems().clear();
        for (Room room : WorldFactory.getAllRooms().values()) {
            List<String> itemIds = room.getItems().stream()
                .map(Item::getItemId)
                .collect(Collectors.toList());
            state.getRoomItems().put(room.getRoomId(), itemIds);
        }

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
        Map<String, Item> itemTemplates = snapshotItemTemplates();

        player.setHp(state.getHp());
        player.setMaxWeight(state.getMaxWeight());
        player.setReputation(state.getReputation());
        player.clearInventory();
        for (String itemId : state.getInventory()) {
            Item item = cloneFromTemplate(itemTemplates, itemId);
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
        questManager.restoreQuestStates(state.getQuestStates());
        defeatedNpcs.clear();
        if (state.getDefeatedNpcs() != null) {
            defeatedNpcs.addAll(state.getDefeatedNpcs());
        }
        playerFlags.clear();
        if (state.getPlayerFlags() != null) {
            playerFlags.addAll(state.getPlayerFlags());
        }
        combatManager.leaveEncounter();
        endDialogue();
        entryDirection = Direction.fromExitKey(state.getEntryDirection());
        spawnAtDoorSide = false;
        currentRoom = WorldFactory.getRoom(state.getCurrentRoomId());
        if (currentRoom == null) {
            currentRoom = WorldFactory.getRoom("outside");
        }
        restoreRoomItems(state, itemTemplates);
        LOG.info("restoreState: room=" + state.getCurrentRoomId()
            + " | inv=" + state.getInventory().size()
            + " | explored=" + state.getExploredRoomIds().size()
            + " | roomItems=" + state.getRoomItems().size());
    }

    private Map<String, Item> snapshotItemTemplates()
    {
        Map<String, Item> templates = new HashMap<>();
        for (Room room : WorldFactory.getAllRooms().values()) {
            for (Item item : room.getItems()) {
                templates.putIfAbsent(item.getItemId(), cloneItem(item));
            }
        }
        String[] knownIds = {
            "welcome-note", "torch", "ale-mug", "key-vault", "key-guard",
            "ancient-tome", "old-barrel", "gem-light", "gold-coins",
            "crystal-shard", "healing-herb", "sword-rusty", "shield-wooden",
            "warp-dust", "magic-cookie",
            "light-mark", "shadow-pact", "balance-book", "guard-medal", "sage-salt"
        };
        for (String id : knownIds) {
            Item known = createKnownItem(id);
            if (known != null) {
                templates.putIfAbsent(id, known);
            }
        }
        return templates;
    }

    private void restoreRoomItems(GameState state, Map<String, Item> itemTemplates)
    {
        Map<String, List<String>> savedRoomItems = state.getRoomItems();
        if (!savedRoomItems.isEmpty()) {
            for (Room room : WorldFactory.getAllRooms().values()) {
                room.getItems().clear();
                List<String> itemIds = savedRoomItems.get(room.getRoomId());
                if (itemIds == null) {
                    continue;
                }
                for (String itemId : itemIds) {
                    Item item = cloneFromTemplate(itemTemplates, itemId);
                    if (item != null) {
                        room.addItem(item);
                    }
                }
            }
            return;
        }

        // Compatibility with older save files that only stored inventory.
        for (String itemId : state.getInventory()) {
            for (Room room : WorldFactory.getAllRooms().values()) {
                while (room.removeItem(itemId) != null) {
                    // Remove every stale copy so already-owned items cannot be duplicated.
                }
            }
        }
    }

    private Item cloneFromTemplate(Map<String, Item> templates, String itemId)
    {
        Item template = templates.get(itemId);
        if (template != null) {
            return cloneItem(template);
        }
        return createKnownItem(itemId);
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
        return createKnownItem(itemId);
    }

    private Item createKnownItem(String itemId)
    {
        switch (itemId) {
            case "torch":
                return new Item("torch", "Torch",
                    "A flickering torch. Useful in dark places.", 1, "light");
            case "key-vault":
                return new Item("key-vault", "Rusty Key",
                    "A rusty key, inscribed 'Vault'.", 0.5, "unlock:vault-door");
            case "key-guard":
                return new Item("key-guard", "Iron Key",
                    "A heavy iron key, marked 'Guard Gate'.", 0.5, "unlock:guard-gate");
            case "gem-light":
                return new Item("gem-light", "Light Gem",
                    "A radiant gem pulsing with pure light.", 3, "light:full");
            case "gold-coins":
                return new Item("gold-coins", "Gold Coins",
                    "A small pile of gold coins. The merchant might find these interesting.",
                    0.5, "barter");
            case "ancient-tome":
                return new Item("ancient-tome", "Ancient Tome",
                    "A heavy tome bound in cracked leather. A collector would pay handsomely.",
                    6, "barter");
            case "old-barrel":
                return new Item("old-barrel", "Old Barrel",
                    "A rotting barrel. Maybe someone can make use of it.", 8, "barter");
            case "warp-dust":
                return new Item("warp-dust", "Warp Dust",
                    "Fine dust that sparkles with teleport energy. A curious trinket.",
                    0.5, "barter");
            case "ale-mug":
                return new Item("ale-mug", "Ale Mug",
                    "A half-empty mug of ale. The merchant collects these.",
                    0.1, "barter");
            case "welcome-note":
                return new Item("welcome-note", "Welcome Note",
                    "A crumpled welcome note. Worth something to the right person.",
                    0.1, "barter");
            case "crystal-shard":
                return new Item("crystal-shard", "Crystal Shard",
                    "A fragment of crystal that hums softly.", 0.5, "reputation:+5");
            case "healing-herb":
                return new Item("healing-herb", "Healing Herb",
                    "A fragrant herb that restores vitality.", 0.5, "heal:20");
            case "sword-rusty":
                return new Item("sword-rusty", "Rusty Sword",
                    "An old sword, still sharp enough. Adds damage in combat while held.",
                    15, "passive:UT战斗攻击+8，回合制攻击+5（背包中持有即生效）");
            case "shield-wooden":
                return new Item("shield-wooden", "Wooden Shield",
                    "A battered wooden shield. Reduces damage taken while held.",
                    14, "passive:受到攻击伤害-3（背包中持有即生效）");
            case "magic-cookie":
                return new Item("magic-cookie", "Magic Cookie",
                    "A glowing cookie. Eating it makes you feel stronger.", 0.5, "maxWeight:+20");
            case "light-mark":
                return new Item("light-mark", "光明印记",
                    "守光祭司赐予的印记，蕴含秩序之力。", 0.5, "reputation:+3");
            case "shadow-pact":
                return new Item("shadow-pact", "暗影之契",
                    "刻有暗影符文的契约，散发着不祥气息。", 0.5, "reputation:-5");
            case "balance-book":
                return new Item("balance-book", "平衡之书",
                    "记载着Realm光暗平衡之秘的古籍。", 1.0, "lore");
            case "guard-medal":
                return new Item("guard-medal", "守卫勋章",
                    "守卫学徒赠予的荣誉勋章。", 0.5, "reputation:+3");
            case "sage-salt":
                return new Item("sage-salt", "贤者之盐",
                    "学者调配的疗愈之盐，可恢复大量生命。", 0.5, "heal:30");
            default:
                return null;
        }
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
