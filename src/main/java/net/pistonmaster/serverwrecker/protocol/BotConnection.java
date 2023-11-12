/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import net.kyori.event.EventBus;
import net.pistonmaster.serverwrecker.AttackManager;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerBotEvent;
import net.pistonmaster.serverwrecker.api.event.attack.PreBotConnectEvent;
import net.pistonmaster.serverwrecker.protocol.bot.BotControlAPI;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public record BotConnection(UUID connectionId, BotConnectionFactory factory, AttackManager attackManager,
                            ServerWreckerServer serverWreckerServer, SettingsHolder settingsHolder,
                            Logger logger, MinecraftProtocol protocol, ViaClientSession session,
                            ExecutorManager executorManager, BotConnectionMeta meta,
                            EventBus<ServerWreckerBotEvent> eventBus) {
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            attackManager.getEventBus().post(new PreBotConnectEvent(this));
            session.connect(true);
        });
    }

    public boolean isOnline() {
        return session.isConnected();
    }

    public SessionDataManager sessionDataManager() {
        return meta.getSessionDataManager();
    }

    public BotControlAPI botControl() {
        return meta.getBotControlAPI();
    }

    public void tick(long ticks, float partialTicks) {
        session.tick(); // Ensure all packets are handled before ticking
        for (var i = 0; i < ticks; i++) {
            try {
                sessionDataManager().tick();
            } catch (Throwable t) {
                logger.error("Error while ticking bot!", t);
            }
        }
    }

    public GlobalTrafficShapingHandler getTrafficHandler() {
        return session.getFlag(SWProtocolConstants.TRAFFIC_HANDLER);
    }

    public CompletableFuture<Void> gracefulDisconnect() {
        return CompletableFuture.runAsync(() -> {
            session.disconnect("Disconnect");

            // Give the server one second to handle the disconnect
            TimeUtil.waitTime(1, TimeUnit.SECONDS);

            // Shut down all executors
            executorManager.shutdownAll();

            // Let threads finish that didn't immediately interrupt
            TimeUtil.waitTime(100, TimeUnit.MILLISECONDS);
        });
    }
}
