package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：light 或 light:&lt;mode&gt;
 *
 * 当前无暗室机制，光照道具仅提供信息提示，不消耗物品。
 * 战斗中通过 {@code CombatActionRegistry} 单独处理致盲效果。
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
            return UseResult.fail(item.getName()
                + " 散发着纯净的光芒。传说它曾属于 Realm 的初代编年史者——\n"
                + "拥有它的人，将面临最终的选择。");
        }
        return UseResult.fail(item.getName()
            + " 照亮了周围。战斗中用它可以让敌人短暂失明。");
    }
}
