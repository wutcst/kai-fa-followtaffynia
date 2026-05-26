package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;

/**
 * 根据玩家状态判定三结局（光明 / 暗影 / 中立）。
 */
public final class EndingEvaluator
{
    public EndingType evaluate(Player player)
    {
        boolean hasGem = player.getInventory().stream()
            .anyMatch(i -> "gem-light".equals(i.getItemId()));
        int reputation = player.getReputation();

        if (hasGem && reputation >= 0) {
            return EndingType.LIGHT;
        }
        if (reputation < 0) {
            return EndingType.SHADOW;
        }
        return EndingType.NEUTRAL;
    }
}
