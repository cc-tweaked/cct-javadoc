package cc.tweaked.javadoc.files;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.asm.GenericSource;
import net.minecraft.util.ResourceLocation;

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
    public ResourceLocation id() {
        return new ResourceLocation();
    }

    public static class EnergyStorage {
    }
}
