package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：lore（阅读古籍，获得知识→声望）
 */
public class LoreEffect implements UseEffect
{
    @Override
    public boolean supports(String effect)
    {
        return "lore".equals(effect);
    }

    @Override
    public UseResult apply(GameEngine engine, Item item, String effect)
    {
        engine.getPlayer().setReputation(engine.getPlayer().getReputation() + 3);
        return UseResult.ok("翻阅了 " + item.getName() + "，获得了远古知识，声望 +3！");
    }
}
