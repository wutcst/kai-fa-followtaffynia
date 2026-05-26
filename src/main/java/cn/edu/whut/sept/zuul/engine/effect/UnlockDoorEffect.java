package cn.edu.whut.sept.zuul.engine.effect;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UseResult;

/**
 * 处理 effect 格式：unlock:&lt;lockId&gt;
 */
public class UnlockDoorEffect implements UseEffect
{
    private static final String PREFIX = "unlock:";

    @Override
    public boolean supports(String effect)
    {
        return effect != null && effect.startsWith(PREFIX);
    }

    @Override
    public UseResult apply(GameEngine engine, Item item, String effect)
    {
        String lockId = effect.substring(PREFIX.length()).trim();
        if (lockId.isEmpty()) {
            return UseResult.fail("这把钥匙似乎坏了。");
        }
        if (engine.isLockUnlocked(lockId)) {
            return UseResult.fail("这扇门已经开了。");
        }
        engine.unlockLock(lockId);
        return UseResult.ok("使用了 " + item.getName() + "，" + lockId + " 已解锁！");
    }
}
