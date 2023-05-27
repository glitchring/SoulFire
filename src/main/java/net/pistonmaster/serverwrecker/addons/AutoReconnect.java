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
package net.pistonmaster.serverwrecker.addons;

import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotDisconnectedEvent;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

public class AutoReconnect implements InternalAddon, EventSubscriber<BotDisconnectedEvent> {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListener(BotDisconnectedEvent.class, this);
    }

    @Override
    public void on(@NonNull BotDisconnectedEvent event) throws Throwable {
        BotSettings botSettings = event.connection().settingsHolder().get(BotSettings.class);
        if (!botSettings.autoReconnect() || event.connection().serverWrecker().getAttackState().isInactive()) {
            return;
        }

        event.connection().serverWrecker().getScheduler().schedule(() -> {
            event.connection().factory().connect()
                    .thenAccept(newConnection -> event.connection().serverWrecker().getBotConnections()
                            .replaceAll(connection1 -> connection1 == event.connection() ? newConnection : connection1));
        }, 1, TimeUnit.SECONDS);
    }
}
