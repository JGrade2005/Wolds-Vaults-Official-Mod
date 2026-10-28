package xyz.iwolfking.woldsvaults.blocks.containers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.iwolfking.woldsvaults.blocks.tiles.VaultFabricatorTileEntity;
import xyz.iwolfking.woldsvaults.client.screens.VanillaContainerStyle;
import xyz.iwolfking.woldsvaults.config.ExtractorCostsConfig;
import xyz.iwolfking.woldsvaults.extraction.costs.ExtractorCosts;
import xyz.iwolfking.woldsvaults.items.FabricationPatternItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Placeholder Vault Fabricator screen: pattern slot with previous/next buttons that imprint the pattern, the 5×5
 * grid showing each cell's required item as a ghost, the output slot, and energy, fluid and progress readouts.
 */
public class VaultFabricatorScreen extends AbstractContainerScreen<VaultFabricatorContainer> {
    private static final int GHOST_WASH = 0x998B8B8B;
    private static final int BAR = 0xFF4FE3C4;

    private static final int LEFT_EDGE = 41;
    private static final int RIGHT_EDGE = 172;

    public VaultFabricatorScreen(VaultFabricatorContainer container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = VaultFabricatorContainer.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new Button(this.leftPos + 6, this.topPos + 38, 14, 14, new TextComponent("<"),
                button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VaultFabricatorContainer.BUTTON_PREVIOUS)));
        this.addRenderableWidget(new Button(this.leftPos + 22, this.topPos + 38, 14, 14, new TextComponent(">"),
                button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, VaultFabricatorContainer.BUTTON_NEXT)));
    }

    @Nullable
    private ExtractorCostsConfig.ComponentEntry recipe() {
        return FabricationPatternItem.recipe(this.menu.getSlot(VaultFabricatorTileEntity.PATTERN_SLOT).getItem());
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTicks);
        this.renderGhosts(pose);
        this.renderTooltip(pose, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        VanillaContainerStyle.panel(pose, x, y, this.imageWidth, this.imageHeight);
        VanillaContainerStyle.slots(pose, this.menu, x, y);
        int ticks = this.menu.data(VaultFabricatorTileEntity.DATA_TICKS);
        int progress = this.menu.data(VaultFabricatorTileEntity.DATA_PROGRESS);
        int barX = x + 136;
        int barY = y + 76;
        VanillaContainerStyle.well(pose, barX - 1, barY - 1, barX + 35, barY + 5);
        if (ticks > 0) {
            fill(pose, barX, barY, barX + Math.round(34.0F * Math.min(1.0F, (float) progress / ticks)), barY + 4, BAR);
        }
    }

    private void renderGhosts(PoseStack pose) {
        ExtractorCostsConfig.ComponentEntry recipe = this.recipe();
        if (recipe == null) {
            return;
        }
        for (int cell = 0; cell < VaultFabricatorTileEntity.GRID_CELLS; cell++) {
            Slot slot = this.menu.getSlot(VaultFabricatorTileEntity.GRID_START + cell);
            Item item = VaultFabricatorTileEntity.cellItem(recipe, cell);
            if (item == null || slot.hasItem()) {
                continue;
            }
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            this.itemRenderer.renderGuiItem(new ItemStack(item), sx, sy);
            RenderSystem.disableDepthTest();
            fill(pose, sx, sy, sx + 16, sy + 16, GHOST_WASH);
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        this.font.draw(pose, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        this.font.draw(pose, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
        ExtractorCostsConfig.ComponentEntry recipe = this.recipe();
        int energy = this.menu.data(VaultFabricatorTileEntity.DATA_ENERGY);
        int fluid = this.menu.data(VaultFabricatorTileEntity.DATA_FLUID);
        String fluidLine;
        if (recipe == null || recipe.fluid.isEmpty() || recipe.fluidAmount <= 0) {
            fluidLine = fluid > 0 ? fluid + " mB" : "no fluid";
        } else {
            fluidLine = String.format("%,d/%,d mB", fluid, recipe.fluidAmount);
        }
        this.drawSmall(pose, String.format("%,d FE", energy), 136, 84, RIGHT_EDGE);
        this.drawSmall(pose, fluidLine, 136, 92, RIGHT_EDGE);
        if (recipe == null) {
            this.drawSmall(pose, "Insert a", 6, 58, LEFT_EDGE);
            this.drawSmall(pose, "pattern,", 6, 65, LEFT_EDGE);
            this.drawSmall(pose, "then pick", 6, 72, LEFT_EDGE);
            this.drawSmall(pose, "a part", 6, 79, LEFT_EDGE);
        } else {
            this.drawSmall(pose, String.format("%,d FE", recipe.fe), 6, 58, LEFT_EDGE);
            this.drawSmall(pose, String.format("%.1f s", recipe.ticks / 20.0F), 6, 65, LEFT_EDGE);
        }
    }

    private void drawSmall(PoseStack pose, String text, int x, int y, int maxX) {
        float scale = Math.min(0.6F, (maxX - x) / (float) Math.max(1, this.font.width(text)));
        pose.pushPose();
        pose.translate(x, y, 0.0D);
        pose.scale(scale, scale, 1.0F);
        this.font.draw(pose, text, 0.0F, 0.0F, 0x404040);
        pose.popPose();
    }

    @Override
    protected void renderTooltip(PoseStack pose, int mouseX, int mouseY) {
        super.renderTooltip(pose, mouseX, mouseY);
        if (this.hoveredSlot == null || this.hoveredSlot.hasItem()) {
            return;
        }
        int index = this.hoveredSlot.index;
        if (index == VaultFabricatorTileEntity.PATTERN_SLOT) {
            this.renderComponentTooltip(pose, List.of(new TextComponent("Fabrication Pattern"),
                    new TextComponent("Insert a pattern, then use < > to pick the component")), mouseX, mouseY);
            return;
        }
        ExtractorCostsConfig.ComponentEntry recipe = this.recipe();
        if (recipe != null && index >= VaultFabricatorTileEntity.GRID_START && index < VaultFabricatorTileEntity.SLOT_COUNT) {
            String id = recipe.cell(index - VaultFabricatorTileEntity.GRID_START);
            if (id != null) {
                this.renderTooltip(pose, new ExtractorCosts.CostLine(id, false, 1).displayName(), mouseX, mouseY);
            }
        }
    }
}
