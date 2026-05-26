package cn.edu.whut.sept.zuul.engine;

/**
 * useItem 等操作的执行结果，供 GUI 显示提示文案。
 */
public final class UseResult
{
    private final boolean success;
    private final String message;

    private UseResult(boolean success, String message)
    {
        this.success = success;
        this.message = message;
    }

    public static UseResult ok(String message)
    {
        return new UseResult(true, message);
    }

    public static UseResult fail(String message)
    {
        return new UseResult(false, message);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public String getMessage()
    {
        return message;
    }
}
