package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 物品使用策略。不同 effect 类型对应不同实现。
 */
public interface UseEffect
{
    boolean supports(String effect);

    UseResult apply(GameEngine engine, Item item, String effect);
}
