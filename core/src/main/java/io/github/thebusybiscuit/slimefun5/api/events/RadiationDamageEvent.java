package io.github.thebusybiscuit.slimefun5.api.events;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * The {@link RadiationDamageEvent} is called when a player takes radiation damage.
 * 
 * @author HoosierTransfer
 */
public class RadiationDamageEvent extends io.github.thebusybiscuit.slimefun4.api.events.RadiationDamageEvent {

    private final Player player;
    private final int exposure;

    /**
     * This constructs a new {@link RadiationDamageEvent}.
     * 
     * @param player The {@link Player} who took radiation damage
     * @param exposure The amount of radiation exposure
     */
    public RadiationDamageEvent(@Nonnull Player player, int exposure) {
        super(false);
        this.player = player;
        this.exposure = exposure;
    }

    /**
     * This returns the {@link Player} who took radiation damage.
     * 
     * @return The {@link Player} who took radiation damage
     */
    public @Nonnull Player getPlayer() {
        return player;
    }

    /**
     * This returns the amount of radiation exposure.
     * 
     * @return The amount of radiation exposure
     */
    public int getExposure() {
        return exposure;
    }

    public static @Nonnull HandlerList getHandlerList() {
        return io.github.thebusybiscuit.slimefun4.api.events.RadiationDamageEvent.getHandlerList();
    }

    @Override
    public @Nonnull HandlerList getHandlers() {
        return getHandlerList();
    }
}

