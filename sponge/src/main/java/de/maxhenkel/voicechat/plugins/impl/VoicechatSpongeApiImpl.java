package de.maxhenkel.voicechat.plugins.impl;

import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;

public class VoicechatSpongeApiImpl extends VoicechatServerApiImpl {

    @Deprecated
    public static final VoicechatSpongeApiImpl SPONGE_INSTANCE = new VoicechatSpongeApiImpl();

    @Override
    public Entity fromEntity(Object entity) {
        return new EntityImpl((net.minecraft.world.entity.Entity) entity);
    }

    @Override
    public ServerLevel fromServerLevel(Object serverLevel) {
        return new ServerLevelImpl((net.minecraft.server.level.ServerLevel) serverLevel);
    }

    @Override
    public ServerPlayer fromServerPlayer(Object serverPlayer) {
        return new ServerPlayerImpl((net.minecraft.server.level.ServerPlayer) serverPlayer);
    }
}
