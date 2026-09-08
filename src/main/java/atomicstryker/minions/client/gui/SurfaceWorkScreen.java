package atomicstryker.minions.client.gui;

import atomicstryker.minions.client.MinionSelection;
import atomicstryker.minions.client.SurfaceWorkSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class SurfaceWorkScreen extends Screen {
    private final Screen parent;

    public SurfaceWorkScreen(Screen parent) {
        super(Component.translatable("screen.minions.surface.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 205;
        int right = width / 2 + 5;
        int y = Math.max(64, height / 4 - 5);
        int row = 32;

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.surface.chest"),
                b -> startSelection(MinionSelection.Mode.MATERIAL_CHEST))
                .bounds(left, y, 200, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.surface.texture"),
                b -> startSurfaceSelection(MinionSelection.Mode.TEXTURE_AREA))
                .bounds(left, y + row, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.surface.texture_options"),
                b -> minecraft.setScreen(new TextureOptionsScreen(this)))
                .bounds(left, y + row * 2, 200, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.surface.weather"),
                b -> startSurfaceSelection(MinionSelection.Mode.WEATHER_AREA))
                .bounds(right, y + row, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.surface.weather_options"),
                b -> minecraft.setScreen(new WeatherOptionsScreen(this)))
                .bounds(right, y + row * 2, 200, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.nevermind"),
                b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 100, y + row * 4, 200, 20).build());
    }

    private void startSurfaceSelection(MinionSelection.Mode mode) {
        if (SurfaceWorkSettings.materialChest() == null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("message.minions.surface_need_chest"), true);
            }
            return;
        }
        startSelection(mode);
    }

    private void startSelection(MinionSelection.Mode mode) {
        MinionSelection.start(mode);
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 28, 0xFFFFFF);

        BlockPos chest = SurfaceWorkSettings.materialChest();
        Component chestText = chest == null
                ? Component.translatable("screen.minions.surface.chest_none")
                : Component.translatable("screen.minions.surface.chest_pos", chest.getX(), chest.getY(), chest.getZ());
        graphics.drawCenteredString(font, chestText, width / 2, 46, chest == null ? 0xFF7777 : 0x88FF88);

        int noteY = Math.max(64, height / 4 - 5) + 102;
        graphics.drawCenteredString(font, Component.translatable("screen.minions.surface.ratio_help"),
                width / 2, noteY, 0xBBBBBB);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.surface.weather_help"),
                width / 2, noteY + 12, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
