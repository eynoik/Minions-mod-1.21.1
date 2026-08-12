package atomicstryker.minions.client;

import atomicstryker.minions.MinionsMod;
import atomicstryker.minions.client.gui.MinionsScreen;
import atomicstryker.minions.client.model.MinionModel;
import atomicstryker.minions.client.render.MinionRenderer;
import atomicstryker.minions.registry.MinionsEntities;
import atomicstryker.minions.registry.MinionsItems;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MinionsMod.MOD_ID, value = Dist.CLIENT)
public final class MinionsClientEvents {
    private static final KeyMapping MENU_KEY = new KeyMapping(
            "key.minions.menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.minions"
    );

    private static boolean suppressOffhandUse;

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MinionModel.LAYER, MinionModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MinionsEntities.MINION.get(), MinionRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MENU_KEY);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            MinionSelection.clear();
            return;
        }

        if (MinionSelection.isActive()) {
            if (!minecraft.player.getMainHandItem().is(MinionsItems.MASTER_STAFF.get())) {
                MinionSelection.clear();
            } else if (minecraft.screen == null) {
                MinionSelection.updateFromCrosshair();
            }
        }

        if (minecraft.screen != null) {
            return;
        }
        while (MENU_KEY.consumeClick()) {
            MinionSelection.clear();
            minecraft.setScreen(new MinionsScreen());
        }
    }

    @SubscribeEvent
    public static void interactionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (suppressOffhandUse && event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.getHand() == InteractionHand.OFF_HAND) {
                suppressOffhandUse = false;
            }
            return;
        }

        if (!event.isUseItem() || !MinionSelection.isActive() || minecraft.player == null) {
            return;
        }
        if (!minecraft.player.getMainHandItem().is(MinionsItems.MASTER_STAFF.get())) {
            MinionSelection.clear();
            return;
        }

        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND && MinionSelection.confirm()) {
            event.setSwingHand(true);
            suppressOffhandUse = true;
        } else {
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void renderSelection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !MinionSelection.isActive()) {
            return;
        }

        AABB box = MinionSelection.mainBox();
        if (box == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        LevelRenderer.renderLineBox(poseStack, lines, box, 0.25F, 0.95F, 1.0F, 0.95F);
        renderGrid(poseStack, lines, box);
        for (AABB helper : MinionSelection.helperBoxes()) {
            LevelRenderer.renderLineBox(poseStack, lines, helper.inflate(0.02D), 1.0F, 0.55F, 0.15F, 0.95F);
        }

        poseStack.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static void renderGrid(PoseStack poseStack, VertexConsumer lines, AABB box) {
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.ceil(box.maxX);
        int maxY = (int) Math.ceil(box.maxY);
        int maxZ = (int) Math.ceil(box.maxZ);

        for (int x = minX + 1; x < maxX; x++) {
            LevelRenderer.renderLineBox(poseStack, lines,
                    new AABB(x, box.minY, box.minZ, x, box.maxY, box.maxZ),
                    0.55F, 0.75F, 0.85F, 0.35F);
        }
        for (int y = minY + 1; y < maxY; y++) {
            LevelRenderer.renderLineBox(poseStack, lines,
                    new AABB(box.minX, y, box.minZ, box.maxX, y, box.maxZ),
                    0.55F, 0.75F, 0.85F, 0.35F);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            LevelRenderer.renderLineBox(poseStack, lines,
                    new AABB(box.minX, box.minY, z, box.maxX, box.maxY, z),
                    0.55F, 0.75F, 0.85F, 0.35F);
        }
    }

    private MinionsClientEvents() {
    }
}
