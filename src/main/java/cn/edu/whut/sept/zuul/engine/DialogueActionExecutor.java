package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Player;

/**
 * 执行对话节点上的 action 字符串（格式与 docs/04 §6 一致）。
 */
public final class DialogueActionExecutor
{
    private final GameEngine engine;

    public DialogueActionExecutor(GameEngine engine)
    {
        this.engine = engine;
    }

    public String apply(String action)
    {
        if (action == null || action.trim().isEmpty()) {
            return null;
        }
        String trimmed = action.trim();
        if (trimmed.startsWith("unlock:")) {
            String lockId = trimmed.substring("unlock:".length()).trim();
            if (lockId.isEmpty()) {
                return "无效的解锁指令。";
            }
            engine.unlockLock(lockId);
            engine.getQuestManager().onLockUnlocked(lockId);
            return lockId + " 已解锁。";
        }
        if (trimmed.startsWith("reputation:")) {
            String value = trimmed.substring("reputation:".length()).trim();
            try {
                int delta = Integer.parseInt(value);
                Player player = engine.getPlayer();
                player.setReputation(player.getReputation() + delta);
                return "声望 " + (delta > 0 ? "+" : "") + delta + "。";
            } catch (NumberFormatException ex) {
                return "声望变化无效。";
            }
        }
        if (trimmed.startsWith("open:")) {
            return "（" + trimmed.substring("open:".length()) + " 功能将由客户端实现）";
        }
        if (trimmed.equals("barter")) {
            return engine.barterJunkForHerbs();
        }
        if (trimmed.startsWith("quest:")) {
            engine.getQuestManager().onDialogueAction(trimmed);
            return null;
        }
        if (trimmed.startsWith("give:")) {
            String itemId = trimmed.substring("give:".length()).trim();
            if (itemId.isEmpty()) return "无效的物品ID。";
            engine.giveItem(itemId);
            return "获得了物品。";
        }
        if (trimmed.startsWith("flag:")) {
            String flag = trimmed.substring("flag:".length()).trim();
            if (!flag.isEmpty()) {
                engine.setFlag(flag);
            }
            return null;
        }
        return "未知的对话效果：" + trimmed;
    }
}
