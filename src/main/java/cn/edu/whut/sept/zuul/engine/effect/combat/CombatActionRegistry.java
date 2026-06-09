package cn.edu.whut.sept.zuul.engine.effect.combat;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.CombatSystem;

/**
 * 战斗中使用物品的效果（combat: 前缀或已知 itemId）。
 */
public final class CombatActionRegistry
{
    public CombatItemResult apply(CombatSystem combat, Player player, String itemId)
    {
        Item item = player.getInventory().stream()
            .filter(i -> i.getItemId().equals(itemId))
            .findFirst().orElse(null);
        if (item == null) {
            return CombatItemResult.fail("背包中没有该物品。");
        }

        String effect = item.getEffect();
        if ("healing-herb".equals(itemId) || (effect != null && effect.startsWith("heal:"))) {
            int amount = parseTrailingInt(effect, "heal:", 20);
            int before = player.getHp();
            player.setHp(Math.min(player.getMaxHp(), before + amount));
            player.removeItem(itemId);
            return CombatItemResult.ok("恢复了 " + (player.getHp() - before) + " 点生命。", true);
        }
        if ("shield-wooden".equals(itemId)) {
            combat.addPlayerDefenseBuff(2);
            return CombatItemResult.ok("举起木盾，接下来两回合减伤。", false);
        }
        if ("torch".equals(itemId)) {
            combat.addNpcBlindTurns(1);
            player.removeItem(itemId);
            return CombatItemResult.ok("火把晃眼，守卫命中率下降。", true);
        }
        if (effect != null && effect.startsWith("combat:")) {
            return applyCombatPrefix(combat, player, item, effect);
        }
        return CombatItemResult.fail(item.getName() + " 无法在战斗中使用。");
    }

    private CombatItemResult applyCombatPrefix(CombatSystem combat, Player player, Item item, String effect)
    {
        if (effect.startsWith("combat:heal:")) {
            int amount = parseTrailingInt(effect, "combat:heal:", 20);
            int before = player.getHp();
            player.setHp(Math.min(player.getMaxHp(), before + amount));
            player.removeItem(item.getItemId());
            return CombatItemResult.ok("恢复了 " + (player.getHp() - before) + " 点生命。", true);
        }
        if (effect.startsWith("combat:buff:defense:")) {
            int turns = parseTrailingInt(effect, "combat:buff:defense:", 2);
            combat.addPlayerDefenseBuff(turns);
            return CombatItemResult.ok("防御提升 " + turns + " 回合。", false);
        }
        if (effect.startsWith("combat:debuff:blind:")) {
            int turns = parseTrailingInt(effect, "combat:debuff:blind:", 1);
            combat.addNpcBlindTurns(turns);
            player.removeItem(item.getItemId());
            return CombatItemResult.ok("敌人被致盲。", true);
        }
        return CombatItemResult.fail("未知的战斗效果。");
    }

    private static int parseTrailingInt(String effect, String prefix, int defaultValue)
    {
        if (effect == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(effect.substring(prefix.length()).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static final class CombatItemResult
    {
        public final boolean success;
        public final String message;
        public final boolean consumed;

        private CombatItemResult(boolean success, String message, boolean consumed)
        {
            this.success = success;
            this.message = message;
            this.consumed = consumed;
        }

        public static CombatItemResult ok(String message, boolean consumed)
        {
            return new CombatItemResult(true, message, consumed);
        }

        public static CombatItemResult fail(String message)
        {
            return new CombatItemResult(false, message, false);
        }
    }
}
