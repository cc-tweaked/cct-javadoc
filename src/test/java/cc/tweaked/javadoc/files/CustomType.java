package cc.tweaked.javadoc.files;

import dan200.computercraft.api.lua.LuaFunction;

import java.util.List;

public class CustomType {
    /**
     * S
     *
     * @cc.module types.One
     */
    public static class One {
        @LuaFunction
        public final int getOne() {
            return 1;
        }
    }

    /**
     * [S, S]
     *
     * @cc.module types.Two
     */
    public static class Two {
        @LuaFunction
        public final List<One> getTwo() {
            return List.of(new One(), new One());
        }
    }
}
