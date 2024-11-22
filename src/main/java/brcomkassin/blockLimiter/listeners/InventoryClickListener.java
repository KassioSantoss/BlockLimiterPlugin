package brcomkassin.blockLimiter.listeners;

import brcomkassin.blockLimiter.inventory.InventoryType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Player player = (Player) event.getWhoClicked();
        if (!view.getTitle().equalsIgnoreCase(InventoryType.LIMITER.getName())) return;
        event.setCancelled(true);
    }

}
