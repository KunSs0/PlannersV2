# 等级算法

等级算法文件位于 `plugins/Planners/module/level/` 目录，定义每个等级升级所需的经验值。

---

## 完整示例

以下是项目自带的 `module/level/example.yml`：

```yaml
def0:
  min: 1
  max: 100
  experience: |
    level <= 10 ? level * 200 :
    level <= 20 ? level * 600 :
    level <= 30 ? level * 900 :
    level <= 40 ? level * 1200 :
    level <= 50 ? level * 1600 :
    level * 3000
```

一个文件中可以定义多个算法，每个算法以其 ID 作为 key（如 `def0`）。

---

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `min` | Int | 是 | 最低等级 |
| `max` | Int | 是 | 最高等级 |
| `experience` | String | 是 | 升级所需经验的 JS 表达式 |

### experience 表达式

这是一个 JavaScript 表达式，根据当前等级计算升级所需经验。

**可用变量**：
- `level` — 当前等级

**示例解读**：

```javascript
level <= 10 ? level * 200 :    // 1~10 级：每级 200*等级 经验
level <= 20 ? level * 600 :    // 11~20 级：每级 600*等级 经验
level <= 30 ? level * 900 :    // 21~30 级：每级 900*等级 经验
level <= 40 ? level * 1200 :   // 31~40 级：每级 1200*等级 经验
level <= 50 ? level * 1600 :   // 41~50 级：每级 1600*等级 经验
level * 3000                    // 51~100 级：每级 3000*等级 经验
```

**具体数值**：
| 等级 | 升级所需经验 |
|------|-------------|
| 1 级 | 200 |
| 5 级 | 1,000 |
| 10 级 | 2,000 |
| 15 级 | 9,000 |
| 20 级 | 12,000 |
| 30 级 | 27,000 |
| 50 级 | 80,000 |
| 100 级 | 300,000 |

---

## 如何使用

在 `config.yml` 中指定使用哪个算法：

```yaml
settings:
  level:
    algorithm: def0    # 对应 module/level/ 中定义的算法 ID
```

在路由配置中也可以为每个路由指定独立的算法：

```yaml
# router/soldier.yml
__option__:
  algorithm:
    level: def0
```

---

## 更多示例

### 线性增长

```yaml
linear:
  min: 1
  max: 50
  experience: |
    level * 1000
```

每级都需要 `等级 × 1000` 经验。1 级需要 1000，50 级需要 50000。

### 指数增长

```yaml
exponential:
  min: 1
  max: 100
  experience: |
    Math.floor(100 * Math.pow(1.5, level))
```

经验需求按 1.5 倍指数增长，后期升级越来越难。

### 固定经验

```yaml
fixed:
  min: 1
  max: 30
  experience: |
    5000
```

每级都需要固定的 5000 经验。
