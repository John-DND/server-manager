package io.github.zap.servermanager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.UUID;

public final class ServerManager extends JavaPlugin implements PluginMessageListener {
    private static final String BUNGEE_CHANNEl = "BungeeCord";
    private static final String HUB_WORLD_NAME = "world";

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CHANNEl);
        getServer().getMessenger().registerIncomingPluginChannel(this, BUNGEE_CHANNEl, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        getLogger().info("Received PluginMessage.");

        if(channel.equals(BUNGEE_CHANNEl)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subchannel = input.readUTF();

            if(subchannel.equals("Forward")) {
                input.readUTF(); //disregard target server
                String packetName = input.readUTF();

                if(packetName.equals("JOIN")) {
                    String serverName = input.readUTF();
                    String playerString = input.readUTF();

                    if(Arrays.stream(playerString.split(",")).map((name) -> getServer().
                            getPlayer(UUID.fromString(name))).allMatch((player1 ->
                            player1 != null && player1.isOnline()))) {

                        String managerName = input.readUTF();
                        String mapName = input.readUTF();
                        boolean isSpectator = input.readBoolean();
                        String targetArena = input.readUTF();

                        ByteArrayDataOutput output = ByteStreams.newDataOutput();

                        //bungeecord Forward protocol
                        output.writeUTF("Forward");
                        output.writeUTF("ALL");

                        //custom protocol
                        output.writeUTF("JOIN");

                        output.writeUTF(serverName);
                        output.writeUTF(playerString);
                        output.writeUTF(managerName);
                        output.writeUTF(mapName);
                        output.writeBoolean(isSpectator);
                        output.writeUTF(targetArena);

                        getServer().sendPluginMessage(this, BUNGEE_CHANNEl, output.toByteArray());
                    }
                    else {
                        //some players are offline; deny teleport
                        ByteArrayDataOutput output = ByteStreams.newDataOutput();

                        //bungeecord Forward protocol
                        output.writeUTF("Forward");
                        output.writeUTF(serverName); //only send to the server that tried to join

                        //custom protocol
                        output.writeUTF("DENY");

                        output.writeUTF(playerString);
                        getServer().sendPluginMessage(this, BUNGEE_CHANNEl, output.toByteArray());
                    }
                }
            }
        }
    }
}