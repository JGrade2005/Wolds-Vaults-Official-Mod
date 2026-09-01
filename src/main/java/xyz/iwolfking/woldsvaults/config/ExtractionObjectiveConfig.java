package xyz.iwolfking.woldsvaults.config;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.Config;
import iskallia.vault.core.util.WeightedList;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;

public class ExtractionObjectiveConfig extends Config {
    @Expose public DefaultExtractorEntry DEFAULT_EXTRACTOR = new DefaultExtractorEntry();
    @Expose public DurabilityEntry DURABILITY = new DurabilityEntry();
    @Expose public DeploymentEntry DEPLOYMENT = new DeploymentEntry();
    @Expose public LinkedHashMap<String, JobEntry> JOBS = new LinkedHashMap<>();
    @Expose public WavesEntry WAVES = new WavesEntry();
    @Expose public CellsEntry CELLS = new CellsEntry();
    @Expose public SitesEntry SITES = new SitesEntry();

    @Override
    public String getName() {
        return "extraction_objective";
    }

    @Override
    protected void reset() {
        DEFAULT_EXTRACTOR = new DefaultExtractorEntry();
        DURABILITY = new DurabilityEntry();
        DEPLOYMENT = new DeploymentEntry();
        WAVES = new WavesEntry();
        CELLS = new CellsEntry();
        SITES = new SitesEntry();
        JOBS = new LinkedHashMap<>();
        JOBS.put("arcane_essence", new JobEntry(50, 2400, 100, "woldsvaults:arcane_essence", 50, 100));
    }

    public JobEntry getJob(String id) {
        return JOBS.get(id);
    }

    public static class DefaultExtractorEntry {
        @Expose public float health = 10000.0F;
        @Expose public float shield = 2000.0F;
        @Expose public float armor = 10.0F;
        @Expose public float shieldToughness = 0.0F;
        @Expose public float shieldRegenPerTick = 5.0F;
        @Expose public int power = 50;
        @Expose public int speed = 100;
        @Expose public float energyCapacity = 1000.0F;
        @Expose public float energyCostPerSecond = 15.0F;
        @Expose public float energyPerCell = 200.0F;
        @Expose public float drillQuantity = 0.0F;
        @Expose public float drillRarity = 0.0F;
        @Expose public int durability = 100000;
        @Expose public String job = "arcane_essence";
    }

    public static class DurabilityEntry {
        @Expose public float drainPerSecondMining = 10.0F;
        @Expose public float destructionLossFraction = 0.45F;
        @Expose public float hpDamagePerDurabilityPoint = 2.0F;
    }

    public static class DeploymentEntry {
        @Expose public float initialEnergyFraction = 0.4F;
        @Expose public float healFractionOnJobComplete = 0.33F;
    }

    public static class JobEntry {
        @Expose public int minPower;
        @Expose public int baseDurationTicks;
        @Expose public int speedReference;
        @Expose public String payoutItem;
        @Expose public int payoutMin;
        @Expose public int payoutMax;

        public JobEntry() {
        }

        public JobEntry(int minPower, int baseDurationTicks, int speedReference, String payoutItem, int payoutMin, int payoutMax) {
            this.minPower = minPower;
            this.baseDurationTicks = baseDurationTicks;
            this.speedReference = speedReference;
            this.payoutItem = payoutItem;
            this.payoutMin = payoutMin;
            this.payoutMax = payoutMax;
        }
    }

    public static class WaveEntry {
        @Expose public int intervalTicks;
        @Expose public int countMin;
        @Expose public int countMax;
        @Expose public WeightedList<ResourceLocation> pool = new WeightedList<>();

        public WaveEntry() {
        }

        public WaveEntry(int intervalTicks, int countMin, int countMax) {
            this.intervalTicks = intervalTicks;
            this.countMin = countMin;
            this.countMax = countMax;
        }
    }

    public static class CarrierEntry {
        @Expose public String baseEntity = "the_vault:blood_tank";
        @Expose public float healthMultiplier = 2.0F;
        @Expose public float damageMultiplier = 1.5F;
        @Expose public int auraRadius = 8;
        @Expose public int auraIntervalTicks = 100;
        @Expose public int auraEffectDurationTicks = 120;
    }

    public static class WavesEntry {
        @Expose public WaveEntry horde = new WaveEntry(100, 10, 15);
        @Expose public WaveEntry assassin = new WaveEntry(200, 5, 10);
        @Expose public WaveEntry tank = new WaveEntry(400, 2, 3);
        @Expose public CarrierEntry carrier = new CarrierEntry();

        public WavesEntry() {
            horde.pool.add(new ResourceLocation("minecraft", "zombie"), 3);
            horde.pool.add(new ResourceLocation("the_vault", "t2_zombie"), 3);
            horde.pool.add(new ResourceLocation("the_vault", "t2_husk"), 3);
            horde.pool.add(new ResourceLocation("the_vault", "t2_drowned"), 3);
            horde.pool.add(new ResourceLocation("the_vault", "blood_horde_t3"), 2);
            assassin.pool.add(new ResourceLocation("the_vault", "t2_skeleton"), 3);
            assassin.pool.add(new ResourceLocation("the_vault", "vault_spider"), 3);
            assassin.pool.add(new ResourceLocation("the_vault", "t2_stray"), 2);
            assassin.pool.add(new ResourceLocation("quark", "wraith"), 1);
            tank.pool.add(new ResourceLocation("the_vault", "blood_tank"), 3);
            tank.pool.add(new ResourceLocation("the_vault", "overgrown_tank"), 2);
            tank.pool.add(new ResourceLocation("minecraft", "piglin_brute"), 1);
        }
    }

    public static class CellsEntry {
        @Expose public double hordeMobDropChance = 0.001D;
        @Expose public double assassinMobDropChance = 0.01D;
        @Expose public double otherMobDropChance = 0.001D;
        @Expose public double chestDropChance = 0.01D;
    }

    public static class SitesEntry {
        @Expose public int count = 3;
        @Expose public int ringMinRooms = 4;
        @Expose public int ringMaxRooms = 9;
        @Expose public int minSeparationRooms = 3;
    }
}
