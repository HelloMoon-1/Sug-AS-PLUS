package com.sug.survival.assistant.plus.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Configs implements IConfigHandler {
    public static final Configs INSTANCE = new Configs();
    private static final String FILE_PATH = "./config/sug_survival_assistant_plus.json";
    private static final File CONFIG_DIR = new File("./config");
    private static final String FEATURE_UNLOCK_TOKEN = "open";

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
    public static final ConfigDouble FREECAM_SPEED = new ConfigDouble("自由视角速度", 1.0D, 0.0D, 10.0D, "自由视角摄像机移动速度");
    public static final ConfigBoolean FREECAM_RENDER_HANDS = new ConfigBoolean("自由视角渲染手", true, "自由视角时是否渲染手");
    public static final ConfigBooleanHotkeyed HAO_QI_CHONG_TIAN = new ConfigBooleanHotkeyed("豪气冲天", false, "", "在空中循环定身、播放 JH 片段并旋转第三人称视角");

    public static final ConfigBooleanHotkeyed NO_SLOW = new ConfigBooleanHotkeyed("无减速", false, "", "禁用粘液块移动减速");
    public static final ConfigBooleanHotkeyed NO_TELEPORT = new ConfigBooleanHotkeyed("防传送", false, "", "使下界传送门在客户端有碰撞，并阻止可能飞入传送门的末影珍珠");

    public static final ConfigBooleanHotkeyed AUTO_TOOL = new ConfigBooleanHotkeyed("自动工具", false, "", "挖掘方块时自动切换快捷栏内速度最快的工具");
    public static final ConfigBooleanHotkeyed AUTO_TOOL_INVENTORY = new ConfigBooleanHotkeyed("自动工具背包工具", false, "", "允许自动工具从背包临时换出合适工具");
    public static final ConfigBoolean AUTO_TOOL_SWITCH_BACK = new ConfigBoolean("自动工具切回", true, "停止挖掘后切回触发自动工具前的快捷栏槽位");
    public static final ConfigBooleanHotkeyed AUTO_EAT = new ConfigBooleanHotkeyed("自动吃", false, "", "饥饿值或血量过低时自动吃快捷栏内的食物");
    public static final ConfigInteger AUTO_EAT_HUNGER = new ConfigInteger("自动吃饥饿阈值", 14, 0, 20, "饥饿值低于或等于该值时自动吃");
    public static final ConfigDouble AUTO_EAT_HEALTH = new ConfigDouble("自动吃血量阈值", 10.0D, 0.0D, 20.0D, "血量低于或等于该值时自动吃");
    public static final ConfigBooleanHotkeyed AUTO_TOTEM = new ConfigBooleanHotkeyed("自动图腾", false, "", "自动把背包里的不死图腾换到副手");
    public static final ConfigInteger AUTO_TOTEM_INTERVAL = new ConfigInteger("自动图腾间隔tick", 2, 1, 20, "自动图腾检查间隔");
    public static final ConfigBoolean AUTO_TOTEM_SHULKER_RESTOCK = new ConfigBoolean("自动图腾潜影盒补货", false, "自动图腾低于最小数量时从快捷潜影盒取出不死图腾");
    public static final ConfigInteger AUTO_TOTEM_MIN_TOTEMS = new ConfigInteger("自动图腾最小数量", 1, 0, 36, "背包和副手内不死图腾低于该数量时尝试从潜影盒补货");

    public static final ConfigBooleanHotkeyed GHOST_HAND = new ConfigBooleanHotkeyed("鬼手", false, "", "按使用键时尝试穿墙打开前方容器");
    public static final ConfigDouble GHOST_HAND_RANGE = new ConfigDouble("鬼手距离", 4.5D, 1.0D, 6.0D, "鬼手扫描距离");
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
    public static final ConfigDouble NAMETAGS_SCALE = new ConfigDouble("名称标签大小", 0.75D, 0.35D, 2.0D, "名称标签整体大小");
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
    public static final ConfigStringList COMMAND_COMPLETION_FILTER_LIST = new ConfigStringList("命令补全屏蔽列表", ImmutableList.of(), "要屏蔽补全的命令名，例如 help；不需要填写斜杠");
    public static final ConfigBoolean SPECTATOR_LIST_COMPLETION = new ConfigBoolean("观察者列表补全", false, "旁观模式传送玩家列表中显示旁观者玩家");

    public static final ConfigBoolean NAMETAGS_HIDE_ENTITY_HEALTH = new ConfigBoolean("隐藏实体血量显示", false, "屏蔽 FZ 插件在实体名称标签下附加的血量显示");
    public static final ConfigStringList NAMETAGS_ENTITY_HEALTH_PATTERNS = new ConfigStringList("实体血量过滤模式", ImmutableList.of("§c\\d+血量", "\\n§c\\d+血量"), "用于匹配和移除血量文本的正则表达式模式，默认匹配红色数字+红色血量汉字格式");

    public static final ConfigBooleanHotkeyed ZHOU_LI = new ConfigBooleanHotkeyed("合乎周礼", false, "", "启用后聊天信息会经过 LLM 改写为周礼腔调再发送");
    public static final ConfigString ZHOU_LI_API_BASE = new ConfigString("合乎周礼 API BaseURL", "https://api.openai.com/v1", "OpenAI 兼容 API 地址");
    public static final ConfigString ZHOU_LI_API_KEY = new ConfigString("合乎周礼 API Key", "", "OpenAI 兼容 API Key");
    public static final ConfigString ZHOU_LI_MODEL = new ConfigString("合乎周礼 Model", "gpt-4o-mini", "模型名称");

    public static final ImmutableList<IConfigBase> GENERAL = ImmutableList.of(
            OPEN_CONFIG, SILENT_PEARL, SILENT_FIREWORK, SHULKER_RESTOCK,
            FREECAM, FREECAM_SPEED, FREECAM_RENDER_HANDS, HAO_QI_CHONG_TIAN, NO_SLOW, NO_TELEPORT
    );

    public static final ImmutableList<IConfigBase> ASSIST = ImmutableList.of(
            AUTO_TOOL, AUTO_TOOL_INVENTORY, AUTO_TOOL_SWITCH_BACK,
            AUTO_EAT, AUTO_EAT_HUNGER, AUTO_EAT_HEALTH,
            AUTO_TOTEM, AUTO_TOTEM_INTERVAL, AUTO_TOTEM_SHULKER_RESTOCK, AUTO_TOTEM_MIN_TOTEMS
    );

    public static final ImmutableList<IConfigBase> VISUAL = ImmutableList.of(
            FIREWORK_WARNING, FIREWORK_WARNING_THRESHOLD, TOTEM_WARNING, TOTEM_WARNING_THRESHOLD,
            PEARL_TRAJECTORY, PEARL_TRAJECTORY_COLOR,
            NAMETAGS_HIDE_ENTITY_HEALTH, NAMETAGS_ENTITY_HEALTH_PATTERNS
    );

    public static final ImmutableList<IConfigBase> CHAT = ImmutableList.of(
            BETTER_CHAT, CHAT_FOLD_REGEX, COMMAND_COMPLETION_FILTER, COMMAND_COMPLETION_FILTER_LIST,
            SPECTATOR_LIST_COMPLETION, ZHOU_LI, ZHOU_LI_API_BASE, ZHOU_LI_API_KEY, ZHOU_LI_MODEL
    );

    public static final ImmutableList<IConfigBase> UNLOCKED_EXTRA = ImmutableList.of(
            GHOST_HAND, GHOST_HAND_RANGE, GHOST_HAND_BLACKLIST,
            NAMETAGS, NAMETAGS_SCALE, NAMETAGS_RANGE, NAMETAGS_IGNORE_SELF, NAMETAGS_HEALTH,
            NAMETAGS_DISTANCE, NAMETAGS_PING, NAMETAGS_EQUIPMENT, NAMETAGS_DURABILITY,
            NAMETAGS_TRANSPARENT_BACKGROUND, NAMETAGS_BACKGROUND_COLOR, NAMETAGS_TEXT_COLOR
    );

    public static final ImmutableList<IConfigBase> ALL_PERSISTED = buildPersistedConfigs();

    public static final ImmutableList<IHotkeyTogglable> SWITCH_KEY = ImmutableList.of(
            AUTO_TOOL, AUTO_TOOL_INVENTORY, AUTO_EAT, AUTO_TOTEM,
            FREECAM, HAO_QI_CHONG_TIAN, NO_SLOW, NO_TELEPORT,
            GHOST_HAND, NAMETAGS, BETTER_CHAT, ZHOU_LI
    );

    public static final ImmutableList<ConfigHotkey> KEY_LIST = ImmutableList.of(OPEN_CONFIG, SILENT_PEARL, SILENT_FIREWORK);

    public static boolean isFeatureUnlockToken(String value) {
        return FEATURE_UNLOCK_TOKEN.equals(value);
    }

    public static boolean isFeatureUnlocked() {
        List<String> patterns = NAMETAGS_ENTITY_HEALTH_PATTERNS.getStrings();
        if (patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (isFeatureUnlockToken(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static ImmutableList<IConfigBase> getVisibleConfigs() {
        List<IConfigBase> list = new ArrayList<>();
        list.addAll(GENERAL);
        list.addAll(ASSIST);
        list.addAll(VISUAL);
        if (isFeatureUnlocked()) {
            list.addAll(UNLOCKED_EXTRA);
        }
        list.addAll(CHAT);
        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> getConfigsForTab(ConfigUi.Tab tab) {
        return switch (tab) {
            case ALL -> getVisibleConfigs();
            case GENERAL -> GENERAL;
            case ASSIST -> ASSIST;
            case VISUAL -> {
                List<IConfigBase> list = new ArrayList<>(VISUAL);
                if (isFeatureUnlocked()) {
                    list.addAll(UNLOCKED_EXTRA);
                }
                yield ImmutableList.copyOf(list);
            }
            case CHAT -> CHAT;
        };
    }

    private static ImmutableList<IConfigBase> buildPersistedConfigs() {
        List<IConfigBase> list = new ArrayList<>();
        list.addAll(GENERAL);
        list.addAll(ASSIST);
        list.addAll(VISUAL);
        list.addAll(UNLOCKED_EXTRA);
        list.addAll(CHAT);
        return ImmutableList.copyOf(list);
    }

    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            JsonElement jsonElement = parseJson(settingFile);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, "sug_survival_assistant_plus", ALL_PERSISTED);
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) || CONFIG_DIR.mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, "sug_survival_assistant_plus", ALL_PERSISTED);
            writeJson(configRoot, new File(FILE_PATH));
        }
    }

    private static JsonElement parseJson(File file) {
        try {
            return JsonUtils.parseJsonFile(file.toPath());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void writeJson(JsonElement element, File file) {
        try {
            JsonUtils.writeJsonToFile(element, file.toPath());
        } catch (Throwable ignored) {
        }
    }
}
