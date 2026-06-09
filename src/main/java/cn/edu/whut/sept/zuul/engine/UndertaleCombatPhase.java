package cn.edu.whut.sept.zuul.engine;

/**
 * Undertale 式战斗的阶段状态机。
 *
 * <pre>
 *   MENU ──FIGHT──► FIGHT_BAR ──press──► MENU
 *   MENU ──ACT────► ACT_TEXT  ──auto───► MENU
 *   MENU ──ITEM───► MENU      (物品使用后回到菜单)
 *   MENU ──MERCY──► check ──┬─ VICTORY
 *                            └─ ENEMY_TURN
 *
 *   任何玩家动作完成后 → ENEMY_TURN（弹幕阶段）
 *   ENEMY_TURN（弹幕结束）→ MENU
 * </pre>
 */
public enum UndertaleCombatPhase
{
    /** 玩家菜单：FIGHT / ACT / ITEM / MERCY */
    MENU,

    /** 攻击节奏条（玩家在适当时机按键） */
    FIGHT_BAR,

    /** ACT 结果文本展示 */
    ACT_TEXT,

    /** 敌人回合：弹幕躲避 */
    ENEMY_TURN,

    /** 战斗结束 */
    RESULT;
}
