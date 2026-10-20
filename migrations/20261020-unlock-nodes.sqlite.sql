-- 仅用于停服后的 LOCAL/SQLite 数据副本；执行前独立备份整个 data.db。
-- 需要旧表存在 choice 字段、SQLite >= 3.35；每个 statement 区块作为一条 SQL 执行。
-- 检查失败使用 RAISE(ROLLBACK) 回滚整个迁移，不猜测缺失选项、不退款、不重复扣费。
-- statement
BEGIN IMMEDIATE;
-- statement
CREATE TEMP TABLE unlock_migration_guard (problem INTEGER NOT NULL);
-- statement
CREATE TEMP TRIGGER unlock_migration_reject
BEFORE INSERT ON unlock_migration_guard WHEN NEW.problem <> 0
BEGIN
    SELECT RAISE(ROLLBACK, '旧功能节点存在异常、重复或新旧冲突，迁移已回滚');
END;
-- statement
CREATE TEMP TABLE unlock_node_mapping (old_node TEXT PRIMARY KEY, node_a TEXT NOT NULL, node_b TEXT NOT NULL);
-- statement
INSERT INTO unlock_node_mapping (old_node, node_a, node_b) VALUES
('knight-F-01', 'knight-F-01_a', 'knight-F-01_b'),
('knight-F-02', 'knight-F-02_a', 'knight-F-02_b'),
('knight-F-04', 'knight-F-04_a', 'knight-F-04_b'),
('knight-F-05', 'knight-F-05_a', 'knight-F-05_b'),
('knight-F-06', 'knight-F-06_a', 'knight-F-06_b'),
('knight-F-07', 'knight-F-07_a', 'knight-F-07_b'),
('knight-F-08', 'knight-F-08_a', 'knight-F-08_b');
-- statement
CREATE TABLE planners_skill_tree_node_before_unlock_20261020 AS SELECT * FROM planners_skill_tree_node;
-- statement
INSERT INTO unlock_migration_guard
SELECT COUNT(*) FROM planners_skill_tree_node n
LEFT JOIN unlock_node_mapping m ON n.tree = 'knight_warder' AND n.node = m.old_node
WHERE (m.old_node IS NOT NULL AND (
    n.level IS NULL OR n.level NOT IN (0, 1) OR
    (n.level = 0 AND n.choice IS NOT NULL AND n.choice <> '') OR
    (n.level = 1 AND (n.choice IS NULL OR n.choice NOT IN ('A', 'B')))
)) OR (m.old_node IS NULL AND n.choice IS NOT NULL AND n.choice <> '');
-- statement
INSERT INTO unlock_migration_guard
SELECT COUNT(*) FROM (
    SELECT n.route, n.tree, n.node FROM planners_skill_tree_node n
    JOIN unlock_node_mapping m ON n.tree = 'knight_warder' AND n.node = m.old_node
    GROUP BY n.route, n.tree, n.node HAVING COUNT(*) > 1
);
-- statement
INSERT INTO unlock_migration_guard
SELECT COUNT(*) FROM planners_skill_tree_node n
JOIN unlock_node_mapping m ON n.tree = 'knight_warder' AND n.node = m.old_node
WHERE EXISTS (
    SELECT 1 FROM planners_skill_tree_node target
    WHERE target.route = n.route AND target.tree = n.tree AND target.node IN (m.node_a, m.node_b)
);
-- statement
UPDATE planners_skill_tree_node
SET node = (SELECT CASE planners_skill_tree_node.choice WHEN 'A' THEN m.node_a WHEN 'B' THEN m.node_b END
            FROM unlock_node_mapping m WHERE m.old_node = planners_skill_tree_node.node)
WHERE tree = 'knight_warder' AND level = 1 AND node IN (SELECT old_node FROM unlock_node_mapping);
-- statement
DELETE FROM planners_skill_tree_node
WHERE tree = 'knight_warder' AND level = 0 AND node IN (SELECT old_node FROM unlock_node_mapping);
-- statement
INSERT INTO unlock_migration_guard
SELECT CASE WHEN
    (SELECT COUNT(*) FROM planners_skill_tree_node_before_unlock_20261020 n JOIN unlock_node_mapping m
     ON n.tree = 'knight_warder' AND n.node = m.old_node AND n.level = 1)
    =
    (SELECT COUNT(*) FROM planners_skill_tree_node n
     JOIN planners_skill_tree_node_before_unlock_20261020 original ON n.id = original.id
     JOIN unlock_node_mapping m ON original.tree = 'knight_warder' AND original.node = m.old_node
     WHERE original.level = 1 AND n.level = 1 AND n.route = original.route AND n.tree = original.tree
       AND n.node = CASE original.choice WHEN 'A' THEN m.node_a WHEN 'B' THEN m.node_b END)
THEN 0 ELSE 1 END;
-- statement
ALTER TABLE planners_skill_tree_node DROP COLUMN choice;
-- statement
DROP TRIGGER unlock_migration_reject;
-- statement
DROP TABLE unlock_migration_guard;
-- statement
DROP TABLE unlock_node_mapping;
-- statement
COMMIT;
