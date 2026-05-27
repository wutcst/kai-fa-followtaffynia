package cn.edu.whut.sept.zuul.engine;

import java.util.Collections;
import java.util.List;

/**
 * 战斗 UI 渲染用快照（无 LibGDX 依赖）。
 */
public final class CombatSnapshot
{
    public final String npcId;
    public final String npcDisplayName;
    public final int playerHp;
    public final int playerMaxHp;
    public final int npcHp;
    public final int npcMaxHp;
    public final String npcStateId;
    public final CombatOutcome outcome;
    public final List<String> logLines;
    public final boolean playerTurn;

    public CombatSnapshot(String npcId, String npcDisplayName,
        int playerHp, int playerMaxHp, int npcHp, int npcMaxHp,
        String npcStateId, CombatOutcome outcome, List<String> logLines, boolean playerTurn)
    {
        this.npcId = npcId;
        this.npcDisplayName = npcDisplayName;
        this.playerHp = playerHp;
        this.playerMaxHp = playerMaxHp;
        this.npcHp = npcHp;
        this.npcMaxHp = npcMaxHp;
        this.npcStateId = npcStateId;
        this.outcome = outcome;
        this.logLines = logLines == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(logLines);
        this.playerTurn = playerTurn;
    }
}
