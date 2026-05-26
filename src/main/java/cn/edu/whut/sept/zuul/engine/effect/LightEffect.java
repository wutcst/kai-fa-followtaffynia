package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：light 或 light:&lt;mode&gt;
 */
public class LightEffect implements UseEffect
{
    @Override
    public boolean supports(String effect)
    {
        return effect != null && (effect.equals("light") || effect.startsWith("light:"));
    }

    @Override
    public UseResult apply(GameEngine engine, Item item, String effect)
    {
        if ("light:full".equals(effect)) {
            return UseResult.ok("使用了 " + item.getName() + "，光明之力充盈全身！");
        }
        return UseResult.ok("使用了 " + item.getName() + "，周围亮了起来！");
    }
}
