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
package net.pistonmaster.serverwrecker.addons;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.GlobalEventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.bot.SWPacketSendingEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.UUIDHelper;
import net.pistonmaster.serverwrecker.util.VelocityConstants;
import picocli.CommandLine;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ForwardingBypass implements InternalAddon {
    private static int findForwardingVersion(int requested, BotConnection player) {
        // TODO: Fix this
        /*
        // Ensure we are in range
        requested = Math.min(requested, VelocityConstants.MODERN_FORWARDING_MAX_VERSION);
        if (requested > VelocityConstants.MODERN_FORWARDING_DEFAULT) {
            if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
                return requested >= VelocityConstants.MODERN_LAZY_SESSION
                        ? VelocityConstants.MODERN_LAZY_SESSION
                        : VelocityConstants.MODERN_FORWARDING_DEFAULT;
            }
            if (player.getIdentifiedKey() != null) {
                // No enhanced switch on java 11
                return switch (player.getIdentifiedKey().getKeyRevision()) {
                    case GENERIC_V1 -> VelocityConstants.MODERN_FORWARDING_WITH_KEY;
                    // Since V2 is not backwards compatible we have to throw the key if v2 and requested is v1
                    case LINKED_V2 -> requested >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                            ? VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                            : VelocityConstants.MODERN_FORWARDING_DEFAULT;
                    default -> VelocityConstants.MODERN_FORWARDING_DEFAULT;
                };
            } else {
                return VelocityConstants.MODERN_FORWARDING_DEFAULT;
            }
        }
         */
        return VelocityConstants.MODERN_FORWARDING_DEFAULT;
    }

    private static ByteBuf createForwardingData(String hmacSecret, String address,
                                                BotConnection player, int requestedVersion) {
        var forwarded = Unpooled.buffer(2048);
        try {
            var actualVersion = findForwardingVersion(requestedVersion, player);

            var codecHelper = player.session().getCodecHelper();
            codecHelper.writeVarInt(forwarded, actualVersion);
            codecHelper.writeString(forwarded, address);
            codecHelper.writeUUID(forwarded, player.meta().getMinecraftAccount().getUUID());
            codecHelper.writeString(forwarded, player.meta().getMinecraftAccount().username());

            // TODO: Fix this
            /*
            // This serves as additional redundancy. The key normally is stored in the
            // login start to the server, but some setups require this.
            if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY
                    && actualVersion < VelocityConstants.MODERN_LAZY_SESSION) {
                IdentifiedKey key = player.getIdentifiedKey();
                assert key != null;
                codecHelper.writePlayerKey(forwarded, key);

                // Provide the signer UUID since the UUID may differ from the
                // assigned UUID. Doing that breaks the signatures anyway but the server
                // should be able to verify the key independently.
                if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2) {
                    if (key.getSignatureHolder() != null) {
                        forwarded.writeBoolean(true);
                        ProtocolUtils.writeUuid(forwarded, key.getSignatureHolder());
                    } else {
                        // Should only not be provided if the player was connected
                        // as offline-mode and the signer UUID was not backfilled
                        forwarded.writeBoolean(false);
                    }
                }
            }
             */

            var key = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.update(forwarded.array(), forwarded.arrayOffset(), forwarded.readableBytes());
            var sig = mac.doFinal();

            return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded);
        } catch (InvalidKeyException e) {
            forwarded.release();
            throw new RuntimeException("Unable to authenticate data", e);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            forwarded.release();
            throw new AssertionError(e);
        }
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
        AddonHelper.registerBotEventConsumer(SWPacketSendingEvent.class, this::onPacket);
        AddonHelper.registerBotEventConsumer(SWPacketReceiveEvent.class, this::onPacketReceive);
    }

    public void onPacket(SWPacketSendingEvent event) {
        if (!(event.getPacket() instanceof ClientIntentionPacket handshake)) {
            return;
        }

        if (!event.connection().settingsHolder().has(ForwardingBypassSettings.class)) {
            return;
        }

        var settings = event.connection().settingsHolder().get(ForwardingBypassSettings.class);
        var hostname = handshake.getHostname();
        var uuid = event.connection().meta().getMinecraftAccount().getUUID();

        switch (settings.forwardingMode()) {
            case LEGACY -> event.setPacket(handshake
                    .withHostname(createLegacyForwardingAddress(uuid, getForwardedIp(), hostname)));
            case BUNGEE_GUARD -> event.setPacket(handshake
                    .withHostname(createBungeeGuardForwardingAddress(uuid, getForwardedIp(), hostname, settings.secret())));
        }
    }

    public void onPacketReceive(SWPacketReceiveEvent event) {
        if (!(event.getPacket() instanceof ClientboundCustomQueryPacket loginPluginMessage)) {
            return;
        }

        if (!loginPluginMessage.getChannel().equals("velocity:player_info")) {
            return;
        }

        if (!event.connection().settingsHolder().has(ForwardingBypassSettings.class)) {
            return;
        }

        var settings = event.connection().settingsHolder().get(ForwardingBypassSettings.class);
        if (settings.forwardingMode() != ForwardingBypassSettings.ForwardingMode.MODERN) {
            log.warn("Received modern forwarding request packet, but forwarding mode is not modern!");
            return;
        }

        var requestedForwardingVersion = VelocityConstants.MODERN_FORWARDING_DEFAULT;
        {
            var buf = Unpooled.wrappedBuffer(loginPluginMessage.getData());
            if (buf.readableBytes() == 1) {
                requestedForwardingVersion = buf.readByte();
            }
        }

        var forwardingData = createForwardingData(settings.secret(),
                getForwardedIp(), event.connection(),
                requestedForwardingVersion);

        var bytes = new byte[forwardingData.readableBytes()];
        forwardingData.readBytes(bytes);
        var response = new ServerboundCustomQueryAnswerPacket(loginPluginMessage.getMessageId(), bytes);
        event.connection().session().send(response);
    }

    private String getForwardedIp() {
        return "127.0.0.1";
    }

    @GlobalEventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new ForwardingBypassPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @GlobalEventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), ForwardingBypassSettings.class, new ForwardingBypassCommand());
    }

    /*
     * This is a modified version of the code from <a href="https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/VelocityServerConnection.java#L171">Velocity</a>.
     */
    private String createLegacyForwardingAddress(UUID botUniqueId, String selfIp, String initialHostname, UnaryOperator<List<GameProfile.Property>> propertiesTransform) {
        // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
        // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
        // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
        var data = new StringBuilder().append(initialHostname)
                .append('\0')
                .append(selfIp)
                .append('\0')
                .append(UUIDHelper.convertToNoDashes(botUniqueId))
                .append('\0');
        ServerWreckerServer.GENERAL_GSON
                .toJson(propertiesTransform.apply(List.of()), data);
        return data.toString();
    }

    private String createLegacyForwardingAddress(UUID botUniqueId, String selfIp, String initialHostname) {
        return createLegacyForwardingAddress(botUniqueId, selfIp, initialHostname, UnaryOperator.identity());
    }

    private String createBungeeGuardForwardingAddress(UUID botUniqueId, String selfIp, String initialHostname, String forwardingSecret) {
        // Append forwarding secret as a BungeeGuard token.
        var property = new GameProfile.Property("bungeeguard-token", forwardingSecret, "");
        return createLegacyForwardingAddress(
                botUniqueId,
                selfIp,
                initialHostname,
                properties -> ImmutableList.<GameProfile.Property>builder().addAll(properties).add(property).build());
    }

    private static class ForwardingBypassPanel extends NavigationItem implements SettingsDuplex<ForwardingBypassSettings> {
        private final JComboBox<ForwardingBypassSettings.ForwardingMode> forwardingMode;
        private final JTextField secret;

        ForwardingBypassPanel(ServerWreckerServer serverWreckerServer) {
            super();
            serverWreckerServer.getSettingsManager().registerDuplex(ForwardingBypassSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Send Client Settings: "));
            forwardingMode = new JComboBox<>();
            for (var mode : ForwardingBypassSettings.ForwardingMode.values()) {
                forwardingMode.addItem(mode);
            }
            forwardingMode.setSelectedItem(ForwardingBypassSettings.DEFAULT_FORWARDING_MODE);
            add(forwardingMode);

            add(new JLabel("Forwarding secret: "));
            secret = new JTextField(ForwardingBypassSettings.DEFAULT_SECRET);
            add(secret);
        }

        @Override
        public String getNavigationName() {
            return "Forwarding Bypass";
        }

        @Override
        public String getNavigationId() {
            return "forwarding-bypass";
        }

        @Override
        public void onSettingsChange(ForwardingBypassSettings settings) {
            forwardingMode.setSelectedItem(settings.forwardingMode());
            secret.setText(settings.secret());
        }

        @Override
        public ForwardingBypassSettings collectSettings() {
            return new ForwardingBypassSettings(
                    (ForwardingBypassSettings.ForwardingMode) forwardingMode.getSelectedItem(),
                    secret.getText()
            );
        }
    }

    private static class ForwardingBypassCommand implements SettingsProvider<ForwardingBypassSettings> {
        @CommandLine.Option(names = {"--forwarding-bypass-mode"}, description = "Bypass backend proxy protocols such as of BungeeCord, BungeeGuard and Velocity")
        private ForwardingBypassSettings.ForwardingMode forwardingMode = ForwardingBypassSettings.DEFAULT_FORWARDING_MODE;
        @CommandLine.Option(names = {"--forwarding-bypass-secret"}, description = "Secret of the forwarding, used in BungeeGuard and Modern mode")
        private String secret = ForwardingBypassSettings.DEFAULT_SECRET;

        @Override
        public ForwardingBypassSettings collectSettings() {
            return new ForwardingBypassSettings(
                    forwardingMode,
                    secret
            );
        }
    }

    private record ForwardingBypassSettings(
            ForwardingMode forwardingMode,
            String secret
    ) implements SettingsObject {
        public static final ForwardingMode DEFAULT_FORWARDING_MODE = ForwardingMode.NONE;
        public static final String DEFAULT_SECRET = "forwarding secret";

        enum ForwardingMode {
            NONE,
            LEGACY,
            BUNGEE_GUARD,
            MODERN
        }
    }
}
