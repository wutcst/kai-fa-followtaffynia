package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DialogueUiTest
{
    @Test
    void staleTrialFlagsAreClearedWhenAConversationStarts()
    {
        assertStaleFlagCleared("priest", "accepted-priest-trial");
        assertStaleFlagCleared("follower", "accepted-follower-trial");
        assertStaleFlagCleared("apprentice", "accepted-apprentice-spar");
    }

    @Test
    void trialCombatRequestIsConsumedExactlyOnce()
    {
        GameEngine engine = new GameEngine("测试者");
        DialogueUi ui = newUi(engine);
        ui.startDialogue(engine.talkNpc("priest"));
        engine.setFlag("accepted-priest-trial");

        assertEquals("priest", ui.consumePendingCombatRequest("priest"));
        assertFalse(engine.hasFlag("accepted-priest-trial"));
        assertNull(ui.consumePendingCombatRequest("priest"));
    }

    @Test
    void passedTrialCannotRestartCombatAfterPostTrialDialogue()
    {
        GameEngine engine = new GameEngine("测试者");
        DialogueUi ui = newUi(engine);
        engine.setFlag("passed-follower");
        ui.startDialogue(engine.talkNpc("follower"));
        engine.setFlag("accepted-follower-trial");

        assertNull(ui.consumePendingCombatRequest("follower"));
        assertFalse(engine.hasFlag("accepted-follower-trial"));
    }

    private void assertStaleFlagCleared(String npcId, String flag)
    {
        GameEngine engine = new GameEngine("测试者");
        DialogueUi ui = newUi(engine);
        engine.setFlag(flag);
        Dialogue dialogue = engine.talkNpc(npcId);

        ui.startDialogue(dialogue);

        assertFalse(engine.hasFlag(flag));
        assertNull(ui.consumePendingCombatRequest(npcId));
    }

    private DialogueUi newUi(GameEngine engine)
    {
        return new DialogueUi(engine, null, null, null, null, null, null, null);
    }
}
