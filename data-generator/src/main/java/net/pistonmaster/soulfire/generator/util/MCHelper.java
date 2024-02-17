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
package net.pistonmaster.soulfire.generator.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public class MCHelper {
  public static ServerLevel getLevel() {
    return getServer().overworld();
  }

  @SuppressWarnings("deprecation")
  public static DedicatedServer getServer() {
    return (DedicatedServer) FabricLoader.getInstance().getGameInstance();
  }

  public static GameTestHelper getGameTestHelper() {
    return new GameTestHelper(null) {
      @Override
      public @NotNull ServerLevel getLevel() {
        return MCHelper.getLevel();
      }
    };
  }
}
