from pathlib import Path


def replace_exact(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Pattern not found in {path}:\n{old}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# Slight durability bump: 10 hearts -> 12 hearts.
replace_exact(
    "src/main/java/atomicstryker/minions/common/entity/MinionEntity.java",
    ".add(Attributes.MAX_HEALTH, 20.0D)",
    ".add(Attributes.MAX_HEALTH, 24.0D)",
)

# Make fall immunity explicit instead of depending only on DamageSource classification.
replace_exact(
    "src/main/java/atomicstryker/minions/common/entity/MinionEntity.java",
    "    @Override\n    public boolean isInvulnerableTo(DamageSource source) {",
    "    @Override\n    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {\n        return false;\n    }\n\n    @Override\n    public boolean isInvulnerableTo(DamageSource source) {",
)

# Master's Staff lightning must never damage or ignite Minions.
replace_exact(
    "src/main/java/atomicstryker/minions/common/StaffMagicEvents.java",
    "import atomicstryker.minions.MinionsMod;\n",
    "import atomicstryker.minions.MinionsMod;\nimport atomicstryker.minions.common.entity.MinionEntity;\n",
)
replace_exact(
    "src/main/java/atomicstryker/minions/common/StaffMagicEvents.java",
    "                target -> target != player\n",
    "                target -> target != player && !(target instanceof MinionEntity)\n",
)

replace_exact(
    "gradle.properties",
    "mod_version=2.0.3-1.21.1-beta.8",
    "mod_version=2.0.3-1.21.1-beta.9",
)

replace_exact(
    "README.md",
    "**`2.0.3-1.21.1-beta.8`**",
    "**`2.0.3-1.21.1-beta.9`**",
)
replace_exact(
    "README.md",
    "- owner-missing despawn timer\n",
    "- 24 HP Minions with explicit fall-damage immunity; environmental hazards remain ignored and Master's Staff lightning cannot hurt or ignite Minions\n- owner-missing despawn timer\n",
)
