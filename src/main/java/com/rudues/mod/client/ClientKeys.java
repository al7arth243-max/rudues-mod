package com.rudues.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public class ClientKeys {
    /** Hold to open the skill wheel. Rebindable in Controls. */
    public static final KeyMapping OPEN_WHEEL = new KeyMapping("key.rudues.open_wheel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_R, "key.categories.rudues");
}
