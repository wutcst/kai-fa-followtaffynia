package cn.edu.whut.sept.zuul.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 assets/combat/&lt;npcId&gt;.json 解析的战斗定义。
 * v3: 支持 battleLines（战前/半血/低血/MERCY 台词 + 颜色）。
 */
public final class NpcCombatDef
{
    public final String npcId;
    public final String displayName;
    public final int maxHp;
    public final String defaultState;
    public final Map<String, Double> stateHpThresholds;
    public final Map<String, List<String>> stateSkills;
    public final Map<String, NpcSkill> skills;
    public final int onDefeatReputation;
    public final String onDefeatUnlock;
    public final boolean onDefeatMarkDefeated;
    /** 战斗胜利后掉落到当前房间的物品 ID（null=不掉落） */
    public final String onDefeatSpawnItem;
    public final Map<String, String> actOptions;

    /** 战斗台词：key=start|hp50|hp10|mercy1|mercy2 → "color: text" */
    public final Map<String, BattleLine> battleLines;

    public NpcCombatDef(String npcId, String displayName, int maxHp, String defaultState,
        Map<String, Double> stateHpThresholds,
        Map<String, List<String>> stateSkills,
        Map<String, NpcSkill> skills,
        int onDefeatReputation, String onDefeatUnlock, boolean onDefeatMarkDefeated)
    {
        this(npcId, displayName, maxHp, defaultState,
            stateHpThresholds, stateSkills, skills,
            onDefeatReputation, onDefeatUnlock, onDefeatMarkDefeated,
            null, Collections.emptyMap(), Collections.emptyMap());
    }

    public NpcCombatDef(String npcId, String displayName, int maxHp, String defaultState,
        Map<String, Double> stateHpThresholds,
        Map<String, List<String>> stateSkills,
        Map<String, NpcSkill> skills,
        int onDefeatReputation, String onDefeatUnlock, boolean onDefeatMarkDefeated,
        String onDefeatSpawnItem,
        Map<String, String> actOptions,
        Map<String, BattleLine> battleLines)
    {
        this.npcId = npcId;
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.defaultState = defaultState;
        this.stateHpThresholds = Collections.unmodifiableMap(new HashMap<>(stateHpThresholds));
        this.stateSkills = Collections.unmodifiableMap(new HashMap<>(stateSkills));
        this.skills = Collections.unmodifiableMap(new HashMap<>(skills));
        this.onDefeatReputation = onDefeatReputation;
        this.onDefeatUnlock = onDefeatUnlock;
        this.onDefeatMarkDefeated = onDefeatMarkDefeated;
        this.onDefeatSpawnItem = onDefeatSpawnItem;
        this.actOptions = Collections.unmodifiableMap(new HashMap<>(actOptions));
        this.battleLines = Collections.unmodifiableMap(new HashMap<>(battleLines));
    }

    public static final class NpcSkill
    {
        public final String id;
        public final int damage;
        public final String text;
        public final boolean appliesDefense;

        public NpcSkill(String id, int damage, String text, boolean appliesDefense)
        {
            this.id = id; this.damage = damage; this.text = text; this.appliesDefense = appliesDefense;
        }
    }

    /** 战斗台词：文本 + 颜色标签 */
    public static final class BattleLine
    {
        public final String text;
        public final String color;  // red|green|blue|white|pink

        public BattleLine(String text, String color)
        {
            this.text = text; this.color = color;
        }
    }
}
