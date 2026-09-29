package xyz.iwolfking.woldsvaults.blocks.containers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import xyz.iwolfking.woldsvaults.blocks.tiles.ExtractorForgeTileEntity;

/** Placeholder screen: flat panels, slot outlines, Fabricate and Repair buttons, a tank readout. */
public class ExtractorForgeScreen extends AbstractContainerScreen<ExtractorForgeContainer> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;

    public ExtractorForgeScreen(ExtractorForgeContainer container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new Button(this.leftPos + 46, this.topPos + 22, 32, 20, new TextComponent("Forge"),
                button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ExtractorForgeContainer.BUTTON_FABRICATE)));
        this.addRenderableWidget(new Button(this.leftPos + 100, this.topPos + 22, 32, 20, new TextComponent("Fix"),
                button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ExtractorForgeContainer.BUTTON_REPAIR)));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTicks);
        this.renderTooltip(pose, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        fill(pose, x, y, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
        fill(pose, x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, PANEL);
        fill(pose, x + 1, y + 1, x + this.imageWidth - 2, y + 2, PANEL_LIGHT);
        for (var slot : this.menu.slots) {
            fill(pose, x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, PANEL_DARK);
            fill(pose, x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT);
        }
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        super.renderLabels(pose, mouseX, mouseY);
        if (this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.getPos()) instanceof ExtractorForgeTileEntity tile) {
            FluidStack fluid = tile.getFluid();
            String tank = fluid.isEmpty() ? "tank empty" : fluid.getAmount() + " mB " + fluid.getDisplayName().getString();
            this.font.draw(pose, tank, 118.0F, 60.0F, 0x404040);
        }
        this.font.draw(pose, "materials (costs TBD)", 8.0F, 44.0F, 0x404040);
    }

    @Override
    protected void renderTooltip(PoseStack pose, int mouseX, int mouseY) {
        super.renderTooltip(pose, mouseX, mouseY);
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            String label = switch (this.hoveredSlot.index) {
                case ExtractorForgeTileEntity.BLUEPRINT_SLOT -> "Identified blueprint";
                case ExtractorForgeTileEntity.OUTPUT_SLOT -> "Fabricated part";
                case ExtractorForgeTileEntity.REPAIR_SLOT -> "Extractor to repair";
                default -> null;
            };
            if (label != null) {
                this.renderTooltip(pose, new TextComponent(label), mouseX, mouseY);
            }
        }
    }
}
