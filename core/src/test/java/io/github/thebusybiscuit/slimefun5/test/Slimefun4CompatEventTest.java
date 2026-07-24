package io.github.thebusybiscuit.slimefun5.test;

import java.lang.reflect.Method;

import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the slimefun4 -> slimefun5 event compatibility layer without booting the server: every
 * relocated {@code slimefun5} event must (1) be a subclass of its {@code slimefun4} counterpart so a
 * plugin compiled against the old package (e.g. RedProtect) can register a listener for it, and (2) share
 * the SAME {@link HandlerList} instance so that old-package listener actually receives the fired event.
 */
class Slimefun4CompatEventTest {

    private static final String[] EVENTS = {
        "AncientAltarCraftEvent", "AndroidFarmEvent", "AndroidMineEvent", "AsyncAutoEnchanterProcessEvent",
        "AsyncMachineOperationFinishEvent", "AsyncProfileLoadEvent", "AutoDisenchantEvent", "AutoEnchantEvent",
        "BlockPlacerPlaceEvent", "ClimbingPickLaunchEvent", "CoolerFeedPlayerEvent", "ExplosiveToolBreakBlocksEvent",
        "GEOResourceGenerationEvent", "MultiBlockCraftEvent", "MultiBlockInteractEvent", "PlayerLanguageChangeEvent",
        "PlayerPreResearchEvent", "PlayerRightClickEvent", "RadiationDamageEvent", "ReactorExplodeEvent",
        "ResearchUnlockEvent", "SlimefunBlockBreakEvent", "SlimefunBlockPlaceEvent", "SlimefunGuideOpenEvent",
        "SlimefunItemRegistryFinalizedEvent", "SlimefunItemSpawnEvent", "TalismanActivateEvent", "WaypointCreateEvent"
    };

    @Test
    @DisplayName("Every slimefun5 event subclasses its slimefun4 shim and shares its HandlerList")
    void everyEventHasWorkingShim() throws Exception {
        for (String name : EVENTS) {
            Class<?> sf4 = Class.forName("io.github.thebusybiscuit.slimefun4.api.events." + name);
            Class<?> sf5 = Class.forName("io.github.thebusybiscuit.slimefun5.api.events." + name);

            Assertions.assertTrue(sf4.isAssignableFrom(sf5),
                sf5.getName() + " must extend the slimefun4 shim " + sf4.getName());

            Method sf4Handlers = sf4.getMethod("getHandlerList");
            Method sf5Handlers = sf5.getMethod("getHandlerList");
            HandlerList fromSf4 = (HandlerList) sf4Handlers.invoke(null);
            HandlerList fromSf5 = (HandlerList) sf5Handlers.invoke(null);

            Assertions.assertNotNull(fromSf4, name + ": slimefun4 getHandlerList() returned null");
            Assertions.assertSame(fromSf4, fromSf5,
                name + ": slimefun5 event must reuse the slimefun4 shim's HandlerList so old listeners fire");
        }
    }
}
