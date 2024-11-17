package brcomkassin.blockLimiter.commands;

import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.utils.Message;
import lombok.SneakyThrows;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BlockLimiterCommand implements CommandExecutor {
    @SneakyThrows
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (args.length < 1) {
            Message.Chat.send(player, "&4Uso correto: /limiter <value>");
            return false;
        }

        switch (args[0]) {
            case "open":
                LimiterInventory.openInventory(player);
                break;
            case "remove":
                BlockLimiter.removeBlockLimit(player);
                break;
            case "open1":
                LimiterInventory.open(player);
                break;
            default:
                try {
                    int limit = Integer.parseInt(args[0]);
                    BlockLimiter.addLimitedBlock(player, itemInHand, limit);
                } catch (IllegalArgumentException e) {
                    Message.Chat.send(player, "&4Você deve passar um valor como parâmetro!");
                    Message.Chat.send(player, "&4Uso correto: /limitar <valor>");
                }
        }
        return false;
    }
}
