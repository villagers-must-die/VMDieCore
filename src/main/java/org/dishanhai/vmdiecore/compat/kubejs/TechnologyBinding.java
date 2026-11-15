package org.dishanhai.vmdiecore.compat.kubejs;

import net.minecraft.world.entity.player.Player;
import org.dishanhai.vmdiecore.monster.Race;

/** Read-only server-side queries for player-controlled technology interactions. */
public final class TechnologyBinding {
    private TechnologyBinding() {}

    public static String getRace(Player player) {
        Race race = Race.of(player);
        return race == null ? "" : race.id();
    }

    public static boolean hasTech(Player player, String technology) {
        Race race = Race.parse(technology);
        if (race == null || race == Race.VAMPIRE) {
            throw new IllegalArgumentException("Technology must be zombie, skeleton, enderman or arthropod: " + technology);
        }
        return Race.hasTech(player, race);
    }
}
