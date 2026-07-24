package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Backwards-compatibility shim for the pre-relocation Slimefun package.
 * <p>
 * This fork relocated Slimefun's API from {@code io.github.thebusybiscuit.slimefun4.*} to
 * {@code io.github.thebusybiscuit.slimefun5.*}. Third-party plugins compiled against the old package
 * register Bukkit listeners for {@code io.github.thebusybiscuit.slimefun4.api.events.CoolerFeedPlayerEvent}; without this
 * class their listener registration fails with a {@link ClassNotFoundException}.
 * <p>
 * The real {@code io.github.thebusybiscuit.slimefun5.api.events.CoolerFeedPlayerEvent} extends this class and reuses this
 * single {@link HandlerList}, so a listener registered against this old type still receives the fired
 * event (it is an {@code instanceof} this type).
 */
public class CoolerFeedPlayerEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    protected CoolerFeedPlayerEvent(Player who) {
        super(who);
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
