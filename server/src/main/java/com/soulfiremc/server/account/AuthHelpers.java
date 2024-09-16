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
package com.soulfiremc.server.account;

import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.BedrockData;
import com.soulfiremc.settings.account.service.OnlineChainJavaData;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

public class AuthHelpers {
  public static MinecraftAccount fromFullBedrockSession(AbstractStep<?, StepFullBedrockSession.FullBedrockSession> flow, StepFullBedrockSession.FullBedrockSession fullBedrockSession) {
    var mcChain = fullBedrockSession.getMcChain();
    var xblXsts = mcChain.getXblXsts();
    var deviceId = xblXsts.getInitialXblSession().getXblDeviceToken().getId();
    var playFabId = fullBedrockSession.getPlayFabToken().getPlayFabId();
    return new MinecraftAccount(
      AuthType.MICROSOFT_BEDROCK_CREDENTIALS,
      mcChain.getId(),
      mcChain.getDisplayName(),
      new BedrockData(
        mcChain.getMojangJwt(),
        mcChain.getIdentityJwt(),
        mcChain.getPublicKey(),
        mcChain.getPrivateKey(),
        deviceId,
        playFabId,
        flow.toJson(fullBedrockSession)));
  }

  public static MinecraftAccount fromFullJavaSession(AbstractStep<?, StepFullJavaSession.FullJavaSession> flow, StepFullJavaSession.FullJavaSession fullJavaSession) {
    var mcProfile = fullJavaSession.getMcProfile();
    var mcToken = mcProfile.getMcToken();
    return new MinecraftAccount(
      AuthType.MICROSOFT_JAVA_CREDENTIALS,
      mcProfile.getId(),
      mcProfile.getName(),
      new OnlineChainJavaData(
        mcToken.getAccessToken(),
        mcToken.getExpireTimeMs(),
        flow.toJson(fullJavaSession)));
  }
}
