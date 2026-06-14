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
            double delta = Double.parseDouble(value);
            double oldMax = engine.getPlayer().getMaxWeight();
            engine.getPlayer().setMaxWeight(oldMax + delta);
            return UseResult.ok("吃下了 " + item.getName() + "，负重上限 +" + fmt(delta)
                + "！（" + fmt(oldMax) + " → " + fmt(engine.getPlayer().getMaxWeight()) + "）");
        } catch (NumberFormatException ex) {
            return UseResult.fail(item.getName() + " 似乎已过期。");
        }
    }

    private static String fmt(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.format("%.1f", v);
    }
}
