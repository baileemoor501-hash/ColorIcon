# Web 端主题编辑器 Widget 配置支持需求说明书

## 1. 项目背景与目标
当前 Web 端主题编辑工具已支持图标（颜色、分层、形状）及壁纸配置。本次迭代旨在新增 **Widget（小组件）** 的配置支持，允许用户导入、编辑、预览 Widget，并在导出主题包时包含相关配置与资源。

## 2. 界面交互需求 (UI/UX)

### 2.1 右侧编辑面板 (配置区)
**入口变更：**
*   在右侧编辑区域的一级导航栏（原壁纸按钮旁）新增 **“Widget”** 选项卡。
*   点击该选项卡，右侧面板切换至 Widget 编辑界面。

**Widget 编辑界面布局：**
该界面主要分为三个区域：**导入操作区**、**属性编辑区**、**资源列表区**。

1.  **导入操作区**
    *   提供两个主要按钮：
        *   **[导入 Zip]**：打开本地文件选择器，上传 Widget 压缩包。
        *   **[链接导入]**：弹窗输入 URL，支持通过 zip 下载链接导入。

2.  **属性编辑区 (当前选中 Widget)**
    *   当用户在“资源列表区”选中某个 Widget 时，显示以下字段并支持修改：
        *   **组件类型 (widget_type)**：下拉选择框 + 自定义输入。
            *   预设值：`Geometry_clock` (几何时钟), `cool_clock_widget` (酷黑时钟), `cmn_new_color_widget` (彩色组件)。
            *   支持手动输入自定义类型。
        *   **组件名称 (widget_name)**：文本输入框。默认从文件名解析，支持自定义。
        *   **下载地址 (download_url)**：只读/可编辑文本框。
            *   本地导入：显示 zip 文件名。
            *   链接导入：显示原始下载链接。
    *   **操作按钮**：
        *   **[删除组件]**：从列表中删除当前选中的 Widget 配置。

3.  **资源列表区 (已导入预览)**
    *   展示方式：**4x4 网格布局**。
    *   内容：显示 Widget 的预览图。
    *   交互：
        *   **点击选中**：高亮显示当前项，上方“属性编辑区”回显对应数据，且左侧“壁纸预览区”同步显示该 Widget。
        *   支持多选（可选需求，若 json 数组支持多 Widget）。

### 2.2 左侧预览面板 (画布区)
*   **渲染逻辑**：在原有壁纸预览层之上，叠加渲染当前选中的 Widget 预览图。
*   **默认位置**：画布水平居中，距离顶部 `100px`。
*   **交互支持**：支持鼠标拖拽调整预览图位置（更新坐标数据，若后续需导出坐标）。

## 3. 业务逻辑与数据处理 (Logic)

### 3.1 数据结构 (Store/Database)
在前端状态管理或 IndexedDB 中建立 Widget 数据模型，包含以下字段：

| 字段名 | 类型 | 说明 | 备注 |
| :--- | :--- | :--- | :--- |
| `id` | Integer | 主键 | 自增 ID，唯一标识 |
| `widget_type` | String | 组件类型 | 默认为 `cool_clock_widget` |
| `widget_name` | String | 组件名称 | 解析自文件名或用户输入 |
| `download_url` | String | 下载地址/文件名 | 远程 URL 或本地 Zip 文件名 |
| `widget_preview` | String (Base64) | 预览图数据 | 用于列表展示和左侧画布预览 |
| `file_blob` | Blob/File | 原始文件对象 | (新增建议) 用于本地导入时的最终打包导出 |

### 3.2 导入解析逻辑
**场景 A：本地 Zip 文件导入**
1.  **缓存文件**：将上传的 zip 文件流暂存至 `widget_zip_cache`（内存或临时存储）。
2.  **自动解析**：
    *   **预览图**：遍历 zip 包内容，查找文件名包含 `Preview` (不区分大小写) 的图片文件 (png/jpg)，转换为 Base64 存入 `widget_preview`。
    *   **名称**：提取 zip 文件名（去除后缀）作为 `widget_name`。
    *   **类型**：默认填充 `cool_clock_widget`。
    *   **URL**：填充 zip 文件全名。
3.  **入库**：生成 ID 并存入数据库。

**场景 B：链接导入**
1.  **基础信息**：直接使用链接作为 `download_url`。
2.  **名称/类型**：使用默认值或需用户手动补充。
3.  **预览图**：使用默认占位图，或尝试请求链接获取（若跨域允许）。

## 4. 导出配置 (Output)

### 4.1 JSON 配置导出
修改 `theme.json` 生成逻辑，新增 `widget_cfgs` 字段。仅当用户在列表中**选中**了 Widget 时才写入此字段。

**JSON 结构示例：**
```json
{
    "id": "我的配色方案1",
    "icon_pack_name": "默认图标包",
    "icon_pack_url": "",
    "//": "其他原有配置...",
    "widget_cfgs": [
        {
            "widget_type": "cool_clock_widget",
            "widget_name": "我的时钟",
            "download_url": "MyClockWidget.zip" 
        },
        {
            "widget_type": "Geometry_clock",
            "widget_name": "几何时钟",
            "download_url": "https://example.com/geometry_clock_widget.zip"
        }
    ]
}
```
*(注：如果支持多选导出，则数组包含多个；若仅支持单选，则数组长度为1)*

### 4.2 主题包 (Zip) 导出
在生成最终主题 Zip 包时，触发以下检查逻辑：
1.  遍历 `widget_cfgs` 数组。
2.  检查 `download_url` 字段。
3.  **判定逻辑**：
    *   如果 `download_url` 是 http/https 链接：**跳过**（仅保留 JSON 中的链接引用）。
    *   如果 `download_url` 是文件名（即通过本地 Zip 导入）：
        *   从 `widget_zip_cache` 中取出对应的原始 Zip 文件数据。
        *   将该 Zip 文件添加到最终导出的主题包根目录（或指定子目录）中。

## 5. 异常处理与边缘情况
*   **Zip 解析失败**：若导入的 Zip 中找不到包含 "Preview" 的图片，需使用默认占位图标，并提示用户“无法自动识别预览图”。
*   **文件重名**：导入同名 Widget 时，需自动重命名（如 `name_1.zip`）或提示覆盖。
*   **预览图大小**：建议限制预览图 Base64 的显示尺寸，防止列表卡顿。

---

**主要优化点说明：**
1.  **结构化**：将界面、数据、逻辑、导出分门别类，便于开发人员按模块实现。
2.  **字段补充**：在数据库设计中增加了 `file_blob` 建议，因为仅存 URL 无法在导出时获取到本地上传的原始 Zip 数据。
3.  **逻辑补全**：明确了“本地导入”与“链接导入”在导出时的不同处理逻辑（本地需打包文件，链接仅保留文本）。
4.  **交互细节**：补充了左侧预览的拖拽和默认坐标细节。

<!-- # 主题工具加上Widget配置支持
    当前项目是web端主题编辑配置工具，支持图标颜色，图标分层上色，支持图标形状，壁纸配置，现在在右侧颜色，装饰，形状，
    壁纸编辑区域加上Widget的配置，以及左侧壁纸预览内容加上Widget预览图显示，支持调整预览图位置
    修改导出json配置，加上widgetcfg的json字段

## 界面修改

### Widget入口

在右侧编辑区域，壁纸按钮加上“Widget”选项，点击切换到Widget编辑分页内容

### Widget编辑区域

    包含widget导入，编辑选项，已导入Widget预览图列表
    
- Widget编辑区域，增加“导入Widget”，“链接导入”按钮，点击可以导入已有的Widget配置的zip文件，或者zip下载链接
- 按钮下显示Widget的可编辑内容
    - widget_type，widget类型，可选值有“Geometry_clock”，“cool_clock_widget”,"cmn_new_color_widget",同时支持用户自定义
    - widget_name，widget名称，用户自定义(内置Clock widget, Weather widget, Calendar widget)
    - download_url, widget配置文件的下载链接，如果是通过导入zip方式的，显示zip文件名，否则显示下载链接
- 下方显示已导入的Widget的预览图，按照网格列表方式（4x4网格），支持点击预览图选中,编辑选项显示对应选中的Widget（widget_type, widget_name, download_url）
- 已添加的Widget配置，支持删除已添加的Widget配置


### 左侧预览界面
- 在左侧壁纸预览界面，显示选中的Widget预览，支持调整显示的预览图的显示位置，默认预览图左右居中，距离顶部100px位置

## 数据处理

### 创建Widget配置的数据数据库

- 创建一个Widget配置的数据数据库，包含字段id, widget_type, widget_name, download_url, widget_preview
    - id, 主键，自增整数
    - widget_type, widget类型，字符串
    - widget_name, widget名称，字符串
    - download_url, widget配置文件的下载链接，字符串
    - widget_preview, widget预览图的base64编码，字符串
- 通过zip文件，或者链接zip文件，导入Widget配置文件，解析出widget_type, widget_name, download_url, widget_preview，
    插入到Widget配置数据库中
- Widget 编辑项，支持编辑widget_type, widget_name, download_url，同步修改数据库中对应字段

### 导入Zip文件，zip下载链接

- 创建widget_zip_cache 目录缓存，缓存导入的Widget zip文件
- 解析zip文件，提取包含Preview名字的图片，作为Widget预览图
- 解析zip文件，将包含zip后缀的文件，提取名字，作为widget_name
- widget_type 默认值为“cool_clock_widget”
- download_url ，如果是通过导入zip文件的，显示zip文件名，否则显示下载链接



## 导入/导出主题json配置
    在原有导出主题json配置基础上，增加widget_cfgs字段，widget_cfgs字段值为Widget配置中选中的Widget配置，，如果没有选中的Widget配置，默认不配置widget_cfgs字段，参考以下json格式
    ```json
        {
            "id": "我的配色方案1",
            "icon_pack_name": "默认图标包",
            "icon_pack_url": "",
            "widget_cfgs":[    // 新增的Widget配置字段
                {
                    "widget_type": "cool_clock_widget",
                    "widget_name": "我的时钟",
                    "download_url": "https://example.com/clock_widget.zip",
                },
                {
                    "widget_type": "Geometry_clock",
                    "widget_name": "我的几何时钟",
                    "download_url": "https://example.com/geometry_clock_widget.zip",
                }
            ]
            ...

    ```

## 导出主题zip包

- 如果有选中的Widget配置，且Widget属于通过zip文件导入的方式，导出主题zip时，将Widget配置的zip文件添加到主题zip包中
 -->
