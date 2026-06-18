package cn.edu.whut.sept.zuul.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * NPC 精灵渲染器：加载精灵表（1行×6列），循环播放 6 帧待机动画。
 */
public class NpcRenderer implements Disposable
{
    private static final int FRAME_COLS = 6;
    private static final int FRAME_ROWS = 1;
    private static final float FRAME_DURATION = 0.15f;
    private static final float SCALE = 0.2f;

    private final Map<String, Animation<TextureRegion>> anims = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, Integer> renderWidths = new HashMap<>();
    private final Map<String, Integer> renderHeights = new HashMap<>();
    private float stateTime;

    public NpcRenderer(String... npcIds)
    {
        for (String npcId : npcIds) {
            load(npcId);
        }
    }

    private void load(String npcId)
    {
        try {
            if ("apprentice".equals(npcId)) {
                loadFrames(npcId, 6);
            } else {
                String path = "npc/" + npcId + ".png";
                Texture tex = new Texture(Gdx.files.internal(path));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                textures.put(npcId, tex);

                int fw = tex.getWidth() / FRAME_COLS;
                int fh = tex.getHeight() / FRAME_ROWS;

                renderWidths.put(npcId, Math.round(fw * SCALE));
                renderHeights.put(npcId, Math.round(fh * SCALE));

                TextureRegion[][] split = TextureRegion.split(tex, fw, fh);
                Animation<TextureRegion> anim = new Animation<>(FRAME_DURATION, split[0]);
                anim.setPlayMode(Animation.PlayMode.LOOP);
                anims.put(npcId, anim);
            }
        } catch (Exception e) {
            System.err.println("[NpcRenderer] Failed to load: " + npcId + " - " + e.getMessage());
        }
    }

    /** 从独立帧图片加载动画（如 apprentice1.png ~ apprentice6.png） */
    private void loadFrames(String npcId, int frameCount)
    {
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            String path = "npc/" + npcId + (i + 1) + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(npcId + "_" + i, tex);
            frames[i] = new TextureRegion(tex);
        }
        int fw = frames[0].getTexture().getWidth();
        int fh = frames[0].getTexture().getHeight();

        renderWidths.put(npcId, Math.round(fw * SCALE));
        renderHeights.put(npcId, Math.round(fh * SCALE));

        Animation<TextureRegion> anim = new Animation<>(FRAME_DURATION, frames);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        anims.put(npcId, anim);
    }

    public void update(float delta)
    {
        stateTime += delta;
    }

    public boolean hasNpc(String npcId)
    {
        return anims.containsKey(npcId);
    }

    public int getRenderW(String npcId)
    {
        Integer w = renderWidths.get(npcId);
        return w != null ? w : 0;
    }

    public int getRenderH(String npcId)
    {
        Integer h = renderHeights.get(npcId);
        return h != null ? h : 0;
    }

    /**
     * 按 SCALE 缩放后的尺寸绘制当前动画帧。
     */
    public void render(SpriteBatch batch, String npcId, float x, float y)
    {
        Animation<TextureRegion> anim = anims.get(npcId);
        if (anim == null) return;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        int rw = getRenderW(npcId);
        int rh = getRenderH(npcId);
        batch.draw(frame, x, y, rw, rh);
    }

    @Override
    public void dispose()
    {
        for (Texture tex : textures.values()) {
            tex.dispose();
        }
        textures.clear();
        anims.clear();
        renderWidths.clear();
        renderHeights.clear();
    }
}
