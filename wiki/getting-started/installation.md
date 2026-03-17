# 安装配置

## 环境要求

| 项目 | 要求 |
|------|------|
| Java | 8 或更高版本 |
| 服务端 | Bukkit / Spigot / Paper（1.12.2+） |
| TabooLib | 6.2.4（插件内置，无需额外安装） |

> **注意**：如果你使用 Java 17+，插件会自动下载 GraalJS 引擎依赖（首次启动需要联网）。Java 8~14 使用内置的 Nashorn 引擎，无需额外依赖。

## 安装步骤

1. 将 `Planners.jar` 放入服务器的 `plugins/` 目录
2. 启动服务器，插件会自动生成默认配置文件到 `plugins/Planners/` 目录
3. 关闭服务器
4. 根据需要修改配置文件（见下方说明）
5. 重新启动服务器

## 生成的目录结构

首次启动后，`plugins/Planners/` 目录下会生成以下文件：

```
plugins/Planners/
├── config.yml              # 主配置文件
├── key-binding.yml         # 按键绑定配置
├── lang/
│   └── zh_CN.yml           # 语言文件
├── job/                    # 职业配置目录（支持子目录）
│   └── soldier/
│       ├── swordsman.yml
│       ├── blade-master.yml
│       └── grand-master.yml
├── router/                 # 路由配置目录
│   └── soldier.yml
├── skill/                  # 技能配置目录（支持子目录）
│   ├── example0.yml
│   └── example1.yml
├── state/                  # 状态配置目录
│   └── example.yml
├── action/                 # 自定义动作目录
│   └── example0.yml
├── module/
│   ├── level/              # 等级算法目录
│   │   └── example.yml
│   └── currency/           # 货币配置目录
│       └── example.yml
└── ui/                     # UI 界面配置
    ├── skill-operator.yml
    ├── skill-upgrade.yml
    ├── router-select.yml
    ├── route-transfer.yml
    └── key-bindings-editor.yml
```

## 数据库配置

打开 `config.yml`，找到 `database` 部分：

```yaml
database:
  use: SQL        # 存储方式：SQL 或 LOCAL
  sql:
    host: 127.0.0.1
    port: 3306
    user: root
    password: 123456
    database: bukkit_plugin
    table: planners_v2
```

### 存储方式说明

| 值 | 说明 |
|----|------|
| `SQL` | 使用 MySQL 数据库存储玩家数据。适合多服务器共享数据 |
| `LOCAL` | 使用本地文件存储。适合单服务器或测试环境 |

如果选择 `SQL`，你需要：
1. 确保 MySQL 服务器已启动
2. 创建对应的数据库（如 `bukkit_plugin`）
3. 填写正确的连接信息

如果选择 `LOCAL`，不需要配置 `sql` 部分，插件会自动在本地存储数据。

## 验证安装

启动服务器后，在控制台或游戏内输入：

```
/planners reload
```

如果看到 `Reloaded.` 提示，说明插件已正常加载。

## 下一步

前往 [快速入门](quickstart.md) 了解如何创建你的第一个职业和技能。
