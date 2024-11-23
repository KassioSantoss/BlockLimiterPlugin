package brcomkassin.blockLimiter.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import brcomkassin.blockLimiter.inventory.InventoryType;
import brcomkassin.blockLimiter.inventory.LimiterInventory;
import net.kyori.adventure.text.Component;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text(InventoryType.LIMITER.getName()))) return;
        
        event.setCancelled(true);
        LimiterInventory.handleInventoryClick(event);
    }
}
