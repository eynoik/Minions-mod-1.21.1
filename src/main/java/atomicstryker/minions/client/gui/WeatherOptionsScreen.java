package atomicstryker.minions.client.gui;

import atomicstryker.minions.client.SurfaceWorkSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WeatherOptionsScreen extends Screen {
    private final Screen parent;
    private final SurfaceWorkSettings.WeatherOptions options = SurfaceWorkSettings.weather();

    public WeatherOptionsScreen(Screen parent) {
        super(Component.translatable("screen.minions.weather_options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = Math.max(52, height / 4 - 14);
        int row = 29;
        int leftCenter = width / 2 - 155;
        int rightCenter = width / 2 + 155;

        addPair(leftCenter, y,
                () -> options.coverage = clamp(options.coverage - 5, 5, 100),
                () -> options.coverage = clamp(options.coverage + 5, 5, 100));
        addPair(leftCenter, y + row,
                () -> options.patchSize = clamp(options.patchSize - 1, 1, 8),
                () -> options.patchSize = clamp(options.patchSize + 1, 1, 8));
        addPair(leftCenter, y + row * 2,
                () -> options.patchStrength = clamp(options.patchStrength - 5, 0, 100),
                () -> options.patchStrength = clamp(options.patchStrength + 5, 0, 100));
        addPair(leftCenter, y + row * 3,
                () -> options.weatherStrength = clamp(options.weatherStrength - 5, 0, 100),
                () -> options.weatherStrength = clamp(options.weatherStrength + 5, 0, 100));

        addPair(rightCenter, y,
                () -> options.groundBias = clamp(options.groundBias - 5, 0, 100),
                () -> options.groundBias = clamp(options.groundBias + 5, 0, 100));
        addPair(rightCenter, y + row,
                () -> options.waterBias = clamp(options.waterBias - 5, 0, 100),
                () -> options.waterBias = clamp(options.waterBias + 5, 0, 100));
        addPair(rightCenter, y + row * 2,
                () -> options.skyBias = clamp(options.skyBias - 5, 0, 100),
                () -> options.skyBias = clamp(options.skyBias + 5, 0, 100));
        addPair(rightCenter, y + row * 3,
                () -> options.waterRadius = clamp(options.waterRadius - 1, 1, 6),
                () -> options.waterRadius = clamp(options.waterRadius + 1, 1, 6));

        addRenderableWidget(Button.builder(surfaceLabel(), b -> {
                    options.surfaceOnly = !options.surfaceOnly;
                    b.setMessage(surfaceLabel());
                }).bounds(width / 2 - 285, y + row * 4, 280, 20).build());
        addRenderableWidget(Button.builder(targetLabel(), b -> {
                    options.targetMode = options.targetMode.next();
                    b.setMessage(targetLabel());
                }).bounds(width / 2 + 5, y + row * 4, 280, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.options.reset"), b -> {
                    options.reset();
                    minecraft.setScreen(new WeatherOptionsScreen(parent));
                }).bounds(width / 2 - 130, y + row * 5, 125, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.options.done"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 + 5, y + row * 5, 125, 20).build());
    }

    private void addPair(int center, int y, Runnable minus, Runnable plus) {
        addRenderableWidget(Button.builder(Component.literal("-"), b -> minus.run())
                .bounds(center - 130, y, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> plus.run())
                .bounds(center + 102, y, 28, 20).build());
    }

    private Component surfaceLabel() {
        return Component.translatable("screen.minions.options.surface_only",
                Component.translatable(options.surfaceOnly ? "screen.minions.options.on" : "screen.minions.options.off"));
    }

    private Component targetLabel() {
        return Component.translatable("screen.minions.options.target_mode",
                TextureOptionsScreen.targetModeName(options.targetMode));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 28, 0xFFFFFF);

        int y = Math.max(52, height / 4 - 14);
        int row = 29;
        int leftCenter = width / 2 - 155;
        int rightCenter = width / 2 + 155;

        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.coverage", options.coverage),
                leftCenter, y + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.patch_size", options.patchSize),
                leftCenter, y + row + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.patch_strength", options.patchStrength),
                leftCenter, y + row * 2 + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.weather_strength", options.weatherStrength),
                leftCenter, y + row * 3 + 6, 0xFFFFFF);

        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.ground_bias", options.groundBias),
                rightCenter, y + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.water_bias", options.waterBias),
                rightCenter, y + row + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.sky_bias", options.skyBias),
                rightCenter, y + row * 2 + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.options.water_radius", options.waterRadius),
                rightCenter, y + row * 3 + 6, 0xFFFFFF);

        graphics.drawCenteredString(font, Component.translatable("screen.minions.weather_options.help"),
                width / 2, y + row * 6 + 4, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
