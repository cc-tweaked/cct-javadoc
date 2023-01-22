package cc.tweaked.javadoc.files;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;

import javax.annotation.Nonnull;

public class ModuleKind {
    /**
     * @cc.module foo
     * @see BarType
     */
    public static class FooModule implements GenericSource {
        @LuaFunction
        public static void something(Object target) {
        }

        @Nonnull
        @Override
        public String id() {
            return "test:foo";
        }
    }

    /**
     * @cc.module [kind=bar_kind] bar.Type
     * @see FooModule
     */
    public static class BarType {
        @LuaFunction
        public final void something() {
        }
    }
}
