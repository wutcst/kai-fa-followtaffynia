package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.infra.WorldFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void movePlayer_validExit_changesRoom()
    {
        assertTrue(engine.movePlayer(Direction.EAST));
        assertEquals("theatre", engine.getCurrentRoom().getRoomId());
    }

    @Test
    void movePlayer_invalidExit_returnsFalse()
    {
        assertFalse(engine.movePlayer(Direction.NORTH));
    }

    @Test
    void moveBack_returnsToPreviousRoom()
    {
        engine.movePlayer(Direction.EAST);
        assertTrue(engine.moveBack());
        assertEquals("outside", engine.getCurrentRoom().getRoomId());
    }

    @Test
    void takeItem_overWeightFails()
    {
        engine.getPlayer().setMaxWeight(0);
        assertFalse(engine.takeItem("welcome-note"));
        assertEquals(1, engine.getCurrentRoom().getItems().size());
    }

    @Test
    void teleportRoom_movesAwayImmediately()
    {
        WorldFactory.getRoom("theatre").setTeleport(true);
        assertTrue(engine.movePlayer(Direction.EAST));
        assertNotEquals("theatre", engine.getCurrentRoom().getRoomId());
    }
}
