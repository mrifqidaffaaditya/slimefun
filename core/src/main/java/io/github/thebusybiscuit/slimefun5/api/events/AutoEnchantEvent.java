package io.github.thebusybiscuit.slimefun5.api.events;

import javax.annotation.Nonnull;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun5.implementation.items.electric.machines.enchanting.AutoEnchanter;

/**
 * An {@link Event} that is called whenever an {@link AutoEnchanter} is trying to enchant
 * an {@link ItemStack}.
 * 
 * @author WalshyDev
 *
 * @see AutoDisenchantEvent
 */
public class AutoEnchantEvent extends io.github.thebusybiscuit.slimefun4.api.events.AutoEnchantEvent {

    private final ItemStack item;

    public AutoEnchantEvent(@Nonnull ItemStack item) {
        // Thread-adaptive: the machine ticker is async normally, but a VIEWED machine ticks on the main
        // thread (TickerTask routes isInventoryViewed blocks through runSync). Bukkit rejects an async-flagged
        // event fired sync (and vice-versa), so match the flag to the current thread.
        super(!org.bukkit.Bukkit.isPrimaryThread());

        this.item = item;
    }

    /**
     * This returns the {@link ItemStack} that is being enchanted.
     * 
     * @return The {@link ItemStack} that is being enchanted
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return io.github.thebusybiscuit.slimefun4.api.events.AutoEnchantEvent.getHandlerList();
    }

}

