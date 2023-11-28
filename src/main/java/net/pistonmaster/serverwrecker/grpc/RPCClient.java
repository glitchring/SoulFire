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
package net.pistonmaster.serverwrecker.grpc;

import io.grpc.CallCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.Getter;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.grpc.generated.AttackServiceGrpc;
import net.pistonmaster.serverwrecker.grpc.generated.CommandServiceGrpc;
import net.pistonmaster.serverwrecker.grpc.generated.ConfigServiceGrpc;
import net.pistonmaster.serverwrecker.grpc.generated.LogsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Getter
public class RPCClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RPCClient.class);

    private final ManagedChannel channel;
    private final LogsServiceGrpc.LogsServiceStub logStub;
    private final CommandServiceGrpc.CommandServiceStub commandStub;
    private final CommandServiceGrpc.CommandServiceBlockingStub commandStubBlocking;
    private final AttackServiceGrpc.AttackServiceStub attackStub;
    private final ConfigServiceGrpc.ConfigServiceBlockingStub configStubBlocking;

    public RPCClient(String host, int port, String jwt) {
        this(new JwtCredential(jwt), Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create())
                .userAgent("ServerWreckerJavaClient/" + BuildData.VERSION).build());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("*** shutting down gRPC client since JVM is shutting down");
            try {
                shutdown();
            } catch (Throwable e) {
                LOGGER.error("Interrupted while shutting down gRPC client", e);
                return;
            }
            LOGGER.info("*** client shut down");
        }));
    }

    public RPCClient(CallCredentials callCredentials, ManagedChannel managedChannel) {
        channel = managedChannel;
        logStub = LogsServiceGrpc.newStub(channel).withCallCredentials(callCredentials).withCompression("gzip");
        commandStub = CommandServiceGrpc.newStub(channel).withCallCredentials(callCredentials).withCompression("gzip");
        commandStubBlocking = CommandServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials).withCompression("gzip");
        attackStub = AttackServiceGrpc.newStub(channel).withCallCredentials(callCredentials).withCompression("gzip");
        configStubBlocking = ConfigServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials).withCompression("gzip");
    }

    public void shutdown() throws InterruptedException {
        if (!channel.shutdown().awaitTermination(3, TimeUnit.SECONDS)
                && !channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Unable to shutdown gRPC client");
        }
    }
}
