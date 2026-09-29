package xyz.iwolfking.woldsvaults.blocks.containers;

import com.blakebr0.cucumber.inventory.BaseItemStackHandler;
import com.blakebr0.cucumber.inventory.slot.OutputSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import xyz.iwolfking.woldsvaults.blocks.tiles.ExtractorForgeTileEntity;
import xyz.iwolfking.woldsvaults.init.ModContainers;

import java.util.function.Function;

public class ExtractorForgeContainer extends AbstractContainerMenu {
    public static final int BUTTON_FABRICATE = 0;
    public static final int BUTTON_REPAIR = 1;
    private static final int PLAYER_SLOTS_START = ExtractorForgeTileEntity.SLOT_COUNT;

    private final Function<Player, Boolean> isUsableByPlayer;
    private final BlockPos pos;

    private ExtractorForgeContainer(MenuType<?> type, int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(type, id, playerInventory, p -> true, clientInventory(playerInventory, buffer.readBlockPos()), null);
    }

    private ExtractorForgeContainer(MenuType<?> type, int id, Inventory playerInventory, Function<Player, Boolean> isUsableByPlayer,
                                    BaseItemStackHandler inventory, BlockPos pos) {
        super(type, id);
        this.isUsableByPlayer = isUsableByPlayer;
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.addSlot(new ValidatedSlot(inventory, ExtractorForgeTileEntity.BLUEPRINT_SLOT, 26, 24));
        this.addSlot(new OutputSlot(inventory, ExtractorForgeTileEntity.OUTPUT_SLOT, 80, 24));
        this.addSlot(new ValidatedSlot(inventory, ExtractorForgeTileEntity.REPAIR_SLOT, 134, 24));
        for (int i = 0; i < ExtractorForgeTileEntity.MATERIAL_SLOTS; i++) {
            this.addSlot(new ValidatedSlot(inventory, ExtractorForgeTileEntity.MATERIAL_SLOT_START + i, 8 + i * 18, 54));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static BaseItemStackHandler clientInventory(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level.getBlockEntity(pos) instanceof ExtractorForgeTileEntity tile) {
            return tile.getInventory();
        }
        return ExtractorForgeTileEntity.createInventoryHandler(null);
    }

    public static ExtractorForgeContainer create(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new ExtractorForgeContainer(ModContainers.EXTRACTOR_FORGE_CONTAINER, windowId, playerInventory, buffer);
    }

    public static ExtractorForgeContainer create(int windowId, Inventory playerInventory, Function<Player, Boolean> isUsableByPlayer,
                                                 BaseItemStackHandler inventory, BlockPos pos) {
        return new ExtractorForgeContainer(ModContainers.EXTRACTOR_FORGE_CONTAINER, windowId, playerInventory, isUsableByPlayer, inventory, pos);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level.isClientSide() || !(player.level.getBlockEntity(this.pos) instanceof ExtractorForgeTileEntity tile)) {
            return false;
        }
        if (id == BUTTON_FABRICATE) {
            tile.fabricate(player);
            return true;
        }
        if (id == BUTTON_REPAIR) {
            tile.repair(player);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_SLOTS_START) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOTS_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (ExtractorForgeTileEntity.slotAccepts(ExtractorForgeTileEntity.BLUEPRINT_SLOT, stack)) {
                moved = this.moveItemStackTo(stack, ExtractorForgeTileEntity.BLUEPRINT_SLOT, ExtractorForgeTileEntity.BLUEPRINT_SLOT + 1, false);
            } else if (ExtractorForgeTileEntity.slotAccepts(ExtractorForgeTileEntity.REPAIR_SLOT, stack)) {
                moved = this.moveItemStackTo(stack, ExtractorForgeTileEntity.REPAIR_SLOT, ExtractorForgeTileEntity.REPAIR_SLOT + 1, false);
            } else {
                moved = this.moveItemStackTo(stack, ExtractorForgeTileEntity.MATERIAL_SLOT_START, PLAYER_SLOTS_START, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isUsableByPlayer.apply(player);
    }

    static class ValidatedSlot extends SlotItemHandler {
        ValidatedSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ExtractorForgeTileEntity.slotAccepts(this.getSlotIndex(), stack);
        }
    }
}
