package dev.meguru.event.impl.render;

import dev.meguru.event.Event;
import net.minecraft.tileentity.TileEntityChest;

public class RenderChestEvent extends Event {

    private final TileEntityChest entity;
    private final Runnable chestRenderer;

    public RenderChestEvent(TileEntityChest entity, Runnable chestRenderer) {
        this.entity = entity;
        this.chestRenderer = chestRenderer;
    }

    public TileEntityChest getEntity() {
        return entity;
    }

    public void drawChest() {
        this.chestRenderer.run();
    }

}
