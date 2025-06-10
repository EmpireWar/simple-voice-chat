package de.maxhenkel.voicechat.net;

import de.maxhenkel.voicechat.Voicechat;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpongeNetManager extends NetManager {

    private final Map<ResourceLocation, RawPlayDataHandler<ServerConnectionState.Game>> outgoingChannels;
    private final List<ResourceLocation> incomingChannels;

    public SpongeNetManager() {
        this.outgoingChannels = new HashMap<>();
        this.incomingChannels = new ArrayList<>();
    }

    public Map<ResourceLocation, RawPlayDataHandler<ServerConnectionState.Game>> getOutgoingChannels() {
        return outgoingChannels;
    }

    public List<ResourceLocation> getIncomingChannels() {
        return incomingChannels;
    }

    @Override
    public <T extends Packet<T>> Channel<T> registerReceiver(Class<T> packetType, boolean toClient, boolean toServer) {
        Channel<T> channel = new Channel<>();
        try {
            T dummyPacket = packetType.getDeclaredConstructor().newInstance();
            ResourceLocation channelName = dummyPacket.type().id();
            if (toServer) {
                outgoingChannels.put(channelName, new PacketHandler<>(channel, packetType));
            } else {
                incomingChannels.add(channelName);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return channel;
    }

    @Override
    protected void sendToServerInternal(Packet<?> packet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendToClient(Packet<?> packet, ServerPlayer player) {
        final ResourceLocation id = packet.type().id();

        final RawDataChannel channel = Sponge.channelManager().ofType(ResourceKey.of(id.getNamespace(), id.getPath()), RawDataChannel.class);
        channel.play().sendTo((org.spongepowered.api.entity.living.player.server.ServerPlayer) player, out -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            out.writeBytes(bytes);
        });
    }

    public void close() {
        outgoingChannels.clear();
        incomingChannels.clear();
    }

    private record PacketHandler<T extends Packet<T>>(Channel<T> channel,
                                                      Class<T> packetType) implements RawPlayDataHandler<ServerConnectionState.Game> {

        @Override
        public void handlePayload(ChannelBuf data, ServerConnectionState.Game state) {
            ServerPlayer player = (ServerPlayer) state.player();

            try {
                T packet = packetType.getDeclaredConstructor().newInstance();
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data.readBytes(data.available())));
                packet.fromBytes(buffer);

                if (!Voicechat.SERVER.isCompatible(player) && !packetType.equals(RequestSecretPacket.class)) {
                    return;
                }

                channel.onServerPacket(player, packet);
            } catch (Exception e) {
                Voicechat.LOGGER.error("Failed to read packet", e);
            }
        }
    }
}
