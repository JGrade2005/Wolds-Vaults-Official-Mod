package xyz.iwolfking.woldsvaults.entities.extraction.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import xyz.iwolfking.woldsvaults.WoldsVaults;
import xyz.iwolfking.woldsvaults.entities.extraction.ExtractorEntity;

import java.util.HashSet;
import java.util.Set;

public class ExtractorGeoModel extends AnimatedGeoModel<ExtractorEntity> {
    private static final Set<String> MISSING_BONES = new HashSet<>();

    @Override
    public ResourceLocation getModelLocation(ExtractorEntity entity) {
        return WoldsVaults.id("geo/extractor.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ExtractorEntity entity) {
        return WoldsVaults.id("textures/entity/extractor.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ExtractorEntity entity) {
        return WoldsVaults.id("animations/extractor.animation.json");
    }

    /**
     * Hides every part-variant bone, then unhides the ones the entity has equipped —
     * one geo, one texture, bone visibility does the modularity.
     */
    @Override
    public void setCustomAnimations(ExtractorEntity entity, int instanceId, AnimationEvent animationEvent) {
        super.setCustomAnimations(entity, instanceId, animationEvent);
        for (String bone : ExtractorEntity.ALL_VARIANT_BONES) {
            this.setBoneHidden(bone, true);
        }
        for (String bone : entity.getEquippedVariantBones()) {
            this.setBoneHidden(bone.trim(), false);
        }
    }

    private void setBoneHidden(String name, boolean hidden) {
        IBone bone = this.getAnimationProcessor().getBone(name);
        if (bone == null) {
            if (MISSING_BONES.add(name)) {
                WoldsVaults.LOGGER.error("Extractor rig has no bone named '{}'; that part variant cannot be toggled.", name);
            }
            return;
        }
        bone.setHidden(hidden);
    }
}
