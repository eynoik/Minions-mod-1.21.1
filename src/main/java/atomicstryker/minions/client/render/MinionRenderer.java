package atomicstryker.minions.client.render;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.client.model.MinionModel;
import atomicstryker.minions.common.entity.MinionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class MinionRenderer extends MobRenderer<MinionEntity, MinionModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MinionsMod.MOD_ID,
            "textures/model/as_entityminion.png"
    );

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new MinionModel(context.bakeLayer(MinionModel.LAYER)), 0.3F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(MinionEntity entity) {
        return TEXTURE;
    }
}
