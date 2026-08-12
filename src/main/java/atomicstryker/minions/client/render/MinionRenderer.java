package atomicstryker.minions.client.render;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Temporary renderer used while the original ModelMinion geometry is being ported.
 * It deliberately keeps the original Minions texture so entity registration can be
 * exercised on a client without leaving the EntityType renderer-less.
 */
public final class MinionRenderer extends MobRenderer<MinionEntity, HumanoidModel<MinionEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MinionsMod.MOD_ID,
            "textures/model/as_entityminion.png"
    );

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(MinionEntity entity) {
        return TEXTURE;
    }
}
