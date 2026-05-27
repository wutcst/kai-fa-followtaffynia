package cn.edu.whut.sept.zuul.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 assets/combat/&lt;npcId&gt;.json 解析的战斗定义。
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

    public NpcCombatDef(String npcId, String displayName, int maxHp, String defaultState,
        Map<String, Double> stateHpThresholds,
        Map<String, List<String>> stateSkills,
        Map<String, NpcSkill> skills,
        int onDefeatReputation, String onDefeatUnlock, boolean onDefeatMarkDefeated)
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
    }

    public static final class NpcSkill
    {
        public final String id;
        public final int damage;
        public final String text;
        public final boolean appliesDefense;

        public NpcSkill(String id, int damage, String text, boolean appliesDefense)
        {
            this.id = id;
            this.damage = damage;
            this.text = text;
            this.appliesDefense = appliesDefense;
        }
    }
}
