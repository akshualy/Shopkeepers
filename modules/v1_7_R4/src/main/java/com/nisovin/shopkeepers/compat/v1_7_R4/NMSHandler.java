package com.nisovin.shopkeepers.compat.v1_7_R4;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftInventoryMerchant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_7_R4.*;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_7_R4";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String name, List<org.bukkit.inventory.ItemStack[]> recipes, Player player) {
		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer) player).getHandle().world, 0);
			if (name != null && !name.isEmpty()) {
				villager.setCustomName(name);
			}

			Field recipeListField = EntityVillager.class.getDeclaredField("bu");
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList) recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (org.bukkit.inventory.ItemStack[] recipe : recipes) {
				recipeList.add(createMerchantRecipe(recipe[0], recipe[1], recipe[2]));
			}

			// set trading player:
			villager.a_(((CraftPlayer) player).getHandle());
			// open trade window:
			((CraftPlayer) player).getHandle().openTrade(villager, villager.getCustomName());

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {
		return openTradeWindow(shopkeeper.getName(), shopkeeper.getRecipes(), player);
	}

	@Override
	public ItemStack[] getUsedTradingRecipe(Inventory merchantInventory) {
		try {
			InventoryMerchant handle = (InventoryMerchant) ((CraftInventoryMerchant) merchantInventory).getInventory();
			MerchantRecipe merchantRecipe = handle.getRecipe();
			ItemStack[] recipe = new ItemStack[3];
			recipe[0] = merchantRecipe.getBuyItem1() != null ? CraftItemStack.asBukkitCopy(merchantRecipe.getBuyItem1()) : null;
			recipe[1] = merchantRecipe.getBuyItem2() != null ? CraftItemStack.asBukkitCopy(merchantRecipe.getBuyItem2()) : null;
			recipe[2] = merchantRecipe.getBuyItem3() != null ? CraftItemStack.asBukkitCopy(merchantRecipe.getBuyItem3()) : null;
			return recipe;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity) entity).getHandle();
			if (!(ev instanceof EntityInsentient)) return;

			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			List<?> list = (List<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (List<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat((EntityInsentient) ev));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void overwriteVillagerAI(LivingEntity villager) {
		try {
			EntityVillager ev = ((CraftVillager) villager).getHandle();

			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			List<?> list = (List<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (List<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getMaxVillagerProfession() {
		return 5;
	}

	@Override
	public void setVillagerProfession(Villager villager, int profession) {
		((CraftVillager) villager).getHandle().setProfession(profession);
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
	}

	@Override
	public void setNoAI(LivingEntity bukkitEntity) {
	}

	private MerchantRecipe createMerchantRecipe(org.bukkit.inventory.ItemStack item1, org.bukkit.inventory.ItemStack item2, org.bukkit.inventory.ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
		try {
			Field maxUsesField = MerchantRecipe.class.getDeclaredField("maxUses");
			maxUsesField.setAccessible(true);
			maxUsesField.set(recipe, 10000);
		} catch (Exception e) {
		}
		return recipe;
	}

	private net.minecraft.server.v1_7_R4.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack.asNMSCopy(item);
	}

	@Override
	public org.bukkit.inventory.ItemStack loadItemAttributesFromString(org.bukkit.inventory.ItemStack item, String data) {
		NBTTagList list = new NBTTagList();
		String[] attrs = data.split(";");
		for (String s : attrs) {
			if (!s.isEmpty()) {
				String[] attrData = s.split(",");
				NBTTagCompound attr = new NBTTagCompound();
				attr.setString("Name", attrData[0]);
				attr.setString("AttributeName", attrData[1]);
				attr.setDouble("Amount", Double.parseDouble(attrData[2]));
				attr.setInt("Operation", Integer.parseInt(attrData[3]));
				attr.setLong("UUIDLeast", Long.parseLong(attrData[4]));
				attr.setLong("UUIDMost", Long.parseLong(attrData[5]));
				list.add(attr);
			}
		}
		net.minecraft.server.v1_7_R4.ItemStack i = CraftItemStack.asNMSCopy(item);
		if (i.tag == null) i.tag = new NBTTagCompound();
		i.tag.set("AttributeModifiers", list);
		return CraftItemStack.asBukkitCopy(i);
	}

	@Override
	public String saveItemAttributesToString(org.bukkit.inventory.ItemStack item) {
		net.minecraft.server.v1_7_R4.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		if (nmsItem == null) return null;
		if (nmsItem.tag == null || !nmsItem.tag.hasKey("AttributeModifiers")) {
			return null;
		}
		String data = "";
		NBTTagList list = nmsItem.tag.getList("AttributeModifiers", 10);
		for (int i = 0; i < list.size(); i++) {
			NBTTagCompound attr = list.get(i);
			data += attr.getString("Name") + ","
					+ attr.getString("AttributeName") + ","
					+ attr.getDouble("Amount") + ","
					+ attr.getInt("Operation") + ","
					+ attr.getLong("UUIDLeast") + ","
					+ attr.getLong("UUIDMost") + ";";
		}
		return data;
	}

	@Override
	public boolean supportsPlayerUUIDs() {
		return true;
	}

	@Override
	public UUID getUUID(OfflinePlayer player) {
		return player.getUniqueId();
	}

	@Override
	public OfflinePlayer getOfflinePlayer(UUID uuid) {
		return Bukkit.getOfflinePlayer(uuid);
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEvent event) {
		return true;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
		return true;
	}
}
