package de.maxhenkel.voicechat.permission;

public class SpongePermissionManager extends PermissionManager {

    @Override
    public Permission createPermissionInternal(String modId, String node, PermissionType type) {
        return new SpongePermission(modId + "." + node, type);
    }
}
