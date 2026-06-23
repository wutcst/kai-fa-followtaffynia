package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.infra.SaveGameService.SlotInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveGameServiceTest
{
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_roundTripsGameState() throws Exception
    {
        Path savePath = tempDir.resolve("slot1.sav");
        GameState state = new GameState();
        state.setPlayerName("测试者");
        state.setCurrentRoomId("outside");
        state.setPlayerX(64f);
        state.setPlayerY(96f);
        state.setEntryDirection("east");
        state.setHp(80);
        state.setMaxHp(100);
        state.setMaxWeight(70.0);
        state.setReputation(5);
        state.getInventory().add("welcome-note");
        state.getExploredRoomIds().add("outside");
        state.getRoomItems().put("outside", java.util.Arrays.asList("torch", "magic-cookie"));

        SaveGameService.save(savePath, state);
        GameState loaded = SaveGameService.load(savePath);

        assertTrue(savePath.toFile().isFile());
        assertEquals("测试者", loaded.getPlayerName());
        assertEquals("outside", loaded.getCurrentRoomId());
        assertEquals(64f, loaded.getPlayerX());
        assertEquals(96f, loaded.getPlayerY());
        assertEquals("east", loaded.getEntryDirection());
        assertEquals(80, loaded.getHp());
        assertEquals(70.0, loaded.getMaxWeight(), 0.01);
        assertEquals(5, loaded.getReputation());
        assertEquals("welcome-note", loaded.getInventory().get(0));
        assertTrue(loaded.getExploredRoomIds().contains("outside"));
        assertEquals(2, loaded.getRoomItems().get("outside").size());
        assertEquals("magic-cookie", loaded.getRoomItems().get("outside").get(1));
    }

    @Test
    void multipleSlots_storeIndependentStates() throws Exception
    {
        Path slot2 = tempDir.resolve("slot2.sav");
        Path slot3 = tempDir.resolve("slot3.sav");

        GameState a = new GameState();
        a.setPlayerName("甲");
        a.setCurrentRoomId("library");
        GameState b = new GameState();
        b.setPlayerName("乙");
        b.setCurrentRoomId("armory");

        SaveGameService.save(slot2, a);
        SaveGameService.save(slot3, b);

        assertEquals("甲", SaveGameService.load(slot2).getPlayerName());
        assertEquals("乙", SaveGameService.load(slot3).getPlayerName());
        assertEquals("library", SaveGameService.load(slot2).getCurrentRoomId());
        assertEquals("armory", SaveGameService.load(slot3).getCurrentRoomId());
    }

    @Test
    void slotPath_rejectsOutOfRange()
    {
        assertThrows(IllegalArgumentException.class, () -> SaveGameService.slotPath(0));
        assertThrows(IllegalArgumentException.class, () -> SaveGameService.slotPath(SaveGameService.MAX_SLOTS + 1));
        // 合法范围内不抛异常，且文件名包含槽位号
        assertTrue(SaveGameService.slotPath(1).toString().contains("slot1"));
        assertTrue(SaveGameService.slotPath(SaveGameService.MAX_SLOTS).toString()
            .contains("slot" + SaveGameService.MAX_SLOTS));
    }

    @Test
    void describeAll_returnsOneInfoPerSlot()
    {
        // 只读操作，不写文件；验证返回 5 项且槽位编号对应
        List<SlotInfo> infos = SaveGameService.describeAll();
        assertEquals(SaveGameService.MAX_SLOTS, infos.size());
        for (int i = 0; i < infos.size(); i++) {
            assertEquals(i + 1, infos.get(i).getSlot());
        }
    }

    @Test
    void slotInfo_reflectsStateAndFormatsTime()
    {
        SlotInfo empty = SlotInfo.empty(2);
        assertFalse(empty.exists());
        assertFalse(empty.isLoadable());
        assertEquals("", empty.formattedTime());

        SlotInfo corrupt = SlotInfo.corrupt(3, 0L);
        assertTrue(corrupt.exists());
        assertTrue(corrupt.isCorrupt());
        assertFalse(corrupt.isLoadable());

        long fixedTime = 1_700_000_000_000L; // 固定时间戳，验证格式化非空
        SlotInfo valid = SlotInfo.of(4, fixedTime, "测试者", "outside");
        assertTrue(valid.exists());
        assertTrue(valid.isLoadable());
        assertEquals("测试者", valid.getPlayerName());
        assertEquals("outside", valid.getRoomId());
        assertEquals(16, valid.formattedTime().length()); // yyyy-MM-dd HH:mm
    }
}
