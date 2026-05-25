package cn.edu.whut.sept.zuul.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        state.setMaxWeight(70);
        state.setReputation(5);
        state.getInventory().add("welcome-note");
        state.getExploredRoomIds().add("outside");

        SaveGameService.save(savePath, state);
        GameState loaded = SaveGameService.load(savePath);

        assertTrue(savePath.toFile().isFile());
        assertEquals("测试者", loaded.getPlayerName());
        assertEquals("outside", loaded.getCurrentRoomId());
        assertEquals(64f, loaded.getPlayerX());
        assertEquals(96f, loaded.getPlayerY());
        assertEquals("east", loaded.getEntryDirection());
        assertEquals(80, loaded.getHp());
        assertEquals(70, loaded.getMaxWeight());
        assertEquals(5, loaded.getReputation());
        assertEquals("welcome-note", loaded.getInventory().get(0));
        assertTrue(loaded.getExploredRoomIds().contains("outside"));
    }
}
