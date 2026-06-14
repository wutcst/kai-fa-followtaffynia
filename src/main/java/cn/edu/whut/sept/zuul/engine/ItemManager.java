package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.infra.GameLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 物品管理器 —— 拾取、丢弃、使用、食用物品。
 */
public class ItemManager
{
    private static final Logger LOG = GameLogger.get();

    private final GameEngine engine;
    private final Set<String> unlockedLocks;
    private final QuestManager questManager;
    private final Consumer<String> onMessage;

    public ItemManager(GameEngine engine, Set<String> unlockedLocks,
                        QuestManager questManager, Consumer<String> onMessage)
    {
        this.engine = engine;
        this.unlockedLocks = unlockedLocks;
        this.questManager = questManager;
        this.onMessage = onMessage;
    }

    public boolean takeItem(String itemId)
    {
        Room room = engine.getCurrentRoom();
        Item item = room.removeItem(itemId);
        if (item == null) return false;
        cn.edu.whut.sept.zuul.domain.Player player = engine.getPlayer();
        if (player.totalWeight() + item.getWeight() > player.getMaxWeight()) {
            room.addItem(item);
            return false;
        }
        player.addItem(item);
        questManager.onItemTaken(item);
        LOG.info("takeItem: " + itemId + " from " + room.getRoomId());
        return true;
    }

    public boolean dropItem(String itemId)
    {
        Item item = engine.getPlayer().removeItem(itemId);
        if (item == null) return false;
        engine.getCurrentRoom().addItem(item);
        return true;
    }

    public void dropAllItems()
    {
        List<Item> copy = new ArrayList<>(engine.getPlayer().getInventory());
        Room room = engine.getCurrentRoom();
        for (Item item : copy) { engine.getPlayer().removeItem(item.getItemId()); room.addItem(item); }
    }

    public ItemUseCheck checkItemUse(String itemId)
    {
        Item item = findInventoryItem(itemId);
        if (item == null) return ItemUseCheck.blocked("背包中没有该物品。");
        String effect = item.getEffect();
        if (effect == null || effect.trim().isEmpty())
            return ItemUseCheck.blocked(item.getName() + " 没有可使用的效果。");
        if (effect.startsWith("passive:")) {
            String desc = effect.substring("passive:".length());
            return ItemUseCheck.allowed("被动: " + desc);
        }
        if (effect.startsWith("unlock:")) {
            String lockId = effect.substring("unlock:".length()).trim();
            if (lockId.isEmpty()) return ItemUseCheck.blocked("这把钥匙似乎坏了。");
            if (unlockedLocks.contains(lockId)) return ItemUseCheck.blocked("对应的门已经打开了。");
            if (!hasAdjacentLockedExit(lockId))
                return ItemUseCheck.needLocation("钥匙需在通往「" + lockId + "」的上锁出口旁使用。");
            return ItemUseCheck.anytime();
        }
        return ItemUseCheck.anytime();
    }

    public String tryUseItem(String itemId)
    {
        ItemUseCheck check = checkItemUse(itemId);
        if (!check.canUse) { onMessage.accept(check.hint); return check.hint; }
        return engine.useItem(itemId);
    }

    public void eatItem(String itemId)
    {
        Item item = engine.getPlayer().removeItem(itemId);
        if (item != null && item.isMagicCookie()) {
            int before = engine.getPlayer().getMaxWeight();
            engine.getPlayer().setMaxWeight(before + Item.COOKIE_WEIGHT_BOOST);
        } else if (item != null) {
            engine.getPlayer().addItem(item);
        }
    }

    private Item findInventoryItem(String itemId)
    {
        return engine.getPlayer().getInventory().stream()
            .filter(i -> i.getItemId().equals(itemId)).findFirst().orElse(null);
    }

    private boolean hasAdjacentLockedExit(String lockId)
    {
        for (String dir : engine.getCurrentRoom().getExitDirections()) {
            Room next = engine.getCurrentRoom().getExit(dir);
            if (next != null && lockId.equals(next.getLockId()) && !unlockedLocks.contains(lockId))
                return true;
        }
        return false;
    }
}
