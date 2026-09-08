package atomicstryker.minions.client.gui;

import atomicstryker.minions.client.SurfaceWorkSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class TextureOptionsScreen extends Screen {
    private final Screen parent;
    private final SurfaceWorkSettings.TextureOptions options = SurfaceWorkSettings.texture();

    public TextureOptionsScreen(Screen parent) {
        super(Component.translatable("screen.minions.texture_options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = Math.max(52, height / 4 - 8);
        int row = 30;

        addValueRow(center, y,
                () -> Component.translatable("screen.minions.options.coverage", options.coverage),
                () -> options.coverage = clamp(options.coverage - 5, 5, 100),
                () -> options.coverage = clamp(options.coverage + 5, 5, 100));
        addValueRow(center, y + row,
                () -> Component.translatable("screen.minions.options.patch_size", options.patchSize),
                () -> options.patchSize = clamp(options.patchSize - 1, 1, 8),
                () -> options.patchSize = clamp(options.patchSize + 1, 1, 8));
        addValueRow(center, y + row * 2,
                () -> Component.translatable("screen.minions.options.patch_strength", options.patchStrength),
                () -> options.patchStrength = clamp(options.patchStrength - 5, 0, 100),
                () -> options.patchStrength = clamp(options.patchStrength + 5, 0, 100));

        addRenderableWidget(Button.builder(surfaceLabel(), b -> {
                    options.surfaceOnly = !options.surfaceOnly;
                    b.setMessage(surfaceLabel());
                }).bounds(center - 130, y + row * 3, 260, 20).build());

        addRenderableWidget(Button.builder(targetLabel(), b -> {
                    options.targetMode = options.targetMode.next();
                    b.setMessage(targetLabel());
                }).bounds(center - 130, y + row * 4, 260, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.options.reset"), b -> {
                    options.reset();
                    minecraft.setScreen(new TextureOptionsScreen(parent));
                }).bounds(center - 130, y + row * 5, 125, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.options.done"), b -> minecraft.setScreen(parent))
                .bounds(center + 5, y + row * 5, 125, 20).build());
    }

    private void addValueRow(int center, int y, Supplier<Component> label, Runnable minus, Runnable plus) {
        Button value = Button.builder(label.get(), b -> {
        }).bounds(center - 110, y, 220, 20).build();
        value.active = false;
        addRenderableWidget(value);

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    minus.run();
                    value.setMessage(label.get());
                }).bounds(center - 145, y, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    plus.run();
                    value.setMessage(label.get());
                }).bounds(center + 117, y, 28, 20).build());
    }

    private Component surfaceLabel() {
        return Component.translatable("screen.minions.options.surface_only",
                Component.translatable(options.surfaceOnly ? "screen.minions.options.on" : "screen.minions.options.off"));
    }

    private Component targetLabel() {
        return Component.translatable("screen.minions.options.target_mode", targetModeName(options.targetMode));
    }

    static Component targetModeName(SurfaceWorkSettings.TargetMode mode) {
        return switch (mode) {
            case SMART -> Component.translatable("screen.minions.options.target.smart");
            case PALETTE_ONLY -> Component.translatable("screen.minions.options.target.palette");
            case ALL_STRUCTURAL -> Component.translatable("screen.minions.options.target.all");
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 28, 0xFFFFFF);

        int y = Math.max(52, height / 4 - 8);
        int row = 30;
        graphics.drawCenteredString(font, Component.translatable("screen.minions.texture_options.help"),
                width / 2, y + row * 6 + 5, 0xAAAAAA);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
