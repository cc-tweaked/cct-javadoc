package cc.tweaked.javadoc.files;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nonnull;

/**
 * @cc.module generic_peripheral
 */
public class GenericPeripheral implements GenericSource {
    @LuaFunction
    public static void basicMethod(EnergyStorage energy) {
    }

    @LuaFunction
    public static void methodWithComputer(EnergyStorage energy, IComputerAccess computer) {
    }

    @LuaFunction
    public static void methodWithLua(EnergyStorage energy, ILuaContext lua) {
    }

    @LuaFunction
    public static void methodWithArgs(EnergyStorage energy, int x) {
    }

    @Nonnull
    @Override
    public String id() {
        return "test:generic";
    }

    public static class EnergyStorage {
    }
}
