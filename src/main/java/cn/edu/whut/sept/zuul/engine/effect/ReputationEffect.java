package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：reputation:&lt;delta&gt;（支持 +5 写法）
 */
public class ReputationEffect implements UseEffect
{
    private static final String PREFIX = "reputation:";

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
            int repDelta = Integer.parseInt(value);
            engine.getPlayer().setReputation(engine.getPlayer().getReputation() + repDelta);
            return UseResult.ok("使用了 " + item.getName() + "，声望 "
                + (repDelta > 0 ? "+" : "") + repDelta + "！");
        } catch (NumberFormatException ex) {
            return UseResult.fail(item.getName() + " 没有产生任何变化。");
        }
    }
}
