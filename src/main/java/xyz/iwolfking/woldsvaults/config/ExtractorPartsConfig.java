package xyz.iwolfking.woldsvaults.config;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.Config;
import iskallia.vault.gear.VaultGearRarity;
import xyz.iwolfking.woldsvaults.extraction.parts.ExtractorPartType;
import xyz.iwolfking.woldsvaults.extraction.parts.ExtractorStat;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every number behind extractor parts, blueprints and assembly: series, per-series stat ranges,
 * attachments, affix pools, the quality curve and the stat scaling families. Defaults mirror the
 * "Extractor Parts and Rolls" workbook tab. Crafting and repair costs are deliberately absent.
 */
public class ExtractorPartsConfig extends Config {
    public static final String SERIES_WOODEN = "wooden";
    public static final String SERIES_CHROMATIC = "chromatic";
    public static final String SERIES_EXPERIMENTAL = "experimental";
    public static final String SERIES_ARMORED = "armored";
    public static final String SERIES_ECHOSTEEL = "echosteel";

    @Expose public LinkedHashMap<String, SeriesEntry> SERIES = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, List<String>> SERIES_BY_RARITY = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, Integer> RARITY_DROP_WEIGHTS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, LinkedHashMap<String, PartRollEntry>> PARTS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, DrillClassEntry> DRILL_CLASSES = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, AttachmentEntry> ATTACHMENTS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, List<AffixEntry>> AFFIX_POOLS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, Integer> AFFIX_COUNT = new LinkedHashMap<>();
    @Expose public QualityEntry QUALITY = new QualityEntry();
    @Expose public ScalesEntry SCALES = new ScalesEntry();
    @Expose public SlotsEntry SLOTS = new SlotsEntry();
    @Expose public float CELL_RESTORE_BASE = 200.0F;

    @Override
    public String getName() {
        return "extractor_parts";
    }

    public static String rarityKey(VaultGearRarity rarity) {
        return rarity.name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static VaultGearRarity rarityOf(String key) {
        if (key == null) {
            return null;
        }
        for (VaultGearRarity rarity : VaultGearRarity.values()) {
            if (rarity.name().equalsIgnoreCase(key)) {
                return rarity;
            }
        }
        return null;
    }

    @Nullable
    public SeriesEntry getSeries(String id) {
        return SERIES.get(id);
    }

    public List<String> getSeriesFor(VaultGearRarity rarity) {
        List<String> allowed = SERIES_BY_RARITY.get(rarityKey(rarity));
        return allowed == null ? new ArrayList<>() : allowed;
    }

    @Nullable
    public PartRollEntry getRoll(ExtractorPartType type, String seriesOrClass) {
        LinkedHashMap<String, PartRollEntry> rolls = PARTS.get(type.getId());
        return rolls == null ? null : rolls.get(seriesOrClass);
    }

    public int getAffixCount(VaultGearRarity rarity) {
        return AFFIX_COUNT.getOrDefault(rarityKey(rarity), 0);
    }

    public int getRarityDropWeight(VaultGearRarity rarity) {
        return RARITY_DROP_WEIGHTS.getOrDefault(rarityKey(rarity), 0);
    }

    public static class Range {
        @Expose public float min;
        @Expose public float max;

        public Range() {
        }

        public Range(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public float lerp(float u) {
            return this.min + (this.max - this.min) * u;
        }
    }

    public static class SeriesEntry {
        @Expose public String name;
        @Expose public String rigSuffix;
        @Expose public LinkedHashMap<String, String> partNames = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, String> rigBones = new LinkedHashMap<>();

        public SeriesEntry() {
        }

        SeriesEntry(String name, String rigSuffix, String chassis, String core, String legs, String shield, String radiator, String scanner) {
            this.name = name;
            this.rigSuffix = rigSuffix;
            this.rigBones.put(ExtractorPartType.CHASSIS.getId(), chassis);
            this.rigBones.put(ExtractorPartType.POWER_CORE.getId(), core);
            this.rigBones.put(ExtractorPartType.LEGS.getId(), legs);
            this.rigBones.put(ExtractorPartType.SHIELD_ARRAY.getId(), shield);
            this.rigBones.put(ExtractorPartType.RADIATOR.getId(), radiator);
            this.rigBones.put(ExtractorPartType.SCANNER.getId(), scanner);
        }

        public String partName(ExtractorPartType type, String fallback) {
            return this.partNames.getOrDefault(type.getId(), fallback);
        }
    }

    /** One row of the workbook: flat stat ranges, multiplier ranges and fixed specials for a part in a series. */
    public static class PartRollEntry {
        @Expose public LinkedHashMap<String, Range> stats = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Range> multipliers = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Float> specials = new LinkedHashMap<>();

        public PartRollEntry() {
        }

        PartRollEntry stat(ExtractorStat stat, float min, float max) {
            this.stats.put(stat.getId(), new Range(min, max));
            return this;
        }

        PartRollEntry mult(ExtractorStat stat, float min, float max) {
            this.multipliers.put(stat.getId(), new Range(min, max));
            return this;
        }

        PartRollEntry special(ExtractorStat stat, float value) {
            this.specials.put(stat.getId(), value);
            return this;
        }
    }

    /**
     * One drill head. {@code minQuality} is the lowest blueprint quality this class can roll at: the grade where a
     * wooden rig built at that quality with average rolls reaches the job's minimum power.
     */
    public static class DrillClassEntry {
        @Expose public String name;
        @Expose public String job;
        @Expose public String rigBone;
        @Expose public String minRarity;
        @Expose public float minQuality;

        public DrillClassEntry() {
        }

        DrillClassEntry(String name, String job, String rigBone, String minRarity, float minQuality) {
            this.name = name;
            this.job = job;
            this.rigBone = rigBone;
            this.minRarity = minRarity;
            this.minQuality = minQuality;
        }
    }

    public static class EffectEntry {
        @Expose public String stat;
        @Expose public float min;
        @Expose public float max;
        @Expose public boolean percent;

        public EffectEntry() {
        }

        EffectEntry(ExtractorStat stat, float min, float max, boolean percent) {
            this.stat = stat.getId();
            this.min = min;
            this.max = max;
            this.percent = percent;
        }
    }

    public static class AttachmentEntry {
        @Expose public String name;
        @Expose public String rigBone;
        @Expose public Range drain = new Range(0.0F, 0.0F);
        @Expose public Range durability = new Range(20000.0F, 30000.0F);
        @Expose public List<EffectEntry> effects = new ArrayList<>();
        @Expose public boolean stackable = false;

        public AttachmentEntry() {
        }

        AttachmentEntry(String name, String rigBone, float drainMin, float drainMax, float duraMin, float duraMax, EffectEntry... effects) {
            this.name = name;
            this.rigBone = rigBone;
            this.drain = new Range(drainMin, drainMax);
            this.durability = new Range(duraMin, duraMax);
            this.effects.addAll(Arrays.asList(effects));
        }
    }

    public static class AffixEntry {
        @Expose public String id;
        @Expose public String stat;
        @Expose public float min;
        @Expose public float max;
        @Expose public boolean percent;

        public AffixEntry() {
        }

        AffixEntry(String id, ExtractorStat stat, float min, float max, boolean percent) {
            this.id = id;
            this.stat = stat.getId();
            this.min = min;
            this.max = max;
            this.percent = percent;
        }
    }

    public static class QualityEntry {
        @Expose public float centerA = 2.16F;
        @Expose public float centerB = 0.85F;
        @Expose public float centerIrScale = 200.0F;
        @Expose public float kneeIr = 3000.0F;
        @Expose public float kneeLogK = 21.0F;
        @Expose public LinkedHashMap<String, Float> rarityMult = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Float> rarityFlat = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Float> rarityWidth = new LinkedHashMap<>();
        @Expose public float widthBase = 0.5F;
        @Expose public float widthSlope = 0.15F;
        @Expose public float costExponent = 1.6F;
        @Expose public float costTailRate = 1.12F;
        @Expose public float rollNoise = 0.0F;
        @Expose public List<BucketEntry> buckets = new ArrayList<>();

        public QualityEntry() {
            this.rarityMult.put("scrappy", 1.00F);
            this.rarityMult.put("common", 1.03F);
            this.rarityMult.put("rare", 1.07F);
            this.rarityMult.put("epic", 1.12F);
            this.rarityMult.put("omega", 1.18F);
            this.rarityFlat.put("scrappy", 0.0F);
            this.rarityFlat.put("common", 0.0F);
            this.rarityFlat.put("rare", 1.0F);
            this.rarityFlat.put("epic", 1.0F);
            this.rarityFlat.put("omega", 2.0F);
            this.rarityWidth.put("scrappy", 1.0F);
            this.rarityWidth.put("common", 1.1F);
            this.rarityWidth.put("rare", 1.25F);
            this.rarityWidth.put("epic", 1.4F);
            this.rarityWidth.put("omega", 1.6F);
            this.buckets.add(new BucketEntry(3, "Early"));
            this.buckets.add(new BucketEntry(8, "Mid"));
            this.buckets.add(new BucketEntry(20, "Late"));
            this.buckets.add(new BucketEntry(Integer.MAX_VALUE, "Endgame"));
        }
    }

    public static class BucketEntry {
        @Expose public int maxQuality;
        @Expose public String label;

        public BucketEntry() {
        }

        BucketEntry(int maxQuality, String label) {
            this.maxQuality = maxQuality;
            this.label = label;
        }
    }

    public static class ScalesEntry {
        @Expose public LinkedHashMap<String, Float> linear = new LinkedHashMap<>();
        @Expose public LinkedHashMap<String, Float> geometric = new LinkedHashMap<>();
        @Expose public float geometricTailQuality = 28.0F;
        @Expose public float geometricTailSlope = 0.04F;

        public ScalesEntry() {
            this.linear.put(ExtractorStat.POWER.getId(), 0.25F);
            this.linear.put(ExtractorStat.SPEED.getId(), 0.12F);
            this.linear.put(ExtractorStat.ENERGY_CAPACITY.getId(), 0.15F);
            this.linear.put(ExtractorStat.DURABILITY.getId(), 0.15F);
            this.linear.put(ExtractorStat.ARMOR.getId(), 0.10F);
            this.linear.put(ExtractorStat.SHIELD_TOUGHNESS.getId(), 0.10F);
            this.linear.put(ExtractorStat.QUANTITY.getId(), 0.10F);
            this.linear.put(ExtractorStat.RARITY.getId(), 0.10F);
            this.linear.put(ExtractorStat.DRAIN.getId(), 0.10F);
            this.linear.put(ExtractorStat.ENERGY_PER_CELL.getId(), 0.10F);
            this.linear.put(ExtractorStat.HEALTH_REGEN.getId(), 0.15F);
            this.geometric.put(ExtractorStat.HEALTH.getId(), 0.15F);
            this.geometric.put(ExtractorStat.SHIELD.getId(), 0.15F);
            this.geometric.put(ExtractorStat.SHIELD_REGEN.getId(), 0.15F);
        }
    }

    public static class SlotsEntry {
        @Expose public LinkedHashMap<String, Integer> chassis = new LinkedHashMap<>();
        @Expose public int omegaCoreBonus = 1;
        @Expose public int max = 3;

        public SlotsEntry() {
            this.chassis.put("scrappy", 0);
            this.chassis.put("common", 1);
            this.chassis.put("rare", 1);
            this.chassis.put("epic", 2);
            this.chassis.put("omega", 2);
        }
    }

    @Override
    protected void reset() {
        SERIES = new LinkedHashMap<>();
        SERIES_BY_RARITY = new LinkedHashMap<>();
        RARITY_DROP_WEIGHTS = new LinkedHashMap<>();
        PARTS = new LinkedHashMap<>();
        DRILL_CLASSES = new LinkedHashMap<>();
        ATTACHMENTS = new LinkedHashMap<>();
        AFFIX_POOLS = new LinkedHashMap<>();
        AFFIX_COUNT = new LinkedHashMap<>();
        QUALITY = new QualityEntry();
        SCALES = new ScalesEntry();
        SLOTS = new SlotsEntry();
        CELL_RESTORE_BASE = 200.0F;

        SeriesEntry wooden = new SeriesEntry("Wooden", "s1",
                "chassis_s1_wooden", "core_s1_combustion", "legs_s1_wooden", "shield_s1_projector", "radiator_s1_grating", "scanner_s1_probe");
        SeriesEntry chromatic = new SeriesEntry("Chromatic Steel", "s2",
                "chassis_s2_chromatic", "core_s2_turbine", "legs_s2_chromatic", "shield_s2_barrier", "radiator_s2_silkvent", "scanner_s2_calibrator");
        SeriesEntry experimental = new SeriesEntry("Experimental", "s3",
                "chassis_s3_suspension", "core_s3_fusion", "legs_s3_suspension", "shield_s3_voidbender", "radiator_s3_painite", "scanner_s3_wutodite");
        SeriesEntry armored = new SeriesEntry("Armored", "s4",
                "chassis_s4_armored", "core_s4_etg", "legs_s4_armored", "shield_s4_impact", "radiator_s4_heatsink", "scanner_s4_vaulterite");
        SeriesEntry echosteel = new SeriesEntry("Echosteel", "s5",
                "chassis_s5_echosteel", "core_s5_echofluid", "legs_s5_echosteel", "shield_s5_echoflare", "radiator_s5_evaporator", "scanner_s5_echolocator");
        names(wooden, "Wooden Chassis", "Combustion Engine", "Wooden Supports", "Shield Projector", "Iron Grating", "Radio Probe");
        names(chromatic, "Chromatic Steel Chassis", "Essence Turbine", "Chromatic Steel Legs", "Essence Barrier", "Silk Vents", "Chromatic Calibrator");
        names(experimental, "Suspension Chassis", "Fusion Core", "Suspension Rig", "Void Bender", "Liquid Painite Cycler", "Quantum Wutodite Sensor");
        names(armored, "Armored Chassis", "Echoreactive Thermoisotope Generator", "Armored Legs", "Impact Shield", "Heat Sink", "Vaulterite Detector");
        names(echosteel, "Echosteel Chassis", "Echofluid Reactor", "Echosteel Legs", "Echoflare Deflector", "Echo Evaporator", "Echolocator");
        SERIES.put(SERIES_WOODEN, wooden);
        SERIES.put(SERIES_CHROMATIC, chromatic);
        SERIES.put(SERIES_EXPERIMENTAL, experimental);
        SERIES.put(SERIES_ARMORED, armored);
        SERIES.put(SERIES_ECHOSTEEL, echosteel);

        SERIES_BY_RARITY.put("scrappy", new ArrayList<>(List.of(SERIES_WOODEN, SERIES_CHROMATIC)));
        SERIES_BY_RARITY.put("common", new ArrayList<>(List.of(SERIES_WOODEN, SERIES_CHROMATIC, SERIES_ARMORED)));
        SERIES_BY_RARITY.put("rare", new ArrayList<>(List.of(SERIES_WOODEN, SERIES_CHROMATIC, SERIES_ARMORED, SERIES_EXPERIMENTAL)));
        SERIES_BY_RARITY.put("epic", new ArrayList<>(List.of(SERIES_WOODEN, SERIES_CHROMATIC, SERIES_ARMORED, SERIES_EXPERIMENTAL, SERIES_ECHOSTEEL)));
        SERIES_BY_RARITY.put("omega", new ArrayList<>(List.of(SERIES_WOODEN, SERIES_CHROMATIC, SERIES_ARMORED, SERIES_EXPERIMENTAL, SERIES_ECHOSTEEL)));

        RARITY_DROP_WEIGHTS.put("scrappy", 45);
        RARITY_DROP_WEIGHTS.put("common", 25);
        RARITY_DROP_WEIGHTS.put("rare", 15);
        RARITY_DROP_WEIGHTS.put("epic", 10);
        RARITY_DROP_WEIGHTS.put("omega", 5);

        LinkedHashMap<String, PartRollEntry> chassis = new LinkedHashMap<>();
        chassis.put(SERIES_WOODEN, new PartRollEntry().stat(ExtractorStat.HEALTH, 3000, 5000).stat(ExtractorStat.ARMOR, 2, 5).stat(ExtractorStat.DRAIN, 1, 2).stat(ExtractorStat.DURABILITY, 40000, 50000));
        chassis.put(SERIES_CHROMATIC, new PartRollEntry().stat(ExtractorStat.HEALTH, 3500, 6000).stat(ExtractorStat.ARMOR, 3, 8).stat(ExtractorStat.DRAIN, 2, 3).stat(ExtractorStat.DURABILITY, 45000, 55000));
        chassis.put(SERIES_EXPERIMENTAL, new PartRollEntry().stat(ExtractorStat.HEALTH, 2000, 3500).stat(ExtractorStat.ARMOR, 1, 3).stat(ExtractorStat.DRAIN, 4, 6).stat(ExtractorStat.DURABILITY, 30000, 40000)
                .special(ExtractorStat.QUANTITY, 10).special(ExtractorStat.RARITY, 10));
        chassis.put(SERIES_ARMORED, new PartRollEntry().stat(ExtractorStat.HEALTH, 4000, 8000).stat(ExtractorStat.ARMOR, 5, 10).stat(ExtractorStat.DRAIN, 3, 4).stat(ExtractorStat.DURABILITY, 50000, 60000));
        chassis.put(SERIES_ECHOSTEEL, new PartRollEntry().stat(ExtractorStat.HEALTH, 3500, 6000).stat(ExtractorStat.ARMOR, 3, 8).stat(ExtractorStat.DRAIN, 2, 3).stat(ExtractorStat.DURABILITY, 60000, 70000));
        PARTS.put(ExtractorPartType.CHASSIS.getId(), chassis);

        LinkedHashMap<String, PartRollEntry> cores = new LinkedHashMap<>();
        cores.put(SERIES_WOODEN, new PartRollEntry().stat(ExtractorStat.POWER, 30, 40).stat(ExtractorStat.SPEED, 40, 60).stat(ExtractorStat.DRAIN, 4, 6).stat(ExtractorStat.ENERGY_CAPACITY, 700, 900).stat(ExtractorStat.ENERGY_PER_CELL, 15, 30).stat(ExtractorStat.DURABILITY, 10000, 15000));
        cores.put(SERIES_CHROMATIC, new PartRollEntry().stat(ExtractorStat.POWER, 35, 50).stat(ExtractorStat.SPEED, 50, 75).stat(ExtractorStat.DRAIN, 8, 10).stat(ExtractorStat.ENERGY_CAPACITY, 900, 1100).stat(ExtractorStat.ENERGY_PER_CELL, 75, 100).stat(ExtractorStat.DURABILITY, 15000, 20000));
        cores.put(SERIES_EXPERIMENTAL, new PartRollEntry().stat(ExtractorStat.POWER, 50, 65).stat(ExtractorStat.SPEED, 55, 80).stat(ExtractorStat.DRAIN, 12, 15).stat(ExtractorStat.ENERGY_CAPACITY, 900, 1100).stat(ExtractorStat.ENERGY_PER_CELL, 50, 75).stat(ExtractorStat.DURABILITY, 8000, 10000));
        cores.put(SERIES_ARMORED, new PartRollEntry().stat(ExtractorStat.POWER, 35, 50).stat(ExtractorStat.SPEED, 50, 75).stat(ExtractorStat.DRAIN, 9, 11).stat(ExtractorStat.ENERGY_CAPACITY, 1400, 1800).stat(ExtractorStat.ENERGY_PER_CELL, 120, 150).stat(ExtractorStat.DURABILITY, 25000, 30000));
        cores.put(SERIES_ECHOSTEEL, new PartRollEntry().stat(ExtractorStat.POWER, 45, 55).stat(ExtractorStat.SPEED, 50, 75).stat(ExtractorStat.DRAIN, 10, 12).stat(ExtractorStat.ENERGY_CAPACITY, 1200, 1500).stat(ExtractorStat.ENERGY_PER_CELL, 100, 130).stat(ExtractorStat.DURABILITY, 25000, 30000));
        PARTS.put(ExtractorPartType.POWER_CORE.getId(), cores);

        LinkedHashMap<String, PartRollEntry> drills = new LinkedHashMap<>();
        drills.put("standard", new PartRollEntry().stat(ExtractorStat.POWER, 10, 15).stat(ExtractorStat.SPEED, 30, 60).stat(ExtractorStat.DRAIN, 5, 7).mult(ExtractorStat.DRAIN_MULT, 1.0F, 1.0F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("laser", new PartRollEntry().stat(ExtractorStat.POWER, 5, 10).stat(ExtractorStat.SPEED, 35, 70).stat(ExtractorStat.DRAIN, 6, 8).mult(ExtractorStat.DRAIN_MULT, 1.1F, 1.1F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("greed_infuser", new PartRollEntry().stat(ExtractorStat.POWER, 10, 15).stat(ExtractorStat.SPEED, 30, 60).stat(ExtractorStat.DRAIN, 5, 7).mult(ExtractorStat.DRAIN_MULT, 1.0F, 1.0F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("archeology", new PartRollEntry().stat(ExtractorStat.POWER, 5, 10).stat(ExtractorStat.SPEED, 35, 70).stat(ExtractorStat.DRAIN, 6, 8).mult(ExtractorStat.DRAIN_MULT, 1.1F, 1.1F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("excavator", new PartRollEntry().stat(ExtractorStat.POWER, 5, 10).stat(ExtractorStat.SPEED, 30, 60).stat(ExtractorStat.DRAIN, 6, 8).mult(ExtractorStat.DRAIN_MULT, 1.05F, 1.05F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("refiner", new PartRollEntry().stat(ExtractorStat.POWER, 10, 15).stat(ExtractorStat.SPEED, 30, 60).stat(ExtractorStat.DRAIN, 6, 8).mult(ExtractorStat.DRAIN_MULT, 1.1F, 1.1F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("heavy", new PartRollEntry().stat(ExtractorStat.POWER, 10, 15).stat(ExtractorStat.SPEED, 35, 70).stat(ExtractorStat.DRAIN, 7, 9).mult(ExtractorStat.DRAIN_MULT, 1.15F, 1.15F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("juicer", new PartRollEntry().stat(ExtractorStat.POWER, 5, 10).stat(ExtractorStat.SPEED, 40, 80).stat(ExtractorStat.DRAIN, 6, 8).mult(ExtractorStat.DRAIN_MULT, 1.1F, 1.1F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("glue_derrick", new PartRollEntry().stat(ExtractorStat.POWER, 15, 20).stat(ExtractorStat.SPEED, 25, 50).stat(ExtractorStat.DRAIN, 5, 7).mult(ExtractorStat.DRAIN_MULT, 1.05F, 1.05F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("crystal", new PartRollEntry().stat(ExtractorStat.POWER, 10, 15).stat(ExtractorStat.SPEED, 30, 60).stat(ExtractorStat.DRAIN, 5, 7).mult(ExtractorStat.DRAIN_MULT, 1.05F, 1.05F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        drills.put("containment", new PartRollEntry().stat(ExtractorStat.POWER, 10, 15).stat(ExtractorStat.SPEED, 35, 75).stat(ExtractorStat.DRAIN, 6, 8).mult(ExtractorStat.DRAIN_MULT, 1.1F, 1.1F).stat(ExtractorStat.DURABILITY, 10000, 15000));
        PARTS.put(ExtractorPartType.DRILL.getId(), drills);

        LinkedHashMap<String, PartRollEntry> legs = new LinkedHashMap<>();
        legs.put(SERIES_WOODEN, new PartRollEntry().stat(ExtractorStat.HEALTH, 1000, 2000).stat(ExtractorStat.ARMOR, 0, 2).stat(ExtractorStat.DRAIN, -1, 0).stat(ExtractorStat.DURABILITY, 15000, 20000));
        legs.put(SERIES_CHROMATIC, new PartRollEntry().stat(ExtractorStat.HEALTH, 1500, 2500).stat(ExtractorStat.ARMOR, 1, 3).stat(ExtractorStat.DRAIN, 1, 2).stat(ExtractorStat.DURABILITY, 20000, 25000));
        legs.put(SERIES_EXPERIMENTAL, new PartRollEntry().stat(ExtractorStat.HEALTH, 500, 1000).stat(ExtractorStat.ARMOR, 0, 1).stat(ExtractorStat.DRAIN, 4, 6).stat(ExtractorStat.DURABILITY, 10000, 15000)
                .special(ExtractorStat.QUANTITY, 10).special(ExtractorStat.RARITY, 10));
        legs.put(SERIES_ARMORED, new PartRollEntry().stat(ExtractorStat.HEALTH, 2000, 3000).stat(ExtractorStat.ARMOR, 3, 5).stat(ExtractorStat.DRAIN, 2, 3).stat(ExtractorStat.DURABILITY, 25000, 30000));
        legs.put(SERIES_ECHOSTEEL, new PartRollEntry().stat(ExtractorStat.HEALTH, 1250, 1750).stat(ExtractorStat.ARMOR, 1, 3).stat(ExtractorStat.DRAIN, 1, 2).stat(ExtractorStat.DURABILITY, 30000, 35000));
        PARTS.put(ExtractorPartType.LEGS.getId(), legs);

        LinkedHashMap<String, PartRollEntry> shields = new LinkedHashMap<>();
        shields.put(SERIES_WOODEN, new PartRollEntry().stat(ExtractorStat.SHIELD, 600, 1200).stat(ExtractorStat.SHIELD_TOUGHNESS, 0, 1).stat(ExtractorStat.SHIELD_REGEN, 60, 80).stat(ExtractorStat.DRAIN, 3, 5).stat(ExtractorStat.DURABILITY, 10000, 15000));
        shields.put(SERIES_CHROMATIC, new PartRollEntry().stat(ExtractorStat.SHIELD, 750, 1500).stat(ExtractorStat.SHIELD_TOUGHNESS, 0, 1).stat(ExtractorStat.SHIELD_REGEN, 70, 90).stat(ExtractorStat.DRAIN, 4, 6).stat(ExtractorStat.DURABILITY, 12000, 17000));
        shields.put(SERIES_EXPERIMENTAL, new PartRollEntry().stat(ExtractorStat.SHIELD, 1500, 2500).stat(ExtractorStat.SHIELD_TOUGHNESS, 2, 3).stat(ExtractorStat.SHIELD_REGEN, 125, 175).stat(ExtractorStat.DRAIN, 8, 12).stat(ExtractorStat.DURABILITY, 8000, 10000));
        shields.put(SERIES_ARMORED, new PartRollEntry().stat(ExtractorStat.SHIELD, 1000, 1750).stat(ExtractorStat.SHIELD_TOUGHNESS, 1, 2).stat(ExtractorStat.SHIELD_REGEN, 60, 80).stat(ExtractorStat.DRAIN, 5, 7).stat(ExtractorStat.DURABILITY, 15000, 20000));
        shields.put(SERIES_ECHOSTEEL, new PartRollEntry().stat(ExtractorStat.SHIELD, 800, 1600).stat(ExtractorStat.SHIELD_TOUGHNESS, 1, 2).stat(ExtractorStat.SHIELD_REGEN, 75, 100).stat(ExtractorStat.DRAIN, 5, 7).stat(ExtractorStat.DURABILITY, 16000, 20000));
        PARTS.put(ExtractorPartType.SHIELD_ARRAY.getId(), shields);

        LinkedHashMap<String, PartRollEntry> radiators = new LinkedHashMap<>();
        radiators.put(SERIES_WOODEN, new PartRollEntry().stat(ExtractorStat.DRAIN, -2, -1).stat(ExtractorStat.POWER, 5, 10).stat(ExtractorStat.SPEED, 3, 5).mult(ExtractorStat.DRAIN_MULT, 0.95F, 0.95F).stat(ExtractorStat.DURABILITY, 5000, 10000));
        radiators.put(SERIES_CHROMATIC, new PartRollEntry().stat(ExtractorStat.DRAIN, -3, -2).stat(ExtractorStat.POWER, 4, 8).stat(ExtractorStat.SPEED, 5, 10).mult(ExtractorStat.DRAIN_MULT, 0.95F, 0.95F).stat(ExtractorStat.DURABILITY, 8000, 16000));
        radiators.put(SERIES_EXPERIMENTAL, new PartRollEntry().stat(ExtractorStat.DRAIN, -8, -6).stat(ExtractorStat.POWER, 5, 10).stat(ExtractorStat.SPEED, 0, 1).mult(ExtractorStat.DRAIN_MULT, 1.0F, 1.0F).mult(ExtractorStat.DURABILITY_DRAIN_MULT, 1.5F, 1.5F).stat(ExtractorStat.DURABILITY, 3000, 6000));
        radiators.put(SERIES_ARMORED, new PartRollEntry().stat(ExtractorStat.DRAIN, -1, 0).stat(ExtractorStat.POWER, 3, 6).stat(ExtractorStat.SPEED, 3, 5).mult(ExtractorStat.DRAIN_MULT, 0.85F, 0.85F).stat(ExtractorStat.DURABILITY, 10000, 20000));
        radiators.put(SERIES_ECHOSTEEL, new PartRollEntry().stat(ExtractorStat.DRAIN, -4, -2).stat(ExtractorStat.POWER, 4, 8).stat(ExtractorStat.SPEED, 4, 6).mult(ExtractorStat.DRAIN_MULT, 0.9F, 0.9F).stat(ExtractorStat.DURABILITY, 12000, 18000));
        PARTS.put(ExtractorPartType.RADIATOR.getId(), radiators);

        LinkedHashMap<String, PartRollEntry> scanners = new LinkedHashMap<>();
        scanners.put(SERIES_WOODEN, new PartRollEntry().stat(ExtractorStat.SPEED, 10, 15).stat(ExtractorStat.QUANTITY, 10, 15).stat(ExtractorStat.RARITY, 10, 25).stat(ExtractorStat.DRAIN, 1, 2).stat(ExtractorStat.DURABILITY, 5000, 10000));
        scanners.put(SERIES_CHROMATIC, new PartRollEntry().stat(ExtractorStat.SPEED, 15, 20).stat(ExtractorStat.QUANTITY, 15, 25).stat(ExtractorStat.RARITY, 10, 15).stat(ExtractorStat.DRAIN, 1, 2).stat(ExtractorStat.DURABILITY, 8000, 16000));
        scanners.put(SERIES_EXPERIMENTAL, new PartRollEntry().stat(ExtractorStat.SPEED, 20, 25).stat(ExtractorStat.QUANTITY, 30, 40).stat(ExtractorStat.RARITY, 15, 25).stat(ExtractorStat.DRAIN, 4, 6).stat(ExtractorStat.DURABILITY, 3000, 6000));
        scanners.put(SERIES_ARMORED, new PartRollEntry().stat(ExtractorStat.SPEED, 0, 5).stat(ExtractorStat.QUANTITY, 10, 15).stat(ExtractorStat.RARITY, 30, 40).stat(ExtractorStat.DRAIN, 2, 3).stat(ExtractorStat.DURABILITY, 10000, 20000));
        scanners.put(SERIES_ECHOSTEEL, new PartRollEntry().stat(ExtractorStat.SPEED, 15, 20).stat(ExtractorStat.QUANTITY, 20, 30).stat(ExtractorStat.RARITY, 20, 30).stat(ExtractorStat.DRAIN, 2, 3).stat(ExtractorStat.DURABILITY, 12000, 18000));
        PARTS.put(ExtractorPartType.SCANNER.getId(), scanners);

        DRILL_CLASSES.put("standard", new DrillClassEntry("Standard", "arcane_essence", "drill_standard", "scrappy", 0.0F));
        DRILL_CLASSES.put("laser", new DrillClassEntry("Laser", "maps", "drill_laser", "scrappy", 0.0F));
        DRILL_CLASSES.put("greed_infuser", new DrillClassEntry("Greed Infuser", "greed_card_upgrade", "drill_greed", "scrappy", 1.46F));
        DRILL_CLASSES.put("archeology", new DrillClassEntry("Archeology", "mythic_charms", "drill_archeology", "scrappy", 2.00F));
        DRILL_CLASSES.put("excavator", new DrillClassEntry("Excavator", "ancient_uniques", "drill_excavator", "scrappy", 4.00F));
        DRILL_CLASSES.put("refiner", new DrillClassEntry("Refiner", "power_sealing", "drill_refiner", "scrappy", 5.10F));
        DRILL_CLASSES.put("heavy", new DrillClassEntry("Heavy", "shiny_card_packs", "drill_heavy", "scrappy", 5.10F));
        DRILL_CLASSES.put("juicer", new DrillClassEntry("Juicer", "card_juice", "drill_juicer", "scrappy", 10.00F));
        DRILL_CLASSES.put("glue_derrick", new DrillClassEntry("Glue Derrick", "prismatic_glue", "drill_glue", "scrappy", 9.34F));
        DRILL_CLASSES.put("crystal", new DrillClassEntry("Crystal", "ancient_crystals", "drill_crystal", "scrappy", 21.46F));
        DRILL_CLASSES.put("containment", new DrillClassEntry("Containment", "nullite", "drill_containment", "scrappy", 32.37F));

        ATTACHMENTS.put("auxiliary_radiator", new AttachmentEntry("Auxiliary Radiator", "aux_radiator", -2, -1, 20000, 30000,
                new EffectEntry(ExtractorStat.SPEED, 5, 10, true), new EffectEntry(ExtractorStat.POWER, 2, 5, true)));
        ATTACHMENTS.put("secondary_shield_charger", new AttachmentEntry("Secondary Shield Charger", "shield_charger", 2, 4, 20000, 30000,
                new EffectEntry(ExtractorStat.SHIELD, 10, 15, true), new EffectEntry(ExtractorStat.SHIELD_REGEN, 20, 30, true)));
        ATTACHMENTS.put("driver_override", new AttachmentEntry("Driver Override", "driver_override", 8, 12, 10000, 15000,
                new EffectEntry(ExtractorStat.SPEED, 20, 30, true), new EffectEntry(ExtractorStat.POWER, -10, -5, true)));
        ATTACHMENTS.put("extra_plating", new AttachmentEntry("Extra Plating", "extra_plating", 1, 2, 20000, 30000,
                new EffectEntry(ExtractorStat.HEALTH, 10, 15, true), new EffectEntry(ExtractorStat.ARMOR, 10, 15, true)));
        ATTACHMENTS.put("fuel_injector", new AttachmentEntry("Fuel Injector", "fuel_injector", -1, 0, 20000, 30000,
                new EffectEntry(ExtractorStat.ENERGY_PER_CELL, 20, 30, true), new EffectEntry(ExtractorStat.DRAIN_MULT, 0.97F, 0.97F, false)));
        ATTACHMENTS.put("battery_array", new AttachmentEntry("Battery Array", "battery_array", -1, 0, 20000, 30000,
                new EffectEntry(ExtractorStat.ENERGY_CAPACITY, 20, 30, true), new EffectEntry(ExtractorStat.POWER, 2, 5, true)));
        ATTACHMENTS.put("ore_cleaner", new AttachmentEntry("Ore Cleaner", "ore_cleaner", 2, 4, 20000, 30000,
                new EffectEntry(ExtractorStat.QUANTITY, 15, 20, true), new EffectEntry(ExtractorStat.RARITY, 15, 20, true)));
        ATTACHMENTS.put("defense_turret", new AttachmentEntry("Defense Turret", "defense_turret", 4, 8, 20000, 30000,
                new EffectEntry(ExtractorStat.TURRET, 1, 1, false)));
        ATTACHMENTS.put("system_reinforcement", new AttachmentEntry("System Reinforcement", "system_reinforcement", 3, 5, 40000, 50000,
                new EffectEntry(ExtractorStat.DURABILITY_DRAIN_MULT, 0.85F, 0.90F, false)));
        ATTACHMENTS.put("regenerative_nanobots", new AttachmentEntry("Regenerative Nanobots", "nanobots", 4, 8, 20000, 30000,
                new EffectEntry(ExtractorStat.HEALTH_REGEN, 3, 5, false), new EffectEntry(ExtractorStat.HEALTH, 5, 10, true)));

        AFFIX_POOLS.put("hardware_prefix", new ArrayList<>(List.of(
                new AffixEntry("reinforced", ExtractorStat.HEALTH, 2, 5, true),
                new AffixEntry("plated", ExtractorStat.ARMOR, 1, 2, false),
                new AffixEntry("sturdy", ExtractorStat.HEALTH, 200, 400, false),
                new AffixEntry("charged", ExtractorStat.POWER, 1, 3, false),
                new AffixEntry("lasting", ExtractorStat.DURABILITY, 4000, 8000, false))));
        AFFIX_POOLS.put("hardware_suffix", new ArrayList<>(List.of(
                new AffixEntry("of_recharging", ExtractorStat.ENERGY_PER_CELL, 20, 40, false),
                new AffixEntry("of_warding", ExtractorStat.EFFECT_RESISTANCE, 10, 20, true),
                new AffixEntry("of_defiance", ExtractorStat.ELITE_RESISTANCE, 5, 10, true),
                new AffixEntry("of_surging", ExtractorStat.POWER, 1, 2, true))));
        AFFIX_POOLS.put("mechanical_prefix", new ArrayList<>(List.of(
                new AffixEntry("driven", ExtractorStat.POWER, 2, 4, false),
                new AffixEntry("swift", ExtractorStat.SPEED, 5, 10, false),
                new AffixEntry("hardened", ExtractorStat.DURABILITY, 3000, 6000, false),
                new AffixEntry("conductive", ExtractorStat.ENERGY_PER_CELL, 30, 60, false),
                new AffixEntry("shielded", ExtractorStat.SHIELD, 5, 10, true))));
        AFFIX_POOLS.put("mechanical_suffix", new ArrayList<>(List.of(
                new AffixEntry("of_plenty", ExtractorStat.QUANTITY, 5, 10, false),
                new AffixEntry("of_fortune", ExtractorStat.RARITY, 5, 10, false),
                new AffixEntry("of_mending", ExtractorStat.HEALING_EFFICIENCY, 10, 15, true),
                new AffixEntry("of_recovery", ExtractorStat.SHIELD_REGEN, 5, 10, true))));

        AFFIX_COUNT.put("scrappy", 0);
        AFFIX_COUNT.put("common", 1);
        AFFIX_COUNT.put("rare", 1);
        AFFIX_COUNT.put("epic", 2);
        AFFIX_COUNT.put("omega", 2);
    }

    private static void names(SeriesEntry series, String chassis, String core, String legs, String shield, String radiator, String scanner) {
        series.partNames.put(ExtractorPartType.CHASSIS.getId(), chassis);
        series.partNames.put(ExtractorPartType.POWER_CORE.getId(), core);
        series.partNames.put(ExtractorPartType.LEGS.getId(), legs);
        series.partNames.put(ExtractorPartType.SHIELD_ARRAY.getId(), shield);
        series.partNames.put(ExtractorPartType.RADIATOR.getId(), radiator);
        series.partNames.put(ExtractorPartType.SCANNER.getId(), scanner);
    }

    public Map<String, Float> getSpecials(ExtractorPartType type, String key) {
        PartRollEntry entry = getRoll(type, key);
        return entry == null ? new LinkedHashMap<>() : entry.specials;
    }
}
