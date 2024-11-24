package brcomkassin.blockLimiter.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import net.kyori.adventure.text.Component;

import brcomkassin.blockLimiter.inventory.AnimatedInventory;
import brcomkassin.blockLimiter.inventory.InventoryType;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        if (event.getView().title().equals(Component.text(InventoryType.LIMITER.getName()))) {
            AnimatedInventory.stopAnimation(player);
        }
    }
}