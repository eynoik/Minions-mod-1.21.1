package atomicstryker.minions.client.gui;

import atomicstryker.minions.client.SurfaceWorkSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

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

        addValueRow(leftCenter, y,
                () -> Component.translatable("screen.minions.options.coverage", options.coverage),
                () -> options.coverage = clamp(options.coverage - 5, 5, 100),
                () -> options.coverage = clamp(options.coverage + 5, 5, 100));
        addValueRow(leftCenter, y + row,
                () -> Component.translatable("screen.minions.options.patch_size", options.patchSize),
                () -> options.patchSize = clamp(options.patchSize - 1, 1, 8),
                () -> options.patchSize = clamp(options.patchSize + 1, 1, 8));
        addValueRow(leftCenter, y + row * 2,
                () -> Component.translatable("screen.minions.options.patch_strength", options.patchStrength),
                () -> options.patchStrength = clamp(options.patchStrength - 5, 0, 100),
                () -> options.patchStrength = clamp(options.patchStrength + 5, 0, 100));
        addValueRow(leftCenter, y + row * 3,
                () -> Component.translatable("screen.minions.options.weather_strength", options.weatherStrength),
                () -> options.weatherStrength = clamp(options.weatherStrength - 5, 0, 100),
                () -> options.weatherStrength = clamp(options.weatherStrength + 5, 0, 100));

        addValueRow(rightCenter, y,
                () -> Component.translatable("screen.minions.options.ground_bias", options.groundBias),
                () -> options.groundBias = clamp(options.groundBias - 5, 0, 100),
                () -> options.groundBias = clamp(options.groundBias + 5, 0, 100));
        addValueRow(rightCenter, y + row,
                () -> Component.translatable("screen.minions.options.water_bias", options.waterBias),
                () -> options.waterBias = clamp(options.waterBias - 5, 0, 100),
                () -> options.waterBias = clamp(options.waterBias + 5, 0, 100));
        addValueRow(rightCenter, y + row * 2,
                () -> Component.translatable("screen.minions.options.sky_bias", options.skyBias),
                () -> options.skyBias = clamp(options.skyBias - 5, 0, 100),
                () -> options.skyBias = clamp(options.skyBias + 5, 0, 100));
        addValueRow(rightCenter, y + row * 3,
                () -> Component.translatable("screen.minions.options.water_radius", options.waterRadius),
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

    private void addValueRow(int center, int y, Supplier<Component> label, Runnable minus, Runnable plus) {
        Button value = Button.builder(label.get(), b -> {
        }).bounds(center - 100, y, 200, 20).build();
        value.active = false;
        addRenderableWidget(value);

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    minus.run();
                    value.setMessage(label.get());
                }).bounds(center - 130, y, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    plus.run();
                    value.setMessage(label.get());
                }).bounds(center + 102, y, 28, 20).build());
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
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 28, 0xFFFFFF);

        int y = Math.max(52, height / 4 - 14);
        int row = 29;
        graphics.drawCenteredString(font, Component.translatable("screen.minions.weather_options.help"),
                width / 2, y + row * 6 + 4, 0xAAAAAA);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
