package cc.ranmc.sign;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;

final class SignFactory {
    private final SignApi plugin;
    private final Map<Player, Menu> inputs = new HashMap<>();

    private static Constructor<PacketPlayOutTileEntityData> constructor;

    private final boolean folia = isFolia();

    static {
        try {
            constructor = PacketPlayOutTileEntityData.class.getDeclaredConstructor(BlockPosition.class, TileEntityTypes.class, NBTTagCompound.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是 Folia 端
     *
     * @return boolean
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

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
            public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                Player player = event.getPlayer();

                Menu menu = inputs.remove(player);

                if (menu == null) {
                    return;
                }
                event.setCancelled(true);

                boolean success = menu.response.test(player, event.getPacket().getStringArrays().read(0));
                if (folia) {
                    if (!success)  Bukkit.getServer().getGlobalRegionScheduler().runDelayed(plugin, bukkitTask -> menu.open(player), 2L);
                    Bukkit.getServer().getRegionScheduler().runDelayed(plugin, menu.location, bukkitTask -> {
                        if (player.isOnline()) {
                            player.sendBlockChange(menu.location, menu.location.getBlock().getBlockData());
                        }
                    }, 2L);
                } else {
                    if (!success)  Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> menu.open(player), 2L);
                    Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendBlockChange(menu.location, menu.location.getBlock().getBlockData());
                        }
                    }, 2L);
                }
            }
        });
    }

    public final class Menu {

        private final List<String> text;
        private BiPredicate<Player, String[]> response;
        private Location location;
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
            location = player.getLocation().clone().add(0, -4, 0);
            player.sendBlockChange(location, Material.CHERRY_WALL_SIGN.createBlockData());

            BlockPosition position = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            PacketPlayOutOpenSignEditor editorPacket = new PacketPlayOutOpenSignEditor(position, true);

            NBTTagCompound compound = new NBTTagCompound();

            NBTTagCompound frontText = new NBTTagCompound();
            NBTTagCompound backText = new NBTTagCompound();
            NBTTagList backMessages = new NBTTagList();
            NBTTagList frontMessages = new NBTTagList();


            for (String s : text) {
                NBTTagString nbtString = NBTTagString.a(String.format("{\"text\":\"%s\"}", s));
                backMessages.add(nbtString);
                frontMessages.add(nbtString);
            }

            backText.a("messages", backMessages);
            frontText.a("messages", frontMessages);
            compound.a("back_text", backText);
            compound.a("front_text", frontText);

            PacketPlayOutTileEntityData tileEntityDataPacket = null;
            try {
                tileEntityDataPacket = constructor.newInstance(position, TileEntityTypes.h, compound);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }

            PlayerConnection connection = ((CraftPlayer) player).getHandle().c;

            connection.a(tileEntityDataPacket);
            connection.a(editorPacket);
            inputs.put(player, this);
        }
    }
}