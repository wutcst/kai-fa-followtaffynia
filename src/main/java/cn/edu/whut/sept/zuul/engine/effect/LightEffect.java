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
        // 当前没有暗室机制，不消耗物品
        if ("light:full".equals(effect)) {
            return UseResult.fail(item.getName() + " 闪耀着纯净的光芒，但这里不需要光源。");
        }
        return UseResult.fail(item.getName() + " 照亮了周围，但这里足够明亮，不需要它。");
    }
}
