package com.tuusuario.altscan;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * AltScan
 * ------
 * Añade /altscan on y /altscan off.
 *
 * /altscan on  -> recorre UNA VEZ a todos los jugadores conectados (tablist)
 *                 y ejecuta "alts <nombre>" por cada uno, reenviando el
 *                 resultado al chat con el prefijo [AltScan].
 * /altscan off -> cancela cualquier escaneo en curso que aún no haya terminado.
 *
 * Requiere permiso de operador (nivel 3) para usarlo, igual que /alts.
 * No implementa el comando "alts" en sí: asume que otro plugin/mod ya lo
 * añade a tu servidor.
 *
 * Nota sobre nombres de clases: desde Minecraft 26.1, Fabric usa los
 * nombres oficiales de Mojang directamente (ServerPlayer, Component,
 * CommandSourceStack...) en vez de los nombres "Yarn" de versiones
 * anteriores (ServerPlayerEntity, Text, ServerCommandSource...).
 */
public class AltScanMod implements ModInitializer {

    private final Deque<String> pendingNames = new ArrayDeque<>();
    private boolean scanning = false;

    // Cada cuántos ticks se dispara el siguiente /alts (20 ticks = 1 segundo)
    private static final int TICKS_BETWEEN_CHECKS = 10; // 0.5s entre jugador y jugador
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("altscan")
                    .requires(src -> src.hasPermission(3))
                    .then(Commands.literal("on").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        startScan(server);
                        int count = server.getPlayerList().getPlayers().size();
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("[AltScan] Escaneo iniciado sobre " + count + " jugador(es)."),
                                false
                        );
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.literal("off").executes(ctx -> {
                        stopScan();
                        ctx.getSource().sendSuccess(() -> Component.literal("[AltScan] Escaneo detenido/cancelado."), false);
                        return Command.SINGLE_SUCCESS;
                    }))
            );
        });

        // Procesamos la cola poco a poco en cada tick del servidor para no
        // saturar de comandos de golpe.
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void startScan(MinecraftServer server) {
        pendingNames.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            pendingNames.add(player.getGameProfile().getName());
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

        runAltsCheck(server, name);
    }

    private void runAltsCheck(MinecraftServer server, String playerName) {
        // CommandSource personalizado: en vez de mandar la respuesta solo a
        // consola, la reenviamos al chat de todo el server con el prefijo
        // [AltScan].
        CommandSource broadcastOutput = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("[AltScan] ").append(message), false
                );
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };

        // Construimos una fuente de comandos equivalente a la del propio
        // servidor (permiso nivel 4), pero con nuestro CommandSource que
        // reenvía la respuesta al chat.
        CommandSourceStack altsSource = new CommandSourceStack(
                broadcastOutput,
                Vec3.ZERO,
                Vec2.ZERO,
                server.overworld(),
                4,
                "AltScan",
                Component.literal("AltScan"),
                server,
                null
        );

        server.getCommands().performPrefixedCommand(altsSource, "alts " + playerName);
    }
}
