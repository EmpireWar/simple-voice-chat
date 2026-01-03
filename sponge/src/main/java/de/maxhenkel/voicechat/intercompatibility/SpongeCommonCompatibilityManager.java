package de.maxhenkel.voicechat.intercompatibility;

import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.voicechat.VoicechatSponge;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.net.SpongeNetManager;
import de.maxhenkel.voicechat.permission.SpongePermissionManager;
import de.maxhenkel.voicechat.plugins.impl.VoicechatSpongeApiImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SpongeCommonCompatibilityManager extends CommonCompatibilityManager {

    private final List<Consumer<MinecraftServer>> serverStartingEvents;
    private final List<Consumer<MinecraftServer>> serverStoppingEvents;
    private final List<Consumer<CommandDispatcher<CommandSourceStack>>> registerServerCommandsEvents;
    private final List<Consumer<ServerPlayer>> playerLoggedInEvents;
    private final List<Consumer<ServerPlayer>> playerLoggedOutEvents;
    private final List<BiConsumer<ServerPlayer, ServerPlayer>> playerHideEvents;
    private final List<BiConsumer<ServerPlayer, ServerPlayer>> playerShowEvents;
    private final List<Consumer<ServerPlayer>> voicechatConnectEvents;
    private final List<Consumer<ServerPlayer>> voicechatCompatibilityCheckSucceededEvents;
    private final List<Consumer<UUID>> voicechatDisconnectEvents;

    public SpongeCommonCompatibilityManager() {
        serverStartingEvents = new CopyOnWriteArrayList<>();
        serverStoppingEvents = new CopyOnWriteArrayList<>();
        registerServerCommandsEvents = new CopyOnWriteArrayList<>();
        playerLoggedInEvents = new CopyOnWriteArrayList<>();
        playerLoggedOutEvents = new CopyOnWriteArrayList<>();
        playerHideEvents = new CopyOnWriteArrayList<>();
        playerShowEvents = new CopyOnWriteArrayList<>();
        voicechatConnectEvents = new CopyOnWriteArrayList<>();
        voicechatCompatibilityCheckSucceededEvents = new CopyOnWriteArrayList<>();
        voicechatDisconnectEvents = new CopyOnWriteArrayList<>();
    }

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        DedicatedServer server = (DedicatedServer) event.engine();
        serverStartingEvents.forEach(consumer -> consumer.accept(server));
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event) {
        DedicatedServer server = (DedicatedServer) event.engine();
        serverStoppingEvents.forEach(consumer -> consumer.accept(server));
        if (netManager != null) {
            netManager.close();
            netManager = null;
        }
    }

    public void onRegisterCommands(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        registerServerCommandsEvents.forEach(consumer -> consumer.accept(commandDispatcher));
    }

    @Listener
    public void playerLoggedIn(ServerSideConnectionEvent.Join event) {
        ServerPlayer serverPlayer = ((ServerPlayer) event.player());
        playerLoggedInEvents.forEach(consumer -> consumer.accept(serverPlayer));
    }

    @Listener
    public void onPlayerQuit(ServerSideConnectionEvent.Leave event) {
        ServerPlayer serverPlayer = ((ServerPlayer) event.player());
        playerLoggedOutEvents.forEach(consumer -> consumer.accept(serverPlayer));
    }

    @Override
    public String getModVersion() {
        return VoicechatSponge.INSTANCE.getContainer().metadata().version().toString();
    }

    @Override
    public String getModName() {
        return "Simple Voice Chat";
    }

    @Override
    public Path getGameDirectory() {
        return Sponge.game().gameDirectory();
    }

    @Override
    public void emitServerVoiceChatConnectedEvent(ServerPlayer player) {
        voicechatConnectEvents.forEach(consumer -> consumer.accept(player));
    }

    @Override
    public void emitServerVoiceChatDisconnectedEvent(UUID clientID) {
        voicechatDisconnectEvents.forEach(consumer -> consumer.accept(clientID));
    }

    @Override
    public void emitPlayerCompatibilityCheckSucceeded(ServerPlayer player) {
        voicechatCompatibilityCheckSucceededEvents.forEach(consumer -> consumer.accept(player));
    }

    @Override
    public void onServerVoiceChatConnected(Consumer<ServerPlayer> onVoiceChatConnected) {
        voicechatConnectEvents.add(onVoiceChatConnected);
    }

    @Override
    public void onServerVoiceChatDisconnected(Consumer<UUID> onVoiceChatDisconnected) {
        voicechatDisconnectEvents.add(onVoiceChatDisconnected);
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> onServerStarting) {
        serverStartingEvents.add(onServerStarting);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> onServerStopping) {
        serverStoppingEvents.add(onServerStopping);
    }

    @Override
    public void onPlayerLoggedIn(Consumer<ServerPlayer> onPlayerLoggedIn) {
        playerLoggedInEvents.add(onPlayerLoggedIn);
    }

    @Override
    public void onPlayerLoggedOut(Consumer<ServerPlayer> onPlayerLoggedOut) {
        playerLoggedOutEvents.add(onPlayerLoggedOut);
    }

    @Override
    public void onPlayerHide(BiConsumer<ServerPlayer, ServerPlayer> onPlayerHide) {
        playerHideEvents.add(onPlayerHide);
    }

    @Override
    public void onPlayerShow(BiConsumer<ServerPlayer, ServerPlayer> onPlayerShow) {
        playerShowEvents.add(onPlayerShow);
    }

    @Override
    public void onPlayerCompatibilityCheckSucceeded(Consumer<ServerPlayer> onPlayerCompatibilityCheckSucceeded) {
        voicechatCompatibilityCheckSucceededEvents.add(onPlayerCompatibilityCheckSucceeded);
    }

    @Override
    public void onRegisterServerCommands(Consumer<CommandDispatcher<CommandSourceStack>> onRegisterServerCommands) {
        registerServerCommandsEvents.add(onRegisterServerCommands);
    }

    private SpongeNetManager netManager;

    @Override
    public NetManager getNetManager() {
        if (netManager == null) {
            netManager = new SpongeNetManager();
        }
        return netManager;
    }

    @Override
    public boolean isDevEnvironment() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Sponge.pluginManager().plugin(modId).isPresent();
    }

    @Override
    public List<VoicechatPlugin> loadPlugins() {
        return VoicechatSponge.INSTANCE.apiService.getPlugins();
    }

    @Override
    public SpongePermissionManager createPermissionManager() {
        return new SpongePermissionManager();
    }

    @Override
    public VoicechatServerApi getServerApi() {
        return VoicechatSpongeApiImpl.SPONGE_INSTANCE;
    }

    @Override
    public Object createRawApiEntity(Entity entity) {
        return entity;
    }

    @Override
    public Object createRawApiPlayer(Player player) {
        return player;
    }

    @Override
    public Object createRawApiLevel(ServerLevel level) {
        return level;
    }

    @Override
    public boolean canSee(ServerPlayer player, ServerPlayer other) {
        return ((org.spongepowered.api.entity.living.player.server.ServerPlayer) player).canSee((org.spongepowered.api.entity.living.player.server.ServerPlayer) other);
    }
}
