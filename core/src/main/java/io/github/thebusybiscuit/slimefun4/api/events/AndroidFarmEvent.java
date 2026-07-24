package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Backwards-compatibility shim for the pre-relocation Slimefun package.
 * <p>
 * This fork relocated Slimefun's API from {@code io.github.thebusybiscuit.slimefun4.*} to
 * {@code io.github.thebusybiscuit.slimefun5.*}. Third-party plugins compiled against the old package
 * register Bukkit listeners for {@code io.github.thebusybiscuit.slimefun4.api.events.AndroidFarmEvent}; without this
 * class their listener registration fails with a {@link ClassNotFoundException}.
 * <p>
 * The real {@code io.github.thebusybiscuit.slimefun5.api.events.AndroidFarmEvent} extends this class and reuses this
 * single {@link HandlerList}, so a listener registered against this old type still receives the fired
 * event (it is an {@code instanceof} this type).
 */
public class AndroidFarmEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    protected AndroidFarmEvent(boolean async) {
        super(async);
    }

    protected boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
