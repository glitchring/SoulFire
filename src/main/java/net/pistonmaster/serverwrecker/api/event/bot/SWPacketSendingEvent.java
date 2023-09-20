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
package net.pistonmaster.serverwrecker.api.event.bot;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.event.AbstractCancellable;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerBotEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;

/**
 * This event is called when a packet is sent to the connected server.
 * Setter is used to change the packet by a plugin.
 */
@AllArgsConstructor
public class SWPacketSendingEvent extends AbstractCancellable implements ServerWreckerBotEvent {
    private final BotConnection connection;
    @Getter
    @Setter
    private MinecraftPacket packet;

    @Override
    public BotConnection connection() {
        return connection;
    }
}
