package cn.edu.whut.sept.zuul.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 玩家精灵渲染器：4方向动画（上下左右），对角线就近映射到主方向。
 *
 * 使用 Tiny Questers Warrior 素材单帧 PNG，四个方向统一处理。
 * 帧尺寸 64×49，渲染缩放至 128×98 以适应瓦片网格。
 */
public class PlayerRenderer implements Disposable
{
    private static final float FRAME_W = 64f;
    private static final float FRAME_H = 49f;
    private static final float SCALE = 2.0f;
    private static final float RENDER_W = FRAME_W * SCALE;
    private static final float RENDER_H = FRAME_H * SCALE;
    private static final float ATTACK_RENDER_W = 128f * SCALE;
    private static final float ATTACK_RENDER_H = 128f * SCALE;
    private static final float COLLISION_W = 16f;
    private static final float COLLISION_H = 16f;
    private static final float IDLE_DURATION = 0.20f;
    private static final float WALK_DURATION = 0.12f;
    private static final float DASH_ANIM_DURATION = 0.06f;
    private static final float ATTACK_ANIM_DURATION = 0.025f;
    private static final int WALK_FRAMES = 4;
    private static final int IDLE_FRAMES = 5;

    private static final String SINGLE_DIR = "role/tiny-questers-warrior-bodyonly-free/png/single/";

    /** 4方向（映射自素材：up=N, down=S, left=W, right=E） */
    public enum FacingDirection {
        N, S, E, W;

        public static FacingDirection fromString(String s) {
            if (s == null) return N;
            switch (s.toLowerCase()) {
                case "north": case "up":    return N;
                case "south": case "down":  return S;
                case "east":  case "right": return E;
                case "west":  case "left":  return W;
                default: return S;
            }
        }

        /** 素材文件名中的方向后缀 */
        public String assetName() {
            switch (this) { case N: return "up"; case S: return "down"; case E: return "right"; case W: return "left"; default: return "down"; }
        }
    }

    private enum AnimState { IDLE, WALKING, DASHING, ATTACKING }

    private final Map<FacingDirection, Animation<TextureRegion>> walkAnims;
    private final Map<FacingDirection, Animation<TextureRegion>> idleAnims;
    private final Map<FacingDirection, Animation<TextureRegion>> dashAnims;
    private final Map<FacingDirection, Animation<TextureRegion>> attackAnims;
    private final java.util.List<Texture> textures;

    private FacingDirection currentDirection = FacingDirection.S;
    private AnimState currentState = AnimState.IDLE;
    private float stateTime;

    public PlayerRenderer() {
        walkAnims = new EnumMap<>(FacingDirection.class);
        idleAnims = new EnumMap<>(FacingDirection.class);
        dashAnims = new EnumMap<>(FacingDirection.class);
        attackAnims = new EnumMap<>(FacingDirection.class);
        textures = new java.util.ArrayList<>();

        // ---- 四个方向统一：walk 用单帧，idle 上下用单帧、左右用 walk 第一帧 ----
        for (FacingDirection dir : FacingDirection.values()) {
            String dirName = dir.assetName();

            // walk: 4 帧
            TextureRegion[] walkFrames = loadFrames(SINGLE_DIR + "walk/" + dirName, WALK_FRAMES);
            walkAnims.put(dir, new Animation<>(WALK_DURATION, walkFrames));
            walkAnims.get(dir).setPlayMode(Animation.PlayMode.LOOP);

            // idle
            if (dir == FacingDirection.N || dir == FacingDirection.S) {
                TextureRegion[] idleFrames = loadFrames(SINGLE_DIR + "idle/" + dirName, IDLE_FRAMES);
                idleAnims.put(dir, new Animation<>(IDLE_DURATION, idleFrames));
                idleAnims.get(dir).setPlayMode(Animation.PlayMode.LOOP);
            } else {
                idleAnims.put(dir, new Animation<>(IDLE_DURATION, walkFrames[0]));
            }

            // dash: 复用 walk 帧加速播放
            dashAnims.put(dir, new Animation<>(DASH_ANIM_DURATION, walkFrames));
            dashAnims.get(dir).setPlayMode(Animation.PlayMode.LOOP);

            // attack: 四个方向均为4帧, 原地攻击播一次
            int attackFrames = 4;
            TextureRegion[] attackFramesArr = loadFrames(SINGLE_DIR + "attack_" + dirName, attackFrames);
            attackAnims.put(dir, new Animation<>(ATTACK_ANIM_DURATION, attackFramesArr));
            attackAnims.get(dir).setPlayMode(Animation.PlayMode.NORMAL);
        }
    }

    private TextureRegion[] loadFrames(String dirPath, int count) {
        TextureRegion[] frames = new TextureRegion[count];
        // 从路径中提取文件名前缀，例如 walk/up -> up
        String prefix = dirPath.substring(dirPath.lastIndexOf('/') + 1);
        for (int i = 1; i <= count; i++) {
            Texture tex = loadTexture(dirPath + "/" + prefix + i + ".png");
            frames[i - 1] = new TextureRegion(tex);
        }
        return frames;
    }

    private Texture loadTexture(String path) {
        Texture tex = new Texture(Gdx.files.internal(path));
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textures.add(tex);
        return tex;
    }

    public void update(float delta, boolean isMoving, boolean isDashing, boolean isAttacking, FacingDirection facing) {
        currentDirection = facing;
        if (isAttacking) {
            if (currentState != AnimState.ATTACKING) {
                stateTime = 0f;
            }
            currentState = AnimState.ATTACKING;
        } else if (isDashing) {
            currentState = AnimState.DASHING;
        } else {
            currentState = isMoving ? AnimState.WALKING : AnimState.IDLE;
        }
        stateTime += delta;
    }

    public void render(SpriteBatch batch, float playerX, float playerY) {
        Animation<TextureRegion> anim;
        switch (currentState) {
            case ATTACKING: anim = attackAnims.get(currentDirection); break;
            case DASHING:   anim = dashAnims.get(currentDirection); break;
            case WALKING:   anim = walkAnims.get(currentDirection); break;
            default:        anim = idleAnims.get(currentDirection); break;
        }
        if (anim == null) return;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        float renderW, renderH;
        if (currentState == AnimState.ATTACKING) {
            renderW = ATTACK_RENDER_W;
            renderH = ATTACK_RENDER_H;
        } else {
            renderW = RENDER_W;
            renderH = RENDER_H;
        }
        float rx = playerX - (renderW - COLLISION_W) / 2f;
        float ry = playerY - (renderH - COLLISION_H) / 2f;
        if (currentState == AnimState.ATTACKING) {
            // 攻击帧角色身体不在画布中心，按方向偏移
            switch (currentDirection) {
                case E: rx += 50f; break;
                case W: rx -= 50f; break;
                case N: ry -= 0f; break;
                case S: ry += 0f; break;
            }
        }
        batch.draw(frame, rx, ry, renderW, renderH);
    }

    public boolean isAttackFinished() {
        if (currentState != AnimState.ATTACKING) return false;
        Animation<TextureRegion> anim = attackAnims.get(currentDirection);
        return anim != null && anim.isAnimationFinished(stateTime);
    }

    @Override
    public void dispose() {
        for (Texture tex : textures) {
            tex.dispose();
        }
        textures.clear();
    }
}
