package com.cyprias.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/*
Classes copied from Acrobot's Breeze code. 
https://github.com/Acrobot/ChestShop-3/blob/master/com/Acrobot/Breeze/
*/

public class InventoryUtil {
    /**
* Returns the amount of the item inside the inventory
*
* @param item Item to check
* @param inventory inventory
* @return amount of the item
*/
    public static int getAmount(ItemStack item, Inventory inventory) {
        if (!inventory.contains(item.getType())) {
            return 0;
        }

        if (inventory.getType() == null) {
            return Integer.MAX_VALUE;
        }

        HashMap<Integer, ? extends ItemStack> items = inventory.all(item.getType());
        int itemAmount = 0;

        for (ItemStack iStack : items.values()) {
            if (!MaterialUtil.equals(iStack, item)) {
                continue;
            }

            itemAmount += iStack.getAmount();
        }

        return itemAmount;
    }

    /**
* Tells if the inventory is empty
*
* @param inventory inventory
* @return Is the inventory empty?
*/
    public static boolean isEmpty(Inventory inventory) {
        for (ItemStack stack : inventory.getContents()) {
            if (!MaterialUtil.isEmpty(stack)) {
                return false;
            }
        }

        return true;
    }

    /**
* Checks if the inventory has stock of this type
*
* @param items items
* @param inventory inventory
* @return Does the inventory contain stock of this type?
*/
    public static boolean hasItems(ItemStack[] items, Inventory inventory) {
        for (ItemStack item : items) {
            if (!inventory.containsAtLeast(item, item.getAmount())) {
                return false;
            }
        }

        return true;
    }

    /**
* Checks if the item fits the inventory
*
* @param item Item to check
* @param inventory inventory
* @return Does item fit inside inventory?
*/
    public static boolean fits(ItemStack item, Inventory inventory) {
        int left = item.getAmount();

        if (inventory.getMaxStackSize() == Integer.MAX_VALUE) {
            return true;
        }

        for (ItemStack iStack : inventory.getContents()) {
            if (left <= 0) {
                return true;
            }

            if (MaterialUtil.isEmpty(iStack)) {
                left -= item.getMaxStackSize();
                continue;
            }

            if (!MaterialUtil.equals(iStack, item)) {
                continue;
            }

            left -= (iStack.getMaxStackSize() - iStack.getAmount());
        }

        return left <= 0;
    }

    /**
* Adds an item to the inventory
* (it currently uses a custom method of adding items, because Bukkit hasn't fixed it for 6 months now)
*
* @param item Item to add
* @param inventory Inventory
* @return Number of leftover items
*/
    public static int add(ItemStack item, Inventory inventory) {
        if (item.getAmount() < 1) {
            return 0;
        }

        int amountLeft = item.getAmount();
        int maxStackSize = item.getMaxStackSize();

        for (int currentSlot = 0; currentSlot < inventory.getSize() && amountLeft > 0; currentSlot++) {
            ItemStack currentItem = inventory.getItem(currentSlot);
            ItemStack duplicate = item.clone();

            if (MaterialUtil.isEmpty(currentItem)) {
                duplicate.setAmount(Math.min(amountLeft, maxStackSize));
                duplicate.addEnchantments(item.getEnchantments());

                amountLeft -= duplicate.getAmount();

                inventory.setItem(currentSlot, duplicate);
            } else if (currentItem.getAmount() < maxStackSize && MaterialUtil.equals(currentItem, item)) {
                int currentAmount = currentItem.getAmount();
                int neededToAdd = Math.min(maxStackSize - currentAmount, amountLeft);

                duplicate.setAmount(currentAmount + neededToAdd);
                duplicate.addEnchantments(item.getEnchantments());

                amountLeft -= neededToAdd;

                inventory.setItem(currentSlot, duplicate);
            }
        }

        return amountLeft;
    }

    /**
* Removes an item from the inventory
*
* @param item Item to remove
* @param inventory Inventory
* @return Number of items that couldn't be removed
*/
    public static int remove(ItemStack item, Inventory inventory) {
        Map<Integer, ItemStack> leftovers = inventory.removeItem(item);
        return countItems(leftovers);
    }

    /**
* If items in arguments are similar, this function merges them into stacks of the same type
*
* @param items Items to merge
* @return Merged stack array
*/
    public static ItemStack[] mergeSimilarStacks(ItemStack... items) {
        List<ItemStack> itemList = new LinkedList<ItemStack>();

        Iterating:
        for (ItemStack item : items) {
            for (ItemStack iStack : itemList) {
                if (MaterialUtil.equals(item, iStack)) {
                    iStack.setAmount(iStack.getAmount() + item.getAmount());
                    continue Iterating;
                }
            }

            itemList.add(item);
        }

        return itemList.toArray(new ItemStack[itemList.size()]);
    }

    /**
* Counts the amount of items in ItemStacks
*
* @param items ItemStacks of items to count
* @return How many items are there?
*/
    public static int countItems(ItemStack... items) {
        int count = 0;

        for (ItemStack item : items) {
            count += item.getAmount();
        }

        return count;
    }

    /**
* Counts leftovers from a map
*
* @param items Leftovers
* @return Number of leftovers
*/
    public static int countItems(Map<Integer, ?> items) {
        int totalLeft = 0;

        for (int left : items.keySet()) {
            totalLeft += left;
        }

        return totalLeft;
    }
}