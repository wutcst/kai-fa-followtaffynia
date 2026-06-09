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

    public void handleInput()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) { panel.resetSelection(); return; }
        panel.clampIndex();
        if (!panel.isInspectMode()) handleListInput(items);
        else handleInspectInput();
    }

    private void handleListInput(List<Item> items)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) panel.moveSelection(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) panel.moveSelection(1);
        for (int i = 0; i < Math.min(9, items.size()); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) panel.selectIndex(i);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) panel.enterInspect();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) tryEatSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.U) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) tryUseSelected();
    }

    private void handleInspectInput()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) panel.exitInspect();
    }

    private void tryUseSelected()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) { panel.close(); return; }
        Item selected = items.get(panel.getInspectIndex());
        engine.checkItemUse(selected.getItemId());
        engine.tryUseItem(selected.getItemId());
        panel.exitInspect();
        panel.clampSelection();
    }

    private void tryEatSelected()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) { panel.close(); return; }
        engine.eatItem(items.get(panel.getInspectIndex()).getItemId());
        panel.clampSelection();
    }
}
