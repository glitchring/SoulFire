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
package com.soulfiremc.server.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

public class SFHelpers {
  private SFHelpers() {}

  public static byte[][] split(byte[] data, byte separator) {
    var count = 0;
    for (var b : data) {
      if (b == separator) {
        count++;
      }
    }

    if (count == 0) {
      return new byte[][]{data};
    }

    var result = new byte[count + 1][];
    var last = 0;
    var index = 0;
    for (var i = 0; i < data.length; i++) {
      if (data[i] == separator) {
        result[index++] = Arrays.copyOfRange(data, last, i);
        last = i + 1;
      }
    }

    result[index] = Arrays.copyOfRange(data, last, data.length);
    return result;
  }

  public static OptionalInt parseInt(String s) {
    try {
      return OptionalInt.of(Integer.parseInt(s));
    } catch (NumberFormatException e) {
      return OptionalInt.empty();
    }
  }

  public static BooleanSupplier not(BooleanSupplier supplier) {
    return () -> !supplier.getAsBoolean();
  }

  public static void writeIfNeeded(Path path, String content) throws IOException {
    if (Files.exists(path)) {
      var existingContent = Files.readString(path);
      if (!existingContent.equals(content)) {
        Files.writeString(path, content);
      }
    } else {
      Files.createDirectories(path.getParent());
      Files.writeString(path, content);
    }
  }
}
