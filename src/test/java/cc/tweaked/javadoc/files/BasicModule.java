package cc.tweaked.javadoc.files;

import dan200.computercraft.api.lua.LuaFunction;

/**
 * A basic module
 * @cc.module basic
 */
public class BasicModule {
    /**
     * Add two numbers together
     * @param x The first number to add
     * @param y The second number to add
     * @return The added values
     * @cc.usage Do something simple.
     * <pre>{@code
     * print("Hello!")
     * print("World")
     * }</pre>
     */
    @LuaFunction
    public int add(int x, int y) {
        return x + y;
    }
}
