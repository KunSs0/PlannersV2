# 添加 Kether 动作

本文将介绍如何向文档站点的 Kether 动作列表添加新的动作。

## 文件位置

所有 Kether 动作数据位于 `src/components/KetherList/actions/` 目录下。目前已有的提供者文件包括：

- `adyeshach.ts` - Adyeshach 插件的 Kether 动作
- `arim.ts` - Arim 插件的 Kether 动作 
- `chemdah.ts` - Chemdah 插件的 Kether 动作
- `dungeonPlus.ts` - DungeonPlus 插件的 Kether 动作
- `index.ts` - 核心配置文件（注册所有模块）
- `invero.ts` - Invero 插件的 Kether 动作
- `kether.ts` - Kether 插件的 Kether 动作
- `taboolib.ts` - TabooLib 核心的 Kether 动作
- `trmenu.ts` - TrMenu 插件的 Kether 动作
- `vulpecula.ts` - Vulpecula 插件的 Kether 动作
- `zaphkiel.ts` - Zaphkiel 插件的 Kether 动作

## 添加到现有插件

如果您想为现有插件添加 Kether 动作：

1. 打开相应插件的文件（例如 `taboolib.ts`）
2. 在 `actions` 数组中添加新的动作对象：

```typescript
{
  id: "action_id",          // 动作ID（小写，无空格）
  name: "Action Name",      // 显示名称
  description: "详细描述",   // 支持 \n 换行和简单格式化
  provider: "插件名称",      // 提供者名称
  type: "public",           // 类型: public（公开）, private（私有）
  categories: ["类别1", "类别2"], // 动作所属类别，若是单个请使用 ["类别"]的格式
  syntax: "语法格式",        // 可选：动作的语法格式
  example: "使用示例"        // 可选：使用示例代码
}
```

系统会自动收集所有动作中使用的类别，无需定义类别，可以直接在 `categories` 字段中使用。

## 创建新的提供者

如果您想添加全新插件的 Kether 动作：

1. 在 `src/components/KetherList/actions/` 目录下创建新文件（例如 `myplugin.ts`）
2. 使用以下模板：

```typescript
import { KetherActionModule } from './index';

const myPlugin: KetherActionModule = {
  name: "MyPlugin",     // 插件名称
  color: "#4CAF50",     // 颜色代码 (用于界面展示)
  actions: [
    // 在这里添加动作，格式同上
    {
      id: "my_action",
      name: "My Action",
      description: "这是我的自定义动作",
      provider: "MyPlugin",
      type: "public",
      categories: ["自定义类别"],
      syntax: "my_action 参数",
      example: "{my_action} 参数值"
    }
    // 可以添加更多动作...
  ]
};

export default myPlugin;
```

3. 在 `src/components/KetherList/actions/index.ts` 中注册提供者

```typescript
// 导入您的模块
import myPlugin from './myplugin';

// 在 modules 数组中添加
export const modules: KetherActionModule[] = [
  // 现有模块...
  myPlugin
];
```

## 动作类别

目前使用的动作类别包括：

- 实体控制 - 与实体相关的操作
- 世界与坐标 - 与世界、位置相关的操作
- 逻辑与数学 - 条件判断、逻辑运算相关
- 游戏系统 - 游戏系统相关的操作
- 数据处理 - 数据处理、变量操作相关
- 脚本控制 - 流程控制、脚本执行相关
- 界面交互 - 与玩家界面交互相关
- 物品管理 - 与物品相关的操作
- 视觉特效 - 视觉效果相关
- 经济 - 经济系统相关
- 系统配置 - 系统设置相关
- 时间与日期 - 与时间相关的操作
- 模型处理 - 与模型相关的操作
- 命令执行 - 执行指令相关的操作
- 函数操作 - 与函数相关的操作
- 文本处理 - 文本操作相关
- 网络信息 - 网络操作相关
- 权限操作 - 权限相关
- 数学运算 - 数学计算相关
- 音效操作 - 声音效果相关
- 变量操作 - 变量管理相关
- 消息显示 - 展示消息相关
- 服务器管理 - 服务器管理相关
- 事件处理 - 事件相关操作
- 正则表达式 - 正则表达式相关
- 药水效果 - 药水效果相关

您可以使用现有类别或创建新的类别，但建议保持类别的一致性。

## 动作描述格式

动作描述中可以使用以下格式：

- `\n` 或 `\\n` - 换行
- 对于复杂的描述，可以使用多行字符串，例如：
  ```typescript
  description: "第一行\n第二行"
  ```

## 示例代码格式

示例代码应该清晰展示动作的使用方法：

- 使用 `\r\\n` 分隔多个示例
- 注意转义特殊字符 