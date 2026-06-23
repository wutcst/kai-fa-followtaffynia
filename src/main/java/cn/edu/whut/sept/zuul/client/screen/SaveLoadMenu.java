package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.audio.GameAudio;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Cue;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.infra.SaveGameService;
import cn.edu.whut.sept.zuul.infra.SaveGameService.SlotInfo;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.List;

/**
 * 存档 / 读档槽位选择覆盖层。独立于游戏其它输入，打开时由调用方负责暂停世界逻辑。
 * 渲染风格沿用 {@link GameUiSkin} 木窗皮肤，与暂停菜单保持一致。
 */
public class SaveLoadMenu
{
    /** 菜单模式：保存或读取。 */
    public enum Mode { SAVE, LOAD }

    /** 一次输入处理的结果类型。 */
    public enum ResultType { NONE, CANCEL, CONFIRM }

    /** 输入处理结果。CONFIRM 时携带选中的槽位与模式。 */
    public static final class Result
    {
        public static final Result NONE = new Result(ResultType.NONE, Mode.LOAD, 0);
        public static final Result CANCEL = new Result(ResultType.CANCEL, Mode.LOAD, 0);

        public final ResultType type;
        public final Mode mode;
        public final int slot;

        private Result(ResultType type, Mode mode, int slot)
        {
            this.type = type;
            this.mode = mode;
            this.slot = slot;
        }

        static Result confirm(Mode mode, int slot)
        {
            return new Result(ResultType.CONFIRM, mode, slot);
        }
    }

    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GameUiSkin uiSkin;
    private final UiDrawUtils draw;
    private final CameraController camera;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final GameAudio audio;

    private boolean open;
    private Mode mode = Mode.LOAD;
    private int selectedIndex;
    private List<SlotInfo> slots;

    public SaveLoadMenu(BitmapFont font, BitmapFont smallFont, GameUiSkin uiSkin,
        UiDrawUtils draw, CameraController camera, SpriteBatch batch, ShapeRenderer shapes,
        GameAudio audio)
    {
        this.font = font;
        this.smallFont = smallFont;
        this.uiSkin = uiSkin;
        this.draw = draw;
        this.camera = camera;
        this.batch = batch;
        this.shapes = shapes;
        this.audio = audio;
    }

    /** 打开菜单，刷新槽位信息并把选择落在第一个可用槽位上。 */
    public void open(Mode mode)
    {
        this.mode = mode;
        this.slots = SaveGameService.describeAll();
        this.selectedIndex = defaultSelection(mode);
        this.open = true;
        audio.play(Cue.MENU_OPEN);
    }

    public boolean isOpen()
    {
        return open;
    }

    public Mode getMode()
    {
        return mode;
    }

    public void close()
    {
        open = false;
    }

    /** LOAD：默认落在第一个可读取槽位；SAVE：默认第一个空槽，没有空槽则第一个。 */
    private int defaultSelection(Mode mode)
    {
        if (slots == null) {
            return 0;
        }
        if (mode == Mode.LOAD) {
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).isLoadable()) {
                    return i;
                }
            }
            return 0;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).exists()) {
                return i;
            }
        }
        return 0;
    }

    /** 轮询键盘并返回本帧的操作结果。仅在菜单打开时调用。 */
    public Result handleInput()
    {
        if (!open) {
            return Result.NONE;
        }
        int count = slots.size();

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedIndex = (selectedIndex - 1 + count) % count;
            audio.play(Cue.CLICK);
            return Result.NONE;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
            || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedIndex = (selectedIndex + 1) % count;
            audio.play(Cue.CLICK);
            return Result.NONE;
        }
        int numKey = pressedNumberSlot();
        if (numKey > 0) {
            selectedIndex = numKey - 1;
            audio.play(Cue.CLICK);
            return Result.NONE;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            open = false;
            audio.play(Cue.MENU_CLOSE);
            return Result.CANCEL;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            return confirmSelection();
        }
        return Result.NONE;
    }

    private int pressedNumberSlot()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) return 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) return 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) return 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) return 4;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) return 5;
        return 0;
    }

    private Result confirmSelection()
    {
        SlotInfo info = slots.get(selectedIndex);
        if (mode == Mode.LOAD && !info.isLoadable()) {
            // 空槽或损坏槽不可读取
            audio.play(Cue.ERROR);
            return Result.NONE;
        }
        open = false;
        return Result.confirm(mode, info.getSlot());
    }

    // ==================== 渲染 ====================

    /** 绘制暗化背景 + 槽位面板。需在主 UI 渲染之后调用。 */
    public void render()
    {
        if (!open) {
            return;
        }
        float w = CameraController.DESIGN_W;
        float h = CameraController.DESIGN_H;

        camera.applyFullViewport();
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // 半透明暗化背景，突出面板
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(0f, 0f, w, h);
        shapes.end();

        float pw = draw.grid(Math.min(640f, w - 96f));
        float ph = draw.grid(Math.min(460f, h - 80f));
        float px = draw.grid((w - pw) / 2f);
        float py = draw.grid((h - ph) / 2f);
        float cx = px + pw / 2f;

        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        uiSkin.drawWindow(batch, px, py, pw, ph);

        // 标题
        font.setColor(draw.getUiLightText());
        draw.drawCentered(batch, mode == Mode.SAVE ? "保存存档" : "读取存档",
            cx, py + ph - 32f);

        // 槽位行
        float rowH = 56f;
        float rowGap = 8f;
        float listX = px + 28f;
        float listW = pw - 56f;
        float firstRowTop = py + ph - 72f;
        for (int i = 0; i < slots.size(); i++) {
            float rowTop = firstRowTop - i * (rowH + rowGap);
            drawSlotRow(slots.get(i), i == selectedIndex, listX, rowTop - rowH, listW, rowH);
        }

        // 底部操作提示
        smallFont.setColor(draw.getUiDarkText());
        draw.drawCenteredWithSmallFont(batch,
            "↑↓ / 数字键 选择    Enter 确认    Esc 取消", cx, py + 26f);

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSlotRow(SlotInfo info, boolean selected, float x, float y,
        float width, float height)
    {
        // 选中行用棕色按钮底，未选中用嵌板底
        if (selected) {
            uiSkin.drawButton(batch, x, y, width, height);
        } else {
            uiSkin.drawInset(batch, x, y, width, height);
        }

        // 图标：保存 / 读取
        float iconSize = 24f;
        draw.drawIcon(batch, mode == Mode.SAVE ? UiDrawUtils.ICON_SAVE : UiDrawUtils.ICON_LOAD,
            x + 14f, y + height / 2f - iconSize / 2f, iconSize);

        float textX = x + 52f;
        float topLineY = y + height - 18f;
        float bottomLineY = y + 22f;

        font.setColor(selected ? draw.getUiLightText() : draw.getUiDarkText());
        smallFont.setColor(selected ? draw.getUiLightText() : draw.getUiDarkText());

        String slotLabel = "存档 " + info.getSlot();
        if (!info.exists()) {
            draw.drawClampedLine(batch, font, slotLabel, textX, topLineY, width - 64f);
            smallFont.setColor(selected ? draw.getUiLightText() : draw.getUiDarkText());
            draw.drawClampedLine(batch, smallFont, "— 空 —", textX, bottomLineY, width - 64f);
            return;
        }
        if (info.isCorrupt()) {
            draw.drawClampedLine(batch, font, slotLabel, textX, topLineY, width - 64f);
            draw.drawClampedLine(batch, smallFont, "存档损坏", textX, bottomLineY, width - 64f);
            return;
        }

        String name = info.getPlayerName() == null ? "?" : info.getPlayerName();
        String room = info.getRoomId() == null ? "?" : info.getRoomId();
        draw.drawClampedLine(batch, font, slotLabel + "   " + name + " · " + room,
            textX, topLineY, width - 64f);
        // 时间右下角对齐到行内
        draw.drawClampedLine(batch, smallFont, info.formattedTime(),
            textX, bottomLineY, width - 64f);
    }
}
