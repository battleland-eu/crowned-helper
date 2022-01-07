package xyz.rgnt.crownedhelper.natives.level;


import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class DummyEntityPlayer
        extends ServerPlayer {

    public DummyEntityPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile);
    }
}
