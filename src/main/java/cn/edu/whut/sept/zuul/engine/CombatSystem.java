package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;

/**
 * 战斗系统接口。不同战斗模式（回合制、Undertale 弹幕式等）实现此接口，
 * 由 GameEngine 通过策略模式调用。
 */
public interface CombatSystem
{
    /** 当前战斗快照，供 UI 渲染。 */
    CombatSnapshot snapshot();

    /** 战斗结果：ONGOING / VICTORY / DEFEAT / FLED */
    CombatOutcome getOutcome();

    /** 对手的静态数据。 */
    NpcCombatDef getDef();

    /** 玩家执行战斗动作（回合制入口）。 */
    CombatSnapshot processPlayerAction(CombatAction action, String itemId);

    /** 给玩家加防御 buff（物品效果）。 */
    void addPlayerDefenseBuff(int turns);

    /** 给 NPC 加致盲 debuff（物品效果）。 */
    void addNpcBlindTurns(int turns);

    /** 本次胜利是否为"仁慈/认输"化解（而非击杀）。用于区分尸体 vs 已离去。 */
    default boolean wasSpared() { return false; }
}
