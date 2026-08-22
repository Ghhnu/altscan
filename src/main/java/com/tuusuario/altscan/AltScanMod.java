package com.tuusuario.altscan;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandOutput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
 * añade a tu servidor (tú ya confirmaste que tienes acceso a él).
 */
public class AltScanMod implements ModInitializer {

    // Cola de jugadores pendientes de escanear
    private final Deque<String> pendingNames = new ArrayDeque<>();
    private boolean scanning = false;

    // Cada cuántos ticks se dispara el siguiente /alts (20 ticks = 1 segundo)
    private static final int TICKS_BETWEEN_CHECKS = 10; // 0.5s entre jugador y jugador
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("altscan")
                    .requires(src -> src.hasPermissionLevel(3))
                    .then(CommandManager.literal("on").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        startScan(server);
                        ctx.getSource().sendFeedback(
                                () -> Text.literal("[AltScan] Escaneo iniciado sobre " + server.getPlayerManager().getPlayerList().size() + " jugador(es)."),
                                false
                        );
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("off").executes(ctx -> {
                        stopScan();
                        ctx.getSource().sendFeedback(() -> Text.literal("[AltScan] Escaneo detenido/cancelado."), false);
                        return Command.SINGLE_SUCCESS;
                    }))
            );
        });

        // Procesamos la cola poco a poco en cada tick del servidor para no
        // saturar de comandos de golpe (y para no chocar con cooldowns del
        // plugin que provee /alts si los tuviera).
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void startScan(MinecraftServer server) {
        pendingNames.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
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
            server.getPlayerManager().broadcast(Text.literal("[AltScan] Escaneo completo."), false);
            return;
        }

        runAltsCheck(server, name);
    }

    private void runAltsCheck(MinecraftServer server, String playerName) {
        // CommandOutput personalizado: en vez de mandar la respuesta solo a
        // consola, la reenviamos al chat de todo el server con el prefijo
        // [AltScan] para que la veas sin mirar la consola.
        CommandOutput broadcastOutput = new CommandOutput() {
            @Override
            public void sendMessage(Text message) {
                server.getPlayerManager().broadcast(
                        Text.literal("[AltScan] ").append(message), false
                );
            }

            @Override
            public boolean shouldReceiveFeedback() {
                return true;
            }

            @Override
            public boolean shouldTrackOutput() {
                return true;
            }

            @Override
            public boolean shouldBroadcastConsoleToOps() {
                return false;
            }
        };

        // Usamos la fuente de comandos del propio servidor (permiso nivel 4)
        // envuelta con nuestro output, así el comando "alts" se ejecuta con
        // permisos de sobra sin depender de que tú estés conectado.
        ServerCommandSource altsSource = server.getCommandSource().withOutput(broadcastOutput);

        server.getCommandManager().executeWithPrefix(altsSource, "alts " + playerName);
    }
}
