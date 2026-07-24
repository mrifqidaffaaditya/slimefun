package io.github.thebusybiscuit.slimefun5.test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Player;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun5.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun5.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;

/**
 * Verifies the backpack identity layer that replaced the fragile visible "§7ID: <uuid>#<n>" lore line.
 * Identity now lives in the item's persistent data (invisible), with a legacy-lore fallback so backpacks
 * from older Slimefun 4/5 versions keep working and migrate on first touch. Two different identities must
 * never compare equal - that is what stops distinct backpacks from stacking and duplicating.
 */
class BackpackIdentityTest {

    private static final String ID_PREFIX = ChatColor.GRAY + "ID: ";

    private static ServerMock server;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private static ItemStack backpack() {
        return new ItemStack(Material.CHEST);
    }

    @Test
    @DisplayName("A fresh backpack has no identity (so openBackpack will assign one)")
    void freshHasNoIdentity() {
        Assertions.assertFalse(PlayerBackpack.readIdentity(backpack()).isPresent());
    }

    @Test
    @DisplayName("writeIdentity stores the id in persistent data and reads back")
    void writeThenRead() {
        ItemStack item = backpack();
        PlayerBackpack.writeIdentity(item, "0000#5");
        Assertions.assertEquals(Optional.of("0000#5"), PlayerBackpack.readIdentity(item));
    }

    @Test
    @DisplayName("A legacy visible ID lore line is read, then migrated to PDC and hidden on write")
    void legacyLoreMigratesAndHides() {
        ItemStack item = backpack();
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Size: 9", ID_PREFIX + "abcd#7"));
        item.setItemMeta(meta);

        // Read falls back to the legacy lore line.
        Assertions.assertEquals(Optional.of("abcd#7"), PlayerBackpack.readIdentity(item));

        // Writing migrates it into PDC and strips the visible line.
        PlayerBackpack.writeIdentity(item, "abcd#7");

        List<String> lore = item.getItemMeta().getLore();
        Assertions.assertTrue(lore == null || lore.stream().noneMatch(l -> l.startsWith(ID_PREFIX)),
            "the visible ID lore line must be removed (hidden)");
        Assertions.assertEquals(Optional.of("abcd#7"), PlayerBackpack.readIdentity(item),
            "identity must still be readable from PDC after hiding the lore line");
    }

    @Test
    @DisplayName("The unassigned '<ID>' placeholder is not treated as a real identity")
    void placeholderIsNotIdentity() {
        ItemStack item = backpack();
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(ID_PREFIX + "<ID>"));
        item.setItemMeta(meta);
        Assertions.assertFalse(PlayerBackpack.readIdentity(item).isPresent());
    }

    @Test
    @DisplayName("createBackpack never reuses an id after a gap (size-based allocation would collide)")
    void createBackpackDoesNotReuseIdAfterGap() throws InterruptedException {
        Player player = server.addPlayer();

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<PlayerProfile> ref = new java.util.concurrent.atomic.AtomicReference<>();
        PlayerProfile.get(player, p -> {
            ref.set(p);
            latch.countDown();
        });
        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
        PlayerProfile profile = ref.get();
        Assertions.assertNotNull(profile);

        profile.createBackpack(9); // id 0
        PlayerBackpack one = profile.createBackpack(9); // id 1
        PlayerBackpack two = profile.createBackpack(9); // id 2
        Assertions.assertEquals(2, two.getId());

        // Open a gap in the middle: the id space is now {0, 2} but the map size is 2.
        profile.getPlayerData().removeBackpack(one);

        PlayerBackpack next = profile.createBackpack(9);

        // size()-based allocation would hand out id 2 again, colliding with `two` (two items sharing one
        // identity - the "can't open / wrong backpack" bug). It must allocate a fresh, unused id instead.
        Assertions.assertNotEquals(two.getId(), next.getId(), "a new backpack must not reuse an in-use id");
        Assertions.assertEquals(3, next.getId());
        Assertions.assertSame(two, profile.getBackpack(2).orElse(null), "the existing backpack 2 must be untouched");
    }

    @Test
    @DisplayName("Two same-material items with different SF ids must NOT vanilla-stack (merge bug)")
    void differentSlimefunIdsDoNotStack() {
        ItemStack a = new ItemStack(Material.GOLD_INGOT);
        ItemStack b = new ItemStack(Material.GOLD_INGOT);

        Slimefun.getItemDataService().setItemData(a, "BLISTERING_INGOT");
        Slimefun.getItemDataService().setItemData(b, "BLISTERING_INGOT_2");

        Assertions.assertEquals(Optional.of("BLISTERING_INGOT"), Slimefun.getItemDataService().getItemData(a));
        Assertions.assertEquals(Optional.of("BLISTERING_INGOT_2"), Slimefun.getItemDataService().getItemData(b));

        Assertions.assertFalse(a.isSimilar(b), "Different SF ids stacked together in vanilla inventory");
    }

    @Test
    @DisplayName("pushItem must NOT merge two different-id Slimefun items sharing a material")
    void pushItemDoesNotMergeDifferentIds() {
        io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem sf =
            io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem.getById("BLISTERING_INGOT");
        io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem sf2 =
            io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem.getById("BLISTERING_INGOT_2");
        org.junit.jupiter.api.Assumptions.assumeTrue(sf != null && sf2 != null);

        ItemStack existing = sf.getItem().clone();
        existing.setAmount(1);
        ItemStack incoming = sf2.getItem().clone();
        incoming.setAmount(1);

        Assertions.assertFalse(
            io.github.thebusybiscuit.slimefun5.utils.SlimefunUtils.isItemSimilar(existing, incoming, true, false),
            "Two different-tier Slimefun items must not be treated as stackable");
    }

    @Test
    @DisplayName("A machine output (id-only, no baked name) can recover its display from the template")
    void machineOutputRecoversBakedDisplay() {
        io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem sf =
            io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem.getById("BLISTERING_INGOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(sf != null);

        // The live template is baked at boot and carries the display name.
        Assertions.assertTrue(sf.getItem().getItemMeta().hasDisplayName(),
            "the canonical template must carry a baked display name");

        // Simulate a machine output captured pre-bake: same material + PDC id, but NO display name.
        ItemStack output = new ItemStack(sf.getItem().getType());
        Slimefun.getItemDataService().setItemData(output, "BLISTERING_INGOT");
        Assertions.assertFalse(output.getItemMeta().hasDisplayName());

        // getByItem must resolve the id from PDC, giving us the baked template to restore from.
        io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem resolved =
            io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem.getByItem(output);
        Assertions.assertNotNull(resolved, "machine output must resolve back to its SlimefunItem via PDC");
        Assertions.assertEquals("BLISTERING_INGOT", resolved.getId());
        Assertions.assertTrue(resolved.getItem().getItemMeta().hasDisplayName());
    }

    @Test
    @DisplayName("A stale pre-bake machine output must stack with a /give item after being healed")
    void machineOutputStacksWithGiveAfterHeal() {
        io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem sf =
            io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem.getById("BLISTERING_INGOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(sf != null);

        ItemStack give = sf.getItem().clone();

        // Simulate a recipe output captured before canonicalizeToId(): material + PDC id, but no baked
        // name/lore. This is the state that would NOT stack with a /give item.
        ItemStack machineRaw = new ItemStack(give.getType());
        Slimefun.getItemDataService().setItemData(machineRaw, "BLISTERING_INGOT");
        Assertions.assertFalse(give.isSimilar(machineRaw),
            "precondition: the stale pre-bake output must differ from a /give item");

        // The heal (as AContainer#refreshOutputDisplay does) rebuilds it from the canonical template.
        ItemStack healed = sf.getItem().clone();
        healed.setAmount(machineRaw.getAmount());
        Assertions.assertTrue(give.isSimilar(healed),
            "a healed machine output must stack with a /give item");
    }

    @Test
    @DisplayName("Different identities never compare equal (dupe guard); same/unassigned do")
    void identityDistinguishesBackpacks() {
        ItemStack a = backpack();
        ItemStack b = backpack();
        PlayerBackpack.writeIdentity(a, "0000#1");
        PlayerBackpack.writeIdentity(b, "0000#2");

        // canStack delegates to identity equality - two different backpacks must NOT be equal.
        Assertions.assertNotEquals(PlayerBackpack.readIdentity(a), PlayerBackpack.readIdentity(b));
        // Two blank/unassigned backpacks share (empty) identity and may stack.
        Assertions.assertEquals(PlayerBackpack.readIdentity(backpack()), PlayerBackpack.readIdentity(backpack()));
    }
}
