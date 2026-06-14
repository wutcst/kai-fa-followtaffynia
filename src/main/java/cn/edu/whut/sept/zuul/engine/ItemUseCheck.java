package cn.edu.whut.sept.zuul.engine;

/**
 * 使用物品前的校验结果，供 GUI 显示与禁用不可选项。
 */
public final class ItemUseCheck
{
    public final boolean canUse;
    public final boolean requiresLocation;
    public final String hint;

    private ItemUseCheck(boolean canUse, boolean requiresLocation, String hint)
    {
        this.canUse = canUse;
        this.requiresLocation = requiresLocation;
        this.hint = hint;
    }

    public static ItemUseCheck anytime()
    {
        return new ItemUseCheck(true, false, "可随时使用");
    }

    public static ItemUseCheck allowed(String hint)
    {
        return new ItemUseCheck(true, false, hint);
    }

    public static ItemUseCheck needLocation(String hint)
    {
        return new ItemUseCheck(false, true, hint);
    }

    public static ItemUseCheck blocked(String hint)
    {
        return new ItemUseCheck(false, false, hint);
    }
}
