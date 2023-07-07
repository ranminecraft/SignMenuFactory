package cc.ranmc.sign;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class SignApi extends JavaPlugin implements Listener {
    @Getter
    private static SignApi instance;
    @Getter
    private static SignFactory factory;
    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("§e-----------------------");
        Bukkit.getConsoleSender().sendMessage("§bSignMenuFactory §dBy Ranica");
        Bukkit.getConsoleSender().sendMessage("§bVersion: "+getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage("§bBukkit: "+getServer().getVersion());
        Bukkit.getConsoleSender().sendMessage("§chttps://www.ranmc.cc/");
        Bukkit.getConsoleSender().sendMessage("§e-----------------------");
        instance = this;
        factory = new SignFactory(this);
        super.onEnable();
    }

    public static SignFactory.Menu newMenu(String text) {
        return SignApi.getFactory().newMenu(Arrays.asList(text, "", "", ""));
    }

    public SignFactory.Menu newMenu(List<String> text) {
        return SignApi.getFactory().newMenu(text);
    }


}
