package de.maxhenkel.voicechat.permission;

import de.maxhenkel.voicechat.VoicechatSponge;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;

public class SpongePermission implements Permission {

    private final String node;
    private final PermissionType type;

    public SpongePermission(String permissionNode, PermissionType type) {
        this.node = permissionNode;
        this.type = type;

        // Register the permission with the permission service
        Sponge.server().serviceProvider().permissionService()
                .newDescriptionBuilder(VoicechatSponge.INSTANCE.getContainer())
                .id(permissionNode)
                .description(Component.text("Allows " + permissionNode))
                .assign(type == PermissionType.EVERYONE ? PermissionDescription.ROLE_USER :
                       type == PermissionType.NOONE ? PermissionDescription.ROLE_USER :
                       PermissionDescription.ROLE_ADMIN, true)
                .register();
    }

    @Override
    public boolean hasPermission(ServerPlayer player) {
        return ((org.spongepowered.api.entity.living.player.server.ServerPlayer) player).hasPermission(node);
    }

    @Override
    public PermissionType getPermissionType() {
        return type;
    }

}
