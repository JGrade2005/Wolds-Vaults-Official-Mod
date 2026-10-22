package xyz.iwolfking.woldsvaults.client.screens.gods;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.core.vault.influence.VaultGod;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;


public final class GodTreeBackdrop {
    private static final String DIR = "textures/gui/gods/backdrop/";
    private static final float DRIFT_SPEED = 3.0F;    // GUI units per second at depth 1
    private static final float DRIFT_ANGLE = 12.0F;   // degrees, 0 = drifting right
    private static final float FAN_SPREAD = 12.0F;    // degrees a fanned nebula turns away from the pan direction
    private static final float METEORS_PER_MINUTE = 10.0F;
    private static final int GALAXY_COUNT = 6;
    private static final int GALAXY_SCALE = 2;
    private static final int SPAN_W = 768, SPAN_H = 432; // sprites wrap inside this many GUI units

    private record Tile(ResourceLocation texture, int size, float depth, float alpha, float fan) {}
    private record Sheet(ResourceLocation texture, int width, int height) {}
    private record Sprite(float x, float y, int u, int v, int w, int h, int scale, float phase) {}

    private record Look(String secondHaze, int[] galaxies, int cluster, int[][] stars) {}

    private static final Sheet GALAXIES = new Sheet(tex("galaxies"), 224, 32);
    private static final Sheet CLUSTERS = new Sheet(tex("clusters"), 96, 32);
    private static final Sheet STARS = new Sheet(tex("star_sprites"), 128, 80);
    private static final Sheet COMETS = new Sheet(tex("comets"), 96, 16);
    private static final Map<VaultGod, Look> LOOKS = new EnumMap<>(Map.of(
            VaultGod.IDONA, new Look("neutral", new int[]{3, 4}, 0, new int[][]{{3, 5}, {2, 3}, {3, 7}, {2, 4}}),
            VaultGod.VELARA, new Look("tenos", new int[]{0, 6}, 2, new int[][]{{0, 5}, {0, 7}, {1, 3}, {0, 6}}),
            VaultGod.WENDARR, new Look("neutral", new int[]{2, 4}, 0, new int[][]{{2, 5}, {2, 7}, {0, 3}, {2, 6}}),
            VaultGod.TENOS, new Look("velara", new int[]{1, 5}, 1, new int[][]{{1, 5}, {1, 7}, {0, 4}, {1, 6}})));
    private static final Map<VaultGod, GodTreeBackdrop> CACHE = new EnumMap<>(VaultGod.class);

    private final Tile hazeGod, hazeSecond, starsFar, nebulaBack, starsMid, nebulaGod, dust, wisps, starsNear;
    private final List<Sprite> galaxies, clusters, brightStars;
    private final List<Meteor> meteors = new ArrayList<>();
    private final Random random = new Random();
    private float lastSeconds = -1.0F, spawnIn = 2.0F;


}
