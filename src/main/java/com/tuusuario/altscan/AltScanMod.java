package com.tuusuario.altscan;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class AltScanMod implements ModInitializer {

    private final Deque<String> pendingNames = new ArrayDeque<>();
    private boolean scanning = false;
    private static final int TICKS_BETWEEN_CHECKS = 10;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("altscan")
                    .then(Commands.literal("on").executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        MinecraftServer server = source.getServer();
                        UUID playerUUID = source.getPlayer().getUUID();
                        if (!server.getPlayerList().isOp(playerUUID)) {
                            source.sendFailure(Component.literal("Necesitas ser operador para usar este comando."));
                            return 0;
                        }
                        startScan(server);
                        int count = server.getPlayerList().getPlayers().size();
                        source.sendSuccess(
                                () -> Component.literal("[AltScan] Escaneo iniciado sobre " + count + " jugador(es)."),
                                false
                        );
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("off").executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        MinecraftServer server = source.getServer();
                        UUID playerUUID = source.getPlayer().getUUID();
                        if (!server.getPlayerList().isOp(playerUUID)) {
                            source.sendFailure(Component.literal("Necesitas ser operador para usar este comando."));
                            return 0;
                        }
                        stopScan();
                        source.sendSuccess(() -> Component.literal("[AltScan] Escaneo detenido/cancelado."), false);
                        return Command.SINGLE_SUCCESS;
                    }))
            );
        });

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void startScan(MinecraftServer server) {
        pendingNames.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            pendingNames.add(player.getName().getString());
        }
        scanning = !pendingNames.isEmpty();
        tickCounter = 0;
    }

    private void stopScan() {
        pendingNames.clear();
        scanning = false;
    }

    private void onServerTick(MinecraftServer server) {
        if (!scanning) return;

        tickCounter++;
        if (tickCounter < TICKS_BETWEEN_CHECKS) return;
        tickCounter = 0;

        String name = pendingNames.poll();
        if (name == null) {
            scanning = false;
            server.getPlayerList().broadcastSystemMessage(Component.literal("[AltScan] Escaneo completo."), false);
            return;
        }

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "alts " + name);
    }
}
