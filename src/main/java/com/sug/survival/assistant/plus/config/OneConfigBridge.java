package com.sug.survival.assistant.plus.config;

import com.sug.survival.assistant.plus.client.Sug_survival_assistant_plusClient;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Optional OneConfig integration. OneConfig is NOT a compile/runtime dependency of this project;
 * put its jars in run/mods yourself. When present, registers a ClickGUI tree bound to the existing
 * malilib config values. When absent, only malilib UI is used.
 */
public final class OneConfigBridge {
    private static final String[] MOD_IDS = {
            "oneconfig",
            "oneconfigbootstrap",
            "oneconfigv1"
    };

    private static final boolean PRESENT;
    private static final boolean API_READY;
    private static Object registeredTree;
    private static String treeId;
    private static Method functionalMethod;
    private static boolean configDirty;
    private static int saveDelayTicks;

    static {
        boolean present = false;
        for (String id : MOD_IDS) {
            if (FabricLoader.getInstance().isModLoaded(id)) {
                present = true;
                break;
            }
        }
        PRESENT = present;

        boolean api = false;
        if (present) {
            try {
                Class.forName("org.polyfrost.oneconfig.api.config.v1.Tree");
                Class.forName("org.polyfrost.oneconfig.api.config.v1.Properties");
                Class.forName("org.polyfrost.oneconfig.api.config.v1.Visualizer");
                Class.forName("org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry");
                Class.forName("org.polyfrost.oneconfig.internal.ui.api.ConfigSource");
                Class.forName("org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen");
                Sug_survival_assistant_plusClient.LOGGER.info("OneConfig mod found.Have init");
                api = true;
            } catch (Throwable t) {
                Sug_survival_assistant_plusClient.LOGGER.warn(
                        "OneConfig mod detected but API classes are missing; keeping malilib only", t);
            }
        }
        API_READY = api;
    }

    private OneConfigBridge() {
    }

    public static boolean isAvailable() {
        return PRESENT && API_READY;
    }

    public static void tick() {
        if (!configDirty || --saveDelayTicks > 0) return;
        flush();
    }

    public static void flush() {
        if (!configDirty) return;
        Configs.INSTANCE.save();
        configDirty = false;
    }

    public static void init() {
        if (!isAvailable()) {
            Sug_survival_assistant_plusClient.LOGGER.info("OneConfig not present — using malilib config UI only");
            return;
        }
        try {
            registerTree();
            Sug_survival_assistant_plusClient.LOGGER.info("OneConfig detected — registered ClickGUI + malilib");
        } catch (Throwable t) {
            Sug_survival_assistant_plusClient.LOGGER.error("Failed to register OneConfig ClickGUI; falling back to malilib only", t);
            registeredTree = null;
        }
    }

    public static boolean openGui() {
        if (!isAvailable()) {
            return false;
        }
        try {
            // Always rebuild so unlock token "open" in health patterns takes effect immediately.
            registerTree();
            Object screen = createUiScreen();
            if (screen instanceof Screen s) {
                Minecraft.getInstance().setScreen(s);
                return true;
            }
        } catch (Throwable t) {
            Sug_survival_assistant_plusClient.LOGGER.error("Failed to open OneConfig UI", t);
        }
        return false;
    }

    public static Screen createConfigScreen(Screen parent) {
        if (!isAvailable()) {
            return null;
        }
        try {
            registerTree();
            Object screen = createUiScreen();
            if (screen instanceof Screen s) {
                return s;
            }
        } catch (Throwable t) {
            Sug_survival_assistant_plusClient.LOGGER.error("Failed to create OneConfig config screen", t);
        }
        return null;
    }

    private static void registerTree() throws Exception {
        Class<?> treeClass = Class.forName("org.polyfrost.oneconfig.api.config.v1.Tree");
        Constructor<?> treeCtor = treeClass.getConstructor(String.class, Object.class, Object.class, java.util.Map.class);
        treeId = Sug_survival_assistant_plusClient.MOD_ID + ".json";
        Object tree = treeCtor.newInstance(treeId, "SUG Survival Assistant PLUS", "Client survival utilities", null);

        Object category = resolveCategory();
        if (category != null) {
            invoke(tree, "addMetadata", new Class<?>[]{String.class, Object.class}, "category", category);
        }

        Method put = treeClass.getMethod("put", Class.forName("org.polyfrost.oneconfig.api.config.v1.Node"));
        // Rebuild from currently visible configs so "open" unlock is respected.
        for (IConfigBase config : Configs.getVisibleConfigs()) {
            Object prop = toProperty(config);
            if (prop != null) {
                put.invoke(tree, prop);
            }
            if (config instanceof ConfigBooleanHotkeyed hotkeyed) {
                Object hotkeyProp = toHotkeyProperty(hotkeyed.getKeybind(), sanitizeId(config.getName()) + "_hotkey", config.getName() + " 热键", safe(config.getComment()), categoryFor(config), "热键");
                if (hotkeyProp != null) put.invoke(tree, hotkeyProp);
            }
        }

        Class<?> registryClass = Class.forName("org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry");
        Object registry = registryClass.getField("INSTANCE").get(null);
        Class<?> sourceClass = Class.forName("org.polyfrost.oneconfig.internal.ui.api.ConfigSource");
        Object source = Enum.valueOf(sourceClass.asSubclass(Enum.class), "OC");

        Method registerTree = null;
        for (Method m : registryClass.getMethods()) {
            if (m.getName().equals("registerTree") && m.getParameterCount() >= 2) {
                Class<?>[] params = m.getParameterTypes();
                if (params[0].isAssignableFrom(treeClass) && params[1].isAssignableFrom(sourceClass)) {
                    registerTree = m;
                    if (m.getParameterCount() == 2) {
                        break;
                    }
                }
            }
        }
        if (registerTree == null) {
            throw new IllegalStateException("ConfigRegistry.registerTree not found");
        }
        if (registerTree.getParameterCount() == 2) {
            registerTree.invoke(registry, tree, source);
        } else if (registerTree.getParameterCount() == 3) {
            registerTree.invoke(registry, tree, source, null);
        } else {
            registerTree.invoke(registry, tree, source, null, true);
        }

        registeredTree = tree;
    }

    private static Object createUiScreen() throws Exception {
        Class<?> screenClass = Class.forName("org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen");
        Class<?> treeClass = Class.forName("org.polyfrost.oneconfig.api.config.v1.Tree");
        try {
            Constructor<?> ctor = screenClass.getConstructor(String.class, String.class, treeClass);
            return ctor.newInstance(treeId, null, registeredTree);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> ctor = screenClass.getConstructor();
            return ctor.newInstance();
        }
    }

    private static Object resolveCategory() {
        try {
            Class<?> categoryClass = Class.forName("org.polyfrost.oneconfig.api.config.v1.Config$Category");
            try {
                return categoryClass.getField("UTILITY").get(null);
            } catch (NoSuchFieldException e) {
                return categoryClass.getField("QOL").get(null);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object toProperty(IConfigBase config) throws Exception {
        String id = sanitizeId(config.getName());
        String title = safe(config.getConfigGuiDisplayName());
        if (title == null || title.isBlank()) {
            title = config.getName();
        }
        String description = safe(config.getComment());
        String category = categoryFor(config);
        String subcategory = subcategoryFor(config);

        if (config instanceof ConfigBoolean booleanConfig) {
            Object prop = functional(
                    booleanConfig::getBooleanValue,
                    value -> {
                        booleanConfig.setBooleanValue(Boolean.TRUE.equals(value));
                        save();
                    },
                    id, title, description, Boolean.TYPE
            );
            return decorate(prop, "SwitchVisualizer", category, subcategory, null, null, null);
        }

        if (config instanceof ConfigInteger integerConfig) {
            Object prop = functional(
                    integerConfig::getIntegerValue,
                    value -> {
                        integerConfig.setIntegerValue(value instanceof Number n ? n.intValue() : integerConfig.getIntegerValue());
                        save();
                    },
                    id, title, description, Integer.TYPE
            );
            return decorate(
                    prop,
                    "SliderVisualizer",
                    category,
                    subcategory,
                    (float) integerConfig.getMinIntegerValue(),
                    (float) integerConfig.getMaxIntegerValue(),
                    1.0f
            );
        }

        if (config instanceof ConfigDouble doubleConfig) {
            Object prop = functional(
                    () -> (float) doubleConfig.getDoubleValue(),
                    value -> {
                        doubleConfig.setDoubleValue(value instanceof Number n ? n.doubleValue() : doubleConfig.getDoubleValue());
                        save();
                    },
                    id, title, description, Float.TYPE
            );
            return decorate(
                    prop,
                    "SliderVisualizer",
                    category,
                    subcategory,
                    (float) doubleConfig.getMinDoubleValue(),
                    (float) doubleConfig.getMaxDoubleValue(),
                    0.05f
            );
        }

        if (config instanceof ConfigString stringConfig) {
            Object prop = functional(
                    stringConfig::getStringValue,
                    value -> {
                        stringConfig.setStringValue(value == null ? "" : String.valueOf(value));
                        save();
                    },
                    id, title, description, String.class
            );
            return decorate(prop, "TextVisualizer", category, subcategory, null, null, null);
        }

        if (config instanceof ConfigColor colorConfig) {
            Object prop = functional(
                    colorConfig::getIntegerValue,
                    value -> {
                        colorConfig.setIntegerValue(value instanceof Number n ? n.intValue() : colorConfig.getIntegerValue());
                        save();
                    },
                    id, title, description, Integer.TYPE
            );
            return decorate(prop, "ColorVisualizer", category, subcategory, null, null, null);
        }

        if (config instanceof ConfigStringList listConfig) {
            Object prop = functional(
                    () -> String.join("\n", listConfig.getStrings()),
                    value -> {
                        List<String> lines = new ArrayList<>();
                        if (value != null) {
                            for (String line : String.valueOf(value).split("\\R", -1)) {
                                if (!line.isBlank()) {
                                    lines.add(line.trim());
                                }
                            }
                        }
                        listConfig.setStrings(lines);
                        save();
                    },
                    id, title, description, String.class
            );
            Object decorated = decorate(prop, "TextVisualizer", category, subcategory, null, null, null);
            invoke(decorated, "addMetadata", new Class<?>[]{String.class, Object.class}, "multiline", true);
            return decorated;
        }

        if (config instanceof ConfigHotkey hotkeyConfig) {
            Object prop = toHotkeyProperty(hotkeyConfig.getKeybind(), id, title, description, category, subcategory);
            if (prop != null) return prop;
            Object fallback = functional(
                    hotkeyConfig::getStringValue,
                    value -> {
                        hotkeyConfig.setValueFromString(value == null ? "" : String.valueOf(value));
                        save();
                    },
                    id, title, description, String.class
            );
            return decorate(fallback, "TextVisualizer", category, subcategory, null, null, null);
        }

        // Fallback: string representation for unknown option types.
        Object prop = functional(
                config::getName,
                value -> {
                },
                id, title, description, String.class
        );
        return decorate(prop, "TextVisualizer", category, subcategory, null, null, null);
    }

    private static Object toHotkeyProperty(IKeybind keybind, String id, String title, String description, String category, String subcategory) {
        try {
            Class<?> keybindClass = Class.forName("org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind");
            Object prop = functional(
                    () -> {
                        try {
                            return createOneConfigKeybind(keybind, keybindClass);
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    },
                    value -> {
                        try {
                            applyOneConfigKeybind(keybind, value);
                            save();
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    },
                    id, title, description, keybindClass
            );
            return decorate(prop, "KeybindVisualizer", category, subcategory, null, null, null);
        } catch (Throwable t) {
            Sug_survival_assistant_plusClient.LOGGER.warn("Unable to adapt OneConfig hotkey {}", title, t);
            return null;
        }
    }

    private static Object createOneConfigKeybind(IKeybind keybind, Class<?> keybindClass) throws ReflectiveOperationException {
        List<Integer> keys = keybind.getKeys();
        int[] keyboard = keys.stream().filter(code -> code >= 0 && !isModifier(code)).mapToInt(Integer::intValue).toArray();
        int[] mouse = keys.stream().filter(code -> code >= -100 && code <= -93).mapToInt(code -> code + 100).toArray();
        byte modifiers = modifierMask(keys);
        Class<?> functionClass = Class.forName("kotlin.jvm.functions.Function1");
        Object action = Proxy.newProxyInstance(
                functionClass.getClassLoader(),
                new Class<?>[]{functionClass},
                (proxy, method, args) -> method.getName().equals("invoke") ? Boolean.TRUE : defaultProxyValue(method)
        );
        Constructor<?> constructor = keybindClass.getConstructor(int[].class, int[].class, byte.class, long.class, functionClass);
        return constructor.newInstance(keyboard.length == 0 ? null : keyboard, mouse.length == 0 ? null : mouse, modifiers, 0L, action);
    }

    private static void applyOneConfigKeybind(IKeybind keybind, Object value) throws ReflectiveOperationException {
        keybind.clearKeys();
        if (value == null) return;
        int[] keyboard = (int[]) value.getClass().getMethod("getKeyCodes").invoke(value);
        int[] mouse = (int[]) value.getClass().getMethod("getMouseBtns").invoke(value);
        byte modifiers = (byte) value.getClass().getMethod("getMods").invoke(value);
        addModifierKeys(keybind, modifiers);
        if (keyboard != null) Arrays.stream(keyboard).filter(code -> !isModifier(code)).forEach(keybind::addKey);
        if (mouse != null) Arrays.stream(mouse).filter(code -> code >= 0 && code <= 7).map(code -> code - 100).forEach(keybind::addKey);
    }

    private static byte modifierMask(List<Integer> keys) {
        int mask = 0;
        if (keys.contains(340) || keys.contains(344)) mask |= 1;
        if (keys.contains(341) || keys.contains(345)) mask |= 2;
        if (keys.contains(342) || keys.contains(346)) mask |= 4;
        if (keys.contains(343) || keys.contains(347)) mask |= 8;
        return (byte) mask;
    }

    private static boolean isModifier(int code) {
        return code >= 340 && code <= 347;
    }

    private static void addModifierKeys(IKeybind keybind, byte modifiers) {
        if ((modifiers & 1) != 0) keybind.addKey(340);
        if ((modifiers & 2) != 0) keybind.addKey(341);
        if ((modifiers & 4) != 0) keybind.addKey(342);
        if ((modifiers & 8) != 0) keybind.addKey(343);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object functional(
            Supplier<?> getter,
            Consumer<Object> setter,
            String id,
            String title,
            String description,
            Class<?> type
    ) throws Exception {
        if (functionalMethod == null) {
            Class<?> properties = Class.forName("org.polyfrost.oneconfig.api.config.v1.Properties");
            for (Method method : properties.getMethods()) {
                if (method.getName().equals("functional") && method.getParameterCount() == 6) {
                    functionalMethod = method;
                    break;
                }
            }
        }
        if (functionalMethod == null) throw new NoSuchMethodException("Properties.functional");

        return functionalMethod.invoke(null, (Supplier) getter::get, (Consumer) setter, id, title, description, type);
    }

    private static Object decorate(
            Object prop,
            String visualizerSimpleName,
            String category,
            String subcategory,
            Float min,
            Float max,
            Float step
    ) throws Exception {
        Class<?> visualizerClass = Class.forName("org.polyfrost.oneconfig.api.config.v1.Visualizer$" + visualizerSimpleName);
        invoke(prop, "addMetadata", new Class<?>[]{String.class, Object.class}, "visualizer", visualizerClass);
        invoke(prop, "addMetadata", new Class<?>[]{String.class, Object.class}, "category", category);
        if (subcategory != null) {
            invoke(prop, "addMetadata", new Class<?>[]{String.class, Object.class}, "subcategory", subcategory);
        }
        if (min != null) {
            invoke(prop, "addMetadata", new Class<?>[]{String.class, Object.class}, "min", min);
        }
        if (max != null) {
            invoke(prop, "addMetadata", new Class<?>[]{String.class, Object.class}, "max", max);
        }
        if (step != null) {
            invoke(prop, "addMetadata", new Class<?>[]{String.class, Object.class}, "step", step);
        }
        return prop;
    }

    private static String categoryFor(IConfigBase config) {
        if (Configs.GENERAL.contains(config)) {
            return "通用";
        }
        if (Configs.ASSIST.contains(config)) {
            return "辅助";
        }
        if (Configs.VISUAL.contains(config) || Configs.UNLOCKED_EXTRA.contains(config)) {
            return "视觉";
        }
        if (Configs.CHAT.contains(config)) {
            return "聊天";
        }
        return "通用";
    }

    private static String subcategoryFor(IConfigBase config) {
        if (Configs.UNLOCKED_EXTRA.contains(config)) {
            if (config.getName() != null && config.getName().contains("鬼手")) {
                return "鬼手";
            }
            return "名称标签";
        }
        if (config instanceof ConfigHotkey) {
            return "热键";
        }
        if (config instanceof ConfigBooleanHotkeyed) {
            return "开关";
        }
        String name = config.getName();
        if (name == null) {
            return "设置";
        }
        if (name.contains("周礼")) {
            return "周礼";
        }
        if (name.contains("聊天") || name.contains("命令") || name.contains("观察者")) {
            return "聊天工具";
        }
        if (name.contains("预警") || name.contains("抛物线")) {
            return "提示";
        }
        if (name.contains("透视")) {
            return "透视";
        }
        if (name.contains("自动")) {
            return "自动化";
        }
        if (name.contains("自由") || name.contains("防传送") || name.contains("无减速") || name.contains("潜影")) {
            return "移动/补给";
        }
        return "设置";
    }

    private static String sanitizeId(String name) {
        String base = name == null ? "option" : name.trim();
        base = base.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fff]", "_");
        if (base.isBlank()) {
            base = "option";
        }
        return base.toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void save() {
        configDirty = true;
        saveDelayTicks = 10;
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getMethod(name, types);
        return method.invoke(target, args);
    }

    private static Object defaultProxyValue(Method method) {
        if (method.getReturnType() == boolean.class) {
            return false;
        }
        if (method.getReturnType() == int.class) {
            return 0;
        }
        if (method.getReturnType() == long.class) {
            return 0L;
        }
        if (method.getReturnType() == float.class) {
            return 0f;
        }
        if (method.getReturnType() == double.class) {
            return 0d;
        }
        return null;
    }
}
