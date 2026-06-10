package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.util.List;

/**
 * 背包输入处理 —— 键盘事件处理，与渲染分离。
 */
public class InventoryInputHandler
{
    private final InventoryPanel panel;
    private final GameEngine engine;

    public InventoryInputHandler(InventoryPanel panel, GameEngine engine)
    {
        this.panel = panel;
        this.engine = engine;
    }

    public String handleInput()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            panel.resetSelection();
            return null;
        }
        panel.clampIndex();
        if (!panel.isInspectMode()) {
            return handleListInput(items);
        }
        return handleInspectInput();
    }

    private String handleListInput(List<Item> items)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) panel.moveSelection(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) panel.moveSelection(1);
        for (int i = 0; i < Math.min(9, items.size()); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) panel.selectIndex(i);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) panel.enterInspect();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) return tryEatSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.U) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) return tryUseSelected();
        return null;
    }

    private String handleInspectInput()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            panel.exitInspect();
            return "已返回背包列表";
        }
        return null;
    }

    private String tryUseSelected()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            panel.close();
            return "背包为空";
        }
        Item selected = items.get(panel.getInspectIndex());
        String msg = engine.tryUseItem(selected.getItemId());
        panel.exitInspect();
        panel.clampSelection();
        return msg;
    }

    private String tryEatSelected()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            panel.close();
            return "背包为空";
        }
        Item selected = items.get(panel.getInspectIndex());
        boolean edible = selected.isMagicCookie();
        engine.eatItem(selected.getItemId());
        panel.clampSelection();
        return edible
            ? "食用了 " + selected.getName() + "，负重上限提升"
            : selected.getName() + " 不能食用";
    }
}
