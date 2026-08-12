package atomicstryker.minions.client.model;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class MinionModel extends HumanoidModel<MinionEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MinionsMod.MOD_ID, "minion"),
            "main"
    );

    public MinionModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 5.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(22, 0)
                        .addBox(-4.0F, -3.0F, -2.0F, 8.0F, 8.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 14.0F, 2.0F));
        body.addOrReplaceChild("backpack",
                CubeListBuilder.create().texOffs(11, 13)
                        .addBox(-3.0F, -2.0F, -2.0F, 6.0F, 7.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, 0.7853982F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 19)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(-4.0F, 11.0F, 1.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 19).mirror()
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(4.0F, 11.0F, 1.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 10)
                        .addBox(-2.0F, 0.0F, 0.0F, 2.0F, 5.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(-1.0F, 19.0F, 1.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 10).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 2.0F, 5.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(1.0F, 19.0F, 1.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MinionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // HumanoidModel assumes vanilla player pivots and rewrites them every frame.
        // The original Minion model uses a much shorter, lower gnome-like skeleton,
        // so restore the baked pivots after vanilla has calculated the rotations.
        head.setPos(0.0F, 12.0F, 0.0F);
        hat.setPos(0.0F, 12.0F, 0.0F);
        body.setPos(0.0F, 14.0F, 2.0F);
        rightArm.setPos(-4.0F, 11.0F, 1.0F);
        leftArm.setPos(4.0F, 11.0F, 1.0F);
        rightLeg.setPos(-1.0F, 19.0F, 1.0F);
        leftLeg.setPos(1.0F, 19.0F, 1.0F);

        if (entity.isVehicle()) {
            rightArm.xRot = (float) Math.PI;
            leftArm.xRot = (float) Math.PI;
        } else if (entity.isWorking()) {
            // Do not merely hold the tool at a bent angle. Drive a continuous,
            // obvious mining arc while WORKING is synced from the server.
            float digSwing = Mth.sin(ageInTicks * 1.35F);
            rightArm.xRot = -1.15F + digSwing * 0.90F;
            rightArm.yRot = 0.0F;
            rightArm.zRot = 0.0F;
            leftArm.xRot = -0.35F - digSwing * 0.15F;
        }
    }
}
