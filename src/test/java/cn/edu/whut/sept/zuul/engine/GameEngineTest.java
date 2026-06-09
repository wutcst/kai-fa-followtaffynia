package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.WorldFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest
{
    private GameEngine engine;

    @BeforeEach
    void setUp()
    {
        engine = new GameEngine("测试者");
    }

    // ==================== 移动 ====================

    @Test
    void movePlayer_validExit_changesRoom()
    {
        // outside 上方(N) → theatre（与 docs/04 一致）
        assertTrue(engine.movePlayer(Direction.NORTH));
        assertEquals("theatre", engine.getCurrentRoom().getRoomId());
    }

    @Test
    void movePlayer_invalidExit_returnsFalse()
    {
        engine.movePlayer(Direction.NORTH);  // outside → theatre
        assertFalse(engine.movePlayer(Direction.NORTH));  // theatre 无 north 出口
    }

    @Test
    void moveBack_returnsToPreviousRoom()
    {
        engine.movePlayer(Direction.NORTH);  // outside → theatre
        assertTrue(engine.moveBack());
        assertEquals("outside", engine.getCurrentRoom().getRoomId());
    }

    @Test
    void moveBack_multipleSteps()
    {
        engine.movePlayer(Direction.NORTH);  // outside → theatre
        engine.movePlayer(Direction.EAST);   // theatre → library
        engine.moveBack();                    // library → theatre
        engine.moveBack();                    // theatre → outside
        assertEquals("outside", engine.getCurrentRoom().getRoomId());
    }

    // ==================== 物品拾取/丢弃 ====================

    @Test
    void takeItem_overWeightFails()
    {
        engine.getPlayer().setMaxWeight(0);
        assertFalse(engine.takeItem("welcome-note"));
        assertEquals(1, engine.getCurrentRoom().getItems().size());
    }

    @Test
    void takeItem_success()
    {
        assertTrue(engine.takeItem("welcome-note"));
        assertTrue(engine.getCurrentRoom().getItems().stream()
            .noneMatch(i -> i.getItemId().equals("welcome-note")));
        assertTrue(engine.getPlayer().getInventory().stream()
            .anyMatch(i -> i.getItemId().equals("welcome-note")));
    }

    @Test
    void restoreState_keepsTakenItemsOutOfRooms()
    {
        assertTrue(engine.takeItem("welcome-note"));
        GameState state = engine.captureState();

        GameEngine loaded = new GameEngine("测试者");
        loaded.restoreState(state);

        assertEquals(1, loaded.getPlayer().getInventory().stream()
            .filter(i -> i.getItemId().equals("welcome-note"))
            .count());
        assertTrue(loaded.getCurrentRoom().getItems().stream()
            .noneMatch(i -> i.getItemId().equals("welcome-note")));
        assertFalse(loaded.takeItem("welcome-note"));
    }

    @Test
    void dropItem_success()
    {
        engine.takeItem("welcome-note");
        assertTrue(engine.dropItem("welcome-note"));
        assertEquals(0, engine.getPlayer().getInventory().size());
        assertTrue(engine.getCurrentRoom().getItems().stream()
            .anyMatch(i -> i.getItemId().equals("welcome-note")));
    }

    @Test
    void dropAllItems()
    {
        engine.takeItem("welcome-note");
        engine.dropAllItems();
        assertEquals(0, engine.getPlayer().getInventory().size());
        assertFalse(engine.getCurrentRoom().getItems().isEmpty());
    }

    // ==================== 传送房 ====================

    @Test
    void teleportRoom_movesAwayImmediately()
    {
        WorldFactory.getRoom("theatre").setTeleport(true);
        assertTrue(engine.movePlayer(Direction.NORTH));  // outside → theatre → teleport
        assertNotEquals("theatre", engine.getCurrentRoom().getRoomId());
    }

    @Test
    void teleportAlcove_existsAndWorks()
    {
        // 从 outside → theatre → library → teleport-alcove
        engine.movePlayer(Direction.NORTH);  // outside → theatre
        engine.movePlayer(Direction.EAST);   // theatre → library
        assertTrue(engine.movePlayer(Direction.EAST));  // library → teleport-alcove
        // 进入传送房后应立即被传走
        assertNotEquals("teleport-alcove", engine.getCurrentRoom().getRoomId());
    }

    // ==================== magic cookie ====================

    @Test
    void eatCookie_increasesMaxWeight()
    {
        int before = engine.getPlayer().getMaxWeight();
        // 模拟获得并吃掉 cookie
        engine.getPlayer().addItem(new Item("magic-cookie", "Magic Cookie",
            "test", 1, "maxWeight:+20"));
        engine.eatItem("magic-cookie");
        assertEquals(before + 20, engine.getPlayer().getMaxWeight());
        // cookie 已从背包移除
        assertTrue(engine.getPlayer().getInventory().stream()
            .noneMatch(i -> i.getItemId().equals("magic-cookie")));
    }

    @Test
    void cookieIsPlacedSomewhere()
    {
        // magic cookie 应在 cellar / library / hidden-shrine 之一中
        boolean found = false;
        String[] candidates = {"cellar", "library", "hidden-shrine"};
        for (String id : candidates) {
            if (WorldFactory.getRoom(id).getItems().stream()
                .anyMatch(i -> i.getItemId().equals("magic-cookie"))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "magic cookie should be placed in one of the 3 candidate rooms");
    }

    // ==================== 房间属性 ====================

    @Test
    void vault_isLocked()
    {
        assertEquals("vault-door", WorldFactory.getRoom("vault").getLockId());
    }

    @Test
    void teleportAlcove_isTeleport()
    {
        assertTrue(WorldFactory.getRoom("teleport-alcove").isTeleport());
    }

    @Test
    void has15Rooms()
    {
        assertEquals(15, WorldFactory.getAllRooms().size());
    }

    @Test
    void totalItemsAtLeast12()
    {
        int count = 0;
        for (Room room : WorldFactory.getAllRooms().values()) {
            count += room.getItems().size();
        }
        // 含 welcome-note + 显式放置的 11 个 + 1 随机 cookie = ≥13
        assertTrue(count >= 12, "Expected >= 12 items, got " + count);
    }

    // ==================== look ====================

    @Test
    void look_returnsDescription()
    {
        String desc = engine.look();
        assertNotNull(desc);
        assertTrue(desc.contains("outside"));
    }

    // ==================== 门锁与谜题 ====================

    @Test
    void lockedRoom_blocksMovement()
    {
        // vault 有 lockId=vault-door，未解锁前不能进入
        goToLab();
        assertFalse(engine.movePlayer(Direction.SOUTH),
            "vault should be locked, cannot enter");
        assertEquals("lab", engine.getCurrentRoom().getRoomId());
        assertTrue(engine.getLastMessage().contains("vault-door"));
    }

    @Test
    void unlockItem_thenEnterLockedRoom()
    {
        // Step 1: 走到 lab，拾取 key-vault
        goToLab();
        assertTrue(engine.takeItem("key-vault"));
        assertTrue(engine.getPlayer().getInventory().stream()
            .anyMatch(i -> i.getItemId().equals("key-vault")));

        // Step 2: 尝试进 vault（仍然锁着）
        assertFalse(engine.movePlayer(Direction.SOUTH));

        // Step 3: 使用 key-vault 解锁
        String result = engine.useItem("key-vault");
        assertTrue(result.contains("已解锁"));
        // 钥匙被消耗
        assertFalse(engine.getPlayer().getInventory().stream()
            .anyMatch(i -> i.getItemId().equals("key-vault")));

        // Step 4: 现在可以进入 vault
        assertTrue(engine.movePlayer(Direction.SOUTH));
        assertEquals("vault", engine.getCurrentRoom().getRoomId());
        // 可以拾取 vault 内的宝物
        assertTrue(engine.takeItem("gem-light"));
    }

    @Test
    void useItem_heal_works()
    {
        engine.getPlayer().setHp(50);
        engine.getPlayer().addItem(new Item("test-heal", "Potion",
            "test", 1, "heal:20"));
        engine.useItem("test-heal");
        assertEquals(70, engine.getPlayer().getHp());
        assertFalse(engine.getPlayer().getInventory().stream()
            .anyMatch(i -> i.getItemId().equals("test-heal")));
    }

    @Test
    void useItem_reputation_works()
    {
        engine.getPlayer().addItem(new Item("test-rep", "Token",
            "test", 1, "reputation:+5"));
        engine.useItem("test-rep");
        assertEquals(5, engine.getPlayer().getReputation());
    }

    @Test
    void useItem_withoutEffect_returnsHint()
    {
        engine.takeItem("welcome-note");
        String result = engine.useItem("welcome-note");
        assertTrue(result.contains("没有特殊效果"));
    }

    @Test
    void checkItemUse_keyRequiresAdjacentLockedExit()
    {
        goToLab();
        engine.takeItem("key-vault");
        ItemUseCheck inLab = engine.checkItemUse("key-vault");
        assertTrue(inLab.canUse);

        engine.movePlayer(Direction.NORTH);
        ItemUseCheck outside = engine.checkItemUse("key-vault");
        assertFalse(outside.canUse);
        assertTrue(outside.requiresLocation);
    }

    @Test
    void tryUseItem_keyBlockedAwayFromDoor()
    {
        goToLab();
        engine.takeItem("key-vault");
        engine.movePlayer(Direction.NORTH);
        String msg = engine.tryUseItem("key-vault");
        assertTrue(msg.contains("上锁出口"));
        assertTrue(engine.getPlayer().getInventory().stream()
            .anyMatch(i -> "key-vault".equals(i.getItemId())));
    }

    @Test
    void checkItemUse_healAnytime()
    {
        engine.getPlayer().addItem(new Item("test-heal", "Potion",
            "test", 1, "heal:20"));
        assertTrue(engine.checkItemUse("test-heal").canUse);
    }

  /** outside 下方(south) → lab */
    private void goToLab()
    {
        engine.movePlayer(Direction.SOUTH);
    }
}
