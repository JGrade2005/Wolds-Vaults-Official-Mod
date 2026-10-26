package xyz.iwolfking.woldsvaults.config;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.Config;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Fabrication, repair and Vault Fabricator costs for extractor parts. The defaults ship inside the jar as
 * {@code woldsvaults_defaults/extractor_costs.json}, exported by the design sheet's generator
 * ({@code redesign/greed-rework/scoping/tools/extractor_costs.py --export}), so the sheet and the game read
 * the same numbers. Edit the generated {@code config/the_vault/extractor_costs.json} to tune a server.
 */
public class ExtractorCostsConfig extends Config {
    public static final String DEFAULTS_RESOURCE = "/woldsvaults_defaults/extractor_costs.json";
    public static final String SCALING_MAIN = "main";
    public static final String SCALING_FABRICATOR = "fabricator";
    public static final String SCALING_GATED = "gated";

    @Expose public CurveEntry CURVE = new CurveEntry();
    @Expose public LinkedHashMap<String, Float> RARITY = new LinkedHashMap<>();
    @Expose public RepairEntry REPAIR = new RepairEntry();
    @Expose public LinkedHashMap<String, LinkedHashMap<String, List<Ingredient>>> PARTS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, List<Ingredient>> DRILLS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, List<Ingredient>> ATTACHMENTS = new LinkedHashMap<>();
    @Expose public LinkedHashMap<String, ComponentEntry> COMPONENTS = new LinkedHashMap<>();

    @Override
    public String getName() {
        return "extractor_costs";
    }

    public static class CurveEntry {
        @Expose public double exponent = 2.0412;
        @Expose public double gatedRate = 0.5;
        @Expose public double fabricatorRate = 0.66;
        @Expose public double accelQuality = 60.0;
        @Expose public double accelFactor = 1.5;
    }

    public static class RepairEntry {
        @Expose public double fraction = 0.20;
        @Expose public String coreItem = "the_vault:repair_core";
        @Expose public double coreBase = 2.0;
    }

    /** One cost row: an item or fluid id, its roll range at the row's gate, the gate, and its scaling class. */
    public static class Ingredient {
        @Expose public String id = "";
        @Expose public boolean fluid;
        @Expose public double min;
        @Expose public double max;
        @Expose public double gate;
        @Expose public String scaling = SCALING_MAIN;
    }

    /** A Vault Fabricator recipe: a 5x5 grid of symbols, what each symbol is, an optional fluid, energy and time. */
    public static class ComponentEntry {
        @Expose public String item = "";
        @Expose public String tier = "basic";
        @Expose public List<String> grid = new ArrayList<>();
        @Expose public LinkedHashMap<String, String> legend = new LinkedHashMap<>();
        @Expose public String fluid = "";
        @Expose public int fluidAmount;
        @Expose public int fe;
        @Expose public int ticks = 100;

        /** The item id for grid cell {@code index} (row-major, 0..24), or null for an empty cell. */
        @Nullable
        public String cell(int index) {
            int row = index / 5;
            int col = index % 5;
            if (row >= this.grid.size()) {
                return null;
            }
            String[] symbols = this.grid.get(row).trim().split("\\s+");
            if (col >= symbols.length || ".".equals(symbols[col])) {
                return null;
            }
            return this.legend.get(symbols[col]);
        }
    }

    @Nullable
    public List<Ingredient> partRecipe(String partType, String series) {
        LinkedHashMap<String, List<Ingredient>> bySeries = this.PARTS.get(partType);
        return bySeries == null ? null : bySeries.get(series);
    }

    @Nullable
    public ComponentEntry componentFor(String key) {
        return this.COMPONENTS.get(key);
    }

    @Override
    protected void reset() {
        try (InputStream in = ExtractorCostsConfig.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (in == null) {
                WoldsVaults.LOGGER.error("Extractor cost defaults {} are missing from the jar; every extractor cost is empty.", DEFAULTS_RESOURCE);
                return;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                ExtractorCostsConfig defaults = this.getGson().fromJson(reader, ExtractorCostsConfig.class);
                this.CURVE = defaults.CURVE;
                this.RARITY = defaults.RARITY;
                this.REPAIR = defaults.REPAIR;
                this.PARTS = defaults.PARTS;
                this.DRILLS = defaults.DRILLS;
                this.ATTACHMENTS = defaults.ATTACHMENTS;
                this.COMPONENTS = defaults.COMPONENTS;
            }
        } catch (Exception e) {
            WoldsVaults.LOGGER.error("Could not read the extractor cost defaults {}; every extractor cost is empty.", DEFAULTS_RESOURCE, e);
        }
    }
}
