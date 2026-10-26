package xyz.iwolfking.woldsvaults.blocks.containers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.iwolfking.woldsvaults.blocks.tiles.ExtractorForgeTileEntity;
import xyz.iwolfking.woldsvaults.client.screens.VanillaContainerStyle;
import xyz.iwolfking.woldsvaults.extraction.costs.CostLedger;
import xyz.iwolfking.woldsvaults.extraction.costs.ExtractorCosts;
import xyz.iwolfking.woldsvaults.items.ExtractorBlueprintItem;

import java.util.List;

/**
 * Placeholder screen until the Forge UI is designed: the blueprint and output slots, the blueprint's cost ledger
 * as a list of icon / paid / owed rows (scroll with the mouse wheel), seven deposit slots and the Fabricate button.
 */
public class ExtractorForgeScreen extends AbstractContainerScreen<ExtractorForgeContainer> {
    private static final int LIST_TOP = 40;
    private static final int ROW_HEIGHT = 12;
    private static final int VISIBLE_ROWS = 6;
    private static final float DEPOSIT_LABEL_Y = LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT + 3;

    private int scroll;

    public ExtractorForgeScreen(ExtractorForgeContainer container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = ExtractorForgeContainer.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new Button(this.leftPos + 70, this.topPos + 16, 60, 20, new TextComponent("Fabricate"),
                button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ExtractorForgeContainer.BUTTON_FABRICATE)));
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
        VanillaContainerStyle.panel(pose, x, y, this.imageWidth, this.imageHeight);
        VanillaContainerStyle.well(pose, x + 7, y + LIST_TOP - 1, x + this.imageWidth - 7, y + LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT + 1);
        VanillaContainerStyle.slots(pose, this.menu, x, y);
    }

    private CompoundTag ledger() {
        ItemStack blueprint = this.menu.getSlot(ExtractorForgeTileEntity.BLUEPRINT_SLOT).getItem();
        if (!(blueprint.getItem() instanceof ExtractorBlueprintItem) || !ExtractorBlueprintItem.isIdentified(blueprint)) {
            return null;
        }
        return blueprint.getOrCreateTag().getCompound(ExtractorBlueprintItem.TAG);
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        this.font.draw(pose, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        this.font.draw(pose, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
        this.font.draw(pose, "Deposit", 8.0F, DEPOSIT_LABEL_Y, 0x404040);
        CompoundTag ledger = this.ledger();
        if (ledger == null || !CostLedger.hasCost(ledger)) {
            this.font.drawShadow(pose, this.font.plainSubstrByWidth("Insert an identified blueprint.", this.imageWidth - 20), 10.0F, LIST_TOP + 2.0F, 0xFFE0E0E0);
            return;
        }
        List<ExtractorCosts.CostLine> lines = CostLedger.lines(ledger);
        this.scroll = Math.max(0, Math.min(this.scroll, lines.size() - VISIBLE_ROWS));
        for (int i = 0; i < VISIBLE_ROWS && i + this.scroll < lines.size(); i++) {
            ExtractorCosts.CostLine line = lines.get(i + this.scroll);
            int rowY = LIST_TOP + i * ROW_HEIGHT;
            Item item = line.fluid() ? null : line.item();
            if (item != null) {
                PoseStack modelView = RenderSystem.getModelViewStack();
                modelView.pushPose();
                modelView.translate(9.0D, rowY + 1.0D, 0.0D);
                modelView.scale(0.625F, 0.625F, 1.0F);
                RenderSystem.applyModelViewMatrix();
                this.itemRenderer.renderGuiItem(new ItemStack(item), 0, 0);
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
            }
            long paid = CostLedger.paid(ledger, line);
            boolean done = paid >= line.amount();
            String name = line.displayName().getString();
            String amounts = String.format("%,d/%,d%s", Math.min(paid, line.amount()), line.amount(), line.fluid() ? " mB" : "");
            int amountsWidth = this.font.width(amounts);
            int nameRoom = this.imageWidth - 14 - 22 - amountsWidth - 4;
            this.font.drawShadow(pose, this.font.plainSubstrByWidth(name, nameRoom), 22.0F, rowY + 2.0F, 0xFFFFFFFF);
            this.font.drawShadow(pose, amounts, this.imageWidth - 9 - amountsWidth, rowY + 2.0F, done ? 0xFF6EF08C : 0xFFFFD27A);
        }
        if (lines.size() > VISIBLE_ROWS) {
            String more = (this.scroll + 1) + "-" + Math.min(lines.size(), this.scroll + VISIBLE_ROWS) + " of " + lines.size();
            this.font.draw(pose, more, this.imageWidth - 9 - this.font.width(more), DEPOSIT_LABEL_Y, 0x404040);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scroll -= (int) Math.signum(delta);
        return true;
    }

    @Override
    protected void renderTooltip(PoseStack pose, int mouseX, int mouseY) {
        super.renderTooltip(pose, mouseX, mouseY);
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            int index = this.hoveredSlot.index;
            String label = index == ExtractorForgeTileEntity.BLUEPRINT_SLOT ? "Identified blueprint"
                    : index == ExtractorForgeTileEntity.OUTPUT_SLOT ? "Fabricated part"
                    : index < ExtractorForgeTileEntity.SLOT_COUNT ? "Deposit: items and filled buckets pay the cost" : null;
            if (label != null) {
                this.renderTooltip(pose, new TextComponent(label), mouseX, mouseY);
            }
        }
    }
}
