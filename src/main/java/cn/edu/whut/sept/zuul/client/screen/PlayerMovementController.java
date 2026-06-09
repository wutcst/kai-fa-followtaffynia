package cn.edu.whut.sept.zuul.client.screen;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;

/**
 * 玩家移动控制器 —— 从 GameScreen 提取。
 */
public class PlayerMovementController
{
    private final NpcPlaceholderManager npcManager;
    private final float tile;
    private final float playerW;
    private final float playerH;
    private final float speed;
    private final float dashSpeedMultiplier;
    private final float dashDuration;
    private final float dashCooldown;
    private final float attackCooldown;

    private boolean isDashing;
    private float dashTimer;
    private float dashCooldownTimer;
    private boolean isAttacking;
    private float attackTimer;
    private float attackCooldownTimer;
    private boolean isMovingLastFrame;
    private String currentFacing = "south";

    public PlayerMovementController(NpcPlaceholderManager npcManager, float tile,
                                     float playerW, float playerH, float speed,
                                     float dashSpeedMultiplier, float dashDuration,
                                     float dashCooldown, float attackCooldown)
    {
        this.npcManager = npcManager;
        this.tile = tile;
        this.playerW = playerW;
        this.playerH = playerH;
        this.speed = speed;
        this.dashSpeedMultiplier = dashSpeedMultiplier;
        this.dashDuration = dashDuration;
        this.dashCooldown = dashCooldown;
        this.attackCooldown = attackCooldown;
    }

    public boolean isDashing() { return isDashing; }
    public boolean isAttacking() { return isAttacking; }
    public boolean isMovingLastFrame() { return isMovingLastFrame; }
    public String getFacing() { return currentFacing; }
    public void setFacing(String facing) { this.currentFacing = facing; }
    public float getDashTimer() { return dashTimer; }
    public float getAttackTimer() { return attackTimer; }

    /** 计时器衰减，每帧调用。 */
    public void update(float delta)
    {
        if (isDashing) {
            dashTimer -= delta;
            if (dashTimer <= 0f) {
                isDashing = false;
                dashTimer = 0f;
                dashCooldownTimer = dashCooldown;
            }
        }
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= delta;
        }
        if (isAttacking) {
            attackTimer -= delta;
            if (attackTimer <= 0f) {
                isAttacking = false;
                attackTimer = 0f;
                attackCooldownTimer = attackCooldown;
            }
        }
        if (attackCooldownTimer > 0f) {
            attackCooldownTimer -= delta;
        }
    }

    /** 通知攻击动画结束，提前中止攻击状态。 */
    public void checkAttackFinished(boolean attackAnimFinished)
    {
        if (isAttacking && attackAnimFinished) {
            isAttacking = false;
            attackTimer = 0f;
            attackCooldownTimer = attackCooldown;
        }
    }

    public boolean canStartDash() {
        return !isDashing && dashCooldownTimer <= 0f && !isAttacking;
    }

    public void startDash() {
        isDashing = true;
        dashTimer = dashDuration;
    }

    /** 重置所有计时器（房间切换时调用）。 */
    public void reset()
    {
        isDashing = false;
        dashTimer = 0f;
        dashCooldownTimer = 0f;
        isAttacking = false;
        attackTimer = 0f;
        attackCooldownTimer = 0f;
    }

    public boolean canStartAttack() {
        return !isAttacking && attackCooldownTimer <= 0f && !isDashing;
    }

    public void startAttack(float attackDuration) {
        isAttacking = true;
        attackTimer = attackDuration;
    }

    /**
     * 应用移动并返回新的玩家坐标。isAttacking 时玩家不能移动。
     */
    public MoveResult applyMovement(float delta, float playerX, float playerY,
                                     boolean wDown, boolean sDown, boolean aDown, boolean dDown,
                                     TiledMapTileLayer wallLayer, float mapPixelWidth,
                                     float mapPixelHeight)
    {
        if (isAttacking) {
            isMovingLastFrame = false;
            return new MoveResult(playerX, playerY, currentFacing, false);
        }

        if (isDashing) {
            return applyDashMovement(delta, playerX, playerY, wallLayer,
                mapPixelWidth, mapPixelHeight);
        }

        return applyNormalMovement(delta, playerX, playerY,
            wDown, sDown, aDown, dDown,
            wallLayer, mapPixelWidth, mapPixelHeight);
    }

    private MoveResult applyNormalMovement(float delta, float playerX, float playerY,
                                            boolean wDown, boolean sDown, boolean aDown, boolean dDown,
                                            TiledMapTileLayer wallLayer, float mapPixelWidth,
                                            float mapPixelHeight)
    {
        boolean moving = wDown || sDown || aDown || dDown;
        if (moving) {
            if (wDown && !sDown) currentFacing = "north";
            else if (sDown && !wDown) currentFacing = "south";
            else if (aDown && !dDown) currentFacing = "west";
            else if (dDown && !aDown) currentFacing = "east";
        }
        isMovingLastFrame = moving;

        float dx = 0f, dy = 0f;
        if (wDown) dy = speed * delta;
        if (sDown) dy = -speed * delta;
        if (aDown) dx = -speed * delta;
        if (dDown) dx = speed * delta;

        if (dx != 0f && canMove(playerX + dx, playerY, wallLayer, mapPixelWidth, mapPixelHeight, false)) {
            playerX += dx;
        }
        if (dy != 0f && canMove(playerX, playerY + dy, wallLayer, mapPixelWidth, mapPixelHeight, false)) {
            playerY += dy;
        }
        playerX = clampX(playerX, mapPixelWidth);
        playerY = clampY(playerY, mapPixelHeight);
        return new MoveResult(playerX, playerY, currentFacing, moving);
    }

    private MoveResult applyDashMovement(float delta, float playerX, float playerY,
                                          TiledMapTileLayer wallLayer, float mapPixelWidth,
                                          float mapPixelHeight)
    {
        float dx = 0f, dy = 0f;
        switch (currentFacing) {
            case "north": dy = speed * dashSpeedMultiplier * delta; break;
            case "south": dy = -speed * dashSpeedMultiplier * delta; break;
            case "west":  dx = -speed * dashSpeedMultiplier * delta; break;
            case "east":  dx = speed * dashSpeedMultiplier * delta; break;
        }
        isMovingLastFrame = true;
        if (dx != 0f && canMove(playerX + dx, playerY, wallLayer,
            mapPixelWidth, mapPixelHeight, true)) {
            playerX += dx;
        }
        if (dy != 0f && canMove(playerX, playerY + dy, wallLayer,
            mapPixelWidth, mapPixelHeight, true)) {
            playerY += dy;
        }
        playerX = clampX(playerX, mapPixelWidth);
        playerY = clampY(playerY, mapPixelHeight);
        return new MoveResult(playerX, playerY, currentFacing, true);
    }

    public float clampX(float x, float mapPixelWidth)
    {
        return Math.max(0f, Math.min(x, Math.max(0f, mapPixelWidth - playerW)));
    }

    public float clampY(float y, float mapPixelHeight)
    {
        return Math.max(0f, Math.min(y, Math.max(0f, mapPixelHeight - playerH)));
    }

    public int gdxYToTiledRow(float gdxY, int mapRows)
    {
        return mapRows - 1 - (int)((gdxY + playerH / 2f) / tile);
    }

    private boolean canMove(float newX, float newY, TiledMapTileLayer wallLayer,
                             float mapPixelWidth, float mapPixelHeight,
                             boolean skipNpcCollision)
    {
        if (newX < 0f || newY < 0f
            || newX + playerW > mapPixelWidth
            || newY + playerH > mapPixelHeight) {
            return false;
        }
        if (!skipNpcCollision && npcManager.collidesNpc(newX, newY, playerW, playerH)) {
            return false;
        }
        if (wallLayer == null) {
            return true;
        }

        float left = newX;
        float right = newX + playerW;
        float bottom = newY;
        float top = newY + playerH;
        int[][] corners = {
            {(int) (left / tile), (int) (bottom / tile)},
            {(int) (right / tile), (int) (bottom / tile)},
            {(int) (left / tile), (int) (top / tile)},
            {(int) (right / tile), (int) (top / tile)}
        };

        for (int[] corner : corners) {
            TiledMapTileLayer.Cell cell = wallLayer.getCell(corner[0], corner[1]);
            if (cell != null && cell.getTile() != null) {
                return false;
            }
        }
        return true;
    }

    /** 移动结果。 */
    public static class MoveResult
    {
        public final float newX;
        public final float newY;
        public final String facing;
        public final boolean isMoving;

        public MoveResult(float newX, float newY, String facing, boolean isMoving)
        {
            this.newX = newX;
            this.newY = newY;
            this.facing = facing;
            this.isMoving = isMoving;
        }
    }
}
