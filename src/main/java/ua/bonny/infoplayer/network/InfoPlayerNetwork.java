package ua.bonny.infoplayer.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ua.bonny.infoplayer.client.ClientPayloadHandler;
import ua.bonny.infoplayer.server.PlayerInfoService;

public final class InfoPlayerNetwork {
    private InfoPlayerNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToServer(ListRequestPayload.TYPE, ListRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerInfoService.sendList(player);
            }
        });
        registrar.playToClient(ListResponsePayload.TYPE, ListResponsePayload.STREAM_CODEC, InfoPlayerNetwork::handleList);
        registrar.playToServer(DetailRequestPayload.TYPE, DetailRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerInfoService.sendDetail(player, payload.playerId());
            }
        });
        registrar.playToClient(DetailResponsePayload.TYPE, DetailResponsePayload.STREAM_CODEC, InfoPlayerNetwork::handleDetail);
        registrar.playToServer(TakeItemRequestPayload.TYPE, TakeItemRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerInfoService.takeItem(
                        player,
                        payload.playerId(),
                        payload.slotIndex(),
                        payload.curiosType(),
                        payload.cosmetic());
            }
        });
    }

    private static void handleList(ListResponsePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPayloadHandler.handleList(payload, context);
        }
    }

    private static void handleDetail(DetailResponsePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPayloadHandler.handleDetail(payload, context);
        }
    }
}
