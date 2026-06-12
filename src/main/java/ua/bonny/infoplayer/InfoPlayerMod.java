package ua.bonny.infoplayer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import ua.bonny.infoplayer.network.InfoPlayerNetwork;
import ua.bonny.infoplayer.server.ServerEvents;

@Mod(InfoPlayerMod.MOD_ID)
public final class InfoPlayerMod {
    public static final String MOD_ID = "infoplayer";

    public InfoPlayerMod(IEventBus modBus) {
        modBus.addListener(InfoPlayerNetwork::register);
        NeoForge.EVENT_BUS.register(ServerEvents.class);
    }
}
