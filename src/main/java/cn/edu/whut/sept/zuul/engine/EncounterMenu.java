package cn.edu.whut.sept.zuul.engine;

/**
 * 靠近 NPC 时的遭遇菜单元数据。
 */
public final class EncounterMenu
{
    public final String npcId;
    public final boolean canTalk;
    public final boolean canFight;
    public final boolean canUndertaleFight;
    public final boolean canLeave;

    public EncounterMenu(String npcId, boolean canTalk, boolean canFight,
        boolean canUndertaleFight, boolean canLeave)
    {
        this.npcId = npcId;
        this.canTalk = canTalk;
        this.canFight = canFight;
        this.canUndertaleFight = canUndertaleFight;
        this.canLeave = canLeave;
    }
}
