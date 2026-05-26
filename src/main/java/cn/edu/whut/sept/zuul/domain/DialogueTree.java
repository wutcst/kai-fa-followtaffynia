package cn.edu.whut.sept.zuul.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 完整 NPC 对话树。
 */
public final class DialogueTree
{
    private final String npcId;
    private final String startNodeId;
    private final Map<String, DialogueNode> nodes;

    public DialogueTree(String npcId, String startNodeId, Map<String, DialogueNode> nodes)
    {
        this.npcId = npcId;
        this.startNodeId = startNodeId;
        this.nodes = Collections.unmodifiableMap(new HashMap<>(nodes));
    }

    public String getNpcId()
    {
        return npcId;
    }

    public String getStartNodeId()
    {
        return startNodeId;
    }

    public DialogueNode getNode(String nodeId)
    {
        return nodes.get(nodeId);
    }

    public DialogueNode getStartNode()
    {
        return nodes.get(startNodeId);
    }
}
