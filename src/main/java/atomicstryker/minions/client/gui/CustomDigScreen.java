package atomicstryker.minions.client.gui;

import atomicstryker.minions.client.MinionSelection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CustomDigScreen extends Screen {
    private final Screen parent;
    private int xzSize;
    private int ySize;

    public CustomDigScreen(Screen parent) {
        super(Component.translatable("screen.minions.custom.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        xzSize = MinionSelection.customSizeXZ();
        ySize = MinionSelection.customSizeY();

        int baseY = height / 4;
        int centerX = width / 2 - 100;

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.custom.confirm"), button -> {
                    MinionSelection.startCustom(xzSize, ySize);
                    minecraft.setScreen(null);
                })
                .bounds(centerX, baseY - 20, 200, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.custom.reset"), button -> {
                    xzSize = 3;
                    ySize = 3;
                })
                .bounds(centerX, baseY + 10, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-10"), button -> changeWidth(-10))
                .bounds(width / 10, baseY + 35, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-2"), button -> changeWidth(-2))
                .bounds(width * 3 / 10, baseY + 35, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+2"), button -> changeWidth(2))
                .bounds(width * 6 / 10, baseY + 35, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), button -> changeWidth(10))
                .bounds(width * 8 / 10, baseY + 35, 50, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-5"), button -> changeHeight(-5))
                .bounds(width / 10, baseY + 75, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), button -> changeHeight(-1))
                .bounds(width * 3 / 10, baseY + 75, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), button -> changeHeight(1))
                .bounds(width * 6 / 10, baseY + 75, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+5"), button -> changeHeight(5))
                .bounds(width * 8 / 10, baseY + 75, 50, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.nevermind"), button -> minecraft.setScreen(parent))
                .bounds(centerX, baseY + 120, 200, 20).build());
    }

    private void changeWidth(int amount) {
        xzSize += amount;
        if ((xzSize & 1) == 0) {
            xzSize += amount >= 0 ? 1 : -1;
        }
        xzSize = Math.max(3, Math.min(71, xzSize));
    }

    private void changeHeight(int amount) {
        ySize = Math.max(3, Math.min(25, ySize + amount));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.custom.width", xzSize), width / 2, height / 4 + 40, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.custom.height", ySize), width / 2, height / 4 + 80, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
