package com.burnedkirby.TurnBasedMinecraft.common;

import com.burnedkirby.TurnBasedMinecraft.client.ClientConfig;
import com.burnedkirby.TurnBasedMinecraft.client.ClientConfigGui;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TurnBasedMinecraftMod.MODID, dist = Dist.CLIENT)
public class TBMM_Client {
    public TBMM_Client(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ClientConfigGui::new);
    }
}
