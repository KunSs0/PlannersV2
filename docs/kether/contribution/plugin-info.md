# 添加插件信息

本指南将介绍如何向文档站点的插件目录添加新的插件信息。

## 文件位置

所有插件数据存储在一个文件中：`src/components/PluginCatalog/data/plugins.ts`

## 添加新插件

在 `plugins.ts` 文件中，找到 `plugins` 数组，添加新的插件对象：

```typescript
{
 
  id: "myplugin",  // 插件ID（小写，无空格）
  name: "MyPlugin", //插件名称（显示在卡片上）
  description: "我的插件简短描述",// 简短描述（30-50字为宜）
  category: "utility", // 插件类别（见下方类别列表）
  letter: "M",// 插件名称首字母（大写，用于字母索引）
  detail: "插件的详细功能介绍，可以包含多行文本描述。这部分会显示在插件详侧边栏。",
  
  // 相关链接
  links: [
    // GitHub链接
    {
      type: "github",
      url: "https://github.com/username/myplugin",
      label: "GitHub"
    },
    // 文档链接
    {
      type: "docs",
      url: "https://docs.myplugin.com",
      label: "文档"
    },
    // 下载链接
    {
      type: "download",
      url: "https://example.com/download/myplugin",
      label: "下载"
    }
    // 可添加更多链接...
  ]
}
```

## 插件类别

目前已有的插件类别包括：

| 类别ID         | 中文名称  | 描述         |
|--------------|-------|------------|
| `attribute`  | 属性系统  | 实体属性相关插件   |
| `chat`       | 聊天系统  | 聊天相关的插件    |
| `dungeon`    | 地牢系统  | 地牢、副本相关插件  |
| `economy`    | 经济系统  | 经济相关插件     |
| `effect`     | 特效系统  | 特效、粒子相关插件  |
| `enchant`    | 附魔系统  | 附魔相关插件     |
| `expansion`  | 扩展插件  | 其他插件的扩展    |
| `item`       | 物品系统  | 自定义物品相关插件  |
| `level`      | 等级系统  | 等级、经验相关插件  |
| `menu`       | 菜单系统  | GUI菜单相关插件  |
| `npc`        | NPC系统 | NPC相关插件    |
| `quest`      | 任务系统  | 任务、对话相关插件  |
| `script`     | 脚本系统  | 脚本相关插件     |
| `strengthen` | 强化系统  | 物品强化相关插件   |
| `ui`         | 界面相关  | UI、HUD相关插件 |
| `utility`    | 实用工具  | 实用功能类插件    |

## 添加新类别

如果需要添加一个新的插件类别，需要修改以下两个文件中的同名函数：

1. 首先在 `src/components/PluginCatalog/PluginDetail.tsx` 中的 `getCategoryDisplayName` 函数中添加新类别映射：

```typescript
// 获取类别显示名称
const getCategoryDisplayName = (category: string): string => {
  const categoryNames: Record<string, string> = {
    'menu': '菜单',
    'npc': 'NPC',
    'quest': '任务',
    // ... 其他已有类别 ...
    'mynewcategory': '我的新类别',  // 添加新类别的中文名称
  };
  
  return categoryNames[category] || category;
};
```

2. 同样在 `src/components/PluginCatalog/index.tsx` 中的 `getCategoryDisplayName` 函数中添加相同的映射：

```typescript
// 获取类别显示名称
const getCategoryDisplayName = (category: string): string => {
  const categoryNames: Record<string, string> = {
    'menu': '菜单',
    'npc': 'NPC',
    'quest': '任务',
    // ... 其他已有类别 ...
    'mynewcategory': '我的新类别',  // 添加新类别的中文名称
  };
  
  return categoryNames[category] || category;
};
```

3. 然后在您的插件数据中使用新的类别ID：

```typescript
{
  id: "myplugin",
  name: "MyPlugin",
  description: "我的插件简短描述",
  category: "mynewcategory",  // 使用新类别ID
  // ... 其他属性 ...
}
```

系统会自动从所有插件数据中收集类别信息（通过 `getCategories()` 函数），无需在其他地方预先注册类别列表。添加新类别后，记得在本文档的"插件类别"表格中也更新相应的信息。

## 链接类型

目前支持的链接类型包括：

| 类型ID | 说明 |
|-------|------|
| `github` | GitHub 仓库链接 |
| `gitee` | Gitee 仓库链接 |
| `mcbbs` | MCBBS 帖子链接 |
| `minebbs` | MineBBS 资源链接 |
| `spigotmc` | SpigotMC 资源链接 |
| `download` | 下载链接 |
| `docs` | 文档链接 |
| `website` | 官方网站 |


## 链接信息

- 如有文档，强烈建议添加文档链接
- 确保所有链接都是有效的，定期检查更新
