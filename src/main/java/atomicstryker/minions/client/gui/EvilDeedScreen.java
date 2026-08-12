package atomicstryker.minions.client.gui;

import atomicstryker.minions.common.EvilDeeds;
import atomicstryker.minions.network.EvilDeedPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EvilDeedScreen extends Screen {
    private final Screen parent;
    private final List<Integer> choices = new ArrayList<>();

    public EvilDeedScreen(Screen parent) {
        super(Component.translatable("screen.minions.deeds.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        choices.clear();
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < EvilDeeds.DEEDS.size(); i++) {
            pool.add(i);
        }
        Collections.shuffle(pool);
        choices.addAll(pool.subList(0, Math.min(3, pool.size())));

        int x = width / 2 - 100;
        int y = height / 4;
        for (int row = 0; row < choices.size(); row++) {
            int deedIndex = choices.get(row);
            EvilDeeds.Deed deed = EvilDeeds.DEEDS.get(deedIndex);
            addRenderableWidget(Button.builder(
                            Component.literal(deed.buttonText()),
                            button -> choose(deedIndex))
                    .bounds(x, y + row * 40, 200, 20).build());
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.nevermind"),
                        button -> minecraft.setScreen(parent))
                .bounds(x, y + 120, 200, 20).build());
    }

    private void choose(int deedIndex) {
        PacketDistributor.sendToServer(new EvilDeedPayload(deedIndex));
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
