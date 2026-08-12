package atomicstryker.minions.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MinionsScreen extends Screen {
    public MinionsScreen() {
        super(Component.translatable("screen.minions.title"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 4 + 20;

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.orders"),
                        button -> minecraft.setScreen(new MinionOrdersScreen(this)))
                .bounds(x, y, 200, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.evildeed"),
                        button -> minecraft.setScreen(new EvilDeedScreen(this)))
                .bounds(x, y + 40, 200, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.nevermind"),
                        button -> onClose())
                .bounds(x, y + 120, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
