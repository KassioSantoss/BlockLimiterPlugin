package brcomkassin.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;

public interface Message {
    class Chat {
        public static void send(Player player, String... message) {
            Arrays.stream(message)
                    .map(string -> ChatColor.translateAlternateColorCodes('&', string))
                    .forEach(player::sendMessage);
        }
    }

    class ActionBar {
        public static void send(Player player, String message) {
            BaseComponent baseComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', message));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, baseComponent);
        }
    }
}

