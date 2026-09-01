package xyz.iwolfking.woldsvaults.entities.extraction;

import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.init.ModConfigs;
import xyz.iwolfking.woldsvaults.init.ModItems;
import xyz.iwolfking.woldsvaults.items.ExtractorItem;
import xyz.iwolfking.woldsvaults.objectives.ExtractionObjective;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

public class ExtractorEntity extends LivingEntity {
    private static final EntityDataAccessor<Boolean> DATA_DRILLING = SynchedEntityData.defineId(ExtractorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_JOB_DONE = SynchedEntityData.defineId(ExtractorEntity.class, EntityDataSerializers.BOOLEAN);

    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    private final SimpleContainer loot = new SimpleContainer(9);

    private float shield;
    private float energy;
    private float currentDurability;
    private float maxShield;
    private float armor;
    private float shieldToughness;
    private float shieldRegenPerTick;
    private int power;
    private int speed;
    private float energyCapacity;
    private float energyCostPerSecond;
    private float energyPerCell;
    private float drillQuantity;
    private float drillRarity;
    private int maxDurability;
    private String jobId = "";
    private int siteIndex = -1;
    private UUID owner;

    public ExtractorEntity(EntityType<? extends ExtractorEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DRILLING, false);
        this.entityData.define(DATA_JOB_DONE, false);
    }

    /** Snapshots the deploying item's stat block onto this entity and charges the initial energy. */
    public void initFromItem(ItemStack stack, Player deployer, int siteIndex, float initialEnergyFraction) {
        CompoundTag data = ExtractorItem.getData(stack);
        this.maxShield = data.getFloat("Shield");
        this.shield = this.maxShield;
        this.armor = data.getFloat("Armor");
        this.shieldToughness = data.getFloat("ShieldToughness");
        this.shieldRegenPerTick = data.getFloat("ShieldRegen");
        this.power = data.getInt("Power");
        this.speed = data.getInt("Speed");
        this.energyCapacity = data.getFloat("EnergyCapacity");
        this.energyCostPerSecond = data.getFloat("EnergyCost");
        this.energyPerCell = data.getFloat("EnergyPerCell");
        this.drillQuantity = data.getFloat("Quantity");
        this.drillRarity = data.getFloat("Rarity");
        this.currentDurability = data.getInt("Durability");
        this.maxDurability = data.getInt("MaxDurability");
        this.jobId = data.getString("Job");
        this.siteIndex = siteIndex;
        this.owner = deployer.getUUID();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1.0F, data.getFloat("MaxHealth")));
        this.setHealth(Math.max(1.0F, data.getFloat("Health")));
        this.energy = this.energyCapacity * initialEnergyFraction;
        this.setYRot(0.0F);
        this.setYHeadRot(0.0F);
        this.yBodyRot = 0.0F;
    }

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (this.level.isClientSide() || this.isRemoved()) {
            return false;
        }
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.hurt(source, amount);
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker instanceof Player) {
            return false;
        }
        if (this.shield > 0.0F) {
            float absorbed = CombatRules.getDamageAfterAbsorb(amount, this.shieldToughness, 0.0F);
            this.shield = Math.max(0.0F, this.shield - absorbed);
            this.level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.6F, 1.4F);
            this.invulnerableTime = 0;
            return true;
        }
        float mitigated = CombatRules.getDamageAfterAbsorb(amount, this.armor, 0.0F);
        float hpDamagePerDurability = ModConfigs.EXTRACTION_OBJECTIVE.DURABILITY.hpDamagePerDurabilityPoint;
        if (hpDamagePerDurability > 0.0F) {
            this.currentDurability = Math.max(0.0F, this.currentDurability - mitigated / hpDamagePerDurability);
        }
        boolean result = super.hurt(source, mitigated);
        this.invulnerableTime = 0;
        return result;
    }

    @Override
    public void die(@Nonnull DamageSource source) {
        if (!this.level.isClientSide()) {
            float loss = ModConfigs.EXTRACTION_OBJECTIVE.DURABILITY.destructionLossFraction * this.maxDurability;
            int remaining = (int) Math.max(0.0F, this.currentDurability - loss);
            writeBackToItem(this.level.getServer(), this.getUUID(), remaining, this.getMaxHealth());
            this.dropLoot();
            VaultUtils.getVault(this.level).flatMap(ExtractionObjective::get)
                    .ifPresentOrElse(objective -> objective.onExtractorDestroyed(this),
                            () -> WoldsVaults.LOGGER.error("Extractor entity {} died outside an extraction objective; site state cannot be updated.", this.getUUID()));
        }
        super.die(source);
    }

    private void dropLoot() {
        for (int i = 0; i < this.loot.getContainerSize(); i++) {
            ItemStack stack = this.loot.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(this.level, this.getX(), this.getY() + 0.5D, this.getZ(), stack);
            }
        }
    }

    /**
     * Finds the deploying item (whoever now holds it) and writes the session's durability and health
     * results onto it, re-arming it. Falls back to the item's own timed self-reset when nobody online
     * holds it.
     */
    public static void writeBackToItem(MinecraftServer server, UUID entityId, int durability, float hp) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof ExtractorItem && entityId.equals(ExtractorItem.getDeployedEntity(stack))) {
                    ExtractorItem.armWithResults(stack, durability, hp);
                    return;
                }
            }
        }
        WoldsVaults.LOGGER.info("No online player holds the extractor item keyed to entity {}; relying on the item's timed self-reset.", entityId);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 localHit, InteractionHand hand) {
        if (this.level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (this.isJobDone()) {
            if (held.getItem() instanceof ExtractorItem && this.getUUID().equals(ExtractorItem.getDeployedEntity(held))) {
                this.pickUp(player, held);
                return InteractionResult.CONSUME;
            }
            if (isLootBoxHit(localHit)) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inv, this.loot, 1),
                        new TextComponent("Extractor Storage")));
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(new TextComponent("Job complete. Right-click with this extractor's item to collect it.").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }
        int deposited = this.depositCellsFrom(player);
        if (deposited > 0) {
            player.displayClientMessage(new TextComponent(String.format("Deposited %d power cell%s (+%,.0f energy).",
                    deposited, deposited == 1 ? "" : "s", deposited * this.energyPerCell)).withStyle(ChatFormatting.YELLOW), true);
            this.level.playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 0.6F);
        } else {
            player.displayClientMessage(new TextComponent(String.format("Energy %,.0f / %,.0f - deposit power cells to keep it drilling.",
                    this.energy, this.energyCapacity)).withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.CONSUME;
    }

    private void pickUp(Player player, ItemStack held) {
        ExtractorItem.armWithResults(held, (int) this.currentDurability, this.getHealth());
        this.dropLoot();
        player.displayClientMessage(new TextComponent("Extractor collected.").withStyle(ChatFormatting.GREEN), true);
        VaultUtils.getVault(this.level).flatMap(ExtractionObjective::get)
                .ifPresent(objective -> objective.onExtractorCollected(this));
        this.discard();
    }

    private static boolean isLootBoxHit(Vec3 localHit) {
        return localHit.x > 0.3D && localHit.y > 0.05D && localHit.y < 1.0D && Math.abs(localHit.z) < 0.45D;
    }

    private int depositCellsFrom(Player player) {
        int consumed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && this.energy < this.energyCapacity; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.POWER_CELL) {
                while (!stack.isEmpty() && this.energy < this.energyCapacity) {
                    stack.shrink(1);
                    this.energy = Math.min(this.energyCapacity, this.energy + this.energyPerCell);
                    consumed++;
                }
            } else {
                consumed += this.depositFromBackpack(stack);
            }
        }
        consumed += this.depositFromCurios(player);
        return consumed;
    }

    private int depositFromBackpack(ItemStack candidate) {
        if (this.energy >= this.energyCapacity || candidate.isEmpty()) {
            return 0;
        }
        try {
            IItemHandler handler = candidate
                    .getCapability(net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper.getCapabilityInstance())
                    .map(wrapper -> (IItemHandler) wrapper.getInventoryHandler())
                    .orElse(null);
            return handler == null ? 0 : this.depositFromHandler(handler);
        } catch (Throwable t) {
            WoldsVaults.LOGGER.error("Sophisticated Backpacks cell sweep failed; only main-inventory cells were deposited.", t);
            return 0;
        }
    }

    private int depositFromCurios(Player player) {
        try {
            return CuriosApi.getCuriosHelper().getEquippedCurios(player).map(curios -> {
                int consumed = 0;
                for (int i = 0; i < curios.getSlots(); i++) {
                    consumed += this.depositFromBackpack(curios.getStackInSlot(i));
                }
                return consumed;
            }).orElse(0);
        } catch (Throwable t) {
            WoldsVaults.LOGGER.error("Curios cell sweep failed; equipped backpacks were skipped.", t);
            return 0;
        }
    }

    private int depositFromHandler(IItemHandler handler) {
        int consumed = 0;
        for (int slot = 0; slot < handler.getSlots() && this.energy < this.energyCapacity; slot++) {
            while (this.energy < this.energyCapacity && handler.getStackInSlot(slot).getItem() == ModItems.POWER_CELL
                    && !handler.extractItem(slot, 1, false).isEmpty()) {
                this.energy = Math.min(this.energyCapacity, this.energy + this.energyPerCell);
                consumed++;
            }
        }
        return consumed;
    }

    /** Rolls the payout into the on-board storage, heals the configured fraction and powers down. */
    public void completeJob(NonNullList<ItemStack> payout) {
        for (ItemStack stack : payout) {
            ItemStack remainder = this.loot.addItem(stack);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(this.level, this.getX(), this.getY() + 0.5D, this.getZ(), remainder);
            }
        }
        this.heal(this.getMaxHealth() * ModConfigs.EXTRACTION_OBJECTIVE.DEPLOYMENT.healFractionOnJobComplete);
        this.setJobDone(true);
        this.setDrillingAnim(false);
        this.level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level.isClientSide() && this.tickCount % 100 == 0 && VaultUtils.getVault(this.level).isEmpty()) {
            WoldsVaults.LOGGER.info("Extractor entity {} found itself outside a vault and self-destructed.", this.getUUID());
            this.discard();
        }
    }

    @Override
    public void travel(@Nonnull Vec3 input) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean canBeAffected(@Nonnull MobEffectInstance effect) {
        return false;
    }

    @Override
    protected int getExperienceReward(@Nonnull Player player) {
        return 0;
    }

    @Override
    protected SoundEvent getHurtSound(@Nonnull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armorItems;
    }

    @Override
    public ItemStack getItemBySlot(@Nonnull EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@Nonnull EquipmentSlot slot, @Nonnull ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("WvShield", this.shield);
        tag.putFloat("WvMaxShield", this.maxShield);
        tag.putFloat("WvEnergy", this.energy);
        tag.putFloat("WvDurability", this.currentDurability);
        tag.putInt("WvMaxDurability", this.maxDurability);
        tag.putFloat("WvArmor", this.armor);
        tag.putFloat("WvShieldToughness", this.shieldToughness);
        tag.putFloat("WvShieldRegen", this.shieldRegenPerTick);
        tag.putInt("WvPower", this.power);
        tag.putInt("WvSpeed", this.speed);
        tag.putFloat("WvEnergyCapacity", this.energyCapacity);
        tag.putFloat("WvEnergyCost", this.energyCostPerSecond);
        tag.putFloat("WvEnergyPerCell", this.energyPerCell);
        tag.putFloat("WvQuantity", this.drillQuantity);
        tag.putFloat("WvRarity", this.drillRarity);
        tag.putString("WvJob", this.jobId);
        tag.putInt("WvSiteIndex", this.siteIndex);
        tag.putBoolean("WvJobDone", this.isJobDone());
        if (this.owner != null) {
            tag.putUUID("WvOwner", this.owner);
        }
        tag.put("WvLoot", this.loot.createTag());
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.shield = tag.getFloat("WvShield");
        this.maxShield = tag.getFloat("WvMaxShield");
        this.energy = tag.getFloat("WvEnergy");
        this.currentDurability = tag.getFloat("WvDurability");
        this.maxDurability = tag.getInt("WvMaxDurability");
        this.armor = tag.getFloat("WvArmor");
        this.shieldToughness = tag.getFloat("WvShieldToughness");
        this.shieldRegenPerTick = tag.getFloat("WvShieldRegen");
        this.power = tag.getInt("WvPower");
        this.speed = tag.getInt("WvSpeed");
        this.energyCapacity = tag.getFloat("WvEnergyCapacity");
        this.energyCostPerSecond = tag.getFloat("WvEnergyCost");
        this.energyPerCell = tag.getFloat("WvEnergyPerCell");
        this.drillQuantity = tag.getFloat("WvQuantity");
        this.drillRarity = tag.getFloat("WvRarity");
        this.jobId = tag.getString("WvJob");
        this.siteIndex = tag.getInt("WvSiteIndex");
        this.setJobDone(tag.getBoolean("WvJobDone"));
        if (tag.hasUUID("WvOwner")) {
            this.owner = tag.getUUID("WvOwner");
        }
        this.loot.fromTag(tag.getList("WvLoot", 10));
    }

    public boolean isDrillingAnim() {
        return this.entityData.get(DATA_DRILLING);
    }

    public void setDrillingAnim(boolean drilling) {
        this.entityData.set(DATA_DRILLING, drilling);
    }

    public boolean isJobDone() {
        return this.entityData.get(DATA_JOB_DONE);
    }

    public void setJobDone(boolean done) {
        this.entityData.set(DATA_JOB_DONE, done);
    }

    public float getShield() {
        return this.shield;
    }

    public void setShield(float shield) {
        this.shield = Math.min(this.maxShield, Math.max(0.0F, shield));
    }

    public float getMaxShield() {
        return this.maxShield;
    }

    public float getShieldRegenPerTick() {
        return this.shieldRegenPerTick;
    }

    public float getEnergy() {
        return this.energy;
    }

    public void setEnergy(float energy) {
        this.energy = Math.min(this.energyCapacity, Math.max(0.0F, energy));
    }

    public float getEnergyCapacity() {
        return this.energyCapacity;
    }

    public float getEnergyCostPerSecond() {
        return this.energyCostPerSecond;
    }

    public float getCurrentDurability() {
        return this.currentDurability;
    }

    public void drainDurability(float amount) {
        this.currentDurability = Math.max(0.0F, this.currentDurability - amount);
    }

    public int getPower() {
        return this.power;
    }

    public int getDrillSpeed() {
        return this.speed;
    }

    public float getDrillQuantity() {
        return this.drillQuantity;
    }

    public float getDrillRarity() {
        return this.drillRarity;
    }

    public String getJobId() {
        return this.jobId;
    }

    public int getSiteIndex() {
        return this.siteIndex;
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(this.owner);
    }
}
