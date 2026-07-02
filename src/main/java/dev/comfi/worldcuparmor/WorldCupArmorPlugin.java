package dev.comfi.worldcuparmor;

import com.comphenix.protocol.ProtocolLibrary;
import dev.comfi.worldcuparmor.gui.HexInputListener;
import dev.comfi.worldcuparmor.gui.MainMenu;
import dev.comfi.worldcuparmor.gui.MenuListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class WorldCupArmorPlugin extends JavaPlugin {

    private TeamColorManager colorManager;
    private DisguiseService disguiseService;
    private HexInputListener hexInput;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        colorManager = new TeamColorManager(this);
        colorManager.load();
        disguiseService = new DisguiseService(this, colorManager);
        disguiseService.start();
        hexInput = new HexInputListener(this);
        ProtocolLibrary.getProtocolManager().addPacketListener(new EquipmentListener(this));
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(disguiseService, this);
        getServer().getPluginManager().registerEvents(hexInput, this);
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        if (disguiseService != null) {
            disguiseService.stop();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (sender instanceof Player player) {
            new MainMenu(this).open(player);
        } else {
            sender.sendMessage("This command opens a GUI and can only be used in game.");
        }
        return true;
    }

    public TeamColorManager colors() {
        return colorManager;
    }

    public DisguiseService disguises() {
        return disguiseService;
    }

    public HexInputListener hexInput() {
        return hexInput;
    }
}
