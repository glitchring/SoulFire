/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.command;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CommandSourceStack(
  CommandSource source,
  @Nullable
  List<UUID> instanceIds,
  @Nullable
  List<UUID> botIds
) {
  public static CommandSourceStack ofUnrestricted(CommandSource source) {
    return new CommandSourceStack(source, null, null);
  }

  public static CommandSourceStack ofInstance(CommandSource source, List<UUID> instanceIds) {
    return new CommandSourceStack(source, instanceIds, null);
  }

  public CommandSourceStack withInstanceIds(List<UUID> instanceIds) {
    if (Objects.equals(this.instanceIds, instanceIds)) {
      return this;
    }

    if (this.instanceIds != null) {
      throw new IllegalStateException("Instance IDs already set");
    }

    return new CommandSourceStack(source, instanceIds, botIds);
  }

  public CommandSourceStack withBotIds(List<UUID> botIds) {
    if (Objects.equals(this.botIds, botIds)) {
      return this;
    }

    if (this.botIds != null) {
      throw new IllegalStateException("Bot names already set");
    }

    return new CommandSourceStack(source, instanceIds, botIds);
  }
}
