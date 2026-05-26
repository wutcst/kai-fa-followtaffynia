package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 主线任务状态机（≥3 条，见 FR-C03）。
 */
public final class QuestManager
{
    public static final String QUEST_VAULT = "vault-seal";
    public static final String QUEST_THRONE = "throne-approach";
    public static final String QUEST_EXPLORE = "realm-explorer";

    public static final String STATE_ACTIVE = "active";
    public static final String STATE_COMPLETED = "completed";

    private static final int EXPLORE_TARGET = 8;

    private final Map<String, String> questStates;

    public QuestManager()
    {
        questStates = new HashMap<>();
        reset();
    }

    public void reset()
    {
        questStates.clear();
        questStates.put(QUEST_VAULT, STATE_ACTIVE);
        questStates.put(QUEST_THRONE, STATE_ACTIVE);
        questStates.put(QUEST_EXPLORE, STATE_ACTIVE);
    }

    public Map<String, String> getQuestStates()
    {
        return Collections.unmodifiableMap(questStates);
    }

    public void restoreQuestStates(Map<String, String> states)
    {
        questStates.clear();
        if (states != null) {
            questStates.putAll(states);
        }
        ensureDefaults();
    }

    private void ensureDefaults()
    {
        if (!questStates.containsKey(QUEST_VAULT)) {
            questStates.put(QUEST_VAULT, STATE_ACTIVE);
        }
        if (!questStates.containsKey(QUEST_THRONE)) {
            questStates.put(QUEST_THRONE, STATE_ACTIVE);
        }
        if (!questStates.containsKey(QUEST_EXPLORE)) {
            questStates.put(QUEST_EXPLORE, STATE_ACTIVE);
        }
    }

    public boolean isCompleted(String questId)
    {
        return STATE_COMPLETED.equals(questStates.get(questId));
    }

    public void onRoomEntered(Room room, Set<String> exploredRoomIds)
    {
        if ("throne-hall".equals(room.getRoomId())) {
            complete(QUEST_THRONE);
        }
        if (exploredRoomIds != null && exploredRoomIds.size() >= EXPLORE_TARGET) {
            complete(QUEST_EXPLORE);
        }
    }

    public void onItemTaken(Item item)
    {
        if (item != null && "gem-light".equals(item.getItemId())) {
            complete(QUEST_VAULT);
        }
    }

    public void onLockUnlocked(String lockId)
    {
        if ("guard-gate".equals(lockId)) {
            // 解锁守卫门是通往王座的必要步骤，保持 throne 任务为 active 直至进入
        }
    }

    public void onDialogueAction(String action)
    {
        // 预留 quest:complete:<id> 等对话 action 扩展
        if (action != null && action.startsWith("quest:complete:")) {
            complete(action.substring("quest:complete:".length()).trim());
        }
    }

    private void complete(String questId)
    {
        if (questId == null || questId.isEmpty()) {
            return;
        }
        questStates.put(questId, STATE_COMPLETED);
    }
}
