package org.dishanhai.vmdiecore.monster;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MonsterPerspective {
    // Keep the released content namespace so existing saves, recipes and golems retain their IDs.
    // VMDieCore owns registration and lifecycle; this is no longer a separate mod entry point.
    public static final String ID="monsterperspective";
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredItem<Item> FLESH=ITEMS.registerSimpleItem("flesh_part");
    public static final DeferredItem<Item> BONE=ITEMS.registerSimpleItem("bone_part");
    public static final DeferredItem<Item> SOUL_GLASS=ITEMS.registerSimpleItem("soul_glass");
    public static final DeferredItem<Item> SOUL_BOTTLE=ITEMS.registerSimpleItem("soul_bottle", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> FULL_SOUL=ITEMS.registerSimpleItem("filled_soul_bottle", new Item.Properties().stacksTo(1));
    public static final DeferredItem<TechItem> SCROLL=ITEMS.registerItem("teleport_scroll", p->new TechItem(p, "scroll"), new Item.Properties().stacksTo(1));
    public static final DeferredItem<TechItem> BOUND=ITEMS.registerItem("binding_pearl", p->new TechItem(p,"binding"), new Item.Properties().stacksTo(1));
    public static final DeferredItem<TechItem> INFINITE=ITEMS.registerItem("infinite_pearl", p->new TechItem(p,"infinite"), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> VENOM=ITEMS.registerSimpleItem("venom_bottle");
    public static final String[] WEAVES={"fortune","looting","silk_touch","sharpness","protection","feather_falling","fire_protection","unbreaking"};
    static { for(String w:WEAVES) ITEMS.registerSimpleItem("silk_"+w); }
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SilkRecipe>> SILK_RECIPE=
        RECIPES.register("silk_enchant", ()->new SimpleCraftingRecipeSerializer<>(SilkRecipe::new));
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue RESELECT;
    public static final ModConfigSpec.IntValue CHANGE_COOLDOWN, BONE_COOLDOWN;
    static {
        var b=new ModConfigSpec.Builder();
        RESELECT=b.comment("Allow players to change race in the archive.").define("allowReselection", true);
        CHANGE_COOLDOWN=b.comment("Race change cooldown in seconds.").defineInRange("raceChangeCooldown", 60, 0, 3600);
        BONE_COOLDOWN=b.comment("Bone golem resurrection cooldown in seconds.").defineInRange("boneReviveCooldown",300,1,36000);
        SPEC=b.build();
        TABS.register("archive", ()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.literal("永夜档案"))
            .icon(()->FULL_SOUL.toStack()).displayItems((p,o)->ITEMS.getEntries().forEach(i->o.accept(i.get()))).build());
    }
    private MonsterPerspective() {}

    public static void initialize(IEventBus bus, ModContainer container) {
        ITEMS.register(bus); RECIPES.register(bus); TABS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER,SPEC,"monsterperspective-server.toml");
        bus.addListener(Network::register);
        NeoForge.EVENT_BUS.register(new RaceEvents());
        NeoForge.EVENT_BUS.register(new TechEvents());
        NeoForge.EVENT_BUS.register(new Commands());
        bus.addListener(Integration::setup);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID,path); }
}
