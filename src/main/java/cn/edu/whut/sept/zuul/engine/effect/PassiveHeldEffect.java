package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理被动装备效果（背包中持有即生效，不消耗）。
 * <p>
 * effect 格式：passive:&lt;描述文字&gt;
 */
public class PassiveHeldEffect implements UseEffect
{
    private static final String PREFIX = "passive:";

    @Override
    public boolean supports(String effect)
    {
        return effect != null && effect.startsWith(PREFIX);
    }

    @Override
    public UseResult apply(GameEngine engine, Item item, String effect)
    {
        String desc = effect.substring(PREFIX.length());
        // 不消耗物品，仅展示效果说明
        return UseResult.fail(item.getName() + " 是装备——" + desc + "。背包中持有即生效，无需使用。");
    }
}
