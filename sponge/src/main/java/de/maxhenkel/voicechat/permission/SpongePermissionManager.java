package de.maxhenkel.voicechat.permission;

import de.maxhenkel.voicechat.VoicechatSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class SpongePermissionManager extends PermissionManager {

    @Override
    public Permission createPermissionInternal(String modId, String node, PermissionType type) {
        return new SpongePermission(modId + "." + node, type);
    }
}
