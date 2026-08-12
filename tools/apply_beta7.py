from pathlib import Path


def load(path):
    return Path(path).read_text(encoding="utf-8")


def save(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly 1 match, got {count}")
    return text.replace(old, new, 1)


# 1) Harvest block drops straight into the Minion backpack and restore environmental immunity.
path = "src/main/java/atomicstryker/minions/common/entity/MinionEntity.java"
s = load(path)
s = replace_once(
    s,
    "import net.minecraft.world.Container;\n",
    "import net.minecraft.world.Container;\nimport net.minecraft.world.damagesource.DamageSource;\n",
    "DamageSource import",
)
s = replace_once(
    s,
    "import net.minecraft.world.level.block.Blocks;\n",
    "import net.minecraft.world.level.block.Block;\nimport net.minecraft.world.level.block.Blocks;\n",
    "Block import",
)
count = s.count("level.destroyBlock(target, true, this);")
if count != 2:
    raise RuntimeError(f"expected two worker destroyBlock calls, got {count}")
s = s.replace("level.destroyBlock(target, true, this);", "harvestBlockIntoInventory(level, target, state);")
marker = "    private void completeCurrentOrder(ServerLevel level) {\n"
helper = '''    /**
     * Legacy Minions do not leave normal mining drops scattered on the floor.
     * The harvested block is converted to drops using the Minion's visible tool
     * and those drops are inserted directly into its 24-slot backpack. Overflow
     * is left in-world and marks the backpack full so return/deposit logic runs.
     * This is deliberately separate from dropStoredItemsToward(), so the manual
     * \"give me your items\" command keeps its old throw-toward-owner behavior.
     */
    private void harvestBlockIntoInventory(ServerLevel level, BlockPos target, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(target) : null;
        List<ItemStack> drops = Block.getDrops(state, level, target, blockEntity, this, getMainHandItem());

        level.destroyBlock(target, false, this);

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack remaining = inventory.addItem(drop.copy());
            if (!remaining.isEmpty()) {
                inventoryFull = true;
                ItemEntity overflow = new ItemEntity(
                        level,
                        target.getX() + 0.5D,
                        target.getY() + 0.5D,
                        target.getZ() + 0.5D,
                        remaining
                );
                level.addFreshEntity(overflow);
            }
        }
    }

'''
s = replace_once(s, marker, helper + marker, "harvest helper insertion")
ambient_marker = "    @Override\n    protected SoundEvent getAmbientSound() {\n"
invuln = '''    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // The 1.12.2 Minion rejected ordinary damage entirely. For the modern
        // port keep player/mob-caused damage semantics available, but make the
        // worker immune to environmental hazards: lava/fire, drowning,
        // suffocation, falling, cactus, stalactites and other source-less cave
        // damage. Projectile/mob damage still has a source entity.
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

'''
s = replace_once(s, ambient_marker, invuln + ambient_marker, "environmental immunity insertion")
save(path, s)


# 2) Make the held tool visibly swing up/down while a Minion is working.
path = "src/main/java/atomicstryker/minions/client/model/MinionModel.java"
s = load(path)
s = replace_once(
    s,
    "import net.minecraft.resources.ResourceLocation;\n",
    "import net.minecraft.resources.ResourceLocation;\nimport net.minecraft.util.Mth;\n",
    "Mth import",
)
s = replace_once(
    s,
    '''        } else if (entity.isWorking()) {
            rightArm.xRot -= 0.65F;
            leftArm.xRot -= 0.35F;
        }
''',
    '''        } else if (entity.isWorking()) {
            // Do not merely hold the tool at a bent angle. Drive a continuous,
            // obvious mining arc while WORKING is synced from the server.
            float digSwing = Mth.sin(ageInTicks * 1.35F);
            rightArm.xRot = -1.15F + digSwing * 0.90F;
            rightArm.yRot = 0.0F;
            rightArm.zRot = 0.0F;
            leftArm.xRot = -0.35F - digSwing * 0.15F;
        }
''',
    "working arm animation",
)
save(path, s)


# 3) Root menu: Orders only with the Master's Staff, Evil Deeds only without it.
path = "src/main/java/atomicstryker/minions/client/gui/MinionsScreen.java"
s = load(path)
s = replace_once(
    s,
    '''        addRenderableWidget(Button.builder(
                        Component.translatable("screen.minions.orders"),
                        button -> minecraft.setScreen(new MinionOrdersScreen(this)))
                .bounds(x, y, 200, 20).build());

        boolean hasMasterStaff = minecraft != null
                && minecraft.player != null
                && EvilDeeds.hasMasterStaff(minecraft.player);
        if (!hasMasterStaff) {
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.minions.evildeed"),
                            button -> minecraft.setScreen(new EvilDeedScreen(this)))
                    .bounds(x, y + 40, 200, 20).build());
        }
''',
    '''        boolean hasMasterStaff = minecraft != null
                && minecraft.player != null
                && EvilDeeds.hasMasterStaff(minecraft.player);

        if (hasMasterStaff) {
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.minions.orders"),
                            button -> minecraft.setScreen(new MinionOrdersScreen(this)))
                    .bounds(x, y, 200, 20).build());
        } else {
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.minions.evildeed"),
                            button -> minecraft.setScreen(new EvilDeedScreen(this)))
                    .bounds(x, y, 200, 20).build());
        }
''',
    "root menu staff gating",
)
save(path, s)


# 4) Make custom Dig dimensions unmissable and draw them above widgets.
path = "src/main/java/atomicstryker/minions/client/gui/CustomDigScreen.java"
s = load(path)
s = replace_once(
    s,
    '''    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.custom.width", xzSize), width / 2, height / 4 + 40, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.custom.height", ySize), width / 2, height / 4 + 80, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
''',
    '''    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int baseY = height / 4;
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);

        graphics.fill(width / 2 - 90, baseY - 48, width / 2 + 90, baseY - 29, 0xC0000000);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.minions.custom.size", xzSize, ySize, xzSize),
                width / 2,
                baseY - 42,
                0xFFFFFF
        );

        graphics.fill(width / 2 - 70, baseY + 32, width / 2 + 70, baseY + 52, 0xC0000000);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.custom.width", xzSize), width / 2, baseY + 39, 0xFFFFFF);

        graphics.fill(width / 2 - 70, baseY + 72, width / 2 + 70, baseY + 92, 0xC0000000);
        graphics.drawCenteredString(font, Component.translatable("screen.minions.custom.height", ySize), width / 2, baseY + 79, 0xFFFFFF);
    }
''',
    "custom dig live dimensions render",
)
save(path, s)


# 5) Add localized live size label.
for path, old, new in [
    (
        "src/main/resources/assets/minions/lang/en_us.json",
        '  "screen.minions.custom.height": "Height: %s",\n',
        '  "screen.minions.custom.height": "Height: %s",\n  "screen.minions.custom.size": "Dig size: %s x %s x %s",\n',
    ),
    (
        "src/main/resources/assets/minions/lang/pl_pl.json",
        '  "screen.minions.custom.height": "Wysokość: %s",\n',
        '  "screen.minions.custom.height": "Wysokość: %s",\n  "screen.minions.custom.size": "Rozmiar kopania: %s x %s x %s",\n',
    ),
]:
    s = load(path)
    s = replace_once(s, old, new, f"translation {path}")
    save(path, s)


# 6) Version bump and README current-version marker.
path = "gradle.properties"
s = load(path)
s = replace_once(s, "mod_version=2.0.3-1.21.1-beta.6", "mod_version=2.0.3-1.21.1-beta.7", "version bump")
save(path, s)

path = "README.md"
s = load(path)
s = replace_once(s, "**`2.0.3-1.21.1-beta.6`**", "**`2.0.3-1.21.1-beta.7`**", "README version")
save(path, s)

print("beta.7 parity patch applied")
