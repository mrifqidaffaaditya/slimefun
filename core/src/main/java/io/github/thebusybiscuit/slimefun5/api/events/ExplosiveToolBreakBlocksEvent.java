package io.github.thebusybiscuit.slimefun5.api.events;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun5.implementation.items.tools.ExplosiveTool;

/**
 * This {@link Event} is called when an {@link ExplosiveTool} is used to break blocks.
 *
 * <p>
 * It extends the relocated {@link io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent}
 * shim and reuses its {@link HandlerList} so third-party plugins compiled against the old
 * {@code slimefun4} package (e.g. RedProtect) still receive this event.
 *
 * @author GallowsDove
 *
 * @see ExplosiveTool
 */
public class ExplosiveToolBreakBlocksEvent extends io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent {

    private final ExplosiveTool explosiveTool;

    @ParametersAreNonnullByDefault
    public ExplosiveToolBreakBlocksEvent(Player player, Block block, List<Block> blocks, ItemStack item, ExplosiveTool explosiveTool) {
        super(player, block, blocks, item);

        Validate.notNull(block, "The center block cannot be null!");
        Validate.notNull(blocks, "Blocks cannot be null");
        Validate.notNull(item, "Item cannot be null");
        Validate.notNull(explosiveTool, "ExplosiveTool cannot be null");

        this.explosiveTool = explosiveTool;
    }

    /**
     * Gets the {@link ExplosiveTool} which triggered this event.
     *
     * @return the {@link ExplosiveTool} that was involved
     */
    @Nonnull
    public ExplosiveTool getExplosiveTool() {
        return this.explosiveTool;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent.getHandlerList();
    }
}
