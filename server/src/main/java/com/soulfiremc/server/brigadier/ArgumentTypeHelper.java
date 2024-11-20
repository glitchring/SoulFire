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
package com.soulfiremc.server.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.UUIDHelper;

public class ArgumentTypeHelper {
  private ArgumentTypeHelper() {
  }

  public static void mustReadSpace(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead() || reader.peek() != ' ') {
      throw new SimpleCommandExceptionType(new LiteralMessage("Expected space")).createWithContext(reader);
    }

    reader.skip();
  }

  public static DoubleAxisData readAxis(StringReader reader) throws CommandSyntaxException {
    if (reader.canRead() && reader.peek() == '~') {
      reader.skip();
      double value = 0;
      if (reader.canRead() && reader.peek() != ' ') {
        value = reader.readDouble();
      }

      return new DoubleAxisData(true, value);
    }

    return new DoubleAxisData(false, reader.readDouble());
  }

  public record DoubleAxisData(boolean relative, double value) {
  }

  public static int parseEntityId(BotConnection bot, String input) {
    var dataManager = bot.dataManager();

    var parsedUniqueId = UUIDHelper.tryParseUniqueId(input);
    var entityId = -1;
    for (var entity : dataManager.entityTrackerState().getEntities()) {
      if (entity.entityType() != EntityType.PLAYER) {
        continue;
      }

      var connectedUsers = dataManager.playerListState();
      var entry = connectedUsers.entries().get(entity.uuid());
      if (entry != null
        && ((parsedUniqueId.isPresent() && entry.getProfileId().equals(parsedUniqueId.get()))
        || (entry.getProfile() != null && entry.getProfile().getName().equalsIgnoreCase(input)))
      ) {
        entityId = entity.entityId();
        break;
      }
    }

    if (entityId == -1) {
      var parsedEntityId = SFHelpers.parseInt(input);
      if (parsedEntityId.isEmpty()) {
        return -1;
      }

      entityId = parsedEntityId.getAsInt();
    }

    return entityId;
  }
}
