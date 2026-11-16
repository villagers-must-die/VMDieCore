package org.dishanhai.vmdiecore.ui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.dishanhai.vmdiecore.api.DungeonApi;
import org.dishanhai.vmdiecore.api.DungeonInfo;

import java.util.List;

public final class DungeonConsoleUI {

    public static ModularUI create(DungeonConsoleMenu.Holder holder) {
        UIElement root = new UIElement();
        root.layout(layout -> layout.width(260).height(190).paddingAll(4).gapAll(4)
                .flexDirection(FlexDirection.COLUMN));
        root.style(style -> style.background(new ColorRectTexture(0xFF1E1E22)));

        TabView tabs = new TabView();
        tabs.layout(layout -> layout.widthPercent(100).flex(1));
        tabs.addTab(tab("概览"), overviewPage(holder));
        tabs.addTab(tab("生成地牢"), createPage(holder));
        tabs.addTab(tab("地牢列表"), listPage(holder));
        root.addChild(tabs);

        return ModularUI.of(UI.of(root), holder.player);
    }

    private static UIElement overviewPage(DungeonConsoleMenu.Holder holder) {
        UIElement page = page();
        page.addChildren(
                label("地牢数量 " + listFor(holder).size() + "/" + holder.snapshot.maxDungeons()),
                label("种类 " + holder.snapshot.themes().size() + " 种"),
                label("维度档 " + holder.snapshot.profileCount() + " 个"),
                label("规则 " + holder.snapshot.ruleCount() + " 条"),
                button("刷新", () -> reopen(holder)));
        return page;
    }

    private static UIElement createPage(DungeonConsoleMenu.Holder holder) {
        UIElement page = page();
        List<DungeonConsoleSnapshot.Theme> themes = holder.snapshot.themes();
        if (themes.isEmpty()) {
            page.addChild(label("没有可用的地牢种类"));
            return page;
        }
        UIElement rows = new UIElement();
        rows.layout(layout -> layout.widthPercent(100).flex(1).gapAll(2).flexDirection(FlexDirection.COLUMN));
        for (DungeonConsoleSnapshot.Theme theme : themes) {
            rows.addChild(themeRow(holder, theme));
        }
        page.addChild(rows);
        return page;
    }

    private static UIElement themeRow(DungeonConsoleMenu.Holder holder, DungeonConsoleSnapshot.Theme theme) {
        UIElement row = new UIElement();
        row.layout(layout -> layout.widthPercent(100).height(18).gapAll(3)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));
        row.addChild(label(theme.name()).layout(layout -> layout.width(72)));
        for (DungeonConsoleSnapshot.Tier tier : theme.tiers()) {
            Button button = new Button();
            button.setText(Component.literal(tier.name()));
            button.layout(layout -> layout.width(52).height(16));
            button.setOnServerClick(event -> {
                if (canAct(holder) && DungeonApi.create(theme.id(), tier.level()).isPresent()) {
                    reopen(holder);
                }
            });
            row.addChild(button);
        }
        return row;
    }

    private static UIElement listPage(DungeonConsoleMenu.Holder holder) {
        UIElement page = page();
        List<DungeonInfo> dungeons = listFor(holder);
        if (dungeons.isEmpty()) {
            page.addChild(label("还没有地牢"));
            return page;
        }
        for (DungeonInfo info : dungeons) {
            UIElement row = new UIElement();
            row.layout(layout -> layout.widthPercent(100).height(16).gapAll(4)
                    .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));
            row.addChildren(
                    label(rowText(info)).layout(layout -> layout.width(180)),
                    deleteButton(holder, info.id()));
            page.addChild(row);
        }
        return page;
    }

    private static String rowText(DungeonInfo info) {
        return "#" + info.id() + " " + info.themeName() + " " + info.levelName() + " 次数" + info.visits();
    }

    private static Button deleteButton(DungeonConsoleMenu.Holder holder, int dungeonId) {
        boolean[] armed = {false};
        Button button = new Button();
        button.setText(Component.literal("删除"));
        button.layout(layout -> layout.width(36).height(14));
        var confirmation = DataBindingBuilder.stringS2C(() -> armed[0] ? "确认" : "删除")
                .build(holder.player.level().isClientSide);
        confirmation.registerListener(button::setText);
        button.addSyncValue(confirmation.getSyncValue());
        button.setOnServerClick(event -> {
            if (!canAct(holder)) return;
            if (!armed[0]) {
                armed[0] = true;
                button.setText(Component.literal("确认"));
                return;
            }
            if (DungeonApi.destroy(dungeonId)) {
                reopen(holder);
            }
        });
        return button;
    }

    private static void reopen(DungeonConsoleMenu.Holder holder) {
        if (!(holder.player instanceof ServerPlayer player) || !canAct(holder)) {
            return;
        }
        player.closeContainer();
        player.server.execute(() -> DungeonConsoleMenu.open(player, holder.pos));
    }

    private static List<DungeonInfo> listFor(DungeonConsoleMenu.Holder holder) {
        return holder.snapshot.dungeons();
    }

    private static boolean canAct(DungeonConsoleMenu.Holder holder) {
        return holder.player instanceof ServerPlayer player && DungeonConsoleMenu.canUse(player, holder.pos);
    }

    private static UIElement page() {
        UIElement page = new UIElement();
        page.layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(4).gapAll(3)
                .flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.FLEX_START));
        return page;
    }

    private static Tab tab(String text) {
        Tab tab = new Tab();
        tab.setText(Component.literal(text));
        return tab;
    }

    private static Label label(String text) {
        Label label = new Label();
        label.setText(Component.literal(text));
        return label;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button();
        button.setText(Component.literal(text));
        button.layout(layout -> layout.width(48).height(16));
        button.setOnServerClick(event -> action.run());
        return button;
    }

    private DungeonConsoleUI() {
    }
}
