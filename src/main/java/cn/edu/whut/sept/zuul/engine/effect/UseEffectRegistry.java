package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

import java.util.Arrays;
import java.util.List;

public final class UseEffectRegistry
{
    private final List<UseEffect> effects;

    public UseEffectRegistry()
    {
        effects = Arrays.asList(
            new UnlockDoorEffect(),
            new HealEffect(),
            new LightEffect(),
            new ReputationEffect()
        );
    }

    public UseResult apply(GameEngine engine, Item item)
    {
        String effect = item.getEffect();
        if (effect == null || effect.trim().isEmpty()) {
            return UseResult.fail(item.getName() + " 没有特殊效果");
        }
        for (UseEffect handler : effects) {
            if (handler.supports(effect)) {
                return handler.apply(engine, item, effect);
            }
        }
        return UseResult.fail(item.getName() + " 暂时无法使用");
    }
}
