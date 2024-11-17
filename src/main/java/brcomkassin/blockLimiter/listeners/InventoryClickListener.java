package brcomkassin.blockLimiter.listeners;

import brcomkassin.blockLimiter.inventory.LimiterInventoryType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equalsIgnoreCase(LimiterInventoryType.NAME.getName())) return;
        event.setCancelled(true);
    }

}
