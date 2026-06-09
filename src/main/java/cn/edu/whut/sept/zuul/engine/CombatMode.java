package cn.edu.whut.sept.zuul.engine;

/**
 * 战斗模式选择。
 */
public enum CombatMode
{
    /** 传统回合制（RPG 互殴） */
    TURN_BASED,

    /** Undertale 风格（菜单 + 弹幕躲避） */
    UNDERTALE;
}
