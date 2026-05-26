package cn.edu.whut.sept.zuul.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话树节点（由 JSON 解析）。
 */
public final class DialogueNode
{
    private final String nodeId;
    private final String text;
    private final String action;
    private final List<DialogueOption> options;

    public DialogueNode(String nodeId, String text, String action, List<DialogueOption> options)
    {
        this.nodeId = nodeId;
        this.text = text;
        this.action = action;
        this.options = options == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(options));
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public String getText()
    {
        return text;
    }

    public String getAction()
    {
        return action;
    }

    public List<DialogueOption> getOptions()
    {
        return options;
    }

    public boolean isTerminal()
    {
        return options.isEmpty();
    }
}
