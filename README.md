# No Memory Required

<p align="center">
  <strong>基于自定义词库的轻量级 Android 输入法</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/language-Java-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/min%20SDK-24-blue?style=flat-square" alt="最低 API 24">
</p>

## 架构图

<p align="center">
  <img src="./docs/architecture.svg" alt="No Memory Required 系统架构图" width="100%">
</p>

## 功能范围

- `MainActivity` 负责词库文件的替换导入、追加导入和清空。
- `MyInputMethodService` 实现 Android 输入法服务、键盘事件处理、拼写缓冲和候选词展示。
- 词库保存在应用私有文件 `user_dict.txt` 中，不使用数据库或网络服务。
- 候选词匹配使用 `英文` 和 `拼音` 字段的大小写不敏感前缀匹配。
- 候选词点击后通过当前 `InputConnection.commitText()` 提交中文字段。

## 数据契约

### 文件格式

每行一个词条，字段顺序固定为：

```text
中文,英文,拼音
```

示例：

```text
里程碑,milestone,lichengbei
码头,dock,matou
蓝图,blueprint,lantu
宇航员,astronaut,yuhangyuan
```

| 位置 | 字段 | 用途 |
| --- | --- | --- |
| `0` | 中文 | 候选词内容，也是最终提交到编辑器的文本。 |
| `1` | 英文 | 参与英文前缀匹配。 |
| `2` | 拼音 | 参与拼音前缀匹配。 |

当前解析逻辑为：

```java
String[] parts = line.split(",");
if (parts.length >= 3) {
    customDictionary.add(new WordEntry(
        parts[1].trim(), // english
        parts[0].trim(), // chinese
        parts[2].trim()  // pinyin
    ));
}
```

导入端会保留包含逗号的行；输入法服务只加载拆分后至少包含三个字段的行。字段内部暂不支持逗号转义、引号包裹或多行文本。

## 运行时流程

1. `MainActivity` 通过 `ACTION_OPEN_DOCUMENT` 打开系统文档选择器，接收 `text/*` 文件。
2. 替换导入使用 `MODE_PRIVATE` 写入 `user_dict.txt`；追加导入使用 `MODE_APPEND`。
3. `MyInputMethodService.onCreate()` 首次加载词库。
4. `onStartInput()` 检查 `user_dict.txt.lastModified()`，发现文件更新时重新加载。
5. 字母按键追加到 `composing`，并通过 `setComposingText()` 更新当前编辑器。
6. `updateCandidates()` 对英文和拼音字段执行 `startsWith()` 匹配，并将中文字段创建为候选项。
7. 候选项点击后调用 `commitText()`，清空 composing 缓冲并移除候选项。
8. 删除键优先删除 composing 缓冲；缓冲为空时删除当前编辑器前一个字符。
9. 回车键在存在 composing 内容时提交原始输入，否则向目标编辑器发送 Enter 事件。

## 源码结构

```text
.
├── app/
│   └── src/main/
│       ├── java/com/example/myinputmethodservice/
│       │   ├── MainActivity.java
│       │   │   └── 词库导入、追加、清空和状态展示
│       │   └── MyInputMethodService.java
│       │       └── 输入法生命周期、键盘事件、匹配和候选词提交
│       ├── res/layout/
│       │   ├── activity_main.xml
│       │   │   └── 词库管理界面
│       │   └── input_method.xml
│       │       └── 候选栏和 KeyboardView 容器
│       ├── res/xml/
│       │   ├── method.xml
│       │   │   └── 输入法服务元数据
│       │   └── qwerty.xml
│       │       └── QWERTY 键盘定义
│       └── AndroidManifest.xml
│           └── Activity 与 InputMethodService 注册
├── docs/
│   └── architecture.svg
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## 构建

### 环境

- Android Studio
- JDK 11
- Android SDK
- 最低 API：24
- Target SDK：36

### Debug 构建

macOS / Linux：

```bash
./gradlew assembleDebug
```

Windows：

```powershell
.\gradlew.bat assembleDebug
```

输出文件：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装与启用

1. 使用 Android Studio 运行 `app`，或安装生成的 Debug APK。
2. 打开应用并导入符合 `中文,英文,拼音` 格式的词库。
3. 在系统设置中启用 **No Memory Required** 输入法。
4. 在任意文本输入框中切换到该输入法。
5. 输入英文或拼音前缀，点击候选栏中的中文词条。

输入法服务通过 `AndroidManifest.xml` 中的 `BIND_INPUT_METHOD` 权限和 `res/xml/method.xml` 元数据注册。

## 技术参数

| 项目 | 实现 |
| --- | --- |
| 应用语言 | Java |
| 输入法基类 | `android.inputmethodservice.InputMethodService` |
| 键盘视图 | `KeyboardView` |
| 键盘布局 | `res/xml/qwerty.xml` |
| 候选栏 | `HorizontalScrollView` + 动态 `TextView` |
| 数据存储 | 应用私有文件 `user_dict.txt` |
| 匹配算法 | 英文 / 拼音字段的大小写不敏感前缀匹配 |
| 词库更新检测 | 文件 `lastModified` 时间戳 |
| 最低 API | 24 |
| Target SDK | 36 |
| Java | 11 |

## 已知限制

- 字段分隔符固定为英文逗号，字段中不能包含未转义的逗号。
- 候选结果不做排序、去重、分页或词频统计。
- 当前只匹配英文和拼音，不支持模糊匹配、拼音音调和联想输入。
- 键盘布局固定使用项目内置的 QWERTY 定义。
- `InputStreamReader` 和 `OutputStreamWriter` 未显式指定字符集，词库文件应使用设备默认兼容的文本编码。
- 仓库当前未包含 `LICENSE` 文件。

## 贡献

涉及词库协议、输入法生命周期或候选词匹配的修改，请同时更新：

- `README.md` 中的数据契约和运行时流程；
- `activity_main.xml` 中的格式提示；
- `MyInputMethodService.java` 中的字段映射；
- 对应的单元测试或仪器测试。

<details>
<summary>English</summary>

## Overview

No Memory Required is a small Android input method backed by a local dictionary file. The dictionary contract is:

```text
Chinese,English,Pinyin
```

`MainActivity` replaces, appends, or clears `user_dict.txt`. `MyInputMethodService` loads the file, matches English or Pinyin prefixes, renders Chinese candidates, and commits the selected candidate through `InputConnection`.

Build with:

```bash
./gradlew assembleDebug
```

The architecture diagram is available at [`docs/architecture.svg`](./docs/architecture.svg).

</details>
