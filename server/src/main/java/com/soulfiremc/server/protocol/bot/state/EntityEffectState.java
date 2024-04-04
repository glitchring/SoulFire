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
package com.soulfiremc.server.protocol.bot.state;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.soulfiremc.server.protocol.bot.model.EffectData;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class EntityEffectState {
  private final Map<Effect, InternalEffectState> effects = new EnumMap<>(Effect.class);

  public void updateEffect(
    Effect effect,
    int amplifier,
    int duration,
    boolean ambient,
    boolean showParticles,
    boolean showIcon,
    CompoundTag factorData) {
    effects.put(
      effect,
      new InternalEffectState(amplifier, ambient, showParticles, showIcon, factorData, duration));
  }

  public void removeEffect(Effect effect) {
    effects.remove(effect);
  }

  public Optional<EffectData> getEffect(Effect effect) {
    var state = effects.get(effect);

    if (state == null) {
      return Optional.empty();
    }

    return Optional.of(
      new EffectData(
        effect,
        state.amplifier(),
        state.duration(),
        state.ambient(),
        state.showParticles(),
        state.showIcon(),
        state.factorData()));
  }

  public void tick() {
    effects.values().forEach(effect -> effect.duration(effect.duration() - 1));
    effects.values().removeIf(effect -> effect.duration() <= 0);
  }

  @Getter
  @Setter
  @AllArgsConstructor
  public static class InternalEffectState {
    private final int amplifier;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final CompoundTag factorData;
    private int duration;
  }
}
