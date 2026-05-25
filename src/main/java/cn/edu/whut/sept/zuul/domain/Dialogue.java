package cn.edu.whut.sept.zuul.domain;

import java.util.Collections;
import java.util.List;

/**
 * NPC 对话数据（由 JSON 解析后交给 DialogueBox 渲染）。
 */
public class Dialogue
{
    private final String npcId;
    private final String text;
    private final List<String> optionTexts;

    public Dialogue(String npcId, String text, List<String> optionTexts)
    {
        this.npcId = npcId;
        this.text = text;
        this.optionTexts = optionTexts == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(optionTexts);
    }

    public String getNpcId()
    {
        return npcId;
    }

    public String getText()
    {
        return text;
    }

    public List<String> getOptionTexts()
    {
        return optionTexts;
    }
}
