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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.blocks.containers.ExtractorForgeContainer;
import xyz.iwolfking.woldsvaults.extraction.parts.PartData;
import xyz.iwolfking.woldsvaults.init.ModBlocks;
import xyz.iwolfking.woldsvaults.init.ModFluids;
import xyz.iwolfking.woldsvaults.init.ModItems;
import xyz.iwolfking.woldsvaults.items.ExtractorBlueprintItem;
import xyz.iwolfking.woldsvaults.items.ExtractorItem;
import xyz.iwolfking.woldsvaults.items.ExtractorPartItem;

/**
 * Slot 0 blueprint, 1 fabricated part, 2 extractor to repair, 3–8 material inputs. Costs are not
 * configured yet: fabrication and repair run free and say so in the log until the cost system lands.
 */
public class ExtractorForgeTileEntity extends BaseInventoryTileEntity implements MenuProvider, ExtractorStationTile {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int REPAIR_SLOT = 2;
    public static final int MATERIAL_SLOT_START = 3;
    public static final int MATERIAL_SLOTS = 6;
    public static final int SLOT_COUNT = MATERIAL_SLOT_START + MATERIAL_SLOTS;
    public static final int TANK_CAPACITY = 16000;
    public static final int WORK_CYCLE_TICKS = 80;

    private final BaseItemStackHandler inventory;
    private int workingTicks;
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == ModFluids.PRISMATIC_GLUE.get()) {
        @Override
        protected void onContentsChanged() {
            ExtractorForgeTileEntity.this.markDirtyAndDispatch();
        }
    };
    private final LazyOptional<IFluidHandler> tankCapability = LazyOptional.of(() -> this.tank);
    private boolean warnedFreeCosts;

    public ExtractorForgeTileEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EXTRACTOR_FORGE_TILE_ENTITY_TYPE, pos, state);
        this.inventory = createInventoryHandler(this::markDirtyAndDispatch);
    }

    public static BaseItemStackHandler createInventoryHandler(Runnable onContentsChanged) {
        BaseItemStackHandler inventory = new BaseItemStackHandler(SLOT_COUNT, onContentsChanged);
        inventory.setOutputSlots(OUTPUT_SLOT);
        inventory.addSlotLimit(BLUEPRINT_SLOT, 1);
        inventory.addSlotLimit(REPAIR_SLOT, 1);
        inventory.setSlotValidator((slot, stack) -> slotAccepts(slot, stack));
        return inventory;
    }

    public static boolean slotAccepts(int slot, ItemStack stack) {
        if (slot == BLUEPRINT_SLOT) {
            return stack.getItem() instanceof ExtractorBlueprintItem;
        }
        if (slot == REPAIR_SLOT) {
            return stack.getItem() instanceof ExtractorItem;
        }
        return slot >= MATERIAL_SLOT_START;
    }

    @Override
    public BaseItemStackHandler getInventory() {
        return this.inventory;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.tank.readFromNBT(tag.getCompound("Tank"));
        this.workingTicks = tag.getInt("Working");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", this.tank.writeToNBT(new CompoundTag()));
        tag.putInt("Working", this.workingTicks);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ExtractorForgeTileEntity tile) {
        if (tile.workingTicks > 0) {
            tile.workingTicks--;
            if (tile.workingTicks == 0) {
                tile.markDirtyAndDispatch();
            }
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

    private void startWorking() {
        this.workingTicks = WORK_CYCLE_TICKS;
        this.markDirtyAndDispatch();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.isRemoved() && cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.orEmpty(cap, this.tankCapability);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.tankCapability.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.woldsvaults.extractor_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return ExtractorForgeContainer.create(windowId, playerInventory, this::isUsableByPlayer, this.inventory, this.getBlockPos());
    }

    /** Placeholder for the cost system: always affordable, logged once per forge so it is never mistaken for a real check. */
    private boolean coverCost(String operation) {
        if (!this.warnedFreeCosts) {
            WoldsVaults.LOGGER.error("Extractor Forge at {} ran '{}' with NO material cost: the cost configuration does not exist yet.", this.getBlockPos(), operation);
            this.warnedFreeCosts = true;
        }
        return true;
    }

    public void fabricate(Player player) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        ItemStack blueprint = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        if (!(blueprint.getItem() instanceof ExtractorBlueprintItem item)) {
            tell(player, "Place an identified blueprint in the blueprint slot.");
            return;
        }
        PartData part = ExtractorBlueprintItem.getPart(blueprint);
        if (part == null) {
            tell(player, "Identify the blueprint first.");
            return;
        }
        if (!this.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            tell(player, "Take the fabricated part out first.");
            return;
        }
        if (!this.coverCost("fabricate " + part.name)) {
            tell(player, "Missing materials.");
            return;
        }
        ExtractorPartItem partItem = ModItems.EXTRACTOR_PARTS.get(item.getPartType());
        this.inventory.setStackInSlot(OUTPUT_SLOT, ExtractorPartItem.create(partItem, part));
        this.inventory.setStackInSlot(BLUEPRINT_SLOT, ItemStack.EMPTY);
        this.startWorking();
        tell(player, "Fabricated " + part.name + ".");
    }

    public void repair(Player player) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        ItemStack extractor = this.inventory.getStackInSlot(REPAIR_SLOT);
        if (!(extractor.getItem() instanceof ExtractorItem)) {
            tell(player, "Place an extractor in the repair slot.");
            return;
        }
        if (ExtractorItem.isDeployed(extractor)) {
            tell(player, "That extractor is deployed; it cannot be repaired right now.");
            return;
        }
        CompoundTag data = ExtractorItem.getData(extractor);
        int max = data.getInt("MaxDurability");
        int current = data.getInt("Durability");
        if (current >= max) {
            tell(player, "That extractor is already at full durability.");
            return;
        }
        if (!this.coverCost("repair " + (max - current) + " durability")) {
            tell(player, "Missing repair materials.");
            return;
        }
        ExtractorItem.setDurability(extractor, max);
        this.startWorking();
        tell(player, "Repaired " + (max - current) + " durability.");
    }

    private static void tell(Player player, String message) {
        player.displayClientMessage(new TextComponent(message).withStyle(ChatFormatting.YELLOW), false);
    }

    public FluidStack getFluid() {
        return this.tank.getFluid();
    }
}
