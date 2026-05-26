package cn.edu.whut.sept.zuul.domain;

/**
 * 对话树中的一个选项。
 */
public final class DialogueOption
{
    private final String text;
    private final String nextNodeId;

    public DialogueOption(String text, String nextNodeId)
    {
        this.text = text;
        this.nextNodeId = nextNodeId;
    }

    public String getText()
    {
        return text;
    }

    public String getNextNodeId()
    {
        return nextNodeId;
    }
}
