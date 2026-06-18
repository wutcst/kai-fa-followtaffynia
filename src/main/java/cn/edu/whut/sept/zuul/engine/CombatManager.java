package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;
import cn.edu.whut.sept.zuul.infra.CombatLoader;
import cn.edu.whut.sept.zuul.infra.GameLogger;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 战斗管理器 —— 从 GameEngine 提取。
 */
public class CombatManager
{
    private static final Logger LOG = GameLogger.get();

    private final Player player;
    private final Set<String> defeatedNpcs;
    private final Set<String> sparedNpcs;
    private final Set<String> unlockedLocks;
    private final CombatActionRegistry combatActionRegistry;
    private final QuestManager questManager;
    private final Consumer<String> onLastMessage;

    private String encounterNpcId;
    private CombatSystem activeCombat;
    private CombatMode combatMode;
    private CombatOutcome lastCombatOutcome;
    private NpcCombatDef lastDef;

    public CombatManager(Player player, Set<String> defeatedNpcs, Set<String> sparedNpcs,
                          Set<String> unlockedLocks,
                          CombatActionRegistry combatActionRegistry, QuestManager questManager,
                          Consumer<String> onLastMessage)
    {
        this.player = player;
        this.defeatedNpcs = defeatedNpcs;
        this.sparedNpcs = sparedNpcs;
        this.unlockedLocks = unlockedLocks;
        this.combatActionRegistry = combatActionRegistry;
        this.questManager = questManager;
        this.onLastMessage = onLastMessage;
        this.lastCombatOutcome = CombatOutcome.ONGOING;
    }

    public boolean isInCombat()
    {
        return activeCombat != null;
    }

    public CombatOutcome getLastCombatOutcome()
    {
        return lastCombatOutcome;
    }

    public boolean isUndertaleCombat()
    {
        return combatMode == CombatMode.UNDERTALE && activeCombat != null;
    }

    public CombatSystem getCombatSystem()
    {
        return activeCombat;
    }

    public EncounterMenu startNpcEncounter(String npcId)
    {
        leaveEncounter();
        encounterNpcId = npcId;
        boolean isDefeated = defeatedNpcs.contains(npcId);
        boolean canTalk = !isDefeated;
        boolean canFight = !"merchant".equals(npcId) && !isDefeated;
        return new EncounterMenu(npcId, canTalk, canFight, true);
    }

    public void leaveEncounter()
    {
        encounterNpcId = null;
    }

    public CombatSnapshot startCombat(String npcId)
    {
        return startCombat(npcId, CombatMode.TURN_BASED);
    }

    public CombatSnapshot startCombat(String npcId, CombatMode mode)
    {
        leaveEncounter();
        try {
            NpcCombatDef def = CombatLoader.load(npcId);
            combatMode = mode;
            if (mode == CombatMode.UNDERTALE) {
                activeCombat = new UndertaleCombatEngine(player, def, combatActionRegistry);
            } else {
                activeCombat = new CombatEngine(player, def, combatActionRegistry);
            }
            encounterNpcId = npcId;
            lastCombatOutcome = CombatOutcome.ONGOING;
            LOG.info("startCombat: " + npcId + " mode=" + mode + " hp=" + def.maxHp);
            return activeCombat.snapshot();
        } catch (IOException ex) {
            LOG.warning("startCombat: failed for " + npcId + ": " + ex.getMessage());
            onLastMessage.accept("无法与 " + npcId + " 开战。");
            return null;
        }
    }

    public CombatSnapshot combatAction(CombatAction action, String itemIdOrNull)
    {
        if (activeCombat == null) {
            onLastMessage.accept("当前没有进行中的战斗。");
            return null;
        }
        CombatSnapshot snapshot = activeCombat.processPlayerAction(action, itemIdOrNull);
        lastCombatOutcome = snapshot.outcome;
        applyCombatOutcome();
        return snapshot;
    }

    public NpcCombatDef getLastDef()
    {
        return lastDef;
    }

    /** MERCY/认输结束战斗时为 true */
    public boolean wasSpared()
    {
        return activeCombat != null && activeCombat.wasSpared();
    }

    public void applyCombatOutcome()
    {
        if (activeCombat == null) return;
        CombatOutcome oc = activeCombat.getOutcome();
        if (oc == CombatOutcome.VICTORY) {
            lastDef = activeCombat.getDef();
            applyCombatVictory(lastDef);
            lastCombatOutcome = oc;
            clearCombat();
        } else if (oc == CombatOutcome.DEFEAT || oc == CombatOutcome.FLED) {
            lastDef = null;
            lastCombatOutcome = oc;
            clearCombat();
        }
    }

    private void applyCombatVictory(NpcCombatDef def)
    {
        if (def.onDefeatReputation != 0) {
            player.setReputation(player.getReputation() + def.onDefeatReputation);
        }
        if (def.onDefeatUnlock != null && !def.onDefeatUnlock.isEmpty()) {
            String lockId = def.onDefeatUnlock;
            if (lockId != null && !lockId.isEmpty()) {
                unlockedLocks.add(lockId);
                questManager.onLockUnlocked(lockId);
            }
        }
        if (def.onDefeatMarkDefeated) {
            defeatedNpcs.add(def.npcId);
            LOG.info("defeatedNpcs: added " + def.npcId);
        }
        // 仁慈/认输化解：记入 sparedNpcs，渲染为"已离去"而非尸体
        if (activeCombat != null && activeCombat.wasSpared()) {
            sparedNpcs.add(def.npcId);
            LOG.info("sparedNpcs: added " + def.npcId);
            onLastMessage.accept(def.displayName + " 放下了戒备，离开了。");
        } else {
            onLastMessage.accept("你击败了 " + def.displayName + "。");
        }
    }

    private void clearCombat()
    {
        activeCombat = null;
        encounterNpcId = null;
    }
}
