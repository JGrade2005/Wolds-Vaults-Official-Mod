package xyz.iwolfking.woldsvaults.blocks.tiles;

import com.blakebr0.cucumber.inventory.BaseItemStackHandler;
import com.blakebr0.cucumber.tileentity.BaseInventoryTileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.blocks.containers.ExtractorForgeContainer;
import xyz.iwolfking.woldsvaults.extraction.costs.CostLedger;
import xyz.iwolfking.woldsvaults.extraction.parts.PartData;
import xyz.iwolfking.woldsvaults.init.ModBlocks;
import xyz.iwolfking.woldsvaults.init.ModItems;
import xyz.iwolfking.woldsvaults.items.ExtractorBlueprintItem;
import xyz.iwolfking.woldsvaults.items.ExtractorPartItem;

import javax.annotation.Nullable;

/**
 * Slot 0 blueprint, 1 fabricated part, 2–8 deposit slots. The blueprint carries its own cost ledger: deposits,
 * pipes and fluid pipes pay into it (nothing is refunded and progress stays on the blueprint), and Fabricate
 * turns the blueprint into its part once every line is paid. Repair moved to the Extractor Assembler.
 */
public class ExtractorForgeTileEntity extends BaseInventoryTileEntity implements MenuProvider, ExtractorStationTile {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int DEPOSIT_SLOT_START = 2;
    public static final int DEPOSIT_SLOTS = 7;
    public static final int SLOT_COUNT = DEPOSIT_SLOT_START + DEPOSIT_SLOTS;
    public static final int WORK_CYCLE_TICKS = 80;
    public static final int PAY_INTERVAL_TICKS = 5;

    private final BaseItemStackHandler inventory;
    private int workingTicks;
    private final LazyOptional<IItemHandler> itemCapability;
    private final LazyOptional<IFluidHandler> fluidCapability;

    public ExtractorForgeTileEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EXTRACTOR_FORGE_TILE_ENTITY_TYPE, pos, state);
        this.inventory = createInventoryHandler(this::markDirtyAndDispatch);
        IItemHandler ledgerItems = CostLedger.itemHandler(this::ledger, this::markDirtyAndDispatch);
        this.itemCapability = LazyOptional.of(() -> new AutomationHandler(ledgerItems));
        this.fluidCapability = LazyOptional.of(() -> CostLedger.fluidHandler(this::ledger, this::markDirtyAndDispatch));
    }

    public static BaseItemStackHandler createInventoryHandler(@Nullable Runnable onContentsChanged) {
        BaseItemStackHandler inventory = new BaseItemStackHandler(SLOT_COUNT, onContentsChanged);
        inventory.setOutputSlots(OUTPUT_SLOT);
        inventory.addSlotLimit(BLUEPRINT_SLOT, 1);
        inventory.setSlotValidator(ExtractorForgeTileEntity::slotAccepts);
        return inventory;
    }

    public static boolean slotAccepts(int slot, ItemStack stack) {
        if (slot == BLUEPRINT_SLOT) {
            return stack.getItem() instanceof ExtractorBlueprintItem;
        }
        return slot >= DEPOSIT_SLOT_START;
    }

    @Override
    public BaseItemStackHandler getInventory() {
        return this.inventory;
    }

    /** The cost ledger of the identified blueprint in the blueprint slot, or null. */
    @Nullable
    public CompoundTag ledger() {
        ItemStack blueprint = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        if (!(blueprint.getItem() instanceof ExtractorBlueprintItem) || !ExtractorBlueprintItem.isIdentified(blueprint)) {
            return null;
        }
        return ExtractorBlueprintItem.ledger(blueprint);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.workingTicks = tag.getInt("Working");
        FluidStack legacy = FluidStack.loadFluidStackFromNBT(tag.getCompound("Tank"));
        if (!legacy.isEmpty()) {
            WoldsVaults.LOGGER.error("Extraction Forge at {} held {} mB of {} in its old tank; the forge no longer has a tank and the fluid was discarded.",
                    this.getBlockPos(), legacy.getAmount(), legacy.getDisplayName().getString());
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Working", this.workingTicks);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExtractorForgeTileEntity tile) {
        if (tile.workingTicks > 0) {
            tile.workingTicks--;
            if (tile.workingTicks == 0) {
                tile.markDirtyAndDispatch();
            }
        }
        if (level.getGameTime() % PAY_INTERVAL_TICKS == 0) {
            tile.payFromDeposits();
        }
    }

    private void payFromDeposits() {
        CompoundTag ledger = this.ledger();
        if (ledger == null) {
            return;
        }
        boolean paid = false;
        for (int slot = DEPOSIT_SLOT_START; slot < SLOT_COUNT; slot++) {
            paid |= CostLedger.payFromSlot(ledger, this.inventory, slot);
        }
        if (paid) {
            this.markDirtyAndDispatch();
        }
    }

    @Override
    public boolean isWorking() {
        return this.workingTicks > 0;
    }

    @Override
    public ItemStack getDisplayExtractor() {
        return ItemStack.EMPTY;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.isRemoved() && cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return this.fluidCapability.cast();
        }
        if (!this.isRemoved() && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return this.itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.itemCapability.invalidate();
        this.fluidCapability.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.woldsvaults.extractor_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return ExtractorForgeContainer.create(windowId, playerInventory, this::isUsableByPlayer, this.inventory, this.getBlockPos());
    }

    /** Null when Fabricate may fire, else the reason it may not. */
    @Nullable
    public String fabricateRefusal() {
        ItemStack blueprint = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        if (!(blueprint.getItem() instanceof ExtractorBlueprintItem)) {
            return "Place an identified blueprint in the blueprint slot.";
        }
        if (ExtractorBlueprintItem.getPart(blueprint) == null) {
            return "Identify the blueprint first.";
        }
        if (!this.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return "Take the fabricated part out first.";
        }
        if (!CostLedger.isComplete(this.ledger())) {
            return "The fabrication cost is not fully paid.";
        }
        return null;
    }

    public void fabricate(Player player) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        String refusal = this.fabricateRefusal();
        if (refusal != null) {
            tell(player, refusal);
            return;
        }
        ItemStack blueprint = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        ExtractorBlueprintItem item = (ExtractorBlueprintItem) blueprint.getItem();
        PartData part = ExtractorBlueprintItem.getPart(blueprint);
        ExtractorPartItem partItem = ModItems.EXTRACTOR_PARTS.get(item.getPartType());
        this.inventory.setStackInSlot(OUTPUT_SLOT, ExtractorPartItem.create(partItem, part));
        this.inventory.setStackInSlot(BLUEPRINT_SLOT, ItemStack.EMPTY);
        this.workingTicks = WORK_CYCLE_TICKS;
        this.markDirtyAndDispatch();
        WoldsVaults.LOGGER.info("{} fabricated {} ({}) at {}.", player.getName().getString(), part.name, part.describeOrigin(), this.getBlockPos());
        tell(player, "Fabricated " + part.name + ".");
    }

    private static void tell(Player player, String message) {
        player.displayClientMessage(new TextComponent(message).withStyle(ChatFormatting.YELLOW), false);
    }

    /** What pipes see: slot 0 is the finished part (extract only); every other slot pays into the blueprint's ledger. */
    private class AutomationHandler implements IItemHandler {
        private final IItemHandler ledger;

        AutomationHandler(IItemHandler ledger) {
            this.ledger = ledger;
        }

        @Override
        public int getSlots() {
            return 1 + this.ledger.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? ExtractorForgeTileEntity.this.inventory.getStackInSlot(OUTPUT_SLOT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? stack : this.ledger.insertItem(slot - 1, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? ExtractorForgeTileEntity.this.inventory.extractItem(OUTPUT_SLOT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 64 : this.ledger.getSlotLimit(slot - 1);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != 0 && this.ledger.isItemValid(slot - 1, stack);
        }
    }
}
