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
package net.pistonmaster.serverwrecker.api.event.attack;

import net.pistonmaster.serverwrecker.AttackManager;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerAttackEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;

/**
 * The event is called the moment after a bot connection object was created.
 * The BotConnection instance has all fields filled, but most methods are unusable as the bot is not connected yet.
 * <br>
 * This event is recommended for when you want to add an addon listener to the bot connection.
 */
public record BotConnectionInitEvent(BotConnection connection) implements ServerWreckerAttackEvent {
    @Override
    public AttackManager attackManager() {
        return connection.attackManager();
    }
}
