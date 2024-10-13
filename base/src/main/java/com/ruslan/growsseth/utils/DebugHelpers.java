package com.ruslan.growsseth.utils;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class DebugHelpers {
    /**
     * Use in ALL breakpoint conditions when developing in Linux, else intellji has a bug where
     * Minecraft steals focus from everything and doesn't allow any input in intellij, for some reason.
     */
    public static boolean breakpointConditionFixLinuxDebug() {
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        return true;
    }
}

