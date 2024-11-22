package brcomkassin.blockLimiter.commands;

import brcomkassin.blockLimiter.inventory.LimiterInventory;
import brcomkassin.blockLimiter.limiter.BlockLimiter;
import brcomkassin.utils.Message;
import lombok.SneakyThrows;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockLimiterCommand implements CommandExecutor, TabExecutor {

    @SneakyThrows
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (args.length == 0) {
            LimiterInventory.openInventory(player);
            return true;
        }

        if (!player.hasPermission("limiter.perm")) {
            Message.Chat.send(player, "&4Você não tem permissão para usar esse comando.");
            return true;
        }

        switch (args[0]) {
            case "add":
                if (args.length < 2) {
                    Message.Chat.send(player, "&4Você deve passar um valor como parâmetro!");
                    Message.Chat.send(player, "&4Uso correto: /limites add <valor>");
                    return true;
                }
                try {
                    int limit = Integer.parseInt(args[1]);
                    BlockLimiter.addLimitedBlock(player, itemInHand, limit);
                    if (args.length >= 3) {
                        BlockLimiter.CONFIGURATOR_BLOCKS.add(itemInHand.getType().name());
                        Message.Chat.send(player, "&aO bloco foi adicionado ao CONFIGURATOR com o limite definido!");
                    }
                } catch (NumberFormatException e) {
                    Message.Chat.send(player, "&4Você deve passar um valor como parâmetro!");
                    Message.Chat.send(player, "&4Uso correto: /limites add <valor>");
                }
                break;
            case "remover":
                BlockLimiter.removeBlockLimit(player);
                break;
            default:
                Message.Chat.send(player, "&4Uso correto: /limites <add>, <remove>");
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String arg, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            return null;
        }

        if (!player.hasPermission("limiter.perm")) {
            return List.of();
        }

        List<String> strings = new ArrayList<>();

        if (args.length == 1) {
            strings.add("add");
            strings.add("remover");
            return strings;
        }
        return List.of();
    }
}
