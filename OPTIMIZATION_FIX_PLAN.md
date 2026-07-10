# SUG Survival Assistant PLUS 修复与优化方案

## 1. 目标与原则

本文档针对当前 Minecraft/Fabric 26.1.2 版本代码中的正确性、性能、异步生命周期、可选 OneConfig 集成和构建可复现性问题给出实施方案。

实施时遵循以下原则：

- 优先修复会导致按键卡住、状态无法恢复或消息永久阻塞的正确性问题。
- 保留现有功能和有效 Mixin，不通过删除或禁用 Mixin 规避问题。
- 高频渲染路径只负责渲染，扫描、正则编译和轨迹模拟移到 Tick 或配置变更阶段。
- OneConfig 继续保持可选，不加入编译依赖；未安装时仍使用 malilib。
- 隐藏配置继续参与加载和保存；`open` 仅作为精确解锁令牌，不作为血量过滤正则。
- 每个阶段单独构建并手动验证，避免一次改动过多导致回归难以定位。

## 2. 优先级总览

| 优先级 | 项目 | 类型 | 主要影响 |
|---|---|---|---|
| P0 | AutoEat 按键与槽位生命周期 | 正确性 | 使用键可能卡住，原槽位可能无法恢复 |
| P0 | 排除 `open` 血量过滤令牌 | 正确性 | 实体名称可能被意外替换 |
| P0 | ZhouLi 超时与状态清理 | 正确性/稳定性 | 请求挂起后功能可能永久处于处理中 |
| P1 | AutoTool 单一触发路径 | 正确性/性能 | 同一挖掘过程可能重复扫描和切换 |
| P1 | HUD 物品计数缓存 | 性能 | 每帧重复扫描背包和潜影盒内容 |
| P1 | 正则预编译缓存 | 性能 | 聊天与名称标签路径重复编译正则 |
| P1 | 珍珠轨迹缓存 | 性能 | 每帧最多执行约 120 次模拟和碰撞检测 |
| P2 | BetterChat 有界存储 | 内存 | 长时间会话中折叠记录持续增长 |
| P2 | 音频解码与资源优化 | 性能/体积 | 每次播放重复读取、解码大型 WAV |
| P2 | OneConfig 热键及保存优化 | UI/性能 | 热键显示不正确，滑块修改频繁写盘 |
| P2 | 构建和元数据清理 | 工程质量 | 新环境构建不可复现、元数据资源缺失 |

## 3. 第一阶段：正确性修复

### 3.1 AutoEat 按键与槽位生命周期

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/AutoEat.java`

**现状**

`AutoEat.tick()` 在满足条件时调用 `keyUse.setDown(true)`，但停止条件依赖 `!keyUse.isDown()` 才恢复槽位。由于该功能自身把按键设置为按下，停止进食后可能无法进入恢复分支，也没有明确释放由功能模拟的使用键。

**修复方案**

1. 增加明确的内部状态，例如：
   - `boolean autoUsing`：当前按键是否由 AutoEat 接管。
   - `int previousSlot`：启用前槽位。
2. 将退出清理集中到一个 `stop(Minecraft client)` 方法：
   - 仅当 `autoUsing` 为 `true` 时释放 `keyUse`。
   - 根据配置和有效槽位恢复 `previousSlot`。
   - 清空所有内部状态。
3. 以下任一情况发生时调用 `stop()`：
   - 功能关闭。
   - 玩家、世界或交互管理器不可用。
   - 打开界面。
   - 饥饿值和生命值均恢复到阈值以上。
   - 找不到可食用物品。
   - 玩家断开连接或切换世界。
4. 不应无条件释放玩家真实按住的使用键。开始接管前记录按键原状态；停止时只撤销 AutoEat 自己施加的状态。若用户原本就在按使用键，应保留用户输入。
5. 避免每 Tick 重复切换同一槽位；仅在目标食物槽位发生变化时执行槽位切换。

**验收标准**

- 自动进食开始后能够持续正常进食。
- 达到停止条件后，使用键不会保持按下。
- 原快捷栏槽位能够恢复。
- 功能中途关闭、打开菜单、死亡或退出服务器后均无卡键。
- 用户手动按住使用键时，AutoEat 停止不会错误地释放用户输入。

### 3.2 将 `open` 作为保留令牌而非正则

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/config/Configs.java`
- `src/main/java/com/sug/survival/assistant/plus/mixin/EntityRendererMixin.java`

**现状**

`NAMETAGS_ENTITY_HEALTH_PATTERNS` 中精确等于 `open` 的条目负责解锁 Ghost Hand 和 Nametags 配置，但 `EntityRendererMixin.filterHealthText()` 仍将该条目传给 `replaceAll()`，导致名称中的 `open` 也可能被删除。

**修复方案**

1. 将保留令牌暴露为统一判断方法，例如：
   - `Configs.isFeatureUnlockToken(String value)`。
2. 该方法只接受精确、区分大小写的 `"open"`，保持现有解锁语义。
3. `Configs.isFeatureUnlocked()` 和实体血量过滤逻辑共同调用该方法，避免两处规则漂移。
4. 实体过滤遍历时跳过：
   - `null`。
   - 空字符串。
   - 精确等于保留令牌的条目。
5. 不改变隐藏配置的持久化逻辑。

**验收标准**

- 列表中存在精确 `open` 时，隐藏配置可见。
- `Open`、` open`、`open ` 等非精确条目不解锁。
- 实体名称中的 `open` 不再被删除。
- 其他有效血量正则仍正常工作。

### 3.3 ZhouLi 请求超时和状态清理

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/ZhouLi.java`
- `src/main/java/com/sug/survival/assistant/plus/mixin/ClientPacketListenerMixin.java`

**现状**

HTTP 请求未配置明确的连接和请求超时。Mixin 只在 `thenAcceptAsync()` 成功执行后清除 `zhouLiProcessing`；异常、取消或长期无响应时，状态可能一直保持为 `true`。

**修复方案**

1. 为 `HttpClient` 配置连接超时，例如 5 秒。
2. 为每个 `HttpRequest` 配置总请求超时，例如 15 秒。
3. 保留异步网络执行，禁止在 Minecraft 客户端线程执行阻塞 HTTP。
4. 将完成逻辑改为统一的完成处理：
   - 成功：发送改写结果。
   - API 失败、JSON 异常、超时：按明确策略发送原消息。
   - 无论成功或失败：在 Minecraft 客户端线程的 `finally` 语义中清除处理状态。
5. 防止二次进入：发送改写消息时应临时保持处理标记，待 `self.sendChat()` 返回后再清除。
6. 对正在处理期间的新消息明确采用一种行为。建议当前阶段保持简单：新消息不改写、按原文直接发送；不要静默丢弃，也暂不引入消息队列。
7. 日志不得输出 API Key；错误响应正文只在必要时截断记录，避免服务端返回敏感数据或超长内容污染日志。

**验收标准**

- 正常请求能够发送改写后的消息。
- API 无响应时在限定时间内恢复，并允许下一条消息继续使用 ZhouLi。
- HTTP 非 200、无效 JSON、Future 异常时状态均被清理。
- API Key 不出现在日志中。
- 网络处理不冻结游戏客户端线程。

### 3.4 AutoTool 单一触发路径

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/AutoTool.java`
- `src/main/java/com/sug/survival/assistant/plus/mixin/ClientPlayerInteractionManagerMixin.java`
- `src/main/java/com/sug/survival/assistant/plus/client/Sug_survival_assistant_plusClient.java`

**现状**

`AutoTool.beforeBlockAttack()` 已由方块交互 Mixin 调用，同时 `AutoTool.tick()` 还会根据 GLFW 左键状态再次调用，可能在同一挖掘过程重复扫描 9 或 36 个槽位并重复切换。

**修复方案**

1. 保留 `ClientPlayerInteractionManagerMixin`，由其负责开始攻击或继续破坏方块时调用工具选择。
2. Tick 路径不再主动执行 `beforeBlockAttack()`；仅负责检测挖掘结束并调用 `stopMining()` 恢复槽位。
3. 使用 Minecraft 的 `client.options.keyAttack.isDown()` 判断攻击键，而不是硬编码 GLFW 鼠标左键，以支持用户改键。
4. 记录上一次目标方块和对应方块状态；目标未变化时不重复扫描工具。
5. 玩家手动改变槽位时应重新确认恢复策略，避免结束挖掘后覆盖用户主动选择。建议记录“自动切换后的槽位”和“用户是否另行切槽”，只在槽位仍由 AutoTool 控制时恢复。
6. 背包工具临时交换逻辑保持原功能，不删除相关 Mixin。

**验收标准**

- 开始挖掘时只进行一次必要的工具选择。
- 持续挖掘同一方块不会每 Tick 重扫背包。
- 攻击键改绑后仍能正确检测停止。
- 热键栏工具和背包工具均可切换及恢复。
- 用户挖掘过程中手动切槽不会被错误覆盖。

## 4. 第二阶段：高频路径优化

### 4.1 HUD 背包和潜影盒计数缓存

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/FireworkWarningHud.java`
- `src/main/java/com/sug/survival/assistant/plus/feature/ShulkerRestock.java`
- `src/main/java/com/sug/survival/assistant/plus/client/Sug_survival_assistant_plusClient.java`

**现状**

HUD 每帧扫描烟花和图腾，并遍历所有潜影盒内容。帧率高于 Tick 率时会执行大量重复工作。

**修复方案**

1. 在 `FireworkWarningHud` 中保存缓存值：
   - 烟花总数。
   - 背包图腾数。
   - 潜影盒图腾数。
   - 缓存是否有效。
2. 增加 `tick(Minecraft client)`，建议每 5 Tick 更新一次；物品变化响应要求更高时可缩短到每 Tick。
3. HUD `render()` 只读取缓存并绘制文本，不再扫描物品。
4. 玩家为空、断开连接或切换世界时清空缓存。
5. `ShulkerRestock.countInShulkers()` 可继续作为低频计算入口，但避免在渲染事件中调用。
6. 后续若需要即时响应，可在容器槽位更新时标记缓存失效；第一版不必引入复杂事件系统。

**验收标准**

- HUD 渲染路径不再遍历背包或潜影盒。
- 拾取、消耗、移动烟花或图腾后，显示在设定 Tick 周期内更新。
- 进入和离开服务器时不会显示上一世界的旧计数。

### 4.2 正则预编译缓存

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/BetterChat.java`
- `src/main/java/com/sug/survival/assistant/plus/mixin/EntityRendererMixin.java`
- 可选新增一个现有包内的小型缓存类；如果只服务单个功能，优先留在各自类中。

**现状**

聊天折叠对每条消息执行 `Pattern.compile()`；实体名称过滤通过 `String.replaceAll()` 在渲染路径隐式重复编译正则。

**修复方案**

1. 为每组配置保存：
   - 上次读取的字符串列表快照。
   - 成功编译的不可变 `Pattern` 列表。
2. 仅当当前配置列表与快照不一致时重新编译。
3. 无效正则在重建缓存时忽略；每个无效表达式最多记录一次警告，避免逐帧刷日志。
4. 实体名称过滤使用 `pattern.matcher(text).replaceAll("")`。
5. 编译实体血量规则时跳过精确 `open` 保留令牌。
6. 空白折叠仍可使用预编译的常量 `Pattern`，或保留简单处理；不要每次调用 `String.replaceAll("\\s+", ...)` 重新编译。

**验收标准**

- 配置未变化时不发生正则重新编译。
- 修改配置后无需重启即可使用新规则。
- 无效正则不会导致崩溃或持续刷日志。
- `open` 不进入实体过滤缓存。

### 4.3 珍珠轨迹模拟缓存

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/PearlTrajectoryRenderer.java`
- `src/main/java/com/sug/survival/assistant/plus/client/Sug_survival_assistant_plusClient.java`

**现状**

渲染事件每帧调用 `buildTrajectory()`，最多执行约 120 次方块射线检测、实体碰撞检测和流体状态查询。

**修复方案**

1. 将轨迹计算拆为 `tick(Minecraft client)`，渲染方法只消费 `POINTS`、`pointCount` 和 `hitPos`。
2. 每个客户端 Tick 最多重算一次。
3. 在以下情况立即使缓存无效：
   - 功能关闭或重新开启。
   - 玩家/世界变化。
   - 主手、 副手是否持有末影珍珠发生变化。
   - 玩家位置、Yaw、Pitch、姿态发生变化。
4. 若同一 Tick 内状态未变化，不重算轨迹。
5. 缓存写入应先在局部变量或备用数组完成，再一次性发布计数，避免渲染读取半成品；Minecraft 主线程顺序执行时也保持该结构清晰。
6. 不降低轨迹点数量，除非实际性能测试表明仍有必要。

**验收标准**

- 高帧率下每 Tick 最多计算一次轨迹。
- 移动或转动视角时轨迹更新及时，无明显抖动。
- 切换手中物品或世界后旧轨迹立即消失。
- 碰撞点和轨迹线与修改前行为一致。

## 5. 第三阶段：内存、资源与配置 UI

### 5.1 BetterChat 有界存储

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/BetterChat.java`

**现状**

`FOLDED_MESSAGES` 在整个聊天会话中按完整消息文本增长。`FoldedMessage.message` 当前未被读取。

**修复方案**

1. 删除未使用的 `FoldedMessage.message` 字段，仅保存计数。
2. 使用有访问顺序的 `LinkedHashMap`，设置合理上限，例如 512 条不同消息。
3. 超过上限时移除最旧记录。
4. 继续在聊天清空、断开连接或切换世界时调用 `BetterChat.clear()`。
5. 确认折叠计数仅针对当前聊天会话，不写入配置文件。

**验收标准**

- 重复消息仍正确显示累计次数。
- 大量不同消息后 Map 大小不超过设定上限。
- 清空聊天或重新连接后旧计数消失。

### 5.2 HaoQiChongTian 音频优化

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/feature/HaoQiChongTian.java`
- `src/main/resources/assets/sug_survival_assistant_plus/sounds/jh.wav`

**现状**

每次播放都会重新读取资源并解码为 PCM；WAV 文件约 3.2 MB，也是产物体积的主要来源。

**分步方案**

**第一步：低风险缓存**

1. 首次播放时加载并解码 PCM。
2. 缓存不可变 PCM 字节、音频格式和帧数。
3. 后续播放只计算随机片段、速度和写入范围。
4. 解码失败时记录一次明确错误，而不是永久静默吞掉异常。
5. 继续使用守护线程，确保不会阻止 JVM 退出。

**第二步：资源格式优化**

1. 将资源转换为 OGG。
2. 优先接入 Minecraft 声音引擎和资源声明，减少直接依赖 Java Sound 设备差异。
3. 如果随机片段和动态变速必须保留，应先验证 Minecraft 声音引擎是否满足要求；无法满足时保留 PCM 缓存方案，不为了体积破坏现有功能。

**验收标准**

- 连续触发时不再重复读取和完整解码资源。
- 随机片段、停止令牌和变速效果保持一致。
- 无音频设备或格式异常时不崩溃客户端。
- 若采用 OGG，比较构建产物体积并验证实际音质。

### 5.3 OneConfig 热键适配

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/config/OneConfigBridge.java`

**现状**

`ConfigBooleanHotkeyed` 可能先匹配普通 `ConfigBoolean` 分支，只显示开关；独立 `ConfigHotkey` 当前使用文本输入而非 OneConfig 的键位可视化器。

**修复方案**

1. 在普通 `ConfigBoolean` 判断之前处理 `ConfigBooleanHotkeyed`。
2. 明确适配两部分：
   - 布尔开关属性。
   - 对应 malilib Keybind 的键位属性。
3. 独立 `ConfigHotkey` 使用 OneConfig `KeybindVisualizer`。
4. 根据本地 OneConfig API 实际键位值类型完成双向转换，不把 malilib 字符串直接假定为 OneConfig 内部键码。
5. 转换失败时仅让该项回退为文本输入，不影响整个配置树打开。
6. 保持反射实现，不在 `build.gradle` 或 `fabric.mod.json` 中增加 OneConfig 强制依赖。

**验收标准**

- 安装 OneConfig 时，普通热键显示键位控件。
- 带热键开关同时可修改开关和按键。
- 修改后 malilib 热键实际生效并持久化。
- 未安装 OneConfig 时 malilib 配置界面仍正常。

### 5.4 OneConfig 反射缓存与保存合并

**涉及文件**

- `src/main/java/com/sug/survival/assistant/plus/config/OneConfigBridge.java`

**现状**

每次打开界面都会重建 Tree、重复查找类和方法；每次 setter 调用立即执行 `Configs.INSTANCE.save()`，拖动滑块可能触发频繁磁盘写入。`registered` 和 `defaultProxyValue()` 目前没有有效用途。

**修复方案**

1. 在桥接类初始化时缓存稳定的反射对象：
   - Tree 构造器。
   - `Tree.put`。
   - `Properties.functional`。
   - `ConfigRegistry.registerTree`。
   - 可视化器 Class。
2. 保留配置树动态重建能力，因为 `open` 会改变可见选项；只缓存反射元数据，不缓存错误的可见列表快照。
3. 保存策略改为防抖：
   - setter 只更新 malilib 值并标记 dirty。
   - 在客户端 Tick 中距最后修改若干 Tick 后保存，或关闭配置界面时保存。
   - 客户端关闭前确保 dirty 配置被保存。
4. 若 OneConfig 本身提供“提交/关闭”事件，优先使用该事件；否则实现简单 Tick 防抖。
5. 删除确认无用途的 `registered`、`defaultProxyValue()` 等死代码。
6. 单个属性适配失败时跳过该属性并记录错误，不让所有配置回退。

**验收标准**

- 多次拖动滑块不会每个变化都写配置文件。
- 关闭界面或等待防抖时间后配置正确保存。
- 添加或移除精确 `open` 后重新打开界面，可见配置立即更新。
- OneConfig 缺失或 API 版本不兼容时安全回退 malilib。

## 6. 第四阶段：构建与项目元数据

### 6.1 Quick Shulker 构建可复现性

**涉及文件**

- `build.gradle`
- `.gitignore`
- `src/main/java/com/sug/survival/assistant/plus/feature/ShulkerRestock.java`

**现状**

`build.gradle` 通过 `implementation files("quickshulker-3.0.1-26.1.jar")` 引用本地 JAR，但 `.gitignore` 忽略所有 JAR。新克隆环境无法获得该文件。当前源代码通过反射访问 Quick Shulker API，不需要直接导入它的类型。

**修复方案**

1. 先删除本地 `implementation files(...)` 后执行完整构建，确认不存在隐藏的编译依赖。
2. 若构建和运行均正常，正式移除该依赖；开发运行时由用户把 Quick Shulker 放入 `run/mods`。
3. `fabric.mod.json` 不增加强制依赖，因为该集成本身是可选功能。
4. 如果未来确实需要编译期 API，则改用可获取的 Maven 坐标；不要依赖被忽略的根目录 JAR。
5. 不提交现有本地 JAR，也不删除用户本地文件。

**验收标准**

- 在没有根目录 Quick Shulker JAR 的干净环境中可完成构建。
- 未安装 Quick Shulker 时相关功能自动禁用且不崩溃。
- 安装 Quick Shulker 后补货功能仍能调用其包接口。

### 6.2 移除项目级本机 JDK 路径

**涉及文件**

- `gradle.properties`

**修复方案**

1. 删除项目中的 `org.gradle.java.home=C:/Program Files/Microsoft/jdk-25`。
2. 保留 `java.toolchain.languageVersion = 25` 作为项目要求。
3. 开发者若需固定本机路径，应放到用户级 `~/.gradle/gradle.properties`，不写入仓库。

**验收标准**

- 不同机器无需相同安装路径即可加载项目。
- Java 25 环境下仍可编译。
- 缺少 Java 25 时 Gradle 给出明确的工具链错误，而不是错误引用其他用户路径。

### 6.3 Gradle 语法更新

**涉及文件**

- `build.gradle`

**修复方案**

将：

```groovy
filteringCharset "UTF-8"
```

改为：

```groovy
filteringCharset = "UTF-8"
```

随后运行带警告模式的构建，检查是否还有本项目可修复的 Gradle 10 弃用项。

**验收标准**

- 资源处理结果不变。
- 对应弃用警告消失。
- `fabric.mod.json` 中版本占位符仍正确展开。

### 6.4 Mod 图标元数据

**涉及文件**

- `src/main/resources/fabric.mod.json`
- `src/main/resources/assets/sug_survival_assistant_plus/icon.png`（当前缺失）

**修复方案**

二选一：

1. 提供合法 PNG 图标并保留 `icon` 字段；建议使用方形、常见尺寸的图标。
2. 暂无图标时删除 `fabric.mod.json` 的 `icon` 字段，避免声明不存在的资源。

**验收标准**

- 构建产物中的元数据不再引用缺失文件。
- 若添加图标，Mod Menu 中能够正确显示。

### 6.5 本地参考目录和临时文件卫生

当前根目录存在多个未跟踪参考目录或临时文件。处理前必须逐项确认用途，不得直接批量删除或提交。

建议：

- 仅将确定属于本机参考资料且不应入库的目录加入 `.git/info/exclude`，避免把个人规则强加给仓库。
- 若团队一致认为这些目录始终是本地材料，再添加到 `.gitignore`。
- 不使用 `git add .`，提交时按明确文件名暂存。
- 不提交令牌、API Key、凭据 JSON 或运行目录内容。

## 7. 不建议在本轮进行的改动

### Freecam

`Freecam.syncFakePlayer()` 每 Tick 复制装备存在小幅优化空间，但装备槽位数量固定且较少。与 HUD 扫描、正则编译和轨迹射线检测相比收益较低，暂不增加复杂的装备版本缓存。

### NametagRenderer

渲染期间存在排序、文本和装备列表临时对象，但实际成本取决于附近玩家数量。应先使用性能分析器确认热点，再决定是否缓存；不要提前引入复杂状态同步。

### 中央 Tick 分发

多数禁用功能会在布尔判断后快速返回。当前无需为每个功能重新设计事件注册系统。优先优化已确认的高频重计算。

### ESP

ESP 当前初始化和 Tick 调用处于注释状态。后续恢复时应采用区块分段缓存和失效机制，而不是大范围每 Tick 扫描；不得通过删除其功能性 Mixin 解决兼容问题。

## 8. 推荐实施批次

### 批次 A：正确性

1. AutoEat 生命周期。
2. `open` 保留令牌。
3. ZhouLi 超时与完成清理。
4. AutoTool 去重和改键支持。

完成后执行编译，并在开发客户端手动验证按键、聊天和挖掘流程。

### 批次 B：运行性能

1. HUD 计数缓存。
2. 聊天和名称标签正则缓存。
3. 珍珠轨迹 Tick 缓存。
4. BetterChat 有界存储。

完成后比较修改前后的帧时间和内存增长，不只依赖编译成功。

### 批次 C：OneConfig 和音频

1. OneConfig 热键适配。
2. 反射元数据缓存与保存防抖。
3. 音频 PCM 缓存。
4. 评估 OGG 与 Minecraft 声音引擎迁移。

必须分别测试“安装 OneConfig”和“未安装 OneConfig”两种运行环境。

### 批次 D：工程清理

1. 移除可选本地 JAR 编译依赖。
2. 移除本机 JDK 路径。
3. 更新 Gradle 语法。
4. 补充或移除图标声明。
5. 整理本地参考目录的忽略策略。

## 9. 每批次验证清单

### 自动验证

```bash
./gradlew compileJava --offline
./gradlew build --offline
```

若调整依赖解析或需要验证干净环境，应另行执行允许联网的构建，不能用已有 Gradle 缓存代替可复现性验证。

### 手动客户端验证

- malilib 配置界面能够打开、分类和保存。
- 有 OneConfig 时优先打开 OneConfig；无 OneConfig 时安全回退 malilib。
- `open` 精确解锁和移除后的可见性正确。
- AutoEat 开始、停止、功能关闭、打开菜单和断线时均不卡键。
- AutoTool 对快捷栏和背包工具均能选择、交换和恢复。
- ZhouLi 正常、超时、HTTP 错误和无效响应路径均可继续发送后续消息。
- HUD 计数能够在允许的延迟内更新。
- 珍珠轨迹随位置和视角变化，无旧轨迹残留。
- 聊天折叠和实体血量过滤行为保持正确。
- 音频播放不会冻结客户端，重复触发能够正确终止上一段。

### 回归检查

- 不删除或停用已有功能性 Mixin。
- 不新增 OneConfig 强制依赖。
- 隐藏配置仍可持久化。
- 不把 API Key、令牌或本地运行文件写入构建产物和 Git。
- 不提交本地参考目录、第三方源码副本或被忽略的 JAR，除非用户明确指定。

## 10. 完成定义

一个修复项只有在以下条件全部满足时才算完成：

1. 代码实现符合本方案且未扩大功能范围。
2. Java 编译和完整 Gradle 构建通过。
3. 对应的客户端黄金路径和异常路径已手动验证。
4. 未引入新的强制可选依赖。
5. 未破坏 malilib、OneConfig 回退、配置持久化或现有 Mixin。
6. 对无法在当前环境手动验证的项目明确记录，而不是仅凭构建成功宣称功能正常。
