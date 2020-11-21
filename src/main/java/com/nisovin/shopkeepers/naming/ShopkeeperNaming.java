package com.nisovin.shopkeepers.naming;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.TextUtils;

public class ShopkeeperNaming {

	private final SKShopkeepersPlugin plugin;
	private final Map<String, Shopkeeper> naming = Collections.synchronizedMap(new HashMap<>());

	public ShopkeeperNaming(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(new ShopNamingListener(plugin, this), plugin);
	}

	public void onDisable() {
		naming.clear();
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		this.endNaming(player);
	}

	public void startNaming(Player player, Shopkeeper shopkeeper) {
		assert player != null && shopkeeper != null;
		naming.put(player.getName(), shopkeeper);
	}

	public Shopkeeper getCurrentlyNamedShopkeeper(Player player) {
		assert player != null;
		return naming.get(player.getName());
	}

	public boolean isNaming(Player player) {
		assert player != null;
		return this.getCurrentlyNamedShopkeeper(player) != null;
	}

	public Shopkeeper endNaming(Player player) {
		assert player != null;
		return naming.remove(player.getName());
	}

	public boolean requestNameChange(Player player, Shopkeeper shopkeeper, String newName) {
		if (player == null) return false;
		if (!shopkeeper.isValid()) return false;

		// Update name:
		if (newName.isEmpty() || newName.equals("-")) {
			// Remove name:
			newName = "";
		} else {
			// Validate name:
			if (!((AbstractShopkeeper) shopkeeper).isValidName(newName)) {
				TextUtils.sendMessage(player, Messages.nameInvalid);
				return false;
			}
		}

		// Apply new name:
		String oldName = shopkeeper.getName();
		shopkeeper.setName(newName);

		// Compare to previous name:
		if (oldName.equals(shopkeeper.getName())) {
			TextUtils.sendMessage(player, Messages.nameHasNotChanged);
			return false;
		}

		// Inform player:
		TextUtils.sendMessage(player, Messages.nameSet);

		// Close all open windows:
		shopkeeper.abortUISessionsDelayed(); // TODO Really needed?

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();

		return true;
	}
}
