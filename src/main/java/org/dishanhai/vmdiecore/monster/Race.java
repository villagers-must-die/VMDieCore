package org.dishanhai.vmdiecore.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public enum Race {
    ZOMBIE("僵尸", "THE UNDYING", 0x88C99C, EntityType.ZOMBIE,
        "死而复生，血肉为疆。", "护甲 +2 · 克隆技术",
        new String[]{"天生护甲 +2；腐肉不再造成饥饿。", "阳光下燃烧，佩戴任意头部装备可豁免。", "受到亡灵杀手额外伤害；治疗与伤害药水反转。", "在水中下沉。虚弱时吃金苹果将直接死亡。"},
        new String[]{"克隆技术", "任意肉类 → 肉傀儡部件。", "部件可作为傀儡装配的血肉材料。", "死亡时消耗一只自己拥有、已加载的肉傀儡，", "在其位置重生并保留物品。"}),
    SKELETON("骷髅", "THE SOULKEEPER", 0xE2CF9E, EntityType.SKELETON,
        "空洞的骨骼，盛满不灭的灵魂。", "速度 +20% · 灵魂技术",
        new String[]{"最终生命 ×0.8，移动速度 +20%。", "阳光燃烧与头盔豁免；亡灵杀手伤害增加。", "治疗/伤害药水反转；没有饥饿，但仍能进食。", "牛奶提供金苹果的再生与伤害吸收。", "满血喝牛奶额外获得骨头；水中上浮，无法游泳。"},
        new String[]{"灵魂技术", "灵魂沙玻璃 → 灵魂瓶。", "携带空灵魂瓶击杀村民、刌民、猪、牛或羊以充能。", "骨头 + 满魂瓶 → 骨傀儡部件。", "骨质傀儡可复活，冷却 5 分钟。", "将满魂瓶装备在 Curios 护符槽，速度 +50%。"}),
    ENDERMAN("末影人", "THE RIFTWALKER", 0xBD9AEF, EntityType.ENDERMAN,
        "越过裂隙，归途即在掌中。", "生命 ×2 · 传送技术",
        new String[]{"最终生命 ×2，碰撞箱与视点升至 3 格身高。", "接触水、雨和水瓶泼溅会受到伤害。", "副手持刀，空中右键割取 2 点生命以获得末影珍珠。"},
        new String[]{"传送技术", "传送卷轴：首次标记，再次使用返回并消耗。", "绑定珍珠：右键生物绑定，再次使用交换位置。", "目标死亡或未加载时，绑定珍珠破碎。", "无限珍珠：反复投掷末影珍珠，冷却 1 秒。", "无限珍珠可用于 Ender IO 传送设备（安装后启用）。"}),
    ARTHROPOD("节肢生物", "THE SILKWEAVER", 0xE9AA75, EntityType.SPIDER,
        "织就蛛网，令猎物无路可逃。", "攀墙 · 纺织技术",
        new String[]{"蜘蛛模型；碰撞箱高 1 格，宽 1.4 格。", "可沿墙面攀爬；潜行可停止攀升。", "受到节肢杀手额外伤害和缓慢。", "食用蜘蛛眼不会中毒；近战攻击使目标中毒。", "持空瓶空中右键获得毒液瓶，可替代发酵蛛眼。"},
        new String[]{"纺织技术", "蛛丝结合矿物，制作 8 种附魔蛛丝布。", "将蛛丝布与适用物品放入工作台即可附魔。", "时运 II / 抢夺 II / 精准采集 / 锋利 III", "保护 III / 摔落保护 IV / 火焰保护 III / 耐久 III", "自动检查适用性与附魔冲突，保留已有附魔。"}),
    VAMPIRE("血族", "THE NIGHTBORN", 0xE88DAB, null,
        "夜幕之下，万族之力尽归于你。", "免疫摔落 · 全部科技",
        new String[]{"保留玩家模型与自己的皮肤。", "免疫摔落伤害；阳光燃烧，头盔可豁免。", "全身与双手均无装备时隐形，无粒子效果。", "受伤时释放蝙蝠吸引附近敌人，冷却 10 秒。"},
        new String[]{"全族科技", "同时解锁克隆、灵魂、传送与纺织配方。", "可使用四族的科技物品与傀儡能力。", "共享科技不赋予其他种族的身体特性。"});

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
