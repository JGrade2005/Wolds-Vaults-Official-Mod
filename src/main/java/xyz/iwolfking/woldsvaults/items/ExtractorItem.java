package xyz.iwolfking.woldsvaults.items;

import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.config.ExtractionObjectiveConfig;
import xyz.iwolfking.woldsvaults.init.ModCreativeTabs;
import xyz.iwolfking.woldsvaults.objectives.ExtractionObjective;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ExtractorItem extends Item {
    public static final String TAG_ROOT = "Extractor";
    private static final int RESET_GRACE_TICKS = 200;

    public ExtractorItem(ResourceLocation id) {
        super(new Properties().tab(ModCreativeTabs.WOLDS_VAULTS).stacksTo(1).fireResistant());
        this.setRegistryName(id);
    }

    public static CompoundTag getData(ItemStack stack) {
        return stack.getOrCreateTag().getCompound(TAG_ROOT);
    }

    private static void setData(ItemStack stack, CompoundTag data) {
        stack.getOrCreateTag().put(TAG_ROOT, data);
    }

    public static boolean isDeployed(ItemStack stack) {
        return getData(stack).getBoolean("Deployed");
    }

    /**
     * Builds a fresh extractor whose stat block, job and durability come straight from the
     * DEFAULT_EXTRACTOR entry of extraction_objective.json.
     */
    public static ItemStack buildDefault(Item item, ExtractionObjectiveConfig config) {
        ExtractionObjectiveConfig.DefaultExtractorEntry cfg = config.DEFAULT_EXTRACTOR;
        ItemStack stack = new ItemStack(item);
        CompoundTag data = new CompoundTag();
        data.putFloat("Health", cfg.health);
        data.putFloat("MaxHealth", cfg.health);
        data.putFloat("Shield", cfg.shield);
        data.putFloat("Armor", cfg.armor);
        data.putFloat("ShieldToughness", cfg.shieldToughness);
        data.putFloat("ShieldRegen", cfg.shieldRegenPerTick);
        data.putInt("Power", cfg.power);
        data.putInt("Speed", cfg.speed);
        data.putFloat("EnergyCapacity", cfg.energyCapacity);
        data.putFloat("EnergyCost", cfg.energyCostPerSecond);
        data.putFloat("EnergyPerCell", cfg.energyPerCell);
        data.putFloat("Quantity", cfg.drillQuantity);
        data.putFloat("Rarity", cfg.drillRarity);
        data.putInt("Durability", cfg.durability);
        data.putInt("MaxDurability", cfg.durability);
        data.putString("Job", cfg.job);
        data.putBoolean("Deployed", false);
        setData(stack, data);
        return stack;
    }

    public static void markDeployed(ItemStack stack, UUID vaultId, UUID entityId) {
        CompoundTag data = getData(stack);
        data.putBoolean("Deployed", true);
        data.putUUID("DeployVault", vaultId);
        data.putUUID("DeployEntity", entityId);
        data.putInt("ResetCounter", 0);
        setData(stack, data);
    }

    /** Writes session results back onto the item and returns it to the armed state. */
    public static void armWithResults(ItemStack stack, int durability, float hp) {
        CompoundTag data = getData(stack);
        data.putInt("Durability", Math.max(0, durability));
        data.putFloat("Health", Math.max(0.0F, Math.min(hp, data.getFloat("MaxHealth"))));
        clearDeployment(data);
        setData(stack, data);
    }

    private static void clearDeployment(CompoundTag data) {
        data.putBoolean("Deployed", false);
        data.remove("DeployVault");
        data.remove("DeployEntity");
        data.remove("ResetCounter");
    }

    @Nullable
    public static UUID getDeployedEntity(ItemStack stack) {
        CompoundTag data = getData(stack);
        return data.hasUUID("DeployEntity") ? data.getUUID("DeployEntity") : null;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        CompoundTag data = getData(stack);
        return data.getInt("MaxDurability") > 0 && data.getInt("Durability") < data.getInt("MaxDurability");
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag data = getData(stack);
        int max = data.getInt("MaxDurability");
        return max <= 0 ? 0 : Math.round(13.0F * data.getInt("Durability") / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        CompoundTag data = getData(stack);
        int max = data.getInt("MaxDurability");
        float fraction = max <= 0 ? 0.0F : Math.max(0.0F, (float) data.getInt("Durability") / max);
        return Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (isDeployed(stack)) {
            player.displayClientMessage(new TextComponent("This extractor is already deployed.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (getData(stack).getInt("Durability") <= 0) {
            player.displayClientMessage(new TextComponent("This extractor is out of durability and cannot drill.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        Optional<Vault> vaultOpt = VaultUtils.getVault(level);
        if (vaultOpt.isEmpty()) {
            player.displayClientMessage(new TextComponent("Extractors only work inside extraction vaults.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        Optional<ExtractionObjective> objective = ExtractionObjective.get(vaultOpt.get());
        if (objective.isEmpty()) {
            player.displayClientMessage(new TextComponent("This vault has no extraction sites.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        return objective.get().tryDeploy(vaultOpt.get(), player, stack, context.getClickedPos());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide() || !isDeployed(stack) || level.getGameTime() % 20L != 0L) {
            return;
        }
        CompoundTag data = getData(stack);
        UUID deployVault = data.hasUUID("DeployVault") ? data.getUUID("DeployVault") : null;
        Optional<Vault> vault = VaultUtils.getVault(holder.level);
        boolean insideDeployVault = deployVault != null && vault.isPresent() && deployVault.equals(vault.get().get(Vault.ID));
        if (insideDeployVault) {
            data.putInt("ResetCounter", 0);
            setData(stack, data);
            return;
        }
        int counter = data.getInt("ResetCounter") + 20;
        if (counter < RESET_GRACE_TICKS) {
            data.putInt("ResetCounter", counter);
            setData(stack, data);
            return;
        }
        clearDeployment(data);
        setData(stack, data);
        WoldsVaults.LOGGER.info("Extractor item held by {} re-armed itself: its deployment vault {} is gone or was left behind.",
                holder.getName().getString(), deployVault);
        if (holder instanceof Player player) {
            player.displayClientMessage(new TextComponent("Your extractor re-armed itself.").withStyle(ChatFormatting.YELLOW), false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag data = getData(stack);
        if (!data.contains("MaxHealth")) {
            tooltip.add(new TextComponent("Uninitialized extractor.").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (data.getBoolean("Deployed")) {
            tooltip.add(new TextComponent("DEPLOYED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        tooltip.add(new TextComponent(String.format("Durability: %,d / %,d", data.getInt("Durability"), data.getInt("MaxDurability"))).withStyle(ChatFormatting.GRAY));
        tooltip.add(new TextComponent(String.format("Health: %,.0f / %,.0f", data.getFloat("Health"), data.getFloat("MaxHealth"))).withStyle(ChatFormatting.GREEN));
        tooltip.add(new TextComponent(String.format("Shield: %,.0f  Armor: %.0f  Shield Toughness: %.0f", data.getFloat("Shield"), data.getFloat("Armor"), data.getFloat("ShieldToughness"))).withStyle(ChatFormatting.AQUA));
        tooltip.add(new TextComponent(String.format("Power: %d  Speed: %d", data.getInt("Power"), data.getInt("Speed"))).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(new TextComponent(String.format("Energy: %,.0f cap, %,.1f/s, +%,.0f per cell", data.getFloat("EnergyCapacity"), data.getFloat("EnergyCost"), data.getFloat("EnergyPerCell"))).withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent("Job: " + data.getString("Job")).withStyle(ChatFormatting.WHITE));
    }
}
