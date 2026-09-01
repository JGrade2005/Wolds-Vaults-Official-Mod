package xyz.iwolfking.woldsvaults.entities.extraction.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.client.init.ModModelLayers;
import xyz.iwolfking.woldsvaults.entities.extraction.ExtractorEntity;

import javax.annotation.Nonnull;

public class ExtractorRenderer extends LivingEntityRenderer<ExtractorEntity, ExtractorModel> {
    private static final ResourceLocation TEXTURE = WoldsVaults.id("textures/entity/extractor.png");

    public ExtractorRenderer(EntityRendererProvider.Context context) {
        super(context, new ExtractorModel(context.bakeLayer(ModModelLayers.EXTRACTOR)), 0.8F);
    }

    @Override
    @Nonnull
    public ResourceLocation getTextureLocation(@Nonnull ExtractorEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(@Nonnull ExtractorEntity entity) {
        return false;
    }
}
