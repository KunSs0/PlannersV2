# Kether 到 Fluxon 语法映射指南

## 基本语法差异

### 1. 字符串插值
```yaml
# Kether
"{{ expression }}"
"{{ &variable }}"
"{{ ctx skill level }}"
"{{ lazy *damage }}"

# Fluxon
"${expression}"
"${&variable}"
"${&level}"  # 需要确保变量在作用域中定义
"${&damage}"
```

### 2. 变量引用
```yaml
# Kether
&variable
lazy variable
lazy *variable
ctx skill level

# Fluxon
&variable  # 严格引用，变量不存在会报错
&?variable  # 可选引用，变量不存在返回 null
```

### 3. 函数定义
```yaml
# Kether
def main = {
  # body
}

# Fluxon
def main() {
  // body
}
```

### 4. 条件语句
```yaml
# Kether
if [ condition ] then {
  # then-branch
}
if condition then {
  # then-branch
}

# Fluxon
if condition then {
  // then-branch
} else {
  // else-branch
}
# 或
if condition {
  // then-branch
}
```

### 5. 变量赋值
```yaml
# Kether
set variable to value
set variable to lazy expression

# Fluxon
variable = value
variable = expression
```

### 6. 检查条件
```yaml
# Kether
check metadata 状态 contains 前摇
check profile mp < lazy mp_cost
check &angle_diff < 45

# Fluxon
# 需要转换为相应的函数调用或操作符
&angle_diff < 45  # 直接比较
```

### 7. 逻辑运算
```yaml
# Kether
any [ cond1 cond2 cond3 ]
all [ cond1 cond2 cond3 ]
not condition

# Fluxon
cond1 || cond2 || cond3
cond1 && cond2 && cond3
!condition
```

### 8. 循环
```yaml
# Kether
foreach &list as item then {
  # body
}

# Fluxon
for item in &list {
  // body
}
```

### 9. 数学表达式
```yaml
# Kether
math 45 + ctx skill level * 12
math &level * 200
math abs(&target_yaw - &player_yaw)

# Fluxon
45 + &level * 12  # 直接写表达式
&level * 200
static java.lang.Math.abs(&target_yaw - &player_yaw)
```

### 10. 函数调用
```yaml
# Kether
call functionName
command inline "..." as console
tell "message"
exit success
cooldown reset

# Fluxon
functionName()  # 显式调用
# command、tell、exit、cooldown 等需要转换为对应的 Fluxon 函数或扩展方法
return null
```

### 11. 扩展函数调用
```yaml
# Kether
entity &target yaw
player yaw
player look location distance lazy range

# Fluxon
&target :: yaw()  # 假设有扩展函数
&sender :: yaw()  # sender 需要在作用域中定义
# 需要根据实际扩展函数 API 调整
```

## 迁移步骤

1. **替换字符串插值**：`{{ ... }}` → `${...}`
2. **更新函数定义**：`def name = {` → `def name() {`
3. **移除 check 关键字**：`if check condition` → `if condition`
4. **替换变量赋值**：`set var to value` → `var = value`
5. **更新数学表达式**：移除 `math` 关键字
6. **替换逻辑运算**：`any [...] ` → `... || ...`
7. **更新函数调用**：`call func` → `func()`
8. **替换特殊命令**：根据 Fluxon API 更新 command、metadata 等调用
9. **更新循环语法**：`foreach` → `for ... in`
10. **替换 lazy 关键字**：`lazy variable` → `&variable`

## 注意事项

- Fluxon 中裸标识符默认是字符串字面量，必须用 `&` 引用变量
- 需要根据实际的 Fluxon 扩展函数 API 调整 command、metadata、profile 等调用
- 一些 Kether 特定的功能（如 `ctx skill level`）需要通过作用域变量访问
- 注释从 `#` 改为 `//`
