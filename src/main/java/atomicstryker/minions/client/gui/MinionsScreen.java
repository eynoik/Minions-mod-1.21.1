package atomicstryker.minions.client.gui;

import atomicstryker.minions.network.MinionCommandPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MinionsScreen extends Screen {
    public MinionsScreen() {
        super(Component.translatable("screen.minions.title"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 105;
        int y = height / 4;
        int w = 100;
        int h = 20;
        int gap = 24;

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.follow"), button -> send(MinionCommandPayload.Command.FOLLOW))
                .bounds(x, y, w, h).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.unsummon"), button -> send(MinionCommandPayload.Command.UNSUMMON))
                .bounds(x + 110, y, w, h).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.mineshaft"), button -> sendTarget(MinionCommandPayload.Command.DIG_STAIRWELL))
                .bounds(x, y + gap, w, h).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.stripmine"), button -> sendTarget(MinionCommandPayload.Command.STRIP_MINE))
                .bounds(x + 110, y + gap, w, h).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.customdig"), button -> sendTarget(MinionCommandPayload.Command.CUSTOM_DIG))
                .bounds(x, y + gap * 2, w, h).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.minevein"), button -> sendTarget(MinionCommandPayload.Command.MINE_VEIN))
                .bounds(x + 110, y + gap * 2, w, h).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.choptree"), button -> sendTarget(MinionCommandPayload.Command.CHOP_TREE))
                .bounds(x, y + gap * 3, w, h).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.drop"), button -> send(MinionCommandPayload.Command.DROP_ITEMS))
                .bounds(x + 110, y + gap * 3, w, h).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 100, y + gap * 5, 200, h).build());
    }

    private void send(MinionCommandPayload.Command command) {
        PacketDistributor.sendToServer(new MinionCommandPayload(command, BlockPos.ZERO));
        onClose();
    }

    private void sendTarget(MinionCommandPayload.Command command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult blockHit) {
            PacketDistributor.sendToServer(new MinionCommandPayload(command, blockHit.getBlockPos()));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 35, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.hint"), width / 2, 50, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
