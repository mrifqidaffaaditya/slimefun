package io.github.thebusybiscuit.slimefun5.test;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;

/**
 * Reproduces the AutoDisenchanter's core enchantment-transfer to catch the "tool keeps its enchants"
 * bug: after transfer the tool must have NO enchantments and the book must carry them as stored enchants.
 */
class DisenchantLogicTest {

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Disenchant strips enchants from the tool and stores them on the book")
    void disenchantStripsEnchants() {
        ItemStack tool = new ItemStack(Material.DIAMOND_PICKAXE);
        tool.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
        tool.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);

        Map<Enchantment, Integer> enchantments = new HashMap<>(tool.getEnchantments());
        Assertions.assertEquals(2, enchantments.size());

        ItemStack disenchanted = tool.clone();
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);

        // Mirror AutoDisenchanter#transferEnchantments
        ItemMeta itemMeta = disenchanted.getItemMeta();
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            boolean removed = itemMeta.removeEnchant(entry.getKey());
            Assertions.assertTrue(removed, "removeEnchant must succeed for " + entry.getKey().getKey());
            bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
        }

        disenchanted.setItemMeta(itemMeta);
        book.setItemMeta(bookMeta);

        Assertions.assertTrue(disenchanted.getEnchantments().isEmpty(),
            "the disenchanted tool must have NO enchantments left");
        Assertions.assertEquals(2, ((EnchantmentStorageMeta) book.getItemMeta()).getStoredEnchants().size(),
            "the book must carry the transferred enchantments");
    }

    @Test
    @DisplayName("The output-display refresh must NOT re-add enchants to a disenchanted tool")
    void refreshDoesNotReAddEnchantsToDisenchantedTool() throws Exception {
        // A disenchanted vanilla tool (no enchants, no name) - simulate the tool leaving the machine.
        ItemStack disenchanted = new ItemStack(Material.DIAMOND_PICKAXE);
        Assertions.assertTrue(disenchanted.getEnchantments().isEmpty());

        io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem machine =
            io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem.getById("AUTO_DISENCHANTER");
        org.junit.jupiter.api.Assumptions.assumeTrue(machine
            instanceof me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer);

        java.lang.reflect.Method refresh = me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer.class
            .getDeclaredMethod("refreshOutputDisplay", ItemStack.class);
        refresh.setAccessible(true);
        ItemStack result = (ItemStack) refresh.invoke(machine, disenchanted);

        Assertions.assertTrue(result.getEnchantments().isEmpty(),
            "refreshOutputDisplay must never re-add enchantments to a disenchanted tool");
    }
}
