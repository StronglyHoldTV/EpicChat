package sk.adonikeoffice.epicchat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.Remain;
import sk.adonikeoffice.epicchat.settings.Settings;
import sk.adonikeoffice.epicchat.util.Chat;

import java.util.List;

public class ChatListener implements Listener {

	@EventHandler
	public void onChat(final AsyncPlayerChatEvent event) {
		event.setCancelled(true);

		final Player player = event.getPlayer();
		String message = event.getMessage();

		timeCheck:
		// FULLY FUNCTIONAL
		{
			if (Settings.Chat.Cooldown.ENABLED) {
				if (Chat.hasPermission(player, Settings.Chat.Cooldown.PERMISSION))
					break timeCheck;

				final long now = System.currentTimeMillis() / 1000;
				final int lastMessageTime = Chat.getInstance().getLastMessageTime();
				final int messageDelay = Settings.Chat.Cooldown.DELAY;

				if ((now - lastMessageTime) < messageDelay) {
					final long time = messageDelay - (now - lastMessageTime);

					final String replacedMessage = Replacer.replaceArray(
							Settings.Chat.Cooldown.MESSAGE,
							"time", time,
							"time_plural", Common.plural(time, "second")
					);

					Chat.sendType(player, replacedMessage);
					return;
				}

				Chat.getInstance().setLastMessageTime(Math.toIntExact(now));
			}
		}

		if (Settings.Chat.Mention.ENABLED) // FULLY FUNCTIONAL
			for (final Player target : Remain.getOnlinePlayers()) {
				final String targetName = target.getName();
				final int thisIndex = message.indexOf(targetName);

				if (thisIndex != -1) {
					final String firstPart = message.substring(0, thisIndex);
					final String lastColor = Common.lastColor(Settings.Chat.MESSAGE_COLOR + firstPart);

					message = message.replace(targetName, Settings.Chat.Mention.COLOR + "@" + targetName + (lastColor != null ? lastColor : Settings.Chat.MESSAGE_COLOR));

					Chat.sendType(player, Settings.PLUGIN_PREFIX + Settings.Chat.Mention.MESSAGE.replace("{0}", player.getName()));
					Settings.Chat.Mention.SOUND.play(target);
				}
			}

		// TODO - REWORK REGEX
		/*antiSwear:
		{
			if (Settings.Chat.AntiSwear.ENABLED) {
				if (Chat.hasPermission(player, Settings.Chat.AntiSwear.PERMISSION))
					break antiSwear;

				for (final String word : Settings.Chat.AntiSwear.WORDS) {
					final String replacedWord = ChatUtil.replaceDiacritic(word);

					if (replacedWord.contains(message)) {
						Messenger.success(player, Settings.Chat.AntiSwear.MESSAGE);

						return;
					}
				}
			}
		}*/

		if (Settings.Chat.PERMISSION_ENABLED) {
			final boolean hasAccess = Chat.hasPermission(player, Settings.Chat.PERMISSION);

			if (hasAccess)
				chat(player, message);
			else
				Messenger.success(player, Settings.Message.PERMISSION_MESSAGE);
		} else
			chat(player, message);
	}

	public void chat(final Player player, final String message) {
		final String format = Variables.replace(Settings.Chat.FORMAT, player);

		final boolean hasColorPermission = Chat.hasPermission(player, Settings.Chat.PERMISSION_COLOR);
		final String replacedFormat = Replacer.replaceArray(format, "message", hasColorPermission ? Settings.Chat.MESSAGE_COLOR + message : Settings.Chat.MESSAGE_COLOR + Common.stripColors(message));

		sendMessage(player, replacedFormat);
	}

	private void sendMessage(final Player player, final String message) {
		final SimpleComponent chat = SimpleComponent.of(message);

		final List<String> hoverMessages = Settings.Chat.HOVER;
		hoverMessages.replaceAll(string -> HookManager.replacePlaceholders(player, string));

		/*Variables.replace(hoverMessages,player);

		if (!hoverMessages.isEmpty())
			for (final String hoverMessage : hoverMessages)
				chat = SimpleComponent.of(message).onHover(HookManager.replacePlaceholders(player, hoverMessage));*/

		for (final Player online : Remain.getOnlinePlayers())
			chat.send(online);

		if (Settings.Chat.LOG_ENABLED)
			Common.log(message);
	}

}
