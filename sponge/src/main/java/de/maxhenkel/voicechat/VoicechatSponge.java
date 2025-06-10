package de.maxhenkel.voicechat;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.voicechat.command.VoicechatCommands;
import de.maxhenkel.voicechat.config.SpongeTranslations;
import de.maxhenkel.voicechat.config.Translations;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.intercompatibility.SpongeCommonCompatibilityManager;
import de.maxhenkel.voicechat.net.SpongeNetManager;
import de.maxhenkel.voicechat.plugins.PluginManager;
import de.maxhenkel.voicechat.plugins.impl.SpongeVoicechatServiceImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

@Plugin("voicechat")
public class VoicechatSponge extends Voicechat {

    public static VoicechatSponge INSTANCE;
    private final PluginContainer container;
    public final SpongeVoicechatServiceImpl apiService;
    private final Path dataFolder;
    private Voicechat voicechat;

    @Inject
    public VoicechatSponge(PluginContainer container, @ConfigDir(sharedRoot = false) Path dataFolder) {
        this.container = container;
        this.apiService = new SpongeVoicechatServiceImpl();
        this.dataFolder = dataFolder;

        INSTANCE = this;

        // We initialise very early so things are ready for the RegisterChannelEvent
        voicechat = new Voicechat() {
            @Override
            public Path getVoicechatConfigFolderInternal() {
                return dataFolder;
            }

            @Override
            protected void initPlugins() {
                //NOOP, this is initialized later so the apiService can gather all plugin registrations
            }

            @Override
            protected void registerCommands() {
                //NOOP, since commands need to get registered even earlier
            }

            @Override
            protected Translations createTranslations(ConfigBuilder builder) {
                return new SpongeTranslations(builder);
            }
        };
        voicechat.initialize();

        Sponge.eventManager().registerListeners(container, CommonCompatibilityManager.INSTANCE, MethodHandles.lookup());
    }

    @Listener
    public void onRegisterChannels(RegisterChannelEvent event) {
        final SpongeCommonCompatibilityManager instance = (SpongeCommonCompatibilityManager) CommonCompatibilityManager.INSTANCE;
        final SpongeNetManager netManager = (SpongeNetManager) instance.getNetManager();
        netManager.getOutgoingChannels().forEach((location, receiver) -> {
            final RawDataChannel channel = event.register(ResourceKey.of(location.getNamespace(), location.getPath()), RawDataChannel.class);
            channel.play().addHandler(ServerConnectionState.Game.class, receiver);
        });

        netManager.getIncomingChannels().forEach(location -> {
            event.register(ResourceKey.of(location.getNamespace(), location.getPath()), RawDataChannel.class);
        });
    }

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = ((MinecraftServer) Sponge.server()).getCommands().getDispatcher();
        SpongeCommonCompatibilityManager manager = (SpongeCommonCompatibilityManager) CommonCompatibilityManager.INSTANCE;
        manager.onRegisterCommands(dispatcher);
        VoicechatCommands.register(dispatcher);
        PluginManager.instance().init();
    }

    public PluginContainer getContainer() {
        return container;
    }
}
