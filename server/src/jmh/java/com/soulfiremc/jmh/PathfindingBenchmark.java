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
package com.soulfiremc.jmh;

import com.google.gson.JsonObject;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.ResourceKey;
import com.soulfiremc.server.pathfinding.BotEntityState;
import com.soulfiremc.server.pathfinding.RouteFinder;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.pathfinding.graph.ProjectedLevel;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.test.utils.TestBlockAccessor;
import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.ResourceHelper;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@Slf4j
@State(Scope.Benchmark)
public class PathfindingBenchmark {
  private RouteFinder routeFinder;
  private BotEntityState initialState;

  @Setup
  public void setup() {
    var byteArrayInputStream =
      new ByteArrayInputStream(ResourceHelper.getResourceBytes("/world_data.json.zip"));
    try (var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
         var reader = new InputStreamReader(gzipInputStream)) {
      log.info("Reading world data...");
      var worldData = GsonInstance.GSON.fromJson(reader, JsonObject.class);
      var definitions = worldData.getAsJsonArray("definitions");
      var blockDefinitions = new String[definitions.size()];
      for (var i = 0; i < definitions.size(); i++) {
        blockDefinitions[i] = definitions.get(i).getAsString();
      }

      var data = GsonInstance.GSON.fromJson(worldData.getAsJsonArray("data"), int[][][].class);

      log.info("Parsing world data...");

      var accessor = new TestBlockAccessor();
      for (var x = 0; x < data.length; x++) {
        var xArray = data[x];
        for (var y = 0; y < xArray.length; y++) {
          var yArray = xArray[y];
          for (var z = 0; z < yArray.length; z++) {
            accessor.setBlockAt(x, y, z, BlockType.getByKey(ResourceKey.fromString(blockDefinitions[yArray[z]])));
          }
        }
      }

      log.info("Calculating world data...");

      // Find the first safe block at 0 0
      var safeY = 0;
      for (var y = 0; y < 255; y++) {
        if (accessor.getBlockStateAt(0, y, 0).blockType() == BlockType.AIR) {
          safeY = y;
          break;
        }
      }

      routeFinder = new RouteFinder(new MinecraftGraph(new TagsState()), new PosGoal(100, 80, 100));

      initialState =
        new BotEntityState(
          new SFVec3i(0, safeY, 0),
          new ProjectedLevel(accessor),
          new ProjectedInventory(new PlayerInventoryContainer(null)));

      log.info("Done loading! Testing...");
    } catch (Exception e) {
      log.error("Failed to load world data!", e);
    }
  }

  @Benchmark
  public void calculatePath() {
    routeFinder.findRoute(initialState, true);
  }
}
