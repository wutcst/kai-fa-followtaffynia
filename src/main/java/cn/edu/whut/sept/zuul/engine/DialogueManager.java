package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.DialogueNode;
import cn.edu.whut.sept.zuul.domain.DialogueOption;
import cn.edu.whut.sept.zuul.domain.DialogueTree;
import cn.edu.whut.sept.zuul.infra.DialogueLoader;
import cn.edu.whut.sept.zuul.infra.GameLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 对话管理器 —— 从 GameEngine 提取。
 */
public class DialogueManager
{
    private static final Logger LOG = GameLogger.get();

    private final DialogueActionExecutor actionExecutor;
    private final Consumer<String> onLastMessage;

    private DialogueTree activeDialogueTree;
    private String activeDialogueNodeId;

    public DialogueManager(DialogueActionExecutor actionExecutor,
                            Consumer<String> onLastMessage)
    {
        this.actionExecutor = actionExecutor;
        this.onLastMessage = onLastMessage;
    }

    public boolean isInDialogue()
    {
        return activeDialogueTree != null;
    }

    /** 该 NPC 是否拥有对话数据（用于决定 mercy 后是否开启和解对话）。 */
    public boolean hasDialogue(String npcId)
    {
        try {
            DialogueLoader.load(npcId);
            return true;
        } catch (IOException ex) {
            return false;
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
            String msg = "无法与 " + npcId + " 对话。";
            onLastMessage.accept(msg);
            return new Dialogue(npcId, msg, new ArrayList<>(), false);
        }
    }

    /** 从指定节点启动对话 */
    public Dialogue talkNpcWithStart(String npcId, String startNodeId)
    {
        endDialogue();
        try {
            activeDialogueTree = DialogueLoader.load(npcId);
            activeDialogueNodeId = startNodeId;
            DialogueNode startNode = activeDialogueTree.getNode(startNodeId);
            if (startNode == null) {
                LOG.warning("talkNpcWithStart: node " + startNodeId + " not found, fallback");
                return talkNpc(npcId);
            }
            LOG.info("talkNpcWithStart: " + npcId + " start=" + startNodeId);
            return presentDialogueNode(startNode);
        } catch (IOException ex) {
            return talkNpc(npcId);
        }
    }

    public Dialogue chooseDialogueOption(int optionIndex)
    {
        if (activeDialogueTree == null || activeDialogueNodeId == null) {
            String msg = "当前没有进行中的对话。";
            onLastMessage.accept(msg);
            return new Dialogue("", msg, new ArrayList<>(), false);
        }
        DialogueNode current = activeDialogueTree.getNode(activeDialogueNodeId);
        if (current == null || optionIndex < 0 || optionIndex >= current.getOptions().size()) {
            String msg = "无效的对话选项。";
            onLastMessage.accept(msg);
            return new Dialogue(activeDialogueTree.getNpcId(), msg,
                new ArrayList<>(), false);
        }
        DialogueOption option = current.getOptions().get(optionIndex);
        DialogueNode next = activeDialogueTree.getNode(option.getNextNodeId());
        if (next == null) {
            endDialogue();
            String msg = "对话数据损坏。";
            onLastMessage.accept(msg);
            return new Dialogue(activeDialogueTree.getNpcId(), msg,
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
        String actionMessage = actionExecutor.apply(node.getAction());
        if (actionMessage != null) {
            onLastMessage.accept(actionMessage);
        }
        java.util.List<String> optionTexts = node.getOptions().stream()
            .map(DialogueOption::getText)
            .collect(Collectors.toList());
        if (node.isTerminal()) {
            endDialogue();
            return new Dialogue(npcId, node.getText(), optionTexts, false);
        }
        return new Dialogue(npcId, node.getText(), optionTexts, true);
    }
}
