package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话 UI —— 对话格式化、分页、气泡渲染、输入处理。
 */
public class DialogueUi
{
    private final GameEngine engine;
    private final SpriteBatch batch;
    private final BitmapFont smallFont;
    private final ShapeRenderer shapes;
    private final UiDrawUtils draw;
    private final GlyphLayout layout;
    private final NpcPlaceholderManager npcManager;
    private final OrthographicCamera worldCamera;

    private Dialogue activeDialogue;
    private final List<String> dialoguePages = new ArrayList<>();
    private int dialoguePageIndex;
    private String playerLastChoice;  // 玩家刚选的对话选项文本
    private String pendingCombatNpcId; // 对话结束后自动触发战斗的 NPC ID

    /** 逐字打字机：每秒显示字数。 */
    private static final float CHARS_PER_SEC = 34f;
    private float typeTimer;     // 当前页打字计时
    private float appearT;       // 对话框淡入进度 0..1

    public DialogueUi(GameEngine engine, SpriteBatch batch, BitmapFont smallFont,
                       ShapeRenderer shapes, UiDrawUtils draw, GlyphLayout layout,
                       NpcPlaceholderManager npcManager, OrthographicCamera worldCamera)
    {
        this.engine = engine;
        this.batch = batch;
        this.smallFont = smallFont;
        this.shapes = shapes;
        this.draw = draw;
        this.layout = layout;
        this.npcManager = npcManager;
        this.worldCamera = worldCamera;
    }

    public boolean isAtChoicePoint()
    {
        if (activeDialogue == null) return false;
        if (!activeDialogue.isActive()) return false;
        List<String> opts = activeDialogue.getOptionTexts();
        if (opts == null || opts.isEmpty()) return false;
        if (dialoguePages.isEmpty()) prepareDialoguePages(activeDialogue);
        return dialoguePageIndex + 1 >= dialoguePages.size() && playerLastChoice == null;
    }

    public boolean isActive() { return activeDialogue != null; }
    public Dialogue getActiveDialogue() { return activeDialogue; }
    public String getPlayerLastChoice() { return playerLastChoice; }

    /** 对话结束后是否有待触发的战斗 NPC ID。 */
    public String getPendingCombatNpcId() { return pendingCombatNpcId; }
    public void clearPendingCombat() { pendingCombatNpcId = null; }

    public void clear()
    {
        activeDialogue = null;
        playerLastChoice = null;
        pendingCombatNpcId = null;
        dialoguePages.clear();
        dialoguePageIndex = 0;
        typeTimer = 0f;
        appearT = 0f;
    }

    public void startDialogue(Dialogue d)
    {
        activeDialogue = d;
        playerLastChoice = null;
        dialoguePages.clear();
        dialoguePageIndex = 0;
        typeTimer = 0f;
        appearT = 0f;
    }

    // ========== 逐字打字机 + 淡入动画 ==========

    /** 每帧推进打字与淡入进度（仅在对话激活时调用）。 */
    public void updateAnim(float delta)
    {
        if (activeDialogue == null) return;
        typeTimer += delta;
        if (appearT < 1f) appearT = Math.min(1f, appearT + delta * 6f);
    }

    /** 对话框淡入进度 0..1（缓出）。 */
    public float getAppearProgress()
    {
        return appearT * (2f - appearT);
    }

    private String currentPageRaw()
    {
        if (dialoguePages.isEmpty() && activeDialogue != null) prepareDialoguePages(activeDialogue);
        if (dialoguePages.isEmpty()) return "";
        return dialoguePages.get(Math.min(dialoguePageIndex, dialoguePages.size() - 1));
    }

    private int shownChars()
    {
        return (int) (typeTimer * CHARS_PER_SEC);
    }

    /** 当前页已显示的文本（逐字揭示）。 */
    public String getVisibleBody()
    {
        String raw = currentPageRaw();
        int n = Math.min(raw.length(), Math.max(0, shownChars()));
        return raw.substring(0, n);
    }

    /** 当前页是否已完整显示（决定选项/提示是否出现）。 */
    public boolean isPageFullyRevealed()
    {
        return shownChars() >= currentPageRaw().length();
    }

    /** 立即显示当前页全部文本（玩家在打字途中按 Enter）。 */
    public void revealCurrentPage()
    {
        typeTimer = (currentPageRaw().length() + 2) / CHARS_PER_SEC;
    }

    /**
     * 处理对话输入，返回 UI 是否仍处于对话状态。
     */
    public void handleInput(StringBuilder actionMessage)
    {
        if (activeDialogue == null) return;

        if (dialoguePages.isEmpty()) {
            prepareDialoguePages(activeDialogue);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            // 打字未完成 → 第一次 Enter 先显示全部，不翻页
            if (!isPageFullyRevealed()) {
                revealCurrentPage();
                return;
            }
            if (dialoguePageIndex + 1 < dialoguePages.size()) {
                dialoguePageIndex++;
                typeTimer = 0f;   // 新页重新打字
                playerLastChoice = null;
                actionMessage.setLength(0);
                actionMessage.append(formatDialogue(activeDialogue));
                return;
            }
            List<String> opts = activeDialogue.getOptionTexts();
            if (opts == null || opts.isEmpty() || !activeDialogue.isActive()) {
                String endedNpcId = activeDialogue != null ? activeDialogue.getNpcId() : null;
                activeDialogue = null;
                playerLastChoice = null;
                engine.endDialogue();
                // 检查是否需要自动触发战斗
                pendingCombatNpcId = resolvePendingCombat(endedNpcId);
                actionMessage.setLength(0);
                actionMessage.append(pendingCombatNpcId != null ? "准备战斗！" : "对话结束。");
                return;
            }
            playerLastChoice = null;  // Enter 翻到选项页时清空上次选择
            actionMessage.setLength(0);
            actionMessage.append(formatDialogue(activeDialogue));
            return;
        }

        if (dialoguePageIndex + 1 < dialoguePages.size()) return;
        if (!isPageFullyRevealed()) return;   // 文字未显示完不接受选项

        List<String> opts = activeDialogue.getOptionTexts();
        if (opts == null || opts.isEmpty()) return;

        for (int i = 0; i < Math.min(9, opts.size()); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                playerLastChoice = opts.get(i);  // 记录玩家说了什么
                activeDialogue = engine.chooseDialogueOption(i);
                prepareDialoguePages(activeDialogue);
                typeTimer = 0f;   // 新回应重新打字
                actionMessage.setLength(0);
                actionMessage.append(formatDialogue(activeDialogue));
                return;
            }
        }
    }

    public String formatDialogue(Dialogue d)
    {
        if (d == null) return "对话结束。";
        StringBuilder sb = new StringBuilder();
        if (dialoguePages.isEmpty()) prepareDialoguePages(d);
        sb.append(dialoguePages.get(Math.min(dialoguePageIndex, dialoguePages.size() - 1)));
        if (d.isActive() && d.getOptionTexts() != null && !d.getOptionTexts().isEmpty()) {
            if (dialoguePageIndex + 1 >= dialoguePages.size()) {
                sb.append("\n（Enter 继续 / 选 1-9）\n");
                for (int i = 0; i < d.getOptionTexts().size() && i < 9; i++)
                    sb.append(i + 1).append(". ").append(d.getOptionTexts().get(i)).append(" ");
            } else {
                sb.append("\n（Enter 继续）");
            }
        } else {
            sb.append(dialoguePageIndex + 1 >= dialoguePages.size()
                ? "\n（Enter 退出）" : "\n（Enter 继续）");
        }
        return sb.toString();
    }

    private void prepareDialoguePages(Dialogue d)
    {
        dialoguePages.clear();
        dialoguePageIndex = 0;
        if (d == null || d.getText() == null || d.getText().trim().isEmpty()) {
            dialoguePages.add("");
            return;
        }
        String[] parts = d.getText().split("\\n\\n");
        for (String p : parts) {
            String page = p.trim();
            if (!page.isEmpty()) dialoguePages.add(page);
        }
        if (dialoguePages.isEmpty()) dialoguePages.add(d.getText());
    }

    /** 根据对话结束时的 flag 判断是否自动开战。 */
    private String resolvePendingCombat(String npcId)
    {
        if (npcId == null) return null;
        if ("priest".equals(npcId) && engine.hasFlag("accepted-priest-trial")
            && !engine.getDefeatedNpcs().contains("priest")) return "priest";
        if ("follower".equals(npcId) && engine.hasFlag("accepted-follower-trial")
            && !engine.getDefeatedNpcs().contains("follower")) return "follower";
        if ("apprentice".equals(npcId) && engine.hasFlag("accepted-apprentice-spar")
            && !engine.getDefeatedNpcs().contains("apprentice")) return "apprentice";
        return null;
    }

    /** 在 world 空间中渲染 NPC 对话气泡。 */
    public void drawNpcDialogueBubble()
    {
        if (activeDialogue == null) return;
        Rectangle npcBounds = npcManager.findNpcBounds(activeDialogue.getNpcId());
        if (npcBounds == null) return;
        String text = formatDialogue(activeDialogue);
        if (text == null || text.trim().isEmpty()) return;

        float padding = 6f;
        float bubbleW = 300f;
        float x = npcBounds.x + npcBounds.width / 2f - bubbleW / 2f;
        float y = npcBounds.y + npcBounds.height + 18f;

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        layout.setText(smallFont, text, Color.WHITE, bubbleW - padding * 2f, Align.left, true);
        float bubbleH = layout.height + padding * 2f + 10f;
        batch.end();

        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(x, y, bubbleW, bubbleH);
        shapes.end();

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, text, x + padding, y + bubbleH - padding);
        batch.end();
    }
}
