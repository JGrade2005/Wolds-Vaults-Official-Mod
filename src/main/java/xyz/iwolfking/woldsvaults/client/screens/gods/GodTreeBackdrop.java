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
}
