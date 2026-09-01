package xyz.iwolfking.woldsvaults.entities.extraction.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import xyz.iwolfking.woldsvaults.entities.extraction.ExtractorEntity;

public class ExtractorGeoRenderer extends GeoEntityRenderer<ExtractorEntity> {
    public ExtractorGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ExtractorGeoModel());
        this.shadowRadius = 0.0F;
    }
}
