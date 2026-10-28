package xyz.iwolfking.woldsvaults.modifiers.gear.riptide;

import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.snapshot.AttributeSnapshot;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.api.gear.sets.GearSetHelper;
import xyz.iwolfking.woldsvaults.gods.combat.FinalDamageStage;
import xyz.iwolfking.woldsvaults.init.ModEffects;
import xyz.iwolfking.woldsvaults.init.ModGearAttributes;

/**
 * The riptide storm: a crackling cloud while dashing, a lightning strike on every mob the dash touches for
 * {@link ModGearAttributes#RIPTIDE_BOLT} times the contact's final damage ({@link ModGearAttributes#RIPTIDE_BOLT_CHILLED}
 * against chilled or frozen mobs), less damage taken while the spin is active, and a hover after the dash.
 */
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID)
public class RiptideStorm implements RiptideDash.Listener {
    private static final float MAX_REDUCTION = 0.95F;

    static {
        FinalDamageStage.register(WoldsVaults.id("riptide_storm_reduction"), FinalDamageStage.ORDER_REDUCTION, (event, amount) -> {
            if (!(event.getEntityLiving() instanceof ServerPlayer player) || !player.isAutoSpinAttack()) {
                return amount;
            }
            float reduction = RiptideDash.snapshot(player).getAttributeValue(ModGearAttributes.RIPTIDE_BOOST_DAMAGE_REDUCTION, VaultGearAttributeTypeMerger.floatSum());
            if (reduction <= 0.0F) {
                return amount;
            }
            return amount * (1.0F - Math.min(MAX_REDUCTION, reduction));
        });
    }

    @Override
    public void onStart(ServerPlayer player, RiptideDash.Dash dash) {
        player.removeEffect(ModEffects.TIDAL_HOVER);
    }

    @Override
    public void onTick(ServerPlayer player, RiptideDash.Dash dash) {
        if (bolt(RiptideDash.snapshot(player)) <= 0.0F) {
            return;
        }
        ServerLevel level = player.getLevel();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), 10, 0.9, 0.9, 0.9, 0.25);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), 3, 0.7, 0.7, 0.7, 0.02);
        if (dash.getTicks() % 6 == 1) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.35F, 1.6F);
        }
    }

    @Override
    public void onContact(ServerPlayer player, RiptideDash.Dash dash, LivingEntity target, float finalDamage) {
        AttributeSnapshot snapshot = RiptideDash.snapshot(player);
        float multiplier = isChilled(target)
                ? snapshot.getAttributeValue(ModGearAttributes.RIPTIDE_BOLT_CHILLED, VaultGearAttributeTypeMerger.floatSum())
                : bolt(snapshot);
        if (multiplier <= 0.0F || finalDamage <= 0.0F) {
            return;
        }
        RiptideDerivedHits.queue(player, target, multiplier * finalDamage);
        if (dash.countVisualBolt() <= GearSetHelper.config().getRiptide().stormVisualBoltsPerDash) {
            LightningBolt visual = EntityType.LIGHTNING_BOLT.create(player.level);
            if (visual != null) {
                visual.moveTo(target.getX(), target.getY(), target.getZ());
                visual.setVisualOnly(true);
                player.level.addFreshEntity(visual);
            }
        } else {
            player.getLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, target.getBbWidth() * 0.5, target.getBbHeight() * 0.5, target.getBbWidth() * 0.5, 0.6);
        }
    }

    @Override
    public void onEnd(ServerPlayer player, RiptideDash.Dash dash) {
        int hover = RiptideDash.snapshot(player).getAttributeValue(ModGearAttributes.RIPTIDE_HOVER, VaultGearAttributeTypeMerger.intSum());
        if (hover > 0 && !player.isOnGround()) {
            player.addEffect(new MobEffectInstance(ModEffects.TIDAL_HOVER, hover, 0, false, false, true));
        }
    }

    @SubscribeEvent
    public static void cancelHoverOnSneak(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.isShiftKeyDown() && player.hasEffect(ModEffects.TIDAL_HOVER)) {
            player.removeEffect(ModEffects.TIDAL_HOVER);
        }
    }

    private static float bolt(AttributeSnapshot snapshot) {
        return snapshot.getAttributeValue(ModGearAttributes.RIPTIDE_BOLT, VaultGearAttributeTypeMerger.floatSum());
    }

    static boolean isChilled(LivingEntity target) {
        return target.hasEffect(iskallia.vault.init.ModEffects.CHILLED)
                || target.hasEffect(iskallia.vault.init.ModEffects.FREEZE)
                || target.isFullyFrozen();
    }
}
