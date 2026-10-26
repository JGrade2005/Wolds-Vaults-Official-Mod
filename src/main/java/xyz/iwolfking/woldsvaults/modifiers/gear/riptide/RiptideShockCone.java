package xyz.iwolfking.woldsvaults.modifiers.gear.riptide;

import iskallia.vault.entity.entity.EternalEntity;
import iskallia.vault.entity.entity.PetEntity;
import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.api.gear.sets.GearSetHelper;
import xyz.iwolfking.woldsvaults.config.GearSetConfig;
import xyz.iwolfking.woldsvaults.init.ModGearAttributes;

import java.util.List;

/**
 * The riptide shock cone: every tick of a dash, mobs inside a cone from {@code shockConeAhead} blocks ahead of the
 * player to a {@code shockConeRadius} radius {@code shockConeBehind} blocks behind are shocked once per dash for
 * {@link ModGearAttributes#RIPTIDE_SHOCK_CONE} times the dash's first contact final damage. Mobs shocked before the
 * dash lands a contact wait for it; if it never lands one they take the player's previous dash value. Area of
 * effect does not scale the cone.
 */
public class RiptideShockCone implements RiptideDash.Listener {
    @Override
    public void onTick(ServerPlayer player, RiptideDash.Dash dash) {
        float multiplier = multiplier(player);
        if (multiplier <= 0.0F) {
            return;
        }
        GearSetConfig.Riptide tuning = GearSetHelper.config().getRiptide();
        Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        Vec3 axis = dash.getDirection();
        double reach = Math.max(tuning.shockConeRadius, Math.max(tuning.shockConeAhead, tuning.shockConeBehind)) + 1.0;
        List<LivingEntity> candidates = player.level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(reach),
                entity -> isTarget(player, entity) && inCone(entity, origin, axis, tuning));
        for (LivingEntity target : candidates) {
            if (!dash.markShocked(target)) {
                continue;
            }
            if (dash.getReferenceDamage() > 0.0F) {
                hit(player, target, multiplier * dash.getReferenceDamage());
            } else {
                dash.getPendingShock().add(target);
            }
        }
        spawnWake(player.getLevel(), origin, axis, tuning);
    }

    @Override
    public void onContact(ServerPlayer player, RiptideDash.Dash dash, LivingEntity target, float finalDamage) {
        flush(player, dash, dash.getReferenceDamage());
    }

    @Override
    public void onEnd(ServerPlayer player, RiptideDash.Dash dash) {
        if (dash.getPendingShock().isEmpty()) {
            return;
        }
        float reference = RiptideDash.lastReference(player);
        if (reference <= 0.0F) {
            WoldsVaults.LOGGER.debug("Riptide shock cone: {} mobs shocked by {} before any riptide contact; no damage to draw from, skipped.", dash.getPendingShock().size(), player.getScoreboardName());
            dash.getPendingShock().clear();
            return;
        }
        flush(player, dash, reference);
    }

    private static void flush(ServerPlayer player, RiptideDash.Dash dash, float reference) {
        if (reference <= 0.0F || dash.getPendingShock().isEmpty()) {
            return;
        }
        float amount = multiplier(player) * reference;
        dash.getPendingShock().forEach(target -> hit(player, target, amount));
        dash.getPendingShock().clear();
    }

    private static float multiplier(ServerPlayer player) {
        return RiptideDash.snapshot(player).getAttributeValue(ModGearAttributes.RIPTIDE_SHOCK_CONE, VaultGearAttributeTypeMerger.floatSum());
    }

    private static void hit(ServerPlayer player, LivingEntity target, float amount) {
        RiptideDerivedHits.queue(player, target, amount);
        player.getLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                12, target.getBbWidth() * 0.4, target.getBbHeight() * 0.4, target.getBbWidth() * 0.4, 0.4);
    }

    static boolean isTarget(Player player, LivingEntity entity) {
        return entity != player && entity.isAlive() && !(entity instanceof Player)
                && !(entity instanceof EternalEntity) && !(entity instanceof PetEntity) && !player.isAlliedTo(entity);
    }

    /** Inside the cone whose apex is {@code shockConeAhead} blocks along the axis and whose base has radius {@code shockConeRadius}. */
    static boolean inCone(LivingEntity entity, Vec3 origin, Vec3 axis, GearSetConfig.Riptide tuning) {
        Vec3 offset = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(origin);
        double along = offset.dot(axis);
        if (along > tuning.shockConeAhead || along < -tuning.shockConeBehind) {
            return false;
        }
        double length = tuning.shockConeAhead + tuning.shockConeBehind;
        double allowed = tuning.shockConeRadius * (tuning.shockConeAhead - along) / length + entity.getBbWidth() * 0.5;
        return offset.subtract(axis.scale(along)).length() <= allowed;
    }

    private static void spawnWake(ServerLevel level, Vec3 origin, Vec3 axis, GearSetConfig.Riptide tuning) {
        Vec3 side = axis.cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.normalize();
        Vec3 up = side.cross(axis).normalize();
        double length = tuning.shockConeAhead + tuning.shockConeBehind;
        for (int i = 0; i < 6; i++) {
            double along = tuning.shockConeAhead - level.random.nextDouble() * length;
            double radius = tuning.shockConeRadius * (tuning.shockConeAhead - along) / length;
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            Vec3 point = origin.add(axis.scale(along)).add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
