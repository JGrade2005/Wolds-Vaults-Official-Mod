package xyz.iwolfking.woldsvaults.config;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.Config;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * How a vault's difficulty multiplier D = (1 + sigil difficulty) x (1 + medallion objective bonus) is turned
 * into objective requirements (config/the_vault/objective_difficulty.json). Every objective raises its base
 * requirement by D to the power listed under {@code exponents}; bingo tiles use the class exponents instead.
 * Threshold maps are keyed by the difficulty increase x = D - 1 as a string ("3.0") and the entry with the
 * highest key not above x applies.
 */
public class ObjectiveDifficultyConfig extends Config {
    public static final String LINEAR = "linear";

    @Expose private Map<String, String> documentation;
    @Expose private Map<String, Double> exponents;
    @Expose private Map<String, Double> bingoTileClasses;
    @Expose private Map<String, String> bingoTileClassByName;
    @Expose private double stackLinearFactor;
    @Expose private double stackBreakpoint;
    @Expose private double stackOffset;
    @Expose private double stackPower;
    @Expose private Map<String, Integer> crateTiersBySigilDifficulty;
    @Expose private Map<String, Integer> scavengerGoalFloorLowLevel;
    @Expose private int scavengerGoalFloorHighLevelFrom;
    @Expose private Map<String, Integer> scavengerGoalFloorHighLevel;
    @Expose private Map<String, Integer> ballisticGridBonus;
    @Expose private int ballisticGridCap;
    @Expose private int survivalReservedWaveGroups;

    private transient TreeMap<Double, Integer> crateTiers;
    private transient TreeMap<Double, Integer> goalFloorLow;
    private transient TreeMap<Double, Integer> goalFloorHigh;
    private transient TreeMap<Double, Integer> gridBonus;

    @Override
    public String getName() {
        return "objective_difficulty";
    }

    @Override
    protected void reset() {
        this.documentation = new LinkedHashMap<>();
        this.documentation.put("D", "Vault difficulty multiplier: (1 + sigil difficulty from sigils/definitions.json) x (1 + medallion objectiveDifficultyBonus / 100). x = D - 1 is the '+x Difficulty' a player sees");
        this.documentation.put("exponents", "requirement = base x D^exponent, per objective. 1 = linear, 0.5 = square root");
        this.documentation.put("bingoTileClasses", "Exponent per bingo tile class; bingoTileClassByName maps a tile's renderer name to a class, unlisted tiles are linear");
        this.documentation.put("stack*", "Challenge stacks from D: stackLinearFactor x D below stackBreakpoint, else stackOffset + D^stackPower; a vault with D <= 1 gets none");
        this.documentation.put("crateTiersBySigilDifficulty", "crate_tier stacks by sigil difficulty (not D): highest key not above the sigil's difficulty applies");
        this.documentation.put("scavengerGoalFloor*", "Minimum scavenger goal count by x, below and from scavengerGoalFloorHighLevelFrom; a higher roll is kept");
        this.documentation.put("ballisticGridBonus", "Extra ballistic bingo rows and columns by x, capped at ballisticGridCap");
        this.documentation.put("survivalReservedWaveGroups", "Survival starts at wave group floor(x), but never later than size minus this many groups");

        this.exponents = new LinkedHashMap<>();
        this.exponents.put("scavenger", 1.0D);
        this.exponents.put("scavenger_bingo", 1.0D);
        this.exponents.put("unhinged_scavenger", 1.0D);
        this.exponents.put("unhinged_scavenger_bingo", 1.0D);
        this.exponents.put("elixir", 1.0D);
        this.exponents.put("enchanted_elixir", 1.0D);
        this.exponents.put("rune_boss", 1.0D);
        this.exponents.put("boss", 0.5D);
        this.exponents.put("cake", 1.0D);
        this.exponents.put("chaos", 0.5D);
        this.exponents.put("corrupted", 1.0D);
        this.exponents.put("zealot", 1.0D);
        this.exponents.put("survival", 0.5D);
        this.exponents.put("alchemy", 0.5D);

        this.bingoTileClasses = new LinkedHashMap<>();
        this.bingoTileClasses.put(LINEAR, 1.0D);
        this.bingoTileClasses.put("soft", 0.7D);
        this.bingoTileClassByName = new LinkedHashMap<>();
        for (String name : List.of("Kill Dwellers", "Rapidly Kill Dwellers", "Kill Tanks", "Kill Hordes", "Defeat an Elite",
                "Free Villager", "Kill Dungeon Boss", "Multi Kill!")) {
            this.bingoTileClassByName.put(name, "soft");
        }

        this.stackLinearFactor = 3.0D;
        this.stackBreakpoint = 4.0D;
        this.stackOffset = 4.0D;
        this.stackPower = 1.5D;

        this.crateTiersBySigilDifficulty = new LinkedHashMap<>();
        this.crateTiersBySigilDifficulty.put("0.5", 2);
        this.crateTiersBySigilDifficulty.put("1.0", 3);
        this.crateTiersBySigilDifficulty.put("1.5", 4);
        this.crateTiersBySigilDifficulty.put("3.0", 5);
        this.crateTiersBySigilDifficulty.put("5.0", 6);
        this.crateTiersBySigilDifficulty.put("6.0", 8);
        this.crateTiersBySigilDifficulty.put("6.5", 9);
        this.crateTiersBySigilDifficulty.put("7.0", 10);
        this.crateTiersBySigilDifficulty.put("7.5", 11);
        this.crateTiersBySigilDifficulty.put("8.0", 12);
        this.crateTiersBySigilDifficulty.put("8.5", 13);
        this.crateTiersBySigilDifficulty.put("9.0", 14);

        this.scavengerGoalFloorLowLevel = new LinkedHashMap<>();
        this.scavengerGoalFloorLowLevel.put("0.5", 5);
        this.scavengerGoalFloorLowLevel.put("3.0", 6);
        this.scavengerGoalFloorHighLevelFrom = 20;
        this.scavengerGoalFloorHighLevel = new LinkedHashMap<>();
        this.scavengerGoalFloorHighLevel.put("0.5", 5);
        this.scavengerGoalFloorHighLevel.put("1.0", 6);
        this.scavengerGoalFloorHighLevel.put("3.0", 7);

        this.ballisticGridBonus = new LinkedHashMap<>();
        this.ballisticGridBonus.put("1.0", 1);
        this.ballisticGridBonus.put("1.5", 2);
        this.ballisticGridBonus.put("5.0", 3);
        this.ballisticGridCap = 9;

        this.survivalReservedWaveGroups = 3;
    }

    public double getExponent(String objective) {
        Double exponent = this.exponents == null ? null : this.exponents.get(objective);
        if (exponent == null) {
            WoldsVaults.LOGGER.warn("objective_difficulty.json has no exponent for '{}', scaling it linearly", objective);
            return 1.0D;
        }
        return exponent;
    }

    public double getTileExponent(String rendererName) {
        String tileClass = rendererName == null || this.bingoTileClassByName == null ? null : this.bingoTileClassByName.get(rendererName);
        if (tileClass == null) {
            tileClass = LINEAR;
        }
        Double exponent = this.bingoTileClasses == null ? null : this.bingoTileClasses.get(tileClass);
        if (exponent == null) {
            WoldsVaults.LOGGER.warn("objective_difficulty.json bingo tile class '{}' (tile '{}') has no exponent, scaling it linearly", tileClass, rendererName);
            return 1.0D;
        }
        return exponent;
    }

    public double getStackLinearFactor() {
        return this.stackLinearFactor;
    }

    public double getStackBreakpoint() {
        return this.stackBreakpoint;
    }

    public double getStackOffset() {
        return this.stackOffset;
    }

    public double getStackPower() {
        return this.stackPower;
    }

    public int getCrateTiers(double sigilDifficulty) {
        if (this.crateTiers == null) {
            this.crateTiers = toTree(this.crateTiersBySigilDifficulty, "crateTiersBySigilDifficulty");
        }
        return floor(this.crateTiers, sigilDifficulty, 0);
    }

    public int getScavengerGoalFloor(double increase, int level) {
        if (level >= this.scavengerGoalFloorHighLevelFrom) {
            if (this.goalFloorHigh == null) {
                this.goalFloorHigh = toTree(this.scavengerGoalFloorHighLevel, "scavengerGoalFloorHighLevel");
            }
            return floor(this.goalFloorHigh, increase, 0);
        }
        if (this.goalFloorLow == null) {
            this.goalFloorLow = toTree(this.scavengerGoalFloorLowLevel, "scavengerGoalFloorLowLevel");
        }
        return floor(this.goalFloorLow, increase, 0);
    }

    public int getBallisticGridBonus(double increase) {
        if (this.gridBonus == null) {
            this.gridBonus = toTree(this.ballisticGridBonus, "ballisticGridBonus");
        }
        return floor(this.gridBonus, increase, 0);
    }

    public int getBallisticGridCap() {
        return this.ballisticGridCap;
    }

    public int getSurvivalReservedWaveGroups() {
        return this.survivalReservedWaveGroups;
    }

    private static int floor(TreeMap<Double, Integer> tree, double key, int fallback) {
        Map.Entry<Double, Integer> entry = tree.floorEntry(key);
        return entry == null ? fallback : entry.getValue();
    }

    private static TreeMap<Double, Integer> toTree(Map<String, Integer> raw, String field) {
        TreeMap<Double, Integer> tree = new TreeMap<>();
        if (raw == null) {
            WoldsVaults.LOGGER.warn("objective_difficulty.json is missing '{}', treating it as empty", field);
            return tree;
        }
        raw.forEach((key, value) -> {
            try {
                tree.put(Double.parseDouble(key), value);
            } catch (NumberFormatException e) {
                WoldsVaults.LOGGER.warn("objective_difficulty.json '{}' has non-numeric threshold '{}', ignoring it", field, key);
            }
        });
        return tree;
    }
}
