package atomicstryker.minions.client.gui;

import atomicstryker.minions.client.MinionSelection;
import atomicstryker.minions.network.MinionCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MinionOrdersScreen extends Screen {
    private final Screen parent;

    public MinionOrdersScreen(Screen parent) {
        super(Component.translatable("screen.minions.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2 - 100;
        int baseY = height / 4;

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.unsummon"),
                        button -> sendAndClose(MinionCommandPayload.Command.UNSUMMON))
                .bounds(centerX, baseY, 200, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.mineshaft"),
                        button -> {
                            MinionSelection.start(MinionSelection.Mode.MINESHAFT);
                            minecraft.setScreen(null);
                        })
                .bounds(centerX, baseY + 40, 200, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.stripmine"),
                        button -> {
                            MinionSelection.start(MinionSelection.Mode.STRIP_MINE);
                            minecraft.setScreen(null);
                        })
                .bounds(centerX, baseY + 80, 200, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.customdig_short"),
                        button -> minecraft.setScreen(new CustomDigScreen(this)))
                .bounds(width * 3 / 4, baseY + 40, 100, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.nevermind"),
                        button -> minecraft.setScreen(parent))
                .bounds(centerX, baseY + 120, 200, 20).build());
    }

    private void sendAndClose(MinionCommandPayload.Command command) {
        PacketDistributor.sendToServer(new MinionCommandPayload(command, BlockPos.ZERO));
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
