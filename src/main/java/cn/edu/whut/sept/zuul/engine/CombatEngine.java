package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.NpcCombatDef.NpcSkill;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 单场回合制战斗逻辑（无 LibGDX 依赖）。
 */
public final class CombatEngine implements CombatSystem
{
    private static final int BASE_ATTACK = 10;
    private static final int SWORD_BONUS = 3;
    private static final double FLEE_CHANCE = 0.7;

    private final Player player;
    private final NpcCombatDef def;
    private final CombatActionRegistry itemRegistry;
    private final Random random;
    private final List<String> log;

    private int npcHp;
    private boolean playerDefending;
    private boolean npcDefending;
    private int playerDefenseBuffTurns;
    private int npcBlindTurns;
    private CombatOutcome outcome;

    public CombatEngine(Player player, NpcCombatDef def, CombatActionRegistry itemRegistry)
    {
        this(player, def, itemRegistry, new Random());
    }

    CombatEngine(Player player, NpcCombatDef def, CombatActionRegistry itemRegistry, Random random)
    {
        this.player = player;
        this.def = def;
        this.itemRegistry = itemRegistry;
        this.random = random;
        this.log = new ArrayList<>();
        this.npcHp = def.maxHp;
        this.outcome = CombatOutcome.ONGOING;
    }

    public NpcCombatDef getDef()
    {
        return def;
    }

    public CombatOutcome getOutcome()
    {
        return outcome;
    }

    public void addPlayerDefenseBuff(int turns)
    {
        playerDefenseBuffTurns = Math.max(playerDefenseBuffTurns, turns);
    }

    public void addNpcBlindTurns(int turns)
    {
        npcBlindTurns = Math.max(npcBlindTurns, turns);
    }

    public CombatSnapshot processPlayerAction(CombatAction action, String itemId)
    {
        if (outcome != CombatOutcome.ONGOING) {
            return snapshot();
        }

        playerDefending = false;
        switch (action) {
            case ATTACK:
                doPlayerAttack();
                break;
            case DEFEND:
                playerDefending = true;
                log.add("你举起防御姿态。");
                break;
            case USE_ITEM:
                if (itemId == null || itemId.isEmpty()) {
                    log.add("请指定要使用的物品。");
                    return snapshot();
                }
                CombatActionRegistry.CombatItemResult itemResult =
                    itemRegistry.apply(this, player, itemId);
                log.add(itemResult.message);
                if (!itemResult.success) {
                    return snapshot();
                }
                break;
            case FLEE:
                if (random.nextDouble() < FLEE_CHANCE) {
                    outcome = CombatOutcome.FLED;
                    log.add("你成功逃离了战斗。");
                    return snapshot();
                }
                log.add("逃跑失败！");
                break;
            default:
                log.add("无效的战斗动作。");
                return snapshot();
        }

        if (outcome == CombatOutcome.ONGOING) {
            resolveNpcTurn();
        }
        checkEndConditions();
        return snapshot();
    }

    private void doPlayerAttack()
    {
        int damage = BASE_ATTACK;
        if (hasItem("sword-rusty")) {
            damage += SWORD_BONUS;
        }
        if (npcDefending) {
            damage = Math.max(1, damage / 2);
            npcDefending = false;
            log.add("守卫的防御抵消了部分伤害。");
        }
        npcHp = Math.max(0, npcHp - damage);
        log.add("你攻击守卫，造成 " + damage + " 点伤害。");
    }

    private void resolveNpcTurn()
    {
        String stateId = pickStateId();
        NpcSkill skill = pickSkill(stateId);
        if (skill == null) {
            log.add("守卫犹豫不决。");
            return;
        }
        log.add(skill.text);
        if (skill.appliesDefense) {
            npcDefending = true;
            return;
        }
        if (skill.damage <= 0) {
            return;
        }

        int damage = skill.damage;
        if (npcBlindTurns > 0 && random.nextDouble() < 0.3) {
            log.add("守卫被火光晃眼，攻击落空！");
            npcBlindTurns--;
            return;
        }
        if (npcBlindTurns > 0) {
            npcBlindTurns--;
        }

        if (playerDefending) {
            damage = Math.max(1, damage / 2);
        }
        if (playerDefenseBuffTurns > 0) {
            damage = Math.max(1, damage / 2);
            playerDefenseBuffTurns--;
        }
        if (hasItem("shield-wooden")) {
            damage = Math.max(1, damage - 3);
        }

        player.setHp(Math.max(0, player.getHp() - damage));
        log.add("你受到 " + damage + " 点伤害。");
    }

    private String pickStateId()
    {
        double ratio = (double) npcHp / def.maxHp;
        String best = def.defaultState;
        double bestThreshold = 2.0;
        for (Map.Entry<String, Double> e : def.stateHpThresholds.entrySet()) {
            double threshold = e.getValue();
            if (ratio <= threshold && threshold < bestThreshold) {
                bestThreshold = threshold;
                best = e.getKey();
            }
        }
        return best;
    }

    private NpcSkill pickSkill(String stateId)
    {
        List<String> ids = def.stateSkills.get(stateId);
        if (ids == null || ids.isEmpty()) {
            ids = def.stateSkills.get(def.defaultState);
        }
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        String skillId = ids.get(random.nextInt(ids.size()));
        return def.skills.get(skillId);
    }

    private void checkEndConditions()
    {
        if (player.getHp() <= 0) {
            outcome = CombatOutcome.DEFEAT;
            log.add("你倒下了……");
        } else if (npcHp <= 0) {
            outcome = CombatOutcome.VICTORY;
            log.add("你击败了 " + def.displayName + "！");
        }
    }

    private boolean hasItem(String itemId)
    {
        return player.getInventory().stream()
            .anyMatch(i -> itemId.equals(i.getItemId()));
    }

    public CombatSnapshot snapshot()
    {
        return new CombatSnapshot(
            def.npcId,
            def.displayName,
            player.getHp(),
            player.getMaxHp(),
            npcHp,
            def.maxHp,
            pickStateId(),
            outcome,
            new ArrayList<>(log),
            outcome == CombatOutcome.ONGOING
        );
    }

}
