package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：heal:&lt;amount&gt;
 */
public class HealEffect implements UseEffect
{
    private static final String PREFIX = "heal:";

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
            int healAmt = Integer.parseInt(value);
            int newHp = Math.min(
                engine.getPlayer().getHp() + healAmt,
                engine.getPlayer().getMaxHp());
            engine.getPlayer().setHp(newHp);
            return UseResult.ok("使用了 " + item.getName() + "，恢复了 " + healAmt + " HP！");
        } catch (NumberFormatException ex) {
            return UseResult.fail(item.getName() + " 似乎已失效。");
        }
    }
}
