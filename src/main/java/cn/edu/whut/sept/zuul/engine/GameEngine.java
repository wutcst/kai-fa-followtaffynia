package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.DialogueNode;
import cn.edu.whut.sept.zuul.domain.DialogueOption;
import cn.edu.whut.sept.zuul.domain.DialogueTree;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.effect.UseEffectRegistry;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;
import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.infra.CombatLoader;
import cn.edu.whut.sept.zuul.infra.DialogueLoader;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.WorldFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.io.IOException;

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
    private final UseEffectRegistry useEffectRegistry;
    private final CombatActionRegistry combatActionRegistry;
    private final QuestManager questManager;
    private final EndingEvaluator endingEvaluator;
    private final DialogueActionExecutor dialogueActionExecutor;
    private String lastMessage;
    /** true 表示刚 moveBack，落点贴在返回的那扇门一侧 */
    private boolean spawnAtDoorSide;
    private DialogueTree activeDialogueTree;
    private String activeDialogueNodeId;
    private EndingType currentEnding;
    private String encounterNpcId;
    private CombatSystem activeCombat;
    private CombatMode combatMode;
    private CombatOutcome lastCombatOutcome;

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
        this.useEffectRegistry = new UseEffectRegistry();
        this.combatActionRegistry = new CombatActionRegistry();
        this.questManager = new QuestManager();
        this.endingEvaluator = new EndingEvaluator();
        this.dialogueActionExecutor = new DialogueActionExecutor(this);
        this.currentEnding = EndingType.NONE;
        this.lastCombatOutcome = CombatOutcome.ONGOING;
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
        return activeDialogueTree != null;
    }

    public boolean isInCombat()
    {
        return activeCombat != null;
    }

    public CombatOutcome getLastCombatOutcome()
    {
        return lastCombatOutcome;
    }

    public Set<String> getDefeatedNpcs()
    {
        return Collections.unmodifiableSet(defeatedNpcs);
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
        leaveEncounter();
        encounterNpcId = npcId;
        boolean isDefeated = defeatedNpcs.contains(npcId);
        boolean canTalk = !isDefeated;
        boolean canFight = !"merchant".equals(npcId) && !isDefeated;
        boolean canUtFight = canFight && CombatLoader.exists(npcId);
        return new EncounterMenu(npcId, canTalk, canFight, canUtFight, true);
    }

    public void leaveEncounter()
    {
        encounterNpcId = null;
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
        leaveEncounter();
        try {
            NpcCombatDef def = CombatLoader.load(npcId);
            combatMode = mode;
            if (mode == CombatMode.UNDERTALE) {
                activeCombat = new UndertaleCombatEngine(player, def, combatActionRegistry);
            } else {
                activeCombat = new CombatEngine(player, def, combatActionRegistry);
            }
            encounterNpcId = npcId;
            lastCombatOutcome = CombatOutcome.ONGOING;
            LOG.info("startCombat: " + npcId + " mode=" + mode + " hp=" + def.maxHp);
            return activeCombat.snapshot();
        } catch (IOException ex) {
            LOG.warning("startCombat: failed for " + npcId + ": " + ex.getMessage());
            lastMessage = "无法与 " + npcId + " 开战。";
            return null;
        }
    }

    /** 获取当前战斗系统，UI 可强转为 UndertaleCombatEngine 访问 UT 专用 API。 */
    public CombatSystem getCombatSystem()
    {
        return activeCombat;
    }

    public boolean isUndertaleCombat()
    {
        return combatMode == CombatMode.UNDERTALE && activeCombat != null;
    }

    public CombatSnapshot combatAction(CombatAction action, String itemIdOrNull)
    {
        if (activeCombat == null) {
            lastMessage = "当前没有进行中的战斗。";
            return null;
        }
        CombatSnapshot snapshot = activeCombat.processPlayerAction(action, itemIdOrNull);
        lastCombatOutcome = snapshot.outcome;
        if (snapshot.outcome == CombatOutcome.VICTORY) {
            applyCombatVictory(activeCombat.getDef());
            clearCombat();
        } else if (snapshot.outcome == CombatOutcome.DEFEAT
            || snapshot.outcome == CombatOutcome.FLED) {
            clearCombat();
        }
        return snapshot;
    }

    private void applyCombatVictory(NpcCombatDef def)
    {
        if (def.onDefeatReputation != 0) {
            player.setReputation(player.getReputation() + def.onDefeatReputation);
        }
        if (def.onDefeatUnlock != null && !def.onDefeatUnlock.isEmpty()) {
            unlockLock(def.onDefeatUnlock);
        }
        if (def.onDefeatMarkDefeated) {
            defeatedNpcs.add(def.npcId);
            LOG.info("defeatedNpcs: added " + def.npcId);
        }
        lastMessage = "你击败了 " + def.displayName + "。";
    }

    private void clearCombat()
    {
        activeCombat = null;
        encounterNpcId = null;
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
        currentRoom = next;
        String toIdBeforeTeleport = currentRoom.getRoomId();
        resolveTeleportIfNeeded();
        exploredRoomIds.add(currentRoom.getRoomId());
        questManager.onRoomEntered(currentRoom, exploredRoomIds);
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
        questManager.onItemTaken(item);
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

    /**
     * 校验物品是否可在当前房间使用（钥匙需靠近对应上锁出口）。
     */
    public ItemUseCheck checkItemUse(String itemId)
    {
        Item item = findInventoryItem(itemId);
        if (item == null) {
            return ItemUseCheck.blocked("背包中没有该物品。");
        }
        String effect = item.getEffect();
        if (effect == null || effect.trim().isEmpty()) {
            return ItemUseCheck.blocked(item.getName() + " 没有可使用的效果。");
        }
        if (effect.startsWith("unlock:")) {
            String lockId = effect.substring("unlock:".length()).trim();
            if (lockId.isEmpty()) {
                return ItemUseCheck.blocked("这把钥匙似乎坏了。");
            }
            if (unlockedLocks.contains(lockId)) {
                return ItemUseCheck.blocked("对应的门已经打开了。");
            }
            if (!hasAdjacentLockedExit(lockId)) {
                return ItemUseCheck.needLocation("钥匙需在通往「" + lockId + "」的上锁出口旁使用。");
            }
            return ItemUseCheck.anytime();
        }
        return ItemUseCheck.anytime();
    }

    public String tryUseItem(String itemId)
    {
        ItemUseCheck check = checkItemUse(itemId);
        if (!check.canUse) {
            lastMessage = check.hint;
            LOG.info("tryUseItem: " + itemId + " blocked -> " + lastMessage);
            return lastMessage;
        }
        return useItem(itemId);
    }

    private boolean hasAdjacentLockedExit(String lockId)
    {
        for (String direction : currentRoom.getExitDirections()) {
            Room next = currentRoom.getExit(direction);
            if (next != null && lockId.equals(next.getLockId())
                && !unlockedLocks.contains(lockId)) {
                return true;
            }
        }
        return false;
    }

    private Item findInventoryItem(String itemId)
    {
        return player.getInventory().stream()
            .filter(i -> i.getItemId().equals(itemId))
            .findFirst().orElse(null);
    }

    public String useItem(String itemId)
    {
        Item item = findInventoryItem(itemId);
        if (item == null) {
            LOG.info("useItem: " + itemId + " = FAIL (not in backpack)");
            lastMessage = "背包中没有 " + itemId;
            return lastMessage;
        }

        UseResult result = useEffectRegistry.apply(this, item);
        if (result.isSuccess()) {
            player.removeItem(itemId);
        }
        lastMessage = result.getMessage();
        LOG.info("useItem: " + itemId + " -> " + lastMessage);
        return lastMessage;
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
        endDialogue();
        try {
            activeDialogueTree = DialogueLoader.load(npcId);
            activeDialogueNodeId = activeDialogueTree.getStartNodeId();
            LOG.info("talkNpc: " + npcId + " start=" + activeDialogueNodeId);
            return presentDialogueNode(activeDialogueTree.getStartNode());
        } catch (IOException ex) {
            LOG.warning("talkNpc: failed to load dialogue for " + npcId + ": " + ex.getMessage());
            lastMessage = "无法与 " + npcId + " 对话。";
            return new Dialogue(npcId, lastMessage, new ArrayList<>(), false);
        }
    }

    public Dialogue chooseDialogueOption(int optionIndex)
    {
        if (activeDialogueTree == null || activeDialogueNodeId == null) {
            lastMessage = "当前没有进行中的对话。";
            return new Dialogue("", lastMessage, new ArrayList<>(), false);
        }
        DialogueNode current = activeDialogueTree.getNode(activeDialogueNodeId);
        if (current == null || optionIndex < 0 || optionIndex >= current.getOptions().size()) {
            lastMessage = "无效的对话选项。";
            return new Dialogue(activeDialogueTree.getNpcId(), lastMessage,
                new ArrayList<>(), false);
        }
        DialogueOption option = current.getOptions().get(optionIndex);
        DialogueNode next = activeDialogueTree.getNode(option.getNextNodeId());
        if (next == null) {
            endDialogue();
            lastMessage = "对话数据损坏。";
            return new Dialogue(activeDialogueTree.getNpcId(), lastMessage,
                new ArrayList<>(), false);
        }
        activeDialogueNodeId = next.getNodeId();
        LOG.info("talkNpc: " + activeDialogueTree.getNpcId() + " -> node " + activeDialogueNodeId);
        return presentDialogueNode(next);
    }

    public void endDialogue()
    {
        activeDialogueTree = null;
        activeDialogueNodeId = null;
    }

    private Dialogue presentDialogueNode(DialogueNode node)
    {
        String npcId = activeDialogueTree.getNpcId();
        String actionMessage = dialogueActionExecutor.apply(node.getAction());
        if (actionMessage != null) {
            lastMessage = actionMessage;
        }
        List<String> optionTexts = node.getOptions().stream()
            .map(DialogueOption::getText)
            .collect(Collectors.toList());
        if (node.isTerminal()) {
            endDialogue();
            return new Dialogue(npcId, node.getText(), optionTexts, false);
        }
        return new Dialogue(npcId, node.getText(), optionTexts, true);
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
            // moveBack 后贴在来时的门边（entry 已取反，再取反回到门侧）
            spawnSide = entryDirection.opposite();
        } else {
            // 从 entry 方向的来向门进入：落在来向门的对侧（刚进门，靠近来向门）
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
        questManager.restoreQuestStates(state.getQuestStates());
        defeatedNpcs.clear();
        if (state.getDefeatedNpcs() != null) {
            defeatedNpcs.addAll(state.getDefeatedNpcs());
        }
        clearCombat();
        endDialogue();
        entryDirection = Direction.fromExitKey(state.getEntryDirection());
        spawnAtDoorSide = false;
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
        return createKnownItem(itemId);
    }

    private Item createKnownItem(String itemId)
    {
        switch (itemId) {
            case "welcome-note":
                return new Item("welcome-note", "note", "A crumpled welcome note.", 1, null);
            case "key-vault":
                return new Item("key-vault", "Rusty Key",
                    "A rusty key, inscribed 'Vault'.", 1, "unlock:vault-door");
            case "key-guard":
                return new Item("key-guard", "Iron Key",
                    "A heavy iron key, marked 'Guard Gate'.", 2, "unlock:guard-gate");
            case "gem-light":
                return new Item("gem-light", "Light Gem",
                    "A radiant gem pulsing with pure light.", 5, "light:full");
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
