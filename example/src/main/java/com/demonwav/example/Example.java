package com.demonwav.example;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Example extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
