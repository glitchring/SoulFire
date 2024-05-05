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
package com.soulfiremc.server.pathfinding.graph.actions;

import com.soulfiremc.server.data.BlockItems;
import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.BlockBreakAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.ObjectReference;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.util.TriState;

public final class DownMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final SFVec3i targetToMineBlock;
  @Getter
  @Setter
  private MovementMiningCost breakCost;
  @Getter
  @Setter
  private int closestBlockToFallOn = Integer.MIN_VALUE;

  public DownMovement() {
    this.targetToMineBlock = FEET_POSITION_RELATIVE_BLOCK.sub(0, 1, 0);
  }

  public static void registerDownMovements(
    Consumer<GraphAction> callback,
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers) {
    callback.accept(registerDownMovement(blockSubscribers, new DownMovement()));
  }

  public static DownMovement registerDownMovement(
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers,
    DownMovement movement) {
    {
      for (var safetyBlock : movement.listSafetyCheckBlocks()) {
        blockSubscribers
          .accept(safetyBlock, new DownMovementBlockSubscription(MinecraftGraph.SubscriptionType.DOWN_SAFETY_CHECK));
      }
    }

    {
      var freeBlock = movement.blockToBreak();
      blockSubscribers
        .accept(freeBlock.key(), new DownMovementBlockSubscription(MinecraftGraph.SubscriptionType.MOVEMENT_FREE, 0, freeBlock.value()));
    }

    {
      var safeBlocks = movement.listCheckSafeMineBlocks();
      for (var i = 0; i < safeBlocks.length; i++) {
        var savedBlock = safeBlocks[i];
        if (savedBlock == null) {
          continue;
        }

        for (var block : savedBlock) {
          blockSubscribers
            .accept(block.position(), new DownMovementBlockSubscription(
              MinecraftGraph.SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK,
              i,
              block.type()));
        }
      }
    }

    return movement;
  }

  public Pair<SFVec3i, BlockFace> blockToBreak() {
    return Pair.of(targetToMineBlock, BlockFace.TOP);
  }

  // These blocks are possibly safe blocks we can fall on top of
  public List<SFVec3i> listSafetyCheckBlocks() {
    var requiredFreeBlocks = new ObjectArrayList<SFVec3i>();

    // Falls one block
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0));

    // Falls two blocks
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0));

    // Falls three blocks
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 4, 0));

    return requiredFreeBlocks;
  }

  public BlockSafetyData[][] listCheckSafeMineBlocks() {
    var results = new BlockSafetyData[1][];

    var firstDirection = SkyDirection.NORTH;
    var oppositeDirection = firstDirection.opposite();
    var leftDirectionSide = firstDirection.leftSide();
    var rightDirectionSide = firstDirection.rightSide();

    results[0] =
      new BlockSafetyData[] {
        new BlockSafetyData(
          firstDirection.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          oppositeDirection.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          leftDirectionSide.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          rightDirectionSide.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS)
      };

    return results;
  }

  @Override
  public List<GraphInstructions> getInstructions(SFVec3i node) {
    if (closestBlockToFallOn == Integer.MIN_VALUE) {
      return Collections.emptyList();
    }

    var cost = 0D;

    cost +=
      switch (closestBlockToFallOn) {
        case -2 -> Costs.FALL_1;
        case -3 -> Costs.FALL_2;
        case -4 -> Costs.FALL_3;
        default -> throw new IllegalStateException("Unexpected value: " + closestBlockToFallOn);
      };

    cost += breakCost.miningCost();

    var absoluteTargetFeetBlock = node.add(0, closestBlockToFallOn + 1, 0);

    return Collections.singletonList(new GraphInstructions(
      absoluteTargetFeetBlock,
      cost,
      List.of(new BlockBreakAction(breakCost))));
  }

  @Override
  public DownMovement copy() {
    return this.clone();
  }

  @Override
  public DownMovement clone() {
    try {
      return (DownMovement) super.clone();
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }

  public record DownMovementBlockSubscription(
    MinecraftGraph.SubscriptionType type,
    int blockArrayIndex,
    BlockFace blockBreakSideHint,
    BlockSafetyData.BlockSafetyType safetyType) implements MinecraftGraph.MovementSubscription<DownMovement> {
    public DownMovementBlockSubscription(MinecraftGraph.SubscriptionType type) {
      this(type, -1, null, null);
    }

    public DownMovementBlockSubscription(MinecraftGraph.SubscriptionType type, int blockArrayIndex, BlockFace blockBreakSideHint) {
      this(type, blockArrayIndex, blockBreakSideHint, null);
    }

    public DownMovementBlockSubscription(
      MinecraftGraph.SubscriptionType subscriptionType,
      int i,
      BlockSafetyData.BlockSafetyType type) {
      this(subscriptionType, i, null, type);
    }

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, DownMovement downMovement, ObjectReference<TriState> isFreeReference,
                                                                BlockState blockState, SFVec3i absolutePositionBlock) {
      return switch (type) {
        case MOVEMENT_FREE -> {
          if (!graph.canBreakBlocks()
            || !BlockTypeHelper.isDiggable(blockState.blockType())
            // Narrows the list down to a reasonable size
            || !BlockItems.hasItemType(blockState.blockType())) {
            // No way to break this block
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          var cacheableMiningCost = graph.inventory().getMiningCosts(graph.tagsState(), blockState);
          // We can mine this block, lets add costs and continue
          downMovement.breakCost(
            new MovementMiningCost(
              absolutePositionBlock,
              cacheableMiningCost.miningCost(),
              cacheableMiningCost.willDrop(),
              blockBreakSideHint));
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case DOWN_SAFETY_CHECK -> {
          var yLevel = key.y;

          if (yLevel < downMovement.closestBlockToFallOn()) {
            // We already found a block to fall on, above this one
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
            // We found a block to fall on
            downMovement.closestBlockToFallOn(yLevel);
          }

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_BREAK_SAFETY_CHECK -> {
          var unsafe = safetyType.isUnsafeBlock(blockState);

          if (unsafe) {
            // We know already WE MUST dig the block below for this action
            // So if one block around the block below is unsafe, we can't do this action
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          // All good, we can continue
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };
    }

    @Override
    public DownMovement castAction(GraphAction action) {
      return (DownMovement) action;
    }
  }
}
