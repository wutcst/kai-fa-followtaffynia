package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：maxWeight:&lt;delta&gt;（增加负重上限）
 */
public class MaxWeightEffect implements UseEffect
{
    private static final String PREFIX = "maxWeight:";

    @Override
    public boolean supports(String effect)
    {
        return effect != null && effect.startsWith(PREFIX);
    }

    @Override
    public UseResult apply(GameEngine engine, Item item, String effect)
    {
        String value = effect.substring(PREFIX.length()).trim();
        try {
            int delta = Integer.parseInt(value);
            int oldMax = engine.getPlayer().getMaxWeight();
            engine.getPlayer().setMaxWeight(oldMax + delta);
            return UseResult.ok("吃下了 " + item.getName() + "，负重上限 +" + delta
                + "！（" + oldMax + " → " + engine.getPlayer().getMaxWeight() + "）");
        } catch (NumberFormatException ex) {
            return UseResult.fail(item.getName() + " 似乎已过期。");
        }
    }
}
