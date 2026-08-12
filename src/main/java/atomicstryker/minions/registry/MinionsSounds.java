package atomicstryker.minions.registry;

import atomicstryker.minions.MinionsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MinionsSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MinionsMod.MOD_ID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_NAME = new HashMap<>();

    public static final DeferredHolder<SoundEvent, SoundEvent> BABY_SEAL_CLUBBING = register("babysealclubbing");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASEBALL_BAT_JINGLE = register("baseballbatjingle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT = register("bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOO = register("boo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOM_HEADSHOT = register("boomheadshot");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN = register("chicken");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORNHOLIO = register("cornholio");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORPORATE_SUPREMACY = register("corporatesupremacy");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRY_SOME_MORE = register("crysomemore");
    public static final DeferredHolder<SoundEvent, SoundEvent> DENIED = register("denied");
    public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_LAUGH = register("evillaugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> FART = register("fart");
    public static final DeferredHolder<SoundEvent, SoundEvent> FFF = register("fff");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOR_YOU = register("foryou");
    public static final DeferredHolder<SoundEvent, SoundEvent> GONG = register("gong");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAB_ANIMAL_ORDER = register("grabanimalorder");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVEYARD = register("graveyard");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAHA = register("haha");
    public static final DeferredHolder<SoundEvent, SoundEvent> KEYBOARD_CAT = register("keyboardcat");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUGHTER_2 = register("laughter2");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEEROY = register("leeroy");
    public static final DeferredHolder<SoundEvent, SoundEvent> MARCH = register("march");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINION_SPAWN = register("minionspawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINION_SQUEAK = register("minionsqueak");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOOO = register("nooo");
    public static final DeferredHolder<SoundEvent, SoundEvent> OH_YEAH = register("ohyeah");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_FOLLOW_PLAYER = register("orderfollowplayer");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_MINESHAFT = register("ordermineshaft");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_TREE_CUTTING = register("ordertreecutting");
    public static final DeferredHolder<SoundEvent, SoundEvent> PFFT = register("pfft");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIOS = register("radios");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANDOM_ORDER = register("randomorder");
    public static final DeferredHolder<SoundEvent, SoundEvent> REPUBLICANS = register("republicans");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROMANCE = register("romance");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN_SATANIST_CULT = register("spawnsatanistcult");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUPERSTAR_ME = register("superstarme");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEABAG_NUNS = register("teabagnuns");
    public static final DeferredHolder<SoundEvent, SoundEvent> GODS_PLEASED = register("thegodsarepleaseedwithyoursacrifice");
    public static final DeferredHolder<SoundEvent, SoundEvent> GODS_REWARDED = register("thegodshaverewardedyouroffering");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        DeferredHolder<SoundEvent, SoundEvent> holder = SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MinionsMod.MOD_ID, name))
        );
        BY_NAME.put(name, holder);
        return holder;
    }

    public static SoundEvent byName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String key = rawName.toLowerCase(Locale.ROOT);
        DeferredHolder<SoundEvent, SoundEvent> holder = BY_NAME.get(key);
        return holder == null ? null : holder.get();
    }

    private MinionsSounds() {
    }
}
