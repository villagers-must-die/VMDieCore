package org.dishanhai.vmdiecore.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public enum Race {
    ZOMBIE("monsterperspective.race.zombie.name", "THE UNDYING", 0x88C99C, EntityType.ZOMBIE,
        "monsterperspective.race.zombie.story", "monsterperspective.race.zombie.summary",
        new String[]{"monsterperspective.race.zombie.trait_1", "monsterperspective.race.zombie.trait_2", "monsterperspective.race.zombie.trait_3", "monsterperspective.race.zombie.trait_4"},
        new String[]{"monsterperspective.race.zombie.technology_1", "monsterperspective.race.zombie.technology_2", "monsterperspective.race.zombie.technology_3", "monsterperspective.race.zombie.technology_4", "monsterperspective.race.zombie.technology_5"}),
    SKELETON("monsterperspective.race.skeleton.name", "THE SOULKEEPER", 0xE2CF9E, EntityType.SKELETON,
        "monsterperspective.race.skeleton.story", "monsterperspective.race.skeleton.summary",
        new String[]{"monsterperspective.race.skeleton.trait_1", "monsterperspective.race.skeleton.trait_2", "monsterperspective.race.skeleton.trait_3", "monsterperspective.race.skeleton.trait_4", "monsterperspective.race.skeleton.trait_5"},
        new String[]{"monsterperspective.race.skeleton.technology_1", "monsterperspective.race.skeleton.technology_2", "monsterperspective.race.skeleton.technology_3", "monsterperspective.race.skeleton.technology_4", "monsterperspective.race.skeleton.technology_5", "monsterperspective.race.skeleton.technology_6"}),
    ENDERMAN("monsterperspective.race.enderman.name", "THE RIFTWALKER", 0xBD9AEF, EntityType.ENDERMAN,
        "monsterperspective.race.enderman.story", "monsterperspective.race.enderman.summary",
        new String[]{"monsterperspective.race.enderman.trait_1", "monsterperspective.race.enderman.trait_2", "monsterperspective.race.enderman.trait_3"},
        new String[]{"monsterperspective.race.enderman.technology_1", "monsterperspective.race.enderman.technology_2", "monsterperspective.race.enderman.technology_3", "monsterperspective.race.enderman.technology_4", "monsterperspective.race.enderman.technology_5", "monsterperspective.race.enderman.technology_6"}),
    ARTHROPOD("monsterperspective.race.arthropod.name", "THE SILKWEAVER", 0xE9AA75, EntityType.SPIDER,
        "monsterperspective.race.arthropod.story", "monsterperspective.race.arthropod.summary",
        new String[]{"monsterperspective.race.arthropod.trait_1", "monsterperspective.race.arthropod.trait_2", "monsterperspective.race.arthropod.trait_3", "monsterperspective.race.arthropod.trait_4", "monsterperspective.race.arthropod.trait_5"},
        new String[]{"monsterperspective.race.arthropod.technology_1", "monsterperspective.race.arthropod.technology_2", "monsterperspective.race.arthropod.technology_3", "monsterperspective.race.arthropod.technology_4", "monsterperspective.race.arthropod.technology_5", "monsterperspective.race.arthropod.technology_6"}),
    VAMPIRE("monsterperspective.race.vampire.name", "THE NIGHTBORN", 0xE88DAB, null,
        "monsterperspective.race.vampire.story", "monsterperspective.race.vampire.summary",
        new String[]{"monsterperspective.race.vampire.trait_1", "monsterperspective.race.vampire.trait_2", "monsterperspective.race.vampire.trait_3", "monsterperspective.race.vampire.trait_4"},
        new String[]{"monsterperspective.race.vampire.technology_1", "monsterperspective.race.vampire.technology_2", "monsterperspective.race.vampire.technology_3", "monsterperspective.race.vampire.technology_4"});

    public final String title, subtitle, story, summary;
    public final int color;
    public final EntityType<? extends LivingEntity> model;
    public final String[] traits, technology;
    Race(String title, String subtitle, int color, EntityType<? extends LivingEntity> model,
         String story, String summary, String[] traits, String[] technology) {
        this.title=title; this.subtitle=subtitle; this.color=color; this.model=model;
        this.story=story; this.summary=summary; this.traits=traits; this.technology=technology;
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public boolean undead() { return this==ZOMBIE || this==SKELETON; }
    public boolean burns() { return undead() || this==VAMPIRE; }
    public static Race parse(String id) { for(Race r:values()) if(r.id().equals(id)) return r; return null; }
    public static Race of(Player player) { return parse(player.getPersistentData().getString("mp_race")); }
    public static boolean hasTech(Player p, Race tech) { Race r=of(p); return r==tech || r==VAMPIRE; }
}
