package xyz.iwolfking.woldsvaults.objectives.extraction;

import iskallia.vault.core.Version;
import iskallia.vault.core.data.DataObject;
import iskallia.vault.core.data.adapter.Adapters;
import iskallia.vault.core.data.key.FieldKey;
import iskallia.vault.core.data.key.registry.FieldRegistry;

import java.util.UUID;

public class ExtractionSite extends DataObject<ExtractionSite> {
    public static final int STATE_UNTOUCHED = 0;
    public static final int STATE_DRILLING = 1;
    public static final int STATE_COMPLETE = 2;

    public static final FieldRegistry FIELDS = new FieldRegistry();
    public static final FieldKey<Integer> REGION_X = FieldKey.of("region_x", Integer.class)
            .with(Version.v1_0, Adapters.INT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Integer> REGION_Z = FieldKey.of("region_z", Integer.class)
            .with(Version.v1_0, Adapters.INT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<String> GOD = FieldKey.of("god", String.class)
            .with(Version.v1_0, Adapters.UTF_8, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Integer> STATE = FieldKey.of("state", Integer.class)
            .with(Version.v1_0, Adapters.INT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Float> PROGRESS = FieldKey.of("progress", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Integer> REQUIRED = FieldKey.of("required", Integer.class)
            .with(Version.v1_0, Adapters.INT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<UUID> ENTITY = FieldKey.of("entity", UUID.class)
            .with(Version.v1_0, Adapters.UUID, DISK.all()).register(FIELDS);
    public static final FieldKey<Float> HP = FieldKey.of("hp", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Float> MAX_HP = FieldKey.of("max_hp", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Float> SHIELD = FieldKey.of("shield", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Float> MAX_SHIELD = FieldKey.of("max_shield", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Float> ENERGY = FieldKey.of("energy", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);
    public static final FieldKey<Float> MAX_ENERGY = FieldKey.of("max_energy", Float.class)
            .with(Version.v1_0, Adapters.FLOAT, DISK.all().or(CLIENT.all())).register(FIELDS);

    public ExtractionSite() {
    }

    public ExtractionSite(int regionX, int regionZ, String god, int requiredTicks) {
        this.set(REGION_X, regionX);
        this.set(REGION_Z, regionZ);
        this.set(GOD, god);
        this.set(STATE, STATE_UNTOUCHED);
        this.set(PROGRESS, 0.0F);
        this.set(REQUIRED, requiredTicks);
        this.set(HP, 0.0F);
        this.set(MAX_HP, 0.0F);
        this.set(SHIELD, 0.0F);
        this.set(MAX_SHIELD, 0.0F);
        this.set(ENERGY, 0.0F);
        this.set(MAX_ENERGY, 0.0F);
    }

    @Override
    public FieldRegistry getFields() {
        return FIELDS;
    }

    public boolean isUntouched() {
        return this.get(STATE) == STATE_UNTOUCHED;
    }

    public boolean isDrilling() {
        return this.get(STATE) == STATE_DRILLING;
    }

    public boolean isComplete() {
        return this.get(STATE) == STATE_COMPLETE;
    }
}
