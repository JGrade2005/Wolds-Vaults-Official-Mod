package xyz.iwolfking.woldsvaults.objectives.extraction;

import iskallia.vault.core.util.WeightedList;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.world.storage.VirtualWorld;
import iskallia.vault.entity.EntityScaler;
import iskallia.vault.entity.champion.ChampionPromoter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.config.ExtractionObjectiveConfig;
import xyz.iwolfking.woldsvaults.entities.extraction.ExtractorEntity;
import xyz.iwolfking.woldsvaults.entities.extraction.ExtractorTargetGoal;
import xyz.iwolfking.woldsvaults.init.ModConfigs;
import xyz.iwolfking.woldsvaults.objectives.ExtractionObjective;
import xyz.iwolfking.woldsvaults.objectives.lib.ObjectiveManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ExtractionSpawnManager extends ObjectiveManager<ExtractionObjective> {
    public static final String TAG_TARGETED = "wv_extraction_targeted";
    public static final String TAG_HORDE = "wv_ext_horde";
    public static final String TAG_ASSASSIN = "wv_ext_assassin";
    public static final String TAG_TANK = "wv_ext_tank";
    public static final String TAG_CARRIER = "wv_ext_carrier";
    public static final String TAG_GREED_CHAMPION = "greed_champion";

    private static final int SPAWN_ATTEMPTS = 24;
    private static final int SWEEP_INTERVAL = 20;

    private final List<UUID> carriers = new ArrayList<>();
    private int hordeTimer;
    private int assassinTimer;
    private int tankTimer;
    private int sweepTimer;
    private int auraTimer;

    public ExtractionSpawnManager(Vault vault, VirtualWorld world, ExtractionObjective objective) {
        super(vault, world, objective);
    }

    @Override
    public void tick() {
        ExtractionObjectiveConfig.WavesEntry waves = ModConfigs.EXTRACTION_OBJECTIVE.WAVES;
        List<ExtractionSite> drilling = objective.getDrillingSitesWithLiveExtractor(world);
        if (drilling.isEmpty()) {
            return;
        }
        if (++hordeTimer >= waves.horde.intervalTicks) {
            hordeTimer = 0;
            drilling.forEach(site -> spawnWave(site, waves.horde, TAG_HORDE, false));
        }
        if (++assassinTimer >= waves.assassin.intervalTicks) {
            assassinTimer = 0;
            drilling.forEach(site -> spawnWave(site, waves.assassin, TAG_ASSASSIN, false));
        }
        if (++tankTimer >= waves.tank.intervalTicks) {
            tankTimer = 0;
            drilling.forEach(site -> {
                spawnWave(site, waves.tank, TAG_TANK, false);
                spawnCarrier(site, waves.carrier);
            });
        }
        if (++sweepTimer >= SWEEP_INTERVAL) {
            sweepTimer = 0;
            drilling.forEach(this::sweepRoomTargets);
        }
        if (++auraTimer >= waves.carrier.auraIntervalTicks) {
            auraTimer = 0;
            pulseCarrierAuras(waves.carrier);
        }
    }

    private void spawnWave(ExtractionSite site, ExtractionObjectiveConfig.WaveEntry wave, String classTag, boolean carrier) {
        int count = wave.countMin >= wave.countMax ? wave.countMin
                : wave.countMin + world.getRandom().nextInt(wave.countMax - wave.countMin + 1);
        BlockPos anchor = objective.randomCaveAnchor(site, world.getRandom());
        for (int i = 0; i < count; i++) {
            spawnOne(site, wave.pool, anchor, classTag);
        }
    }

    private LivingEntity spawnOne(ExtractionSite site, WeightedList<ResourceLocation> pool, BlockPos anchor, String classTag) {
        ResourceLocation id = pool.getRandom(world.getRandom()).orElse(null);
        if (id == null || !ForgeRegistries.ENTITIES.containsKey(id)) {
            WoldsVaults.LOGGER.error("Extraction wave pool rolled unknown entity id {}; skipping this spawn.", id);
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(id);
        BlockPos pos = findSpawnPos(anchor, type);
        if (pos == null) {
            WoldsVaults.LOGGER.error("Extraction wave found no clear spawn spot near cave anchor {} for {}; skipping this spawn.", anchor, id);
            return null;
        }
        Entity created = type.create(world);
        if (!(created instanceof LivingEntity living)) {
            WoldsVaults.LOGGER.error("Extraction wave entity {} is not a living entity; skipping this spawn.", id);
            return null;
        }
        living.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, world.getRandom().nextFloat() * 360.0F, 0.0F);
        living.addTag(classTag);
        if (living instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.spawnAnim();
        }
        world.addWithUUID(living);
        EntityScaler.scale(vault, living);
        attachTargetGoal(living, site);
        return living;
    }

    private void spawnCarrier(ExtractionSite site, ExtractionObjectiveConfig.CarrierEntry carrierCfg) {
        WeightedList<ResourceLocation> pool = new WeightedList<>();
        pool.add(new ResourceLocation(carrierCfg.baseEntity), 1);
        LivingEntity carrier = spawnOne(site, pool, objective.randomCaveAnchor(site, world.getRandom()), TAG_CARRIER);
        if (carrier == null) {
            return;
        }
        carrier.addTag("vault_champion");
        ChampionPromoter.applyRandomChampion(carrier);
        multiplyAttribute(carrier, Attributes.MAX_HEALTH, carrierCfg.healthMultiplier);
        carrier.setHealth(carrier.getMaxHealth());
        multiplyAttribute(carrier, Attributes.ATTACK_DAMAGE, carrierCfg.damageMultiplier);
        carrier.setCustomName(new TextComponent("Power Cell Carrier").withStyle(ChatFormatting.YELLOW));
        carrier.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
        carriers.add(carrier.getUUID());
    }

    private static void multiplyAttribute(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, float multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            WoldsVaults.LOGGER.error("Power cell carrier {} has no {} attribute; its multiplier was skipped.", entity.getName().getString(), attribute.getDescriptionId());
            return;
        }
        instance.setBaseValue(instance.getBaseValue() * multiplier);
    }

    private void sweepRoomTargets(ExtractionSite site) {
        ExtractorEntity extractor = objective.getLiveExtractor(world, site);
        if (extractor == null) {
            return;
        }
        AABB bounds = objective.siteRoomBounds(site);
        for (Mob mob : world.getEntitiesOfClass(Mob.class, bounds)) {
            if (!(mob instanceof Enemy) || mob.getTags().contains(TAG_TARGETED) || mob.getTags().contains(TAG_GREED_CHAMPION)) {
                continue;
            }
            attachTargetGoal(mob, site);
        }
    }

    private void attachTargetGoal(LivingEntity living, ExtractionSite site) {
        ExtractorEntity extractor = objective.getLiveExtractor(world, site);
        if (extractor == null || !(living instanceof Mob mob) || mob.getTags().contains(TAG_GREED_CHAMPION)) {
            return;
        }
        mob.targetSelector.addGoal(0, new ExtractorTargetGoal(mob, extractor.getUUID()));
        mob.addTag(TAG_TARGETED);
    }

    private void pulseCarrierAuras(ExtractionObjectiveConfig.CarrierEntry carrierCfg) {
        Iterator<UUID> iterator = carriers.iterator();
        while (iterator.hasNext()) {
            Entity entity = world.getEntity(iterator.next());
            if (!(entity instanceof LivingEntity carrier) || !carrier.isAlive()) {
                iterator.remove();
                continue;
            }
            AABB range = carrier.getBoundingBox().inflate(carrierCfg.auraRadius);
            for (Mob mob : world.getEntitiesOfClass(Mob.class, range)) {
                if (mob == carrier || !(mob instanceof Enemy)) {
                    continue;
                }
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, carrierCfg.auraEffectDurationTicks, 0));
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, carrierCfg.auraEffectDurationTicks, 0));
            }
        }
    }

    private BlockPos findSpawnPos(BlockPos anchor, EntityType<?> type) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            int x = anchor.getX() + world.getRandom().nextInt(7) - 3;
            int z = anchor.getZ() + world.getRandom().nextInt(7) - 3;
            for (int yOffset = -3; yOffset <= 3; yOffset++) {
                BlockPos candidate = new BlockPos(x, anchor.getY() + yOffset, z);
                BlockPos floor = candidate.below();
                if (!world.getBlockState(floor).isValidSpawn(world, floor, type)) {
                    continue;
                }
                AABB box = type.getAABB(x + 0.5D, candidate.getY(), z + 0.5D);
                if (!world.noCollision(box)) {
                    continue;
                }
                return candidate;
            }
        }
        return null;
    }
}
