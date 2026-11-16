package org.dishanhai.vmdiecore.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.api.DungeonProfile;
import org.dishanhai.vmdiecore.api.DungeonRule;
import org.dishanhai.vmdiecore.api.DungeonTheme;
import org.dishanhai.vmdiecore.api.event.RegisterDungeonProfilesEvent;
import org.dishanhai.vmdiecore.api.event.RegisterDungeonRulesEvent;
import org.dishanhai.vmdiecore.api.event.RegisterDungeonThemesEvent;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DungeonDefinitions {

    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<ResourceLocation, DungeonTheme> themes = Map.of();
    private static volatile Map<ResourceLocation, DungeonProfile> profiles = Map.of();
    private static volatile Map<ResourceLocation, DungeonRule> rules = Map.of();

    private DungeonDefinitions() {
    }

    /** 注册三类定义的重载监听。 */
    public static void onAddReload(AddReloadListenerEvent event) {
        event.addListener(new ThemeListener());
        event.addListener(new ProfileListener());
        event.addListener(new RuleListener());
    }

    /** 返回全部地牢种类。 */
    public static Collection<DungeonTheme> themes() {
        return themes.values();
    }

    /** 按 id 查种类。 */
    public static Optional<DungeonTheme> theme(ResourceLocation id) {
        return Optional.ofNullable(themes.get(id));
    }

    /** 按 id 查维度档。 */
    public static Optional<DungeonProfile> profile(ResourceLocation id) {
        return Optional.ofNullable(profiles.get(id));
    }

    /** 按 id 查进入规则。 */
    public static Optional<DungeonRule> rule(ResourceLocation id) {
        return Optional.ofNullable(rules.get(id));
    }

    public static int themeCount() {
        return themes.size();
    }

    public static int profileCount() {
        return profiles.size();
    }

    public static int ruleCount() {
        return rules.size();
    }

    /** 绕过 reload 监听立即重扫数据包定义。 */
    public static void reloadNow(MinecraftServer server) {
        ResourceManager manager = server.getResourceManager();
        applyThemes(scan(manager, "dungeon_theme"));
        applyProfiles(scan(manager, "dungeon_profile"));
        applyRules(scan(manager, "dungeon_rule"));
    }

    private static Map<ResourceLocation, JsonElement> scan(ResourceManager manager, String directory) {
        Map<ResourceLocation, JsonElement> found = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(directory, path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath();
            String name = path.substring(directory.length() + 1, path.length() - ".json".length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), name);
            try (Reader reader = entry.getValue().openAsReader()) {
                found.put(id, JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException exception) {
                VMDieCore.LOGGER.warn("读取定义 {} 失败", file, exception);
            }
        }
        return found;
    }

    private static void applyThemes(Map<ResourceLocation, JsonElement> objects) {
        Map<ResourceLocation, DungeonTheme> merged = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                merged.put(entry.getKey(), ThemeJson.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                VMDieCore.LOGGER.warn("地牢种类 {} 解析失败", entry.getKey(), exception);
            }
        }
        RegisterDungeonThemesEvent event = new RegisterDungeonThemesEvent();
        NeoForge.EVENT_BUS.post(event);
        for (DungeonTheme registered : event.definitions()) {
            merged.put(registered.id(), registered);
        }
        themes = Map.copyOf(merged);
        VMDieCore.LOGGER.info("地牢种类 {} 个", themes.size());
    }

    private static void applyProfiles(Map<ResourceLocation, JsonElement> objects) {
        Map<ResourceLocation, DungeonProfile> merged = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                merged.put(entry.getKey(), ProfileJson.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                VMDieCore.LOGGER.warn("维度档 {} 解析失败", entry.getKey(), exception);
            }
        }
        RegisterDungeonProfilesEvent event = new RegisterDungeonProfilesEvent();
        NeoForge.EVENT_BUS.post(event);
        for (DungeonProfile registered : event.definitions()) {
            merged.put(registered.id(), registered);
        }
        profiles = Map.copyOf(merged);
        VMDieCore.LOGGER.info("维度档 {} 个", profiles.size());
    }

    private static void applyRules(Map<ResourceLocation, JsonElement> objects) {
        Map<ResourceLocation, DungeonRule> merged = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                merged.put(entry.getKey(), RuleJson.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                VMDieCore.LOGGER.warn("进入规则 {} 解析失败", entry.getKey(), exception);
            }
        }
        RegisterDungeonRulesEvent event = new RegisterDungeonRulesEvent();
        NeoForge.EVENT_BUS.post(event);
        for (DungeonRule registered : event.definitions()) {
            merged.put(registered.id(), registered);
        }
        rules = Map.copyOf(merged);
        VMDieCore.LOGGER.info("进入规则 {} 个", rules.size());
    }

    private static final class ThemeListener extends SimpleJsonResourceReloadListener {
        private ThemeListener() {
            super(GSON, "dungeon_theme");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            applyThemes(objects);
        }
    }

    private static final class ProfileListener extends SimpleJsonResourceReloadListener {
        private ProfileListener() {
            super(GSON, "dungeon_profile");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            applyProfiles(objects);
        }
    }

    private static final class RuleListener extends SimpleJsonResourceReloadListener {
        private RuleListener() {
            super(GSON, "dungeon_rule");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            applyRules(objects);
        }
    }
}
