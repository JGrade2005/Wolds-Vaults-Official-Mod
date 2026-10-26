package xyz.iwolfking.woldsvaults.config;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.Config;
import iskallia.vault.core.util.WeightedList;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public class ExtractionObjectiveConfig extends Config {
    @Expose public DefaultExtractorEntry DEFAULT_EXTRACTOR = new DefaultExtractorEntry();
    @Expose public DurabilityEntry DURABILITY = new DurabilityEntry();
    @Expose public DeploymentEntry DEPLOYMENT = new DeploymentEntry();
    @Expose public LinkedHashMap<String, JobEntry> JOBS = new LinkedHashMap<>();
    @Expose public WavesEntry WAVES = new WavesEntry();
    @Expose public CellsEntry CELLS = new CellsEntry();
    @Expose public SitesEntry SITES = new SitesEntry();
    @Expose public GreedInfuserEntry GREED_INFUSER = new GreedInfuserEntry();
    @Expose public FluidPayoutEntry FLUID_PAYOUT = new FluidPayoutEntry();
    @Expose public AncientCrystalEntry ANCIENT_CRYSTAL = new AncientCrystalEntry();

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
        GREED_INFUSER = new GreedInfuserEntry();
        FLUID_PAYOUT = new FluidPayoutEntry();
        ANCIENT_CRYSTAL = new AncientCrystalEntry();
        JOBS = new LinkedHashMap<>();

        JobEntry arcane = new JobEntry(50, 0, "standard", 2400);
        arcane.payout = PayoutEntry.item("woldsvaults:arcane_essence", 50, 100, "linear", 50, "sqrt");
        JOBS.put("arcane_essence", arcane);

        JobEntry maps = new JobEntry(50, 3, "laser", 3600);
        maps.timePowerReference = 75;
        maps.payout = PayoutEntry.of("maps", 2, 4, "sqrt", 50, "sqrt");
        maps.payout.rarityWeights.put("rare", 70.0F);
        maps.payout.rarityWeights.put("epic", 20.0F);
        maps.payout.rarityWeights.put("omega", 10.0F);
        maps.payout.rarityPerExtraPower.put("epic", 0.2F);
        maps.payout.rarityPerExtraPowerSquared.put("omega", 0.002F);
        maps.payout.itemRarityDampedKey = "rare";
        maps.payout.itemRarityDampK = 500.0F;
        maps.payout.tierWindows.add(new TierWindow(0, 1, 3));
        maps.payout.tierWindows.add(new TierWindow(100, 2, 4));
        maps.payout.tierWindows.add(new TierWindow(175, 3, 5));
        maps.payout.tierWindows.add(new TierWindow(300, 4, 6));
        JOBS.put("maps", maps);

        JobEntry cards = new JobEntry(75, 4, "greed_infuser", 2400);
        cards.requiresArming = true;
        cards.timePowerRoot = 4.0F;
        cards.timeShrinksWithPower = true;
        cards.payout = PayoutEntry.of("cards", 3, 3, "none", 75, "none");
        JOBS.put("greed_card_upgrade", cards);

        JobEntry charms = new JobEntry(75, 4, "archeology", 4800);
        charms.payout = PayoutEntry.of("charms", 3, 4, "sqrt", 75, "sqrt");
        charms.payout.rarityWeights.put("omega", 70.0F);
        charms.payout.rarityWeights.put("mythic", 30.0F);
        charms.payout.rarityPerExtraPowerSquared.put("mythic", 0.0004F);
        charms.payout.itemRarityDampedKey = "omega";
        charms.payout.itemRarityDampK = 1000.0F;
        JOBS.put("mythic_charms", charms);

        JobEntry uniques = new JobEntry(100, 7, "excavator", 3600);
        uniques.payout = PayoutEntry.of("uniques", 2, 3, "sqrt", 100, "cbrt");
        uniques.payout.rarityWeights.put("unique", 95.0F);
        uniques.payout.rarityWeights.put("ancient", 5.0F);
        uniques.payout.rarityPerExtraPower.put("ancient", 0.06F);
        uniques.payout.itemRarityDampedKey = "unique";
        uniques.payout.itemRarityDampK = 1000.0F;
        JOBS.put("ancient_uniques", uniques);

        JobEntry sealing = new JobEntry(125, 5, "refiner", 6000);
        sealing.payout = PayoutEntry.of("seal", 1, 1, "none", 125, "none");
        JOBS.put("power_sealing", sealing);

        JobEntry packs = new JobEntry(125, 4, "heavy", 3600);
        packs.payout = PayoutEntry.item("the_vault:booster_pack", 3, 5, "sqrt", 75, "sqrt");
        packs.payout.itemNbtId = "the_vault:shiny_pack";
        JOBS.put("shiny_card_packs", packs);

        JobEntry juice = new JobEntry(175, 10, "juicer", 4800);
        juice.payout = PayoutEntry.item("the_vault:card_juice", 50, 100, "sqrt", 100, "sqrt");
        JOBS.put("card_juice", juice);

        JobEntry glue = new JobEntry(200, 8, "glue_derrick", 4800);
        glue.payout = PayoutEntry.of("fluid", 100, 300, "sqrt", 200, "sqrt");
        glue.payout.item = "woldsvaults:prismatic_glue";
        JOBS.put("prismatic_glue", glue);

        JobEntry crystals = new JobEntry(350, 10, "crystal", 6000);
        crystals.payout = PayoutEntry.of("crystals", 2, 2, "none", 350, "none");
        JOBS.put("ancient_crystals", crystals);

        JobEntry nullite = new JobEntry(500, 13, "containment", 12000);
        nullite.speedK = 500.0F;
        nullite.speedBase = 400.0F;
        nullite.payout = PayoutEntry.item("woldsvaults:nullite_crystal", 12, 24, "linear", 500, "sqrt");
        JOBS.put("nullite", nullite);
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

    /**
     * Passive drilling wear is {@code wearCoefficient x max(1, Power)^wearPowerExponent} per second,
     * so a stronger rig burns its hull faster and a series' durability edge shows through. Installed
     * attachments wear from their own pools at {@code attachmentDrainPerSecondMining} instead.
     */
    public static class DurabilityEntry {
        @Expose public float wearCoefficient = 1.0F;
        @Expose public float wearPowerExponent = 0.65F;
        @Expose public float attachmentDrainPerSecondMining = 20.0F;
        @Expose public float destructionLossFraction = 0.45F;
        @Expose public float hpDamagePerDurabilityPoint = 2.0F;
    }

    public static class DeploymentEntry {
        @Expose public float initialEnergyFraction = 0.4F;
        @Expose public float healFractionOnJobComplete = 0.33F;
    }

    /**
     * One extraction job. Duration = baseDurationTicks × (TP / timePowerReference)^(±1/timePowerRoot)
     * × speedK / (speedBase + speed); payouts follow the {@link PayoutEntry}.
     */
    public static class JobEntry {
        @Expose public int minPower;
        @Expose public int minRank;
        @Expose public String drillClass = "";
        @Expose public boolean requiresArming;
        @Expose public int baseDurationTicks;
        @Expose public float timePowerRoot = 3.0F;
        @Expose public float timePowerReference;
        @Expose public boolean timeShrinksWithPower;
        @Expose public float speedK = 300.0F;
        @Expose public float speedBase = 200.0F;
        @Expose public PayoutEntry payout = new PayoutEntry();

        public JobEntry() {
        }

        public JobEntry(int minPower, int minRank, String drillClass, int baseDurationTicks) {
            this.minPower = minPower;
            this.minRank = minRank;
            this.drillClass = drillClass;
            this.baseDurationTicks = baseDurationTicks;
        }

        public int durationTicks(int totalPower, int speed) {
            float reference = this.timePowerReference > 0.0F ? this.timePowerReference : Math.max(1, this.minPower);
            double powerTerm = 1.0D;
            if (this.timePowerRoot > 0.0F && totalPower > 0) {
                double exponent = (this.timeShrinksWithPower ? -1.0D : 1.0D) / this.timePowerRoot;
                powerTerm = Math.pow(Math.max(totalPower, 1) / (double) reference, exponent);
            }
            double speedTerm = this.speedK <= 0.0F ? 1.0D : this.speedK / (this.speedBase + Math.max(0, speed));
            return Math.max(20, (int) Math.round(this.baseDurationTicks * powerTerm * speedTerm));
        }
    }

    public static class TierWindow {
        @Expose public int minPower;
        @Expose public int tierMin;
        @Expose public int tierMax;

        public TierWindow() {
        }

        public TierWindow(int minPower, int tierMin, int tierMax) {
            this.minPower = minPower;
            this.tierMin = tierMin;
            this.tierMax = tierMax;
        }
    }

    /**
     * Payout shape. count = uniform[countMin, countMax] × powerScaling(TP / powerReference) × quantScaling(1 + quant / 100);
     * rarity weights shift by extra power (TP − minPower) and the item-rarity stat damps one key by K / (K + IR).
     */
    public static class PayoutEntry {
        @Expose public String type = "item";
        @Expose public String item = "";
        @Expose public String itemNbtId = "";
        @Expose public float countMin;
        @Expose public float countMax;
        @Expose public String powerScaling = "linear";
        @Expose public float powerReference;
        @Expose public String quantScaling = "sqrt";
        @Expose public LinkedHashMap<String, Float> rarityWeights = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Float> rarityPerExtraPower = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Float> rarityPerExtraPowerSquared = new LinkedHashMap<>();
        @Expose public String itemRarityDampedKey = "";
        @Expose public float itemRarityDampK = 1000.0F;
        @Expose public List<TierWindow> tierWindows = new ArrayList<>();

        public PayoutEntry() {
        }

        static PayoutEntry of(String type, float countMin, float countMax, String powerScaling, float powerReference, String quantScaling) {
            PayoutEntry entry = new PayoutEntry();
            entry.type = type;
            entry.countMin = countMin;
            entry.countMax = countMax;
            entry.powerScaling = powerScaling;
            entry.powerReference = powerReference;
            entry.quantScaling = quantScaling;
            return entry;
        }

        static PayoutEntry item(String item, float countMin, float countMax, String powerScaling, float powerReference, String quantScaling) {
            PayoutEntry entry = of("item", countMin, countMax, powerScaling, powerReference, quantScaling);
            entry.item = item;
            return entry;
        }
    }

    public static class Count {
        @Expose public int min;
        @Expose public int max;

        public Count() {
        }

        public Count(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public static Count of(int fixed) {
            return new Count(fixed, fixed);
        }

        public int roll(Random random) {
            if (this.max <= this.min) {
                return Math.max(0, this.min);
            }
            return this.min + random.nextInt(this.max - this.min + 1);
        }
    }

    /** One wave on its own cooldown; every class count it does not use stays at zero. */
    public static class TimedWaveEntry {
        @Expose public int intervalTicks;
        @Expose public Count horde = new Count(0, 0);
        @Expose public Count assassins = new Count(0, 0);
        @Expose public Count tanks = new Count(0, 0);
        @Expose public Count elites = new Count(0, 0);
        @Expose public Count bosses = new Count(0, 0);

        public TimedWaveEntry() {
        }

        public TimedWaveEntry(int intervalTicks) {
            this.intervalTicks = intervalTicks;
        }

        static TimedWaveEntry horde(int intervalTicks, int min, int max) {
            TimedWaveEntry entry = new TimedWaveEntry(intervalTicks);
            entry.horde = new Count(min, max);
            return entry;
        }

        static TimedWaveEntry assassins(int intervalTicks, int count) {
            TimedWaveEntry entry = new TimedWaveEntry(intervalTicks);
            entry.assassins = Count.of(count);
            return entry;
        }

        static TimedWaveEntry mixed(int intervalTicks, int horde, int assassins, int tanks, int elites, int bosses) {
            TimedWaveEntry entry = new TimedWaveEntry(intervalTicks);
            entry.horde = Count.of(horde);
            entry.assassins = Count.of(assassins);
            entry.tanks = Count.of(tanks);
            entry.elites = Count.of(elites);
            entry.bosses = Count.of(bosses);
            return entry;
        }
    }

    /**
     * The wave table one site runs, picked once when its extractor starts drilling: the highest tier whose
     * {@code minPower} the extractor's power reaches. Pools are per tier so composition can shift with power.
     */
    public static class TierEntry {
        @Expose public String name = "";
        @Expose public int minPower;
        @Expose public TimedWaveEntry trickle = new TimedWaveEntry();
        @Expose public TimedWaveEntry horde = new TimedWaveEntry();
        @Expose public TimedWaveEntry combat = new TimedWaveEntry();
        @Expose public TimedWaveEntry assault = new TimedWaveEntry();
        @Expose public TimedWaveEntry siege = new TimedWaveEntry();
        @Expose public WeightedList<ResourceLocation> hordePool = new WeightedList<>();
        @Expose public WeightedList<ResourceLocation> assassinPool = new WeightedList<>();
        @Expose public WeightedList<ResourceLocation> tankPool = new WeightedList<>();
        @Expose public WeightedList<ResourceLocation> elitePool = new WeightedList<>();

        public TierEntry() {
        }

        public TierEntry(String name, int minPower) {
            this.name = name;
            this.minPower = minPower;
        }
    }

    public static class FloorAnchor {
        @Expose public int power;
        @Expose public int medallionRank;

        public FloorAnchor() {
        }

        public FloorAnchor(int power, int medallionRank) {
            this.power = power;
            this.medallionRank = medallionRank;
        }
    }

    /**
     * Wave mobs are raised to the health and damage they would have under the medallion rank a site's extractor
     * power maps to, interpolated exponentially between anchors and flat past the last one; a vault whose own
     * medallion already meets the floor is left alone.
     */
    public static class StatFloorEntry {
        @Expose public boolean enabled = true;
        @Expose public List<FloorAnchor> anchors = new ArrayList<>();
    }

    public static class BossEntry {
        @Expose public WeightedList<ResourceLocation> pool = new WeightedList<>();
        @Expose public float speedMultiplier = 0.65F;
        @Expose public float knockbackResistance = 1.0F;
        @Expose public int infernalModifierCount = 6;
        @Expose public int spawnSearchRadius = 5;
    }

    public static class EliteEntry {
        @Expose public float knockbackResistance = 1.0F;
    }

    public static class CarrierEntry {
        @Expose public String baseEntity = "the_vault:blood_tank";
        @Expose public int intervalTicks = 400;
        @Expose public float healthMultiplier = 2.0F;
        @Expose public float damageMultiplier = 1.5F;
        @Expose public int auraRadius = 8;
        @Expose public int auraIntervalTicks = 100;
        @Expose public int auraEffectDurationTicks = 120;
    }

    public static class WavesEntry {
        @Expose public int liveMobCapPerSite = 1000;
        @Expose public StatFloorEntry statFloor = new StatFloorEntry();
        @Expose public BossEntry boss = new BossEntry();
        @Expose public EliteEntry elite = new EliteEntry();
        @Expose public CarrierEntry carrier = new CarrierEntry();
        @Expose public List<TierEntry> tiers = new ArrayList<>();

        public WavesEntry() {
            statFloor.anchors.add(new FloorAnchor(0, 4));
            statFloor.anchors.add(new FloorAnchor(110, 8));
            statFloor.anchors.add(new FloorAnchor(200, 11));
            statFloor.anchors.add(new FloorAnchor(420, 13));
            statFloor.anchors.add(new FloorAnchor(700, 15));

            boss.pool.add(vh("elite_zombie"), 20);
            boss.pool.add(vh("elite_husk"), 20);
            boss.pool.add(vh("elite_skeleton"), 20);
            boss.pool.add(vh("elite_stray"), 20);
            boss.pool.add(vh("elite_drowned"), 20);
            boss.pool.add(vh("elite_wither_skeleton"), 20);
            boss.pool.add(vh("elite_witch"), 20);
            boss.pool.add(vh("boogieman"), 10);
            boss.pool.add(vh("blue_blaze"), 10);

            TierEntry salvage = new TierEntry("salvage", 0);
            salvage.trickle = TimedWaveEntry.horde(80, 2, 3);
            salvage.horde = TimedWaveEntry.horde(200, 8, 12);
            salvage.combat = TimedWaveEntry.assassins(320, 3);
            salvage.assault = TimedWaveEntry.mixed(500, 0, 6, 1, 0, 0);
            salvage.siege = TimedWaveEntry.mixed(1400, 12, 5, 2, 0, 0);
            salvage.hordePool.add(new ResourceLocation("minecraft", "zombie"), 3);
            salvage.hordePool.add(vh("t1_zombie"), 3);
            salvage.hordePool.add(vh("t1_husk"), 2);
            salvage.hordePool.add(vh("t1_drowned"), 2);
            salvage.hordePool.add(vh("t2_zombie"), 2);
            salvage.hordePool.add(vh("blood_horde_t1"), 2);
            salvage.hordePool.add(vh("miner_zombie_t1"), 1);
            salvage.assassinPool.add(vh("t1_skeleton"), 3);
            salvage.assassinPool.add(vh("vault_spider"), 3);
            salvage.assassinPool.add(vh("t1_stray"), 2);
            salvage.assassinPool.add(vh("t1_cave_skeleton"), 1);
            salvage.assassinPool.add(vh("blood_skeleton_t1"), 1);
            salvage.tankPool.add(vh("blood_tank"), 3);
            salvage.tankPool.add(vh("overgrown_tank"), 2);
            salvage.tankPool.add(vh("t1_wither_skeleton"), 1);
            addElitePool(salvage);
            tiers.add(salvage);

            TierEntry industrial = new TierEntry("industrial", 110);
            industrial.trickle = TimedWaveEntry.horde(70, 2, 3);
            industrial.horde = TimedWaveEntry.horde(180, 8, 12);
            industrial.combat = TimedWaveEntry.assassins(300, 4);
            industrial.assault = TimedWaveEntry.mixed(450, 0, 7, 1, 0, 0);
            industrial.siege = TimedWaveEntry.mixed(1300, 13, 6, 2, 0, 0);
            industrial.hordePool.add(vh("t2_zombie"), 3);
            industrial.hordePool.add(vh("t2_husk"), 3);
            industrial.hordePool.add(vh("t2_drowned"), 3);
            industrial.hordePool.add(vh("blood_horde_t2"), 2);
            industrial.hordePool.add(vh("miner_zombie_t2"), 2);
            industrial.hordePool.add(vh("overgrown_zombie_t2"), 1);
            industrial.hordePool.add(vh("skeleton_pirate_t2"), 1);
            industrial.assassinPool.add(vh("t2_skeleton"), 3);
            industrial.assassinPool.add(vh("vault_spider"), 3);
            industrial.assassinPool.add(vh("t2_stray"), 2);
            industrial.assassinPool.add(vh("blood_skeleton_t2"), 2);
            industrial.assassinPool.add(new ResourceLocation("quark", "wraith"), 1);
            industrial.tankPool.add(vh("blood_tank"), 3);
            industrial.tankPool.add(vh("overgrown_tank"), 2);
            industrial.tankPool.add(vh("t2_wither_skeleton"), 1);
            addElitePool(industrial);
            tiers.add(industrial);

            TierEntry fortified = new TierEntry("fortified", 200);
            fortified.trickle = TimedWaveEntry.horde(60, 2, 4);
            fortified.horde = TimedWaveEntry.horde(160, 9, 13);
            fortified.combat = TimedWaveEntry.assassins(280, 5);
            fortified.assault = TimedWaveEntry.mixed(400, 0, 8, 1, 0, 0);
            fortified.siege = TimedWaveEntry.mixed(1200, 15, 8, 3, 1, 0);
            fortified.hordePool.add(vh("t2_zombie"), 2);
            fortified.hordePool.add(vh("t2_husk"), 2);
            fortified.hordePool.add(vh("t2_drowned"), 2);
            fortified.hordePool.add(vh("blood_horde_t3"), 3);
            fortified.hordePool.add(vh("miner_zombie_t3"), 2);
            fortified.hordePool.add(vh("overgrown_zombie_t3"), 2);
            fortified.hordePool.add(vh("winterwalker_t3"), 1);
            fortified.hordePool.add(vh("mummy_t1"), 1);
            fortified.assassinPool.add(vh("t2_skeleton"), 3);
            fortified.assassinPool.add(vh("vault_spider"), 2);
            fortified.assassinPool.add(vh("t2_stray"), 2);
            fortified.assassinPool.add(vh("blood_skeleton_t3"), 2);
            fortified.assassinPool.add(vh("t2_cave_skeleton"), 1);
            fortified.assassinPool.add(vh("vault_wraith_white"), 1);
            fortified.assassinPool.add(new ResourceLocation("quark", "wraith"), 1);
            fortified.tankPool.add(vh("blood_tank"), 3);
            fortified.tankPool.add(vh("overgrown_tank"), 2);
            fortified.tankPool.add(vh("t2_wither_skeleton"), 1);
            fortified.tankPool.add(vh("plastic_tank"), 1);
            addElitePool(fortified);
            tiers.add(fortified);

            TierEntry siege = new TierEntry("siege", 420);
            siege.trickle = TimedWaveEntry.horde(50, 3, 4);
            siege.horde = TimedWaveEntry.horde(140, 10, 14);
            siege.combat = TimedWaveEntry.assassins(260, 5);
            siege.assault = TimedWaveEntry.mixed(350, 0, 9, 2, 1, 0);
            siege.siege = TimedWaveEntry.mixed(1100, 18, 9, 4, 1, 1);
            siege.hordePool.add(vh("t3_zombie"), 3);
            siege.hordePool.add(vh("t3_husk"), 3);
            siege.hordePool.add(vh("t3_drowned"), 2);
            siege.hordePool.add(vh("blood_horde_t4"), 3);
            siege.hordePool.add(vh("miner_zombie_t4"), 2);
            siege.hordePool.add(vh("overgrown_zombie_t4"), 2);
            siege.hordePool.add(vh("winterwalker_t4"), 1);
            siege.hordePool.add(vh("mummy_t2"), 1);
            siege.hordePool.add(vh("skeleton_pirate_t4"), 1);
            siege.assassinPool.add(vh("t3_skeleton"), 3);
            siege.assassinPool.add(vh("t3_stray"), 2);
            siege.assassinPool.add(vh("blood_skeleton_t4"), 2);
            siege.assassinPool.add(vh("t3_cave_skeleton"), 2);
            siege.assassinPool.add(vh("vault_wraith_yellow"), 1);
            siege.assassinPool.add(vh("deep_dark_skeleton"), 1);
            siege.tankPool.add(vh("blood_tank"), 2);
            siege.tankPool.add(vh("overgrown_tank"), 2);
            siege.tankPool.add(vh("t3_wither_skeleton"), 2);
            siege.tankPool.add(vh("pirate_guardian_tank"), 1);
            addElitePool(siege);
            tiers.add(siege);

            TierEntry cataclysm = new TierEntry("cataclysm", 700);
            cataclysm.trickle = TimedWaveEntry.horde(40, 3, 4);
            cataclysm.horde = TimedWaveEntry.horde(120, 10, 15);
            cataclysm.combat = TimedWaveEntry.assassins(240, 6);
            cataclysm.assault = TimedWaveEntry.mixed(300, 0, 10, 2, 1, 0);
            cataclysm.siege = TimedWaveEntry.mixed(1000, 20, 10, 5, 1, 1);
            cataclysm.hordePool.add(vh("t3_zombie"), 3);
            cataclysm.hordePool.add(vh("t3_husk"), 3);
            cataclysm.hordePool.add(vh("t3_drowned"), 2);
            cataclysm.hordePool.add(vh("blood_horde_t5"), 3);
            cataclysm.hordePool.add(vh("miner_zombie_t5"), 2);
            cataclysm.hordePool.add(vh("overgrown_zombie_t5"), 2);
            cataclysm.hordePool.add(vh("winterwalker_t5"), 1);
            cataclysm.hordePool.add(vh("mummy_t2"), 1);
            cataclysm.hordePool.add(vh("skeleton_pirate_t5"), 1);
            cataclysm.assassinPool.add(vh("t3_skeleton"), 3);
            cataclysm.assassinPool.add(vh("t3_stray"), 2);
            cataclysm.assassinPool.add(vh("blood_skeleton_t5"), 3);
            cataclysm.assassinPool.add(vh("t4_cave_skeleton"), 2);
            cataclysm.assassinPool.add(vh("vault_wraith_yellow"), 1);
            cataclysm.assassinPool.add(vh("deep_dark_skeleton"), 1);
            cataclysm.tankPool.add(vh("blood_tank"), 2);
            cataclysm.tankPool.add(vh("overgrown_tank"), 2);
            cataclysm.tankPool.add(vh("t3_wither_skeleton"), 2);
            cataclysm.tankPool.add(vh("pirate_guardian_tank"), 2);
            addElitePool(cataclysm);
            tiers.add(cataclysm);
        }

        private static void addElitePool(TierEntry tier) {
            tier.elitePool.add(vh("elite_zombie"), 3);
            tier.elitePool.add(vh("elite_husk"), 3);
            tier.elitePool.add(vh("elite_skeleton"), 3);
            tier.elitePool.add(vh("elite_stray"), 2);
            tier.elitePool.add(vh("elite_drowned"), 2);
            tier.elitePool.add(vh("elite_wither_skeleton"), 2);
            tier.elitePool.add(vh("vault_fighter_4"), 2);
            tier.elitePool.add(vh("bruiser_guardian"), 1);
            tier.elitePool.add(vh("arbalist_guardian"), 1);
        }

        private static ResourceLocation vh(String path) {
            return new ResourceLocation("the_vault", path);
        }

        /** The highest tier whose {@code minPower} the given power reaches, or null when no tier is configured. */
        public TierEntry tierFor(int power) {
            TierEntry best = null;
            for (TierEntry tier : this.tiers) {
                if (tier.minPower <= power && (best == null || tier.minPower >= best.minPower)) {
                    best = tier;
                }
            }
            return best;
        }
    }

    public static class CellsEntry {
        @Expose public double hordeMobDropChance = 0.001D;
        @Expose public double assassinMobDropChance = 0.01D;
        @Expose public double eliteMobDropChance = 0.05D;
        @Expose public double bossMobDropChance = 1.0D;
        @Expose public double otherMobDropChance = 0.001D;
        @Expose public double chestDropChance = 0.01D;
    }

    public static class SitesEntry {
        @Expose public int count = 3;
        @Expose public int ringMinRooms = 4;
        @Expose public int ringMaxRooms = 9;
        @Expose public int minSeparationRooms = 3;
    }

    /**
     * The Greed Infuser's card slots and class ladder. Slots = {@code baseSlots} + one per {@code powerPerExtraSlot}
     * above the job's minimum power, capped at {@code maxSlots}. A card's class is found in the card modifier pool
     * {@code cardPoolPrefix + class}; the placer's class is the highest one whose greed trader pool
     * {@code traderPoolPrefix + class} their greed rank can see. {@code classOrder} runs weakest to strongest.
     */
    public static class GreedInfuserEntry {
        public static final int SLOT_LIMIT = 27;

        @Expose public int baseSlots = 3;
        @Expose public int powerPerExtraSlot = 100;
        @Expose public int maxSlots = 18;
        @Expose public List<String> classOrder = new ArrayList<>(List.of("weak", "medium", "strong", "elite"));
        @Expose public String cardPoolPrefix = "greed_";
        @Expose public String legacyCardPool = "greed";
        @Expose public String traderPoolPrefix = "greed_cards_";

        public int slotsFor(int power, int minPower) {
            int step = Math.max(1, this.powerPerExtraSlot);
            int slots = this.baseSlots + Math.max(0, power - minPower) / step;
            return Math.max(1, Math.min(Math.min(this.maxSlots, SLOT_LIMIT), slots));
        }
    }

    /** A finished fluid job keeps its fluid on board; while any is left the rig refuses pickup for this long. */
    public static class FluidPayoutEntry {
        @Expose public int pickupLockSeconds = 45;
    }

    /** One weighted source of crystal modifiers: a vault modifier pool ({@code "vault"}) or a greed trader modifier list ({@code "greed_trader"}). */
    public static class ModifierSource {
        @Expose public String type = "vault";
        @Expose public String pool = "";
        @Expose public float weight = 1.0F;

        public ModifierSource() {
        }

        public ModifierSource(String type, String pool, float weight) {
            this.type = type;
            this.pool = pool;
            this.weight = weight;
        }
    }

    public static class WeightedId {
        @Expose public String id = "";
        @Expose public float weight = 1.0F;

        public WeightedId() {
        }

        public WeightedId(String id, float weight) {
            this.id = id;
            this.weight = weight;
        }
    }

    /**
     * The Ancient Crystal payout. {@code crystalRolls} tries at {@code crystalChance}, plus one try per
     * {@code quantityPerExtraRoll} quantity (the remainder is a partial try). Each crystal gets the strong rolls, then
     * {@code randomRolls} + power / {@code powerPerRandomRoll} + rarity / {@code rarityPerRandomRoll} random rolls that
     * are positive with weight {@code positiveWeight} + {@code positiveWeightPerRarity} x rarity against
     * {@code negativeWeight}. Every roll picks one source by weight, then one modifier from it. Capacity is
     * {@code capacityMin..capacityMax} + power / {@code powerPerCapacity} + rarity / {@code rarityPerCapacity} plus the
     * placer's own crystal-capacity bonuses. The objective comes from {@code objectivePool} at the vault's level minus
     * {@code excludedObjectiveTypes}; the sigil is drawn from {@code sigils}.
     */
    public static class AncientCrystalEntry {
        @Expose public int crystalRolls = 2;
        @Expose public float crystalChance = 0.25F;
        @Expose public float quantityPerExtraRoll = 250.0F;
        @Expose public int strongNegativeRolls = 10;
        @Expose public int strongPositiveRolls = 10;
        @Expose public int randomRolls = 15;
        @Expose public float powerPerRandomRoll = 30.0F;
        @Expose public float rarityPerRandomRoll = 50.0F;
        @Expose public float positiveWeight = 60.0F;
        @Expose public float negativeWeight = 40.0F;
        @Expose public float positiveWeightPerRarity = 0.012F;
        @Expose public int capacityMin = 140;
        @Expose public int capacityMax = 175;
        @Expose public float powerPerCapacity = 10.0F;
        @Expose public float rarityPerCapacity = 10.0F;
        @Expose public boolean vaultRandomModifiers = false;
        @Expose public List<ModifierSource> strongPositive = new ArrayList<>();
        @Expose public List<ModifierSource> strongNegative = new ArrayList<>();
        @Expose public List<ModifierSource> randomPositive = new ArrayList<>();
        @Expose public List<ModifierSource> randomNegative = new ArrayList<>();
        @Expose public String objectivePool = "the_vault:default";
        @Expose public List<String> excludedObjectiveTypes = new ArrayList<>(List.of("monolith"));
        @Expose public List<WeightedId> sigils = new ArrayList<>();

        public AncientCrystalEntry() {
            this.strongPositive.add(new ModifierSource("greed_trader", "normal_modifiers", 14.0F));
            this.strongPositive.add(new ModifierSource("greed_trader", "treasure_modifiers", 1.5F));
            this.strongPositive.add(new ModifierSource("greed_trader", "temporal_modifiers", 1.0F));
            this.strongNegative.add(new ModifierSource("vault", "woldsvaults:concealed_chaos_backfire", 1.0F));
            this.randomPositive.add(new ModifierSource("greed_trader", "normal_modifiers", 14.0F));
            this.randomPositive.add(new ModifierSource("greed_trader", "treasure_modifiers", 1.5F));
            this.randomPositive.add(new ModifierSource("greed_trader", "temporal_modifiers", 1.0F));
            this.randomPositive.add(new ModifierSource("vault", "the_vault:omega_positive", 16.5F));
            this.randomNegative.add(new ModifierSource("vault", "woldsvaults:concealed_chaos_backfire", 1.0F));
            this.randomNegative.add(new ModifierSource("vault", "the_vault:alchemy_strong_negative", 1.0F));
            for (String step : List.of("70", "75", "80", "85", "90", "95", "100")) {
                this.sigils.add(new WeightedId("ancient_" + step, 1.0F));
            }
        }
    }
}
