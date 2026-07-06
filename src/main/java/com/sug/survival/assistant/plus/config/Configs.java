package com.sug.survival.assistant.plus.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import com.sug.survival.assistant.plus.client.Sug_survival_assistant_plusClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Configs implements IConfigHandler {
    public static final Configs INSTANCE = new Configs();
    private static final String FILE_PATH = "./config/" + Sug_survival_assistant_plusClient.MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");

    public static final ConfigHotkey OPEN_CONFIG = new ConfigHotkey("打开设置菜单", "Z,L", "打开 SUG Survival Assistant PLUS 设置菜单");
    public static final ConfigHotkey SILENT_PEARL = new ConfigHotkey("静默珍珠", "", "从背包静默切换并使用末影珍珠");
    public static final ConfigHotkey SILENT_FIREWORK = new ConfigHotkey("静默烟花", "", "从背包静默切换并使用烟花火箭");
    public static final ConfigBoolean FIREWORK_WARNING = new ConfigBoolean("烟花预警", true, "烟花数量低于阈值时在物品栏上方提示");
    public static final ConfigInteger FIREWORK_WARNING_THRESHOLD = new ConfigInteger("烟花预警阈值", 16, 0, 512, "烟花数量小于或等于该值时显示预警");
    public static final ConfigBoolean TOTEM_WARNING = new ConfigBoolean("图腾预警", true, "背包和潜影盒内不死图腾总数低于阈值时在物品栏上方提示");
    public static final ConfigInteger TOTEM_WARNING_THRESHOLD = new ConfigInteger("图腾预警阈值", 2, 0, 512, "背包和潜影盒内不死图腾总数小于或等于该值时显示预警");
    public static final ConfigBoolean PEARL_TRAJECTORY = new ConfigBoolean("珍珠抛物线", true, "渲染末影珍珠预测抛物线");
    public static final ConfigColor PEARL_TRAJECTORY_COLOR = new ConfigColor("珍珠抛物线颜色", "#FF7333FF", "末影珍珠预测抛物线颜色");
    public static final ConfigBooleanHotkeyed FREECAM = new ConfigBooleanHotkeyed("自由视角", false, "", "让摄像机离开玩家本体自由移动");
    public static final ConfigDouble FREECAM_SPEED = new ConfigDouble("自由视角速度", 1.0, 0.0, 10.0, "自由视角摄像机移动速度");
    public static final ConfigBoolean FREECAM_RENDER_HANDS = new ConfigBoolean("自由视角渲染手", true, "自由视角时是否渲染手");
    public static final ConfigBooleanHotkeyed HAO_QI_CHONG_TIAN = new ConfigBooleanHotkeyed("豪气冲天", false, "", "在空中循环定身、播放 JH 片段并旋转第三人称视角");

    public static final ConfigBooleanHotkeyed NO_SLOW = new ConfigBooleanHotkeyed("无减速", false, "", "禁用粘液块移动减速");
    public static final ConfigBooleanHotkeyed NO_TELEPORT = new ConfigBooleanHotkeyed("防传送", false, "", "使下界传送门在客户端有碰撞，并阻止可能飞入传送门的末影珍珠");

    public static final ConfigBooleanHotkeyed AUTO_TOOL = new ConfigBooleanHotkeyed("自动工具", false, "", "挖掘方块时自动切换快捷栏内速度最快的工具");
    public static final ConfigBooleanHotkeyed AUTO_TOOL_INVENTORY = new ConfigBooleanHotkeyed("自动工具背包工具", false, "", "允许自动工具从背包临时换出合适工具");
    public static final ConfigBoolean AUTO_TOOL_SWITCH_BACK = new ConfigBoolean("自动工具切回", true, "停止挖掘后切回触发自动工具前的快捷栏槽位");
    public static final ConfigBooleanHotkeyed AUTO_EAT = new ConfigBooleanHotkeyed("自动吃", false, "", "饥饿值或血量过低时自动吃快捷栏内的食物");
    public static final ConfigInteger AUTO_EAT_HUNGER = new ConfigInteger("自动吃饥饿阈值", 14, 0, 20, "饥饿值低于或等于该值时自动吃");
    public static final ConfigDouble AUTO_EAT_HEALTH = new ConfigDouble("自动吃血量阈值", 10.0, 0.0, 20.0, "血量低于或等于该值时自动吃");
    public static final ConfigBooleanHotkeyed AUTO_TOTEM = new ConfigBooleanHotkeyed("自动图腾", false, "", "自动把背包里的不死图腾换到副手");
    public static final ConfigInteger AUTO_TOTEM_INTERVAL = new ConfigInteger("自动图腾间隔tick", 2, 1, 20, "自动图腾检查间隔");
    public static final ConfigBoolean AUTO_TOTEM_SHULKER_RESTOCK = new ConfigBoolean("自动图腾潜影盒补货", false, "自动图腾低于最小数量时从快捷潜影盒取出不死图腾");
    public static final ConfigInteger AUTO_TOTEM_MIN_TOTEMS = new ConfigInteger("自动图腾最小数量", 1, 0, 36, "背包和副手内不死图腾低于该数量时尝试从潜影盒补货");

    public static final ConfigBooleanHotkeyed GHOST_HAND = new ConfigBooleanHotkeyed("鬼手", false, "", "按使用键时尝试穿墙打开前方容器");
    public static final ConfigDouble GHOST_HAND_RANGE = new ConfigDouble("鬼手距离", 4.5, 1.0, 6.0, "鬼手扫描距离");
    public static final ConfigStringList GHOST_HAND_BLACKLIST = new ConfigStringList("鬼手黑名单", ImmutableList.of("minecraft:ender_chest"), "不会被鬼手打开的方块ID");

    public static final ConfigBooleanHotkeyed CONTAINER_ESP = new ConfigBooleanHotkeyed("容器透视", false, "", "高亮附近容器方块");
    public static final ConfigInteger CONTAINER_ESP_RANGE = new ConfigInteger("容器透视范围", 64, 8, 256, "扫描并渲染玩家周围容器的半径");
    public static final ConfigColor CONTAINER_ESP_LINE_COLOR = new ConfigColor("容器透视线框颜色", "#FF00FFCC", "容器透视线框颜色");
    public static final ConfigColor CONTAINER_ESP_FILL_COLOR = new ConfigColor("容器透视填充颜色", "#2E00FFCC", "容器透视填充颜色");

    public static final ConfigBooleanHotkeyed BLOCK_ESP = new ConfigBooleanHotkeyed("方块透视", false, "", "高亮指定方块列表");
    public static final ConfigInteger BLOCK_ESP_RANGE = new ConfigInteger("方块透视范围", 64, 8, 256, "扫描并渲染玩家周围指定方块的半径");
    public static final ConfigStringList BLOCK_ESP_BLOCKS = new ConfigStringList("方块透视列表", ImmutableList.of("minecraft:ancient_debris"), "需要高亮的方块ID列表");
    public static final ConfigColor BLOCK_ESP_LINE_COLOR = new ConfigColor("方块透视线框颜色", "#FFFF3333", "方块透视线框颜色");
    public static final ConfigColor BLOCK_ESP_FILL_COLOR = new ConfigColor("方块透视填充颜色", "#2EFF3333", "方块透视填充颜色");

    public static final ConfigBooleanHotkeyed NAMETAGS = new ConfigBooleanHotkeyed("名称标签", false, "", "渲染 Meteor 风格玩家名称标签");
    public static final ConfigDouble NAMETAGS_SCALE = new ConfigDouble("名称标签大小", 0.75, 0.35, 2.0, "名称标签整体大小");
    public static final ConfigInteger NAMETAGS_RANGE = new ConfigInteger("名称标签距离", 128, 8, 512, "名称标签最大渲染距离");
    public static final ConfigBoolean NAMETAGS_IGNORE_SELF = new ConfigBoolean("名称标签忽略自己", true, "不渲染自己的名称标签");
    public static final ConfigBoolean NAMETAGS_HEALTH = new ConfigBoolean("名称标签血量", true, "显示玩家血量");
    public static final ConfigBoolean NAMETAGS_DISTANCE = new ConfigBoolean("名称标签距离显示", true, "显示玩家距离");
    public static final ConfigBoolean NAMETAGS_PING = new ConfigBoolean("名称标签Ping", true, "显示玩家 ping");
    public static final ConfigBoolean NAMETAGS_EQUIPMENT = new ConfigBoolean("名称标签装备", true, "显示玩家手持物和盔甲");
    public static final ConfigBoolean NAMETAGS_DURABILITY = new ConfigBoolean("名称标签耐久", true, "显示装备耐久条");
    public static final ConfigBoolean NAMETAGS_TRANSPARENT_BACKGROUND = new ConfigBoolean("名称标签透明背景", false, "不渲染名称标签背景");
    public static final ConfigColor NAMETAGS_BACKGROUND_COLOR = new ConfigColor("名称标签背景颜色", "#80000000", "名称标签背景颜色");
    public static final ConfigColor NAMETAGS_TEXT_COLOR = new ConfigColor("名称标签文字颜色", "#FFFFFFFF", "名称标签文字颜色");

    public static final ConfigBoolean SHULKER_RESTOCK = new ConfigBoolean("潜影盒补货", false, "从快捷潜影盒中自动补充珍珠和烟花（需要安装 Quick Shulker 否则没效果）");
    public static final ConfigBooleanHotkeyed BETTER_CHAT = new ConfigBooleanHotkeyed("更好的聊天", false, "", "启用聊天正则折叠/过滤功能");
    public static final ConfigStringList CHAT_FOLD_REGEX = new ConfigStringList("聊天折叠正则", ImmutableList.of(), "匹配到的聊天内容会在后续版本折叠显示");
    public static final ConfigBoolean COMMAND_COMPLETION_FILTER = new ConfigBoolean("命令补全屏蔽", false, "屏蔽指定命令在聊天 Tab 补全候选中显示和补全");
    public static final ConfigStringList COMMAND_COMPLETION_FILTER_LIST = new ConfigStringList("命令补全屏蔽列表", ImmutableList.of(), "要屏蔽补全的命令名，例如 help；不需要填写斜杠 哦不不不 spark指令导致我无法补全观察者我无疑是愤怒的");
    public static final ConfigBoolean SPECTATOR_LIST_COMPLETION = new ConfigBoolean("观察者列表补全", false, "旁观模式传送玩家列表中显示旁观者玩家");

    public static final ConfigBoolean NAMETAGS_HIDE_ENTITY_HEALTH = new ConfigBoolean("隐藏实体血量显示", false, "屏蔽 FZ 插件在实体名称标签下附加的血量显示");
    public static final ConfigStringList NAMETAGS_ENTITY_HEALTH_PATTERNS = new ConfigStringList("实体血量过滤模式", ImmutableList.of("§c\\d+血量", "\\n§c\\d+血量"), "用于匹配和移除血量文本的正则表达式模式，默认匹配红色数字+红色血量汉字格式");

    public static final ConfigBooleanHotkeyed ZHOU_LI = new ConfigBooleanHotkeyed("合乎周礼", false, "", "启用后聊天信息会经过 LLM 改写为周礼腔调再发送");
    public static final ConfigString ZHOU_LI_API_BASE = new ConfigString("合乎周礼 API BaseURL", "https://api.openai.com/v1", "OpenAI 兼容 API 地址");
    public static final ConfigString ZHOU_LI_API_KEY = new ConfigString("合乎周礼 API Key", "", "OpenAI 兼容 API Key");
    public static final ConfigString ZHOU_LI_MODEL = new ConfigString("合乎周礼 Model", "gpt-4o-mini", "模型名称");

    public static final ImmutableList<IConfigBase> ALL_CONFIGS = createAllConfigs();
    public static final ImmutableList<IHotkeyTogglable> SWITCH_KEY = ImmutableList.of(
            AUTO_TOOL,
            AUTO_TOOL_INVENTORY,
            AUTO_EAT,
            AUTO_TOTEM,
            FREECAM,
            HAO_QI_CHONG_TIAN,
            NO_SLOW,
            NO_TELEPORT,
            GHOST_HAND,
            CONTAINER_ESP,
            BLOCK_ESP,
            NAMETAGS,
            BETTER_CHAT,
            ZHOU_LI
    );
    public static final ImmutableList<ConfigHotkey> KEY_LIST = ImmutableList.of(OPEN_CONFIG, SILENT_PEARL, SILENT_FIREWORK);

    private static ImmutableList<IConfigBase> createAllConfigs() {
        List<IConfigBase> list = new ArrayList<>();
        list.add(OPEN_CONFIG);
        list.add(SILENT_PEARL);
        list.add(SILENT_FIREWORK);
        list.add(SHULKER_RESTOCK);
        list.add(FIREWORK_WARNING);
        list.add(FIREWORK_WARNING_THRESHOLD);
        list.add(TOTEM_WARNING);
        list.add(TOTEM_WARNING_THRESHOLD);
        list.add(PEARL_TRAJECTORY);
        list.add(PEARL_TRAJECTORY_COLOR);
        list.add(FREECAM);
        list.add(FREECAM_SPEED);
        list.add(FREECAM_RENDER_HANDS);
        list.add(HAO_QI_CHONG_TIAN);
        list.add(NO_SLOW);
        list.add(NO_TELEPORT);
        list.add(AUTO_TOOL);
        list.add(AUTO_TOOL_INVENTORY);
        list.add(AUTO_TOOL_SWITCH_BACK);
        list.add(AUTO_EAT);
        list.add(AUTO_EAT_HUNGER);
        list.add(AUTO_EAT_HEALTH);
        list.add(AUTO_TOTEM);
        list.add(AUTO_TOTEM_INTERVAL);
        list.add(AUTO_TOTEM_SHULKER_RESTOCK);
        list.add(AUTO_TOTEM_MIN_TOTEMS);
//        list.add(GHOST_HAND);
//        list.add(GHOST_HAND_RANGE);
//        list.add(GHOST_HAND_BLACKLIST);
//        list.add(NAMETAGS);
//        list.add(NAMETAGS_SCALE);
//        list.add(NAMETAGS_RANGE);
//        list.add(NAMETAGS_IGNORE_SELF);
//        list.add(NAMETAGS_HEALTH);
//        list.add(NAMETAGS_DISTANCE);
//        list.add(NAMETAGS_PING);
//        list.add(NAMETAGS_EQUIPMENT);
//        list.add(NAMETAGS_DURABILITY);
//        list.add(NAMETAGS_TRANSPARENT_BACKGROUND);
//        list.add(NAMETAGS_BACKGROUND_COLOR);
//        list.add(NAMETAGS_TEXT_COLOR);
        list.add(BETTER_CHAT);
        list.add(CHAT_FOLD_REGEX);
        list.add(COMMAND_COMPLETION_FILTER);
        list.add(COMMAND_COMPLETION_FILTER_LIST);
        list.add(SPECTATOR_LIST_COMPLETION);
        list.add(NAMETAGS_HIDE_ENTITY_HEALTH);
        list.add(NAMETAGS_ENTITY_HEALTH_PATTERNS);
        list.add(ZHOU_LI);
        list.add(ZHOU_LI_API_BASE);
        list.add(ZHOU_LI_API_KEY);
        list.add(ZHOU_LI_MODEL);
        return ImmutableList.copyOf(list);
    }

    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile.toPath());
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, Sug_survival_assistant_plusClient.MOD_ID, ALL_CONFIGS);
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) || CONFIG_DIR.mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, Sug_survival_assistant_plusClient.MOD_ID, ALL_CONFIGS);
            JsonUtils.writeJsonToFile(configRoot, new File(FILE_PATH).toPath());
        }
    }
}
