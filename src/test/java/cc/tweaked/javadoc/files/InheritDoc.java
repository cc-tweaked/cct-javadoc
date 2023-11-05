package cc.tweaked.javadoc.files;

import dan200.computercraft.api.lua.LuaFunction;

public class InheritDoc {
    public static class Base {
        /**
         * Add two numbers together.
         *
         * @param x The first number to add.
         * @param y The second number to add.
         * @return The result of adding two numbers.
         */
        public int add(int x, int y) {
            return 0;
        }
    }

    /**
     * @cc.module types.Adder
     */
    public static class Adder extends Base {
        /**
         * {@inheritDoc}
         */
        @Override
        @LuaFunction
        public int add(int x, int y) {
            return super.add(x, y);
        }
    }
}
