package xyz.iwolfking.woldsvaults.client.screens.gods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import iskallia.vault.client.gui.helper.ScreenDrawHelper;
import iskallia.vault.core.vault.influence.VaultGod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.gods.node.GodNodeType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Light around the god tree's stars: spiral arms, motes swirling in, rays beaming out and a core glow, tuned per node
 * type in {@code god_tree/node_effects.json} (the export of the God Node Vortex tuning page). Distances there are
 * pixels of the 64px star cell, so every effect scales with its node. Light is coloured through the god's row of
 * {@code star_ramps.png}: faint light takes the god's deep tones and bright light turns pale. Each layer draws every
 * node in view as one additive batch.
 */
public final class GodNodeEffects {
    private static final ResourceLocation SETTINGS = WoldsVaults.id("god_tree/node_effects.json");
    private static final ResourceLocation RAMPS = WoldsVaults.id("textures/gui/gods/star_ramps.png");
    private static final float CELL = GodTreeTheme.STAR_CELL;
    private static final float TAU = (float) (Math.PI * 2.0D);
    private static final int MAX_MOTES = 200;
    private static final int MAX_RAYS = 32;
    private static final int GLOW_RINGS = 6;
    private static final int RING_SEGMENTS = 20;
    private static final float SEGMENT_PIXELS = 3.0F; // aim for pieces about this many screen pixels long
    private static final float MIN_NODE_PIXELS = 6.0F; // stars smaller than this on screen draw no effects

    // the tuning page's random tables (mulberry32, same seeds), so a star there matches the star here
    private static final float[][] MOTES = randomTable(1337, MAX_MOTES, 5);
    private static final float[][] RAYS = randomTable(4242, MAX_RAYS, 3);

    private static Map<GodNodeType, Look> looks;
    private static int[][] ramps;

    private record Light(float gain, float toneFloor, int toneSteps) {}

    private record Arms(boolean enabled, int count, float inner, float outer, float twist, float spin, float widthCore,
                        float widthTip, float brightness, int segments, float fill, float flow) {}

    private record Motes(boolean enabled, int count, float start, float end, float trip, float swirl, boolean rideArms,
                         int trail, float size, float brightness, boolean overStar) {}

    private record Rays(boolean enabled, int count, float start, float length, float lengthSpread, float widthBase,
                        float widthTip, float spin, float lean, float pulseRate, float stretchPulse, float brightnessPulse,
                        int waves, float randomness, float brightness, boolean fadeToTip, boolean overStar) {}

    private record Glow(boolean enabled, float radius, float brightness, float pulse, float pulseRate) {}

    private record Look(Light light, Arms arms, Motes motes, Rays rays, Glow glow) {
        /** How far from the centre anything can reach, in cell pixels; used to skip stars out of view. */
        float reach() {
            float reach = 0.0F;
            if (this.arms.enabled()) {
                reach = Math.max(reach, Math.max(this.arms.inner(), this.arms.outer()) + this.arms.widthTip());
            }
            if (this.motes.enabled()) {
                reach = Math.max(reach, Math.max(this.motes.start() * 1.15F, this.motes.end()) + this.motes.size());
            }
            if (this.rays.enabled()) {
                reach = Math.max(reach, this.rays.start() + this.rays.length() + this.rays.lengthSpread()
                        + Math.max(this.rays.widthBase(), this.rays.widthTip()));
            }
            if (this.glow.enabled()) {
                reach = Math.max(reach, this.glow.radius());
            }
            return reach + 2.0F;
        }
    }

    private GodNodeEffects() {
    }

    @Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ReloadHook {
        private ReloadHook() {
        }

        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                looks = null;
                ramps = null;
            });
        }
    }

    /**
     * Draws one layer of effects for every star in the given tree-space rectangle. With {@code front} false it draws
     * what sits behind the stars (glow, arms, and motes or rays not set over the star); with {@code front} true, the
     * motes and rays set over the star. {@code viewportScale} is the pan region's zoom.
     */
    public static void render(PoseStack poseStack, VaultGod god, Collection<GodNodeWidget> widgets, boolean front,
                              float minX, float minY, float maxX, float maxY, float viewportScale) {
        Map<GodNodeType, Look> settings = looks();
        if (settings.isEmpty()) {
            return;
        }
        int[] ramp = ramps()[god.ordinal()];
        float screenPerUnit = viewportScale * (float) Minecraft.getInstance().getWindow().getGuiScale();
        double seconds = (Util.getMillis() % 86_400_000L) / 1000.0D;
        Matrix4f pose = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        ScreenDrawHelper.draw(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, buf -> {
            for (GodNodeWidget widget : widgets) {
                Look look = settings.get(widget.getNode().type());
                if (look == null || widget.getWidth() * screenPerUnit < MIN_NODE_PIXELS) {
                    continue;
                }
                float scale = widget.getWidth() / CELL;
                float cx = widget.x + widget.getWidth() / 2.0F;
                float cy = widget.y + widget.getHeight() / 2.0F;
                float reach = look.reach() * scale;
                if (cx + reach < minX || cx - reach > maxX || cy + reach < minY || cy - reach > maxY) {
                    continue;
                }
                // each star runs on its own clock and faces its own way, so neighbours don't pulse in step
                int seed = widget.effectSeed();
                double time = seconds + (seed & 1023) / 1023.0D * 17.0D;
                float turn = ((seed >>> 10) & 1023) / 1023.0F * TAU;
                Painter painter = new Painter(buf, pose, cx, cy, scale, scale * screenPerUnit, look.light(), ramp,
                        widget.effectOpacity());
                if (!front) {
                    glow(painter, look.glow(), time);
                    arms(painter, look.arms(), time, turn);
                }
                if (look.motes().overStar() == front) {
                    motes(painter, look.motes(), look.arms(), time, turn);
                }
                if (look.rays().overStar() == front) {
                    rays(painter, look.rays(), time, turn);
                }
            }
        });
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.defaultBlendFunc();
    }

    // parts --------------------------------------------------------------------------------------------

    private static void glow(Painter painter, Glow glow, double time) {
        if (!glow.enabled() || glow.radius() <= 0.0F || glow.brightness() <= 0.0F) {
            return;
        }
        float strength = glow.brightness() * (1.0F + glow.pulse() * (float) Math.sin(TAU * glow.pulseRate() * time));
        for (int i = 0; i < GLOW_RINGS; i++) {
            float inner = glow.radius() * i / GLOW_RINGS;
            float outer = glow.radius() * (i + 1) / GLOW_RINGS;
            float falloff = 1.0F - (inner + outer) / 2.0F / glow.radius();
            painter.ring(inner, outer, painter.colour(strength * falloff * falloff));
        }
    }

    private static float armAngle(Arms arms, int arm, int count, double time, float turn) {
        return (float) (arm * TAU / count + arms.spin() * time * TAU) + turn;
    }

    private static void arms(Painter painter, Arms arms, double time, float turn) {
        if (!arms.enabled() || arms.count() <= 0 || arms.brightness() <= 0.0F) {
            return;
        }
        float inner = arms.inner();
        float outer = Math.max(inner + 1.0F, arms.outer());
        float length = outer - inner;
        float curl = arms.twist() * (inner + outer) / 2.0F / length;
        float arc = length * (float) Math.sqrt(1.0F + curl * curl);
        int pieces = painter.pieces(arc, 4, 96);
        for (int arm = 0; arm < arms.count(); arm++) {
            float phi = armAngle(arms, arm, arms.count(), time, turn);
            for (int k = 0; k < pieces; k++) {
                float s0 = (float) k / pieces;
                float s1 = (float) (k + 1) / pieces;
                float mid = (s0 + s1) / 2.0F;
                float mask = 1.0F;
                if (arms.segments() > 0) {
                    float d = fract(mid * arms.segments() + time * arms.flow());
                    mask = d < arms.fill() ? 0.55F + 0.45F * (1.0F - d / arms.fill()) : 0.0F;
                }
                int colour = painter.colour(arms.brightness() * (float) Math.pow(Math.sin(Math.PI * mid), 0.7D) * mask);
                if (colour == 0) {
                    continue;
                }
                float r0 = inner + length * s0;
                float r1 = inner + length * s1;
                float a0 = phi + arms.twist() * s0;
                float a1 = phi + arms.twist() * s1;
                painter.segment(Mth.cos(a0) * r0, Mth.sin(a0) * r0, Mth.cos(a1) * r1, Mth.sin(a1) * r1,
                        lerp(arms.widthCore(), arms.widthTip(), s0), lerp(arms.widthCore(), arms.widthTip(), s1), colour);
            }
        }
    }

    private static void motes(Painter painter, Motes motes, Arms arms, double time, float turn) {
        if (!motes.enabled() || motes.brightness() <= 0.0F) {
            return;
        }
        float life = Math.max(0.1F, motes.trip());
        int count = Math.min(MAX_MOTES, motes.count());
        float[] head = new float[2];
        float[] point = new float[2];
        for (int i = 0; i < count; i++) {
            float[] mote = MOTES[i];
            float progress = fract(time / life + mote[0]);
            float fade = smooth(0.0F, 0.15F, progress) * (1.0F - smooth(0.72F, 1.0F, progress));
            float value = motes.brightness() * fade;
            if (value <= 0.0F) {
                continue;
            }
            motePosition(motes, arms, mote, progress, time, turn, head);
            painter.square(head[0], head[1], motes.size(), painter.colour(value));
            // the trail: earlier points of the same path, each a little dimmer
            for (int j = 1; j <= motes.trail(); j++) {
                float earlier = progress - j * 0.018F;
                if (earlier < 0.0F) {
                    break;
                }
                motePosition(motes, arms, mote, earlier, time - j * 0.018F * life, turn, point);
                float f = 1.0F - (float) j / (motes.trail() + 1);
                painter.segment(head[0], head[1], point[0], point[1], motes.size(), motes.size(),
                        painter.colour(value * (f + 1.0F / (motes.trail() + 1))), painter.colour(value * f));
                head[0] = point[0];
                head[1] = point[1];
            }
        }
    }

    private static void motePosition(Motes motes, Arms arms, float[] mote, float progress, double time, float turn, float[] out) {
        float start = motes.start() * (0.85F + mote[2] * 0.3F);
        float radius = start + (motes.end() - start) * (float) Math.pow(progress, 1.35D);
        float angle;
        if (motes.rideArms()) {
            int count = Math.max(1, arms.count());
            float inner = arms.inner();
            float outer = Math.max(inner + 1.0F, arms.outer());
            float along = (radius - inner) / (outer - inner);
            angle = armAngle(arms, (int) (mote[3] * 64.0F) % count, count, time, turn) + arms.twist() * along
                    + (mote[4] - 0.5F) * 0.35F;
        } else {
            angle = mote[1] * TAU + motes.swirl() * TAU * (float) Math.pow(progress, 1.6D) + turn;
        }
        out[0] = Mth.cos(angle) * radius;
        out[1] = Mth.sin(angle) * radius;
    }

    private static void rays(Painter painter, Rays rays, double time, float turn) {
        if (!rays.enabled() || rays.brightness() <= 0.0F) {
            return;
        }
        int count = Math.min(MAX_RAYS, rays.count());
        float lean = rays.lean() * TAU / 360.0F;
        for (int i = 0; i < count; i++) {
            float[] ray = RAYS[i];
            float angle = (float) (i * TAU / count + rays.spin() * time * TAU) + (ray[2] - 0.5F) * (TAU / count) * 0.3F + turn;
            float pulse = 0.5F + 0.5F * (float) Math.sin(TAU * rays.pulseRate() * time + (float) i / count * TAU * rays.waves()
                    + ray[0] * TAU * rays.randomness());
            float full = Math.max(1.0F, rays.length() + (ray[1] * 2.0F - 1.0F) * rays.lengthSpread());
            float length = full * (1.0F - rays.stretchPulse() + rays.stretchPulse() * pulse);
            float value = rays.brightness() * (1.0F - rays.brightnessPulse() + rays.brightnessPulse() * pulse);
            if (value <= 0.0F || length <= 0.0F) {
                continue;
            }
            float sx = Mth.cos(angle) * rays.start();
            float sy = Mth.sin(angle) * rays.start();
            float ex = sx + Mth.cos(angle + lean) * length;
            float ey = sy + Mth.sin(angle + lean) * length;
            if (!rays.fadeToTip()) {
                painter.segment(sx, sy, ex, ey, rays.widthBase(), rays.widthTip(), painter.colour(value * 1.5F));
                continue;
            }
            // brightest where the ray leaves the star, fading to nothing at the tip in flat bands
            int pieces = Math.max(2, Math.min(24, (int) Math.ceil(length / 3.0F)));
            for (int k = 0; k < pieces; k++) {
                float a = (float) k / pieces;
                float b = (float) (k + 1) / pieces;
                int colour = painter.colour(value * 1.8F * (float) Math.pow(1.0F - (a + b) / 2.0F, 1.4D));
                painter.segment(sx + (ex - sx) * a, sy + (ey - sy) * a, sx + (ex - sx) * b, sy + (ey - sy) * b,
                        lerp(rays.widthBase(), rays.widthTip(), a), lerp(rays.widthBase(), rays.widthTip(), b), colour);
            }
        }
    }

    // drawing ----------------------------------------------------------------------------------------------

    /** Emits quads for one star: positions and widths in cell pixels around its centre, light values to colours. */
    private static final class Painter {
        private final BufferBuilder buf;
        private final Matrix4f pose;
        private final float cx;
        private final float cy;
        private final float scale;
        private final float screenPerCell;
        private final Light light;
        private final int[] ramp;
        private final float opacity;
        private final float minWidth;

        Painter(BufferBuilder buf, Matrix4f pose, float cx, float cy, float scale, float screenPerCell, Light light,
                int[] ramp, float opacity) {
            this.buf = buf;
            this.pose = pose;
            this.cx = cx;
            this.cy = cy;
            this.scale = scale;
            this.screenPerCell = screenPerCell;
            this.light = light;
            this.ramp = ramp;
            this.opacity = opacity;
            // never thinner than a screen pixel, or lines shimmer as the view zooms out
            this.minWidth = Math.max(1.0F, 1.0F / screenPerCell);
        }

        /** How many pieces to cut a path of {@code length} cell pixels into, about {@link #SEGMENT_PIXELS} each on screen. */
        int pieces(float length, int min, int max) {
            return Mth.clamp((int) Math.ceil(length * this.screenPerCell / SEGMENT_PIXELS), min, max);
        }

        /**
         * The colour light of strength {@code value} adds, as ARGB for additive blending, or 0 when it adds nothing.
         * Same mapping as the tuning page: soft-clipped, banded into tone steps, looked up on the god's ramp.
         */
        int colour(float value) {
            if (value <= 0.0F) {
                return 0;
            }
            float a = 1.0F - (float) Math.exp(-value * this.light.gain());
            if (this.light.toneSteps() > 0) {
                a = Math.round(a * this.light.toneSteps()) / (float) this.light.toneSteps();
            }
            int alpha = (int) (a * this.opacity * 255.0F);
            if (alpha <= 0) {
                return 0;
            }
            int tone = Math.min(255, (int) (this.light.toneFloor() + (255.0F - this.light.toneFloor()) * a));
            return Math.min(255, alpha) << 24 | this.ramp[tone];
        }

        void segment(float x0, float y0, float x1, float y1, float width0, float width1, int argb) {
            this.segment(x0, y0, x1, y1, width0, width1, argb, argb);
        }

        /** A quad from (x0, y0) to (x1, y1), its width and colour changing from the first end to the second. */
        void segment(float x0, float y0, float x1, float y1, float width0, float width1, int argb0, int argb1) {
            if (argb0 == 0 && argb1 == 0) {
                return;
            }
            float dx = x1 - x0;
            float dy = y1 - y0;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.001F) {
                return;
            }
            float nx = -dy / length;
            float ny = dx / length;
            float h0 = Math.max(this.minWidth, width0) / 2.0F;
            float h1 = Math.max(this.minWidth, width1) / 2.0F;
            this.vertex(x0 + nx * h0, y0 + ny * h0, argb0);
            this.vertex(x1 + nx * h1, y1 + ny * h1, argb1);
            this.vertex(x1 - nx * h1, y1 - ny * h1, argb1);
            this.vertex(x0 - nx * h0, y0 - ny * h0, argb0);
        }

        void square(float x, float y, float size, int argb) {
            if (argb == 0) {
                return;
            }
            float half = Math.max(this.minWidth, size) / 2.0F;
            this.vertex(x - half, y + half, argb);
            this.vertex(x + half, y + half, argb);
            this.vertex(x + half, y - half, argb);
            this.vertex(x - half, y - half, argb);
        }

        void ring(float inner, float outer, int argb) {
            if (argb == 0 || outer <= inner) {
                return;
            }
            for (int k = 0; k < RING_SEGMENTS; k++) {
                float a0 = TAU * k / RING_SEGMENTS;
                float a1 = TAU * (k + 1) / RING_SEGMENTS;
                this.vertex(Mth.cos(a0) * inner, Mth.sin(a0) * inner, argb);
                this.vertex(Mth.cos(a1) * inner, Mth.sin(a1) * inner, argb);
                this.vertex(Mth.cos(a1) * outer, Mth.sin(a1) * outer, argb);
                this.vertex(Mth.cos(a0) * outer, Mth.sin(a0) * outer, argb);
            }
        }

        private void vertex(float x, float y, int argb) {
            this.buf.vertex(this.pose, this.cx + x * this.scale, this.cy + y * this.scale, 0.0F)
                    .color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                    .endVertex();
        }
    }

    // settings ------------------------------------------------------------------------------------------------

    private static Map<GodNodeType, Look> looks() {
        if (looks == null) {
            looks = new EnumMap<>(GodNodeType.class);
            try (Resource resource = Minecraft.getInstance().getResourceManager().getResource(SETTINGS);
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject nodes = GsonHelper.getAsJsonObject(JsonParser.parseReader(reader).getAsJsonObject(), "nodes");
                for (GodNodeType type : GodNodeType.values()) {
                    if (nodes.has(type.name())) {
                        looks.put(type, look(nodes.getAsJsonObject(type.name())));
                    }
                }
            } catch (IOException | RuntimeException e) {
                WoldsVaults.LOGGER.warn("God node effects {} could not be read; stars draw without effects", SETTINGS, e);
            }
        }
        return looks;
    }

    // missing values fall back to the tuning page's own defaults
    private static Look look(JsonObject node) {
        JsonObject light = part(node, "light");
        JsonObject arms = part(node, "arms");
        JsonObject motes = part(node, "motes");
        JsonObject rays = part(node, "rays");
        JsonObject glow = part(node, "glow");
        return new Look(
                new Light(f(light, "brightness", 1.2F), f(light, "toneFloor", 70.0F), i(light, "toneSteps", 4)),
                new Arms(b(arms, "enabled", true), i(arms, "count", 5), f(arms, "innerRadius", 14.0F),
                        f(arms, "outerRadius", 48.0F), f(arms, "twist", 2.6F), f(arms, "spin", 0.18F),
                        f(arms, "widthCore", 1.0F), f(arms, "widthTip", 3.0F), f(arms, "brightness", 0.55F),
                        i(arms, "segments", 3), Math.max(0.01F, f(arms, "segmentFill", 0.6F)), f(arms, "segmentFlow", 0.8F)),
                new Motes(b(motes, "enabled", true), i(motes, "count", 45), f(motes, "startRadius", 52.0F),
                        f(motes, "fadeRadius", 9.0F), f(motes, "tripTime", 2.6F), f(motes, "swirl", 1.1F),
                        b(motes, "rideArms", false), i(motes, "trail", 6), f(motes, "size", 1.0F),
                        f(motes, "brightness", 1.1F), b(motes, "overStar", false)),
                new Rays(b(rays, "enabled", true), i(rays, "count", 8), f(rays, "startRadius", 4.0F),
                        f(rays, "length", 30.0F), f(rays, "lengthSpread", 4.0F), f(rays, "widthBase", 2.0F),
                        f(rays, "widthTip", 1.0F), f(rays, "orbitSpeed", -0.04F), f(rays, "angle", 0.0F),
                        f(rays, "pulseRate", 0.8F), f(rays, "stretchPulse", 0.45F), f(rays, "brightnessPulse", 0.6F),
                        i(rays, "pulseWaves", 1), f(rays, "pulseRandomness", 0.3F), f(rays, "brightness", 1.0F),
                        b(rays, "fadeToTip", true), b(rays, "overStar", false)),
                new Glow(b(glow, "enabled", true), f(glow, "radius", 20.0F), f(glow, "brightness", 0.35F),
                        f(glow, "pulse", 0.25F), f(glow, "pulseRate", 0.5F)));
    }

    private static JsonObject part(JsonObject node, String name) {
        return GsonHelper.getAsJsonObject(node, name, new JsonObject());
    }

    private static float f(JsonObject json, String key, float fallback) {
        return GsonHelper.getAsFloat(json, key, fallback);
    }

    private static int i(JsonObject json, String key, int fallback) {
        return Math.round(GsonHelper.getAsFloat(json, key, fallback));
    }

    private static boolean b(JsonObject json, String key, boolean fallback) {
        return GsonHelper.getAsBoolean(json, key, fallback);
    }

    private static int[][] ramps() {
        if (ramps == null) {
            ramps = new int[VaultGod.values().length][];
            try (Resource resource = Minecraft.getInstance().getResourceManager().getResource(RAMPS);
                 NativeImage image = NativeImage.read(resource.getInputStream())) {
                for (VaultGod god : VaultGod.values()) {
                    if (god.ordinal() >= image.getHeight()) {
                        continue;
                    }
                    int[] row = new int[256];
                    for (int t = 0; t < 256; t++) {
                        int abgr = image.getPixelRGBA(Math.min(t, image.getWidth() - 1), god.ordinal());
                        row[t] = (abgr & 0xFF) << 16 | (abgr >> 8 & 0xFF) << 8 | (abgr >> 16 & 0xFF);
                    }
                    ramps[god.ordinal()] = row;
                }
            } catch (IOException e) {
                WoldsVaults.LOGGER.warn("God star ramps {} could not be read; using the theme colours instead", RAMPS, e);
            }
            for (VaultGod god : VaultGod.values()) {
                if (ramps[god.ordinal()] == null) {
                    ramps[god.ordinal()] = themeRamp(god);
                }
            }
        }
        return ramps;
    }

    /** Black through the god's deep, dim and accent colours to white, when the ramp texture is missing. */
    private static int[] themeRamp(VaultGod god) {
        int[] stops = {0x000000, GodTreeTheme.accentDeep(god) & 0xFFFFFF, GodTreeTheme.accentDim(god) & 0xFFFFFF,
                GodTreeTheme.accent(god) & 0xFFFFFF, 0xFFFFFF};
        int[] row = new int[256];
        for (int t = 0; t < 256; t++) {
            float at = t / 255.0F * (stops.length - 1);
            int low = Math.min(stops.length - 2, (int) at);
            float mix = at - low;
            int from = stops[low];
            int to = stops[low + 1];
            int r = (int) lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, mix);
            int g = (int) lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, mix);
            int b = (int) lerp(from & 0xFF, to & 0xFF, mix);
            row[t] = r << 16 | g << 8 | b;
        }
        return row;
    }

    // maths -----------------------------------------------------------------------------------------------------

    private static float[][] randomTable(int seed, int rows, int columns) {
        float[][] table = new float[rows][columns];
        int a = seed;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                a += 0x6D2B79F5;
                int t = (a ^ (a >>> 15)) * (1 | a);
                t = (t + (t ^ (t >>> 7)) * (61 | t)) ^ t;
                table[r][c] = ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0F;
            }
        }
        return table;
    }

    private static float fract(double value) {
        return (float) (value - Math.floor(value));
    }

    private static float smooth(float from, float to, float value) {
        float t = Mth.clamp((value - from) / (to - from), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
