package cc.ranmc.sign;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

public final class SignFactory {
    private final SignApi plugin;
    private final Map<Player, Menu> inputs = new HashMap<>();

    public SignFactory(SignApi plugin) {
        this.plugin = plugin;
        this.listen();
    }

    public Menu newMenu(List<String> text) {
        return new SignFactory.Menu(text);
    }


    private void listen() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();

                Menu menu = inputs.remove(player);

                if (menu == null) {
                    return;
                }
                event.setCancelled(true);

                boolean success = menu.response.test(player, event.getPacket().getStringArrays().read(0));

                if (!success) Bukkit.getScheduler().runTaskLater(plugin, () -> menu.open(player), 2L);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        Location location = menu.position.toLocation(player.getWorld());
                        player.sendBlockChange(location, location.getBlock().getBlockData());
                    }
                }, 2L);
            }
        });
    }

    public final class Menu {

        private final List<String> text;
        private BiPredicate<Player, String[]> response;
        private BlockPosition position;
        Menu(List<String> text) {
            this.text = text;
        }
        public Menu response(BiPredicate<Player, String[]> response) {
            this.response = response;
            return this;
        }
        public void open(Player player) {
            Objects.requireNonNull(player, "player");
            if (!player.isOnline()) return;
            Location location = player.getLocation().clone().add(0, -4, 0);
            player.sendBlockChange(location, Material.CHERRY_SIGN.createBlockData());
            player.sendSignChange(location, text.toArray(new String[3]));
            PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
            position = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            openSign.getBlockPositionModifier().write(0, position);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);
            inputs.put(player, this);
        }
    }
}