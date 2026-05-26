package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.DialogueNode;
import cn.edu.whut.sept.zuul.domain.DialogueTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueLoaderTest
{
    @Test
    void loadGuardDialogue() throws Exception
    {
        DialogueTree tree = DialogueLoader.load("guard");
        assertEquals("guard", tree.getNpcId());
        assertEquals("greeting", tree.getStartNodeId());

        DialogueNode greeting = tree.getStartNode();
        assertNotNull(greeting);
        assertTrue(greeting.getText().contains("站住"));
        assertEquals(2, greeting.getOptions().size());

        DialogueNode friendly = tree.getNode("friendly");
        assertNotNull(friendly);
        assertEquals("unlock:guard-gate", friendly.getAction());
        assertTrue(friendly.isTerminal());
    }

    @Test
    void parseHermitDialogueInline()
    {
        String json = "{\n"
            + "  \"npcId\": \"hermit\",\n"
            + "  \"startNode\": \"greeting\",\n"
            + "  \"nodes\": {\n"
            + "    \"greeting\": {\n"
            + "      \"text\": \"hello\",\n"
            + "      \"options\": [{\"text\": \"ok\", \"next\": \"done\"}]\n"
            + "    },\n"
            + "    \"done\": {\n"
            + "      \"text\": \"bye\",\n"
            + "      \"action\": \"reputation:+3\",\n"
            + "      \"next\": null\n"
            + "    }\n"
            + "  }\n"
            + "}";
        DialogueTree tree = DialogueLoader.parse(json, "hermit");
        assertEquals("hermit", tree.getNpcId());
        assertFalse(tree.getNode("greeting").isTerminal());
        assertTrue(tree.getNode("done").isTerminal());
    }
}
