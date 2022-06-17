/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
package net.pistonmaster.serverwrecker.version.v1_16;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.common.SessionEventBus;

@RequiredArgsConstructor
public class SessionListener1_16 extends SessionAdapter {
    private final SessionEventBus bus;
    private final ProtocolWrapper1_16 wrapper;

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerChatPacket chatPacket) {
            // Message API was replaced in version 1.16
            Component message = chatPacket.getMessage();
            bus.onChat(PlainTextComponentSerializer.plainText().serialize(message));
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket posPacket) {
            double posX = posPacket.getX();
            double posY = posPacket.getY();
            double posZ = posPacket.getZ();
            float pitch = posPacket.getPitch();
            float yaw = posPacket.getYaw();
            bus.onPosition(posX, posY, posZ, pitch, yaw);
        } else if (receiveEvent.getPacket() instanceof ServerPlayerHealthPacket healthPacket) {
            bus.onHealth(healthPacket.getHealth(), healthPacket.getFood());
        } else if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            bus.onJoin();
        } else if (receiveEvent.getPacket() instanceof ServerDisconnectPacket disconnectPacket) {
            bus.onDisconnect(PlainTextComponentSerializer.plainText().serialize(disconnectPacket.getReason()), null);
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        bus.onDisconnect(event.getReason(), event.getCause());
    }
}