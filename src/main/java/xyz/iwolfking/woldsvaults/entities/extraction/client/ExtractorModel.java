package xyz.iwolfking.woldsvaults.entities.extraction.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import xyz.iwolfking.woldsvaults.entities.extraction.ExtractorEntity;

public class ExtractorModel extends EntityModel<ExtractorEntity> {
    private final ModelPart root;
    private final ModelPart drill;

    public ExtractorModel(ModelPart root) {
        this.root = root;
        this.drill = root.getChild("drill");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -6.0F, -8.0F, 16.0F, 6.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-5.0F, -16.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("crate", CubeListBuilder.create().texOffs(40, 22)
                .addBox(5.0F, -14.0F, -4.0F, 5.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("drill", CubeListBuilder.create().texOffs(0, 42)
                        .addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F)
                        .texOffs(16, 42).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(ExtractorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.drill.yRot = entity.isDrillingAnim() ? ageInTicks * 0.7F : 0.0F;
        this.drill.y = entity.isDrillingAnim() ? 8.0F + (float) Math.sin(ageInTicks * 0.5F) * 0.5F : 8.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
