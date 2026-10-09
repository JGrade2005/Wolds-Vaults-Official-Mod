package xyz.iwolfking.woldsvaults.client.events;

import iskallia.vault.config.TrinketConfig;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.data.GearDataCache;
import iskallia.vault.gear.item.VaultGearItem;
import iskallia.vault.gear.trinket.TrinketEffect;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.VaultCrystalItem;
import iskallia.vault.item.gear.TrinketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.api.util.AncientUniqueHelper;
import xyz.iwolfking.woldsvaults.api.util.PouchHelper;
import xyz.iwolfking.woldsvaults.client.init.ModKeybinds;
import xyz.iwolfking.woldsvaults.effect.trinkets.SpeedLimitTrinketEffect;
import xyz.iwolfking.woldsvaults.extraction.jobs.AncientCrystalFlag;
import xyz.iwolfking.woldsvaults.items.CombinedTrinketItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TooltipEvents {
    private static boolean loggedInsertFallback = false;

    @SubscribeEvent
    public static void onTrinketPouchTooltip(ItemTooltipEvent event) {
        if (!PouchHelper.isPouch(event.getItemStack())) {
            return;
        }
        event.getToolTip().add(new TranslatableComponent("item.woldsvaults.trinket_pouch.open").withStyle(ChatFormatting.GRAY));
        event.getToolTip().add((ModKeybinds.openTrinketPouch.isUnbound()
                ? new TranslatableComponent("item.woldsvaults.trinket_pouch.unbound")
                : new TranslatableComponent("item.woldsvaults.trinket_pouch.shortcut", ModKeybinds.openTrinketPouch.getTranslatedKeyMessage()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!SpeedLimitTrinketEffect.hasBootsEffect(stack) || !TrinketItem.isIdentified(stack)) {
            return;
        }
        int capPercent = SpeedLimitTrinketEffect.getCapPercent(stack);
        Component line = new TextComponent("Current speed limit: " + (capPercent <= 0 ? "Uncapped" : capPercent + "%")).withStyle(ChatFormatting.GRAY);
        List<Component> tooltip = event.getToolTip();
        int insertAt = findInsertIndex(stack, tooltip);
        if (insertAt >= 0) {
            tooltip.add(insertAt, line);
            return;
        }
        tooltip.add(line);
        if (!loggedInsertFallback) {
            loggedInsertFallback = true;
            WoldsVaults.LOGGER.warn("Weighted Boots: could not find the expected tooltip position, appended the speed limit line at the end instead.");
        }
    }

    /** Unidentified gear flagged ancient explains what it will become, right under its name. */
    @SubscribeEvent
    public static void onUnidentifiedAncientTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof VaultGearItem) || !stack.hasTag()) {
            return;
        }
        GearDataCache cache = GearDataCache.of(stack);
        if (cache.getState() != VaultGearState.UNIDENTIFIED || !cache.hasAttribute(xyz.iwolfking.woldsvaults.init.ModGearAttributes.ANCIENT_UNIQUE)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        int at = Math.min(1, tooltip.size());
        tooltip.add(at, new TextComponent("Identifies into an Ancient unique").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(at, new TextComponent("Ancient Unique").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AncientUniqueHelper.ANCIENT_COLOR)).withBold(true)));
    }

    /** Crystals made by the Ancient Crystals extraction job say so right under their name. */
    @SubscribeEvent
    public static void onAncientCrystalTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof VaultCrystalItem) || !AncientCrystalFlag.isAncient(CrystalData.read(stack))) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        int at = Math.min(1, tooltip.size());
        tooltip.add(at, new TextComponent("Maps and sigils cannot be applied").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(at, new TextComponent("Ancient Crystal").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private static int findInsertIndex(ItemStack stack, List<Component> tooltip) {
        if (stack.getItem() instanceof CombinedTrinketItem) {
            String bootsName = null;
            Set<String> otherNames = new HashSet<>();
            for (TrinketEffect<?> effect : CombinedTrinketItem.getTrinkets(stack)) {
                TrinketConfig.Trinket cfg = effect.getTrinketConfig();
                if (cfg == null) {
                    continue;
                }
                if (effect instanceof SpeedLimitTrinketEffect) {
                    bootsName = cfg.getName();
                } else {
                    otherNames.add(cfg.getName());
                }
            }
            if (bootsName != null) {
                for (int i = 1; i < tooltip.size(); i++) {
                    if (!tooltip.get(i).getString().equals(bootsName)) {
                        continue;
                    }
                    int insertAt = i + 1;
                    while (insertAt < tooltip.size() && !tooltip.get(insertAt).getString().isEmpty() && !otherNames.contains(tooltip.get(insertAt).getString())) {
                        insertAt++;
                    }
                    return insertAt;
                }
            }
        }
        for (int i = 1; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
