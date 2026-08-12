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
        super(Component.translatable("screen.minions.orders"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 205;
        int right = width / 2 + 5;
        int y = Math.max(55, height / 4 - 5);
        int row = 30;

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.follow"),
                b -> sendAndClose(MinionCommandPayload.Command.FOLLOW)).bounds(left, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.move"),
                b -> startSelection(MinionSelection.Mode.MOVE)).bounds(left, y + row, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.choptree"),
                b -> startSelection(MinionSelection.Mode.CHOP_TREE)).bounds(left, y + row * 2, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.minevein"),
                b -> startSelection(MinionSelection.Mode.MINE_VEIN)).bounds(left, y + row * 3, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.chest"),
                b -> startSelection(MinionSelection.Mode.ASSIGN_CHEST)).bounds(left, y + row * 4, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.drop"),
                b -> sendAndClose(MinionCommandPayload.Command.DROP_ITEMS)).bounds(left, y + row * 5, 200, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.minions.stop"),
                b -> sendAndClose(MinionCommandPayload.Command.STOP_WORK)).bounds(right, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.unsummon"),
                b -> sendAndClose(MinionCommandPayload.Command.UNSUMMON)).bounds(right, y + row, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.mineshaft"),
                b -> startSelection(MinionSelection.Mode.MINESHAFT)).bounds(right, y + row * 2, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.stripmine"),
                b -> startSelection(MinionSelection.Mode.STRIP_MINE)).bounds(right, y + row * 3, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.customdig_short"),
                b -> minecraft.setScreen(new CustomDigScreen(this))).bounds(right, y + row * 4, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minions.nevermind"),
                b -> minecraft.setScreen(parent)).bounds(right, y + row * 5, 200, 20).build());
    }

    private void startSelection(MinionSelection.Mode mode) {
        MinionSelection.start(mode);
        minecraft.setScreen(null);
    }

    private void sendAndClose(MinionCommandPayload.Command command) {
        PacketDistributor.sendToServer(new MinionCommandPayload(command, BlockPos.ZERO));
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 32, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
