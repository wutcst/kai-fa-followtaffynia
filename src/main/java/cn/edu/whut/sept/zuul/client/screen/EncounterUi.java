package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.CombatAction;
import cn.edu.whut.sept.zuul.engine.CombatMode;
import cn.edu.whut.sept.zuul.engine.CombatOutcome;
import cn.edu.whut.sept.zuul.engine.CombatSnapshot;
import cn.edu.whut.sept.zuul.engine.EncounterMenu;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatPhase;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.List;

/**
 * 遭遇/战斗 UI —— 遭遇菜单、战斗输入、战斗快照格式化。
 */
public class EncounterUi
{
    private final GameEngine engine;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final DialogueUi dialogueUi;

    private boolean encounterMenuOpen;
    private EncounterMenu encounterMenu;
    private CombatSnapshot activeCombatSnapshot;

    public EncounterUi(GameEngine engine, SpriteBatch batch, ShapeRenderer shapes,
                        BitmapFont font, BitmapFont smallFont, DialogueUi dialogueUi)
    {
        this.engine = engine;
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.smallFont = smallFont;
        this.dialogueUi = dialogueUi;
    }

    public boolean isMenuOpen() { return encounterMenuOpen; }
    public boolean isCombatSnapshotActive() { return activeCombatSnapshot != null; }

    /** 强制进入 UT 战斗（不经过遭遇菜单），用于自动遭遇（如金库魔像）。 */
    public void forceUtCombat(String npcId)
    {
        encounterMenuOpen = false;
        encounterMenu = null;
        dialogueUi.clear();
        activeCombatSnapshot = engine.startCombat(npcId, CombatMode.UNDERTALE);
    }

    public boolean openMenu(String npcId)
    {
        EncounterMenu menu = engine.startNpcEncounter(npcId);
        if (menu == null) {
            closeMenu();
            return false;
        }
        // 只能交流不能战斗时——直接进入对话，不弹遭遇菜单
        if (menu.canTalk && !menu.canFight) {
            Dialogue d = engine.talkNpc(npcId);
            dialogueUi.startDialogue(d);
            encounterMenu = null;
            encounterMenuOpen = false;
            return d != null;
        }
        // 没有任何可执行动作时不要创建一个无法退出的空菜单。
        if (!menu.canTalk && !menu.canFight) {
            closeMenu();
            return false;
        }
        encounterMenu = menu;
        encounterMenuOpen = true;
        dialogueUi.clear();
        activeCombatSnapshot = null;
        return true;
    }

    public void closeMenu()
    {
        engine.leaveEncounter();
        encounterMenu = null;
        encounterMenuOpen = false;
        activeCombatSnapshot = null;
    }

    /**
     * 处理遭遇菜单输入。返回 null 如果菜单仍在，或返回格式化后的文本。
     */
    public String handleMenuInput()
    {
        if (!encounterMenuOpen || encounterMenu == null) {
            encounterMenuOpen = false;
            encounterMenu = null;
            return null;
        }
        String npcId = encounterMenu.npcId;

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            if (!encounterMenu.canTalk) return "你不能交流。";
            Dialogue d = engine.talkNpc(npcId);
            dialogueUi.startDialogue(d);
            encounterMenuOpen = false;
            encounterMenu = null;
            activeCombatSnapshot = null;
            return dialogueUi.formatDialogue(d);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            if (!encounterMenu.canFight) return "你不能战斗。";
            activeCombatSnapshot = engine.startCombat(npcId, CombatMode.UNDERTALE);
            encounterMenuOpen = false;
            encounterMenu = null;
            dialogueUi.clear();
            return "[UT] " + formatCombatSnapshot(activeCombatSnapshot);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            String msg = encounterMenu.canLeave ? "你离开了。" : "你现在不能离开。";
            if (encounterMenu.canLeave) engine.leaveEncounter();
            encounterMenuOpen = false;
            encounterMenu = null;
            return msg;
        }
        return null;
    }

    /**
     * 处理战斗输入。返回格式化后的快照文本，战斗结束时返回 null。
     */
    public String handleCombatInput()
    {
        if (!engine.isInCombat()) { activeCombatSnapshot = null; return null; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            activeCombatSnapshot = engine.combatAction(CombatAction.ATTACK, null);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            activeCombatSnapshot = engine.combatAction(CombatAction.DEFEND, null);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
            activeCombatSnapshot = engine.combatAction(CombatAction.FLEE, null);
        else {
            List<Item> inv = engine.getPlayer().getInventory();
            for (int i = 0; i < Math.min(6, inv.size()); i++) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4 + i)) {
                    activeCombatSnapshot = engine.combatAction(CombatAction.USE_ITEM,
                        inv.get(i).getItemId());
                    break;
                }
            }
        }

        String msg = formatCombatSnapshot(activeCombatSnapshot);
        if (!engine.isInCombat()) activeCombatSnapshot = null;
        return msg;
    }

    /**
     * 处理 UT 战斗输入。返回格式化后的消息。
     */
    public String handleUtCombatInput(float delta, UndertaleCombatEngine ut)
    {
        if (ut == null) { activeCombatSnapshot = null; return null; }

        // 战斗台词画中画：Enter 继续，屏蔽战斗输入
        if (ut.isShowingBattleLine()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                ut.dismissBattleLine();
                String msg = ut.getPhaseMessage();
                activeCombatSnapshot = ut.snapshot();
                if (ut.getOutcome() != CombatOutcome.ONGOING) {
                    engine.applyCombatOutcome();
                    activeCombatSnapshot = null;
                    // MERCY 退出后不自动对话——玩家需再按E交谈
                }
                return msg;
            }
            return null;
        }

        ut.updateFightBar(delta);
        ut.updateEnemyTurn(delta);
        UndertaleCombatPhase phase = ut.getPhase();

        if (phase == UndertaleCombatPhase.MENU) {
            // 仅数字键 1-4 操作（WASD/方向键保留给弹幕躲避，避免一键多用混淆）
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) ut.selectFight();
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                if (!ut.getDef().actOptions.isEmpty()) {
                    String actId = ut.getDef().actOptions.keySet().iterator().next();
                    ut.selectAct(actId);
                }
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                for (Item it : engine.getPlayer().getInventory()) {
                    ut.selectItem(it.getItemId());
                    break;
                }
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) && ut.canMercy()) ut.selectMercy();
        } else if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                ut.pressFightBar();
        } else if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            float mx = 0f, my = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) mx = -1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) mx = 1f;
            if (ut.isGravityMode()) {
                // 重力/平台模式：A/D 走动，空格跳跃（可二段跳）
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(Input.Keys.W)
                    || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                    ut.soulJump();
                }
                ut.moveSoul(mx, 0f, delta);
            } else {
                if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) my = 1f;
                if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) my = -1f;
                ut.moveSoul(mx, my, delta);
            }
        }

        activeCombatSnapshot = ut.snapshot();
        if (ut.getOutcome() != CombatOutcome.ONGOING) {
            engine.applyCombatOutcome();
            activeCombatSnapshot = null;
            // MERCY 退出后不自动对话——玩家需再按E交谈
        }
        return null;
    }

    public String formatEncounterMenu(EncounterMenu menu)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("遇到 ").append(menu.npcId).append("。\n");
        if (menu.canTalk) sb.append("按 1 交流。 ");
        if (menu.canFight) sb.append("按 2 战斗。 ");
        if (menu.canLeave) sb.append("按 3 离开。 ");
        return sb.toString();
    }

    private String formatCombatSnapshot(CombatSnapshot snap)
    {
        if (snap == null) return "战斗尚未开始。";
        String last = snap.logLines.isEmpty() ? "" : snap.logLines.get(snap.logLines.size() - 1);
        StringBuilder sb = new StringBuilder();
        sb.append("守卫 ").append(snap.npcHp).append("/").append(snap.npcMaxHp)
            .append(" | 你 ").append(snap.playerHp).append("/").append(snap.playerMaxHp)
            .append("\n").append(last);
        if (snap.outcome != null && snap.outcome != CombatOutcome.ONGOING)
            sb.append("\n结果：").append(snap.outcome);
        else sb.append("\n战斗操作：1攻 2防 3逃 4-9用物品");
        return sb.toString();
    }
}
