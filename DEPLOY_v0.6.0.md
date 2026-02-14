# RootLink v0.6.0 部署指南

## 📦 版本信息

**版本**: v0.6.0  
**发布日期**: 2026-02-09  
**更新内容**: 挽联缅怀系统（核心亮点功能）  
**开发进度**: 50%

---

## 🆕 本次更新内容

### ✅ 挽联缅怀系统（完整实现）

| 功能特性 | 状态 | 说明 |
|---------|------|------|
| 挽联提交 | ✅ | 为已离世亲属提交挽联 |
| 直系两代圈定 | ✅ | NebulaGraph图查询算法 |
| 全员100%通过 | ✅ | 所有直系两代必须通过 |
| 一票否决制 | ✅ | 任何一人拒绝即拒绝 |
| 无直系禁用 | ✅ | 无直系亲属不允许提交 |
| 挽联墙展示 | ✅ | 展示所有通过审核的挽联 |

---

## 🔑 核心功能说明

### 1. 直系两代圈定算法⭐

**定义**：
- **第一代长辈**：父母
- **同代**：配偶
- **第一代晚辈**：子女  
- **第二代长辈**：祖父母
- **第二代晚辈**：孙子女

**实现方式**：

#### 方式一：NebulaGraph图查询（推荐）

```java
// 查询父母
MATCH (p:Person)-[:Parent_Child]->(target:Person) 
WHERE id(target) == {targetUserId} 
RETURN id(p) AS user_id

// 查询配偶
MATCH (target:Person)-[:Spouse]-(s:Person) 
WHERE id(target) == {targetUserId} 
RETURN id(s) AS user_id

// 查询子女
MATCH (target:Person)-[:Parent_Child]->(c:Person) 
WHERE id(target) == {targetUserId} 
RETURN id(c) AS user_id

// 查询祖父母（两跳）
MATCH (gp:Person)-[:Parent_Child]->(p:Person)-[:Parent_Child]->(target:Person)
WHERE id(target) == {targetUserId}
RETURN id(gp) AS user_id

// 查询孙子女（两跳）
MATCH (target:Person)-[:Parent_Child]->(c:Person)-[:Parent_Child]->(gc:Person)
WHERE id(target) == {targetUserId}
RETURN id(gc) AS user_id
```

#### 方式二：MySQL查询（Fallback）

如果NebulaGraph不可用，自动降级到MySQL查询。

---

### 2. 全员100%通过审核机制⭐

**规则**：
- 所有直系两代亲属都必须审核通过
- 只要有1人未审核，挽联保持待审核状态
- 全部通过后，挽联自动发布到挽联墙

**流程**：
```
提交挽联
  ↓
系统圈定直系两代亲属（如：5人）
  ↓
为每人创建审核记录（5条）
  ↓
审核进度：0/5 → 3/5 → 4/5 → 5/5 ✅
  ↓
全员通过 → 自动发布
```

---

### 3. 一票否决制⭐

**规则**：
- 任何一个直系两代亲属拒绝
- 整个挽联立即被拒绝
- 其他审核人无需继续审核

**示例**：
```
审核进度：3人通过，1人待审核，1人拒绝
  ↓
一票否决触发
  ↓
挽联状态：审核拒绝 ❌
记录拒绝人ID和理由
```

---

### 4. 无直系亲属禁用挽联⭐

**规则**：
- 提交挽联前，系统检查是否有直系两代亲属
- 如果没有任何直系两代亲属，拒绝提交
- 错误提示："该用户无直系亲属，无法提交挽联"

**原因**：
确保挽联的严肃性和审核机制的有效性。

---

## 🗄️ 数据库更新

### 新增表（2张）

#### 1. eulogy - 挽联表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| target_user_id | BIGINT | 缅怀对象ID |
| submitter_user_id | BIGINT | 提交人ID |
| content | TEXT | 挽联内容 |
| review_status | TINYINT | 审核状态 0-2 |
| publish_time | DATETIME | 发布时间 |
| rejector_user_id | BIGINT | 拒绝人ID |

**审核状态**：
- 0: 待审核
- 1: 审核通过（发布到挽联墙）
- 2: 审核拒绝（一票否决）

#### 2. eulogy_reviewer - 挽联审核人表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| eulogy_id | BIGINT | 挽联ID |
| reviewer_user_id | BIGINT | 审核人ID |
| relation_type | TINYINT | 关系类型 1-5 |
| review_status | TINYINT | 审核状态 0-2 |
| review_time | DATETIME | 审核时间 |

**关系类型**：
- 1: 父母
- 2: 配偶
- 3: 子女
- 4: 祖父母
- 5: 孙子女

### 执行更新

```bash
cd rootlink-backend
mysql -u root -p < init-mysql.sql
```

**注意**: `init-mysql.sql` 已包含所有14张表。

---

## 🔧 后端实现

### 核心Service

**EulogyService.java** (500+行)

关键方法：

```java
// 提交挽联
void submitEulogy(Long submitterUserId, Long targetUserId, String content)
  ↓
1. 验证目标用户是已离世状态
2. 调用直系两代圈定算法 ⭐
3. 验证是否有直系亲属
4. 创建挽联记录
5. 为每个直系亲属创建审核记录

// 审核挽联
void reviewEulogy(Long reviewerUserId, Long eulogyId, Integer status, String opinion)
  ↓
1. 验证审核人权限
2. 更新审核人状态
3. 一票否决检查 ⭐
4. 全员通过检查 ⭐

// 直系两代圈定算法（核心）
List<DirectRelative> getDirectTwoGenerationRelatives(Long targetUserId)
  ↓
1. 优先使用NebulaGraph查询 ⭐
2. 失败时降级到MySQL查询
3. 返回去重后的亲属列表

// NebulaGraph查询
List<DirectRelative> queryDirectRelativesFromNebula(Long targetUserId)
  ↓
执行5个nGQL查询：
- 父母
- 配偶  
- 子女
- 祖父母（两跳）
- 孙子女（两跳）

// MySQL查询（Fallback）
List<DirectRelative> queryDirectRelativesFromMySQL(Long targetUserId)
  ↓
从user_relation表递归查询
```

---

## 📊 API接口

| 接口 | 方法 | 功能 |
|------|------|------|
| `/v1/eulogy/submit` | POST | 提交挽联 |
| `/v1/eulogy/{id}/review` | POST | 审核挽联 |
| `/v1/eulogy/my-pending-reviews` | GET | 我的待审核列表 |
| `/v1/eulogy/wall/{userId}` | GET | 挽联墙 |
| `/v1/eulogy/{id}` | GET | 挽联详情 |

---

## 🚀 快速部署

### 1. 数据库初始化

```bash
cd rootlink-backend
mysql -u root -p < init-mysql.sql
```

### 2. NebulaGraph配置（推荐）

**如果使用NebulaGraph**：

确保NebulaGraph服务运行，配置已在 `application.yml` 中：

```yaml
nebula:
  graph:
    hosts: 127.0.0.1:9669
    username: root
    password: nebula
    space: rootlink_family
```

**如果不使用NebulaGraph**：

注释掉 `config/NebulaConfig.java` 的 `@Configuration`，系统自动使用MySQL查询。

### 3. 启动服务

```bash
# 后端
mvn spring-boot:run

# Web前端
cd ../rootlink-web
npm install
npm run dev
```

---

## 🧪 完整测试流程

### 场景：三代家庭挽联提交与审核

```
家庭结构：
- 爷爷A（已离世）
- 奶奶B
- 父亲C
- 母亲D
- 儿子E
- 孙子F

直系两代圈子（以A为中心）：
- 配偶：B
- 子女：C
- 孙子女：F
总共3人审核

测试步骤：

1. 准备工作
   - 注册用户A, B, C, D, E, F
   - 建立关系：
     * A-B 配偶
     * A-C 父子
     * C-E 父子
     * E-F 父子
   - 同步到NebulaGraph
   - 设置A为已离世（life_status=3）

2. 提交挽联
   - 用户E提交挽联给A
   - 内容："爷爷，您一路走好"
   - 系统自动圈定审核人：B, C, F

3. 审核过程
   场景1：全员通过 ✅
   - B审核：通过
   - C审核：通过
   - F审核：通过
   - 结果：挽联自动发布到挽联墙
   
   场景2：一票否决 ❌
   - B审核：通过
   - C审核：拒绝（理由："内容不妥"）
   - F审核：无需审核（已被否决）
   - 结果：挽联被拒绝，记录C为拒绝人

4. 验证结果
   - 查询挽联墙：应该能看到通过的挽联
   - 查询审核记录：查看每人的审核状态
   - 验证一票否决：拒绝的挽联不在挽联墙
```

### SQL验证

```sql
-- 1. 查看挽联
SELECT * FROM eulogy WHERE target_user_id = ?;

-- 2. 查看审核人列表
SELECT 
  er.id,
  er.reviewer_user_id,
  u.real_name AS reviewer_name,
  er.relation_type,
  er.review_status,
  er.review_time
FROM eulogy_reviewer er
LEFT JOIN user u ON er.reviewer_user_id = u.id
WHERE er.eulogy_id = ?;

-- 3. 查看审核进度
SELECT 
  eulogy_id,
  COUNT(*) AS total_reviewers,
  SUM(CASE WHEN review_status = 1 THEN 1 ELSE 0 END) AS approved,
  SUM(CASE WHEN review_status = 2 THEN 1 ELSE 0 END) AS rejected,
  SUM(CASE WHEN review_status = 0 THEN 1 ELSE 0 END) AS pending
FROM eulogy_reviewer
WHERE eulogy_id = ?
GROUP BY eulogy_id;

-- 4. 查看挽联墙（已通过）
SELECT 
  e.id,
  e.content,
  u.real_name AS submitter_name,
  e.publish_time
FROM eulogy e
LEFT JOIN user u ON e.submitter_user_id = u.id
WHERE e.target_user_id = ?
AND e.review_status = 1
ORDER BY e.publish_time DESC;
```

---

## ⚙️ 配置说明

### NebulaGraph查询超时

默认超时：10秒

如果查询较慢，修改：

```java
// NebulaUtil.java
session.execute(nGQL, 10000);  // 10秒超时
```

### 直系两代算法切换

强制使用MySQL查询（调试用）：

```java
// EulogyService.java
private List<DirectRelative> getDirectTwoGenerationRelatives(Long targetUserId) {
    // 注释掉NebulaGraph查询
    // return queryDirectRelativesFromNebula(targetUserId);
    
    // 直接使用MySQL
    return queryDirectRelativesFromMySQL(targetUserId);
}
```

---

## 📈 性能优化

### NebulaGraph查询优化

```java
// 批量查询（优化）
String batchQuery = 
    "MATCH (p:Person)-[:Parent_Child]->(target:Person) WHERE id(target) == %d RETURN id(p); " +
    "MATCH (target:Person)-[:Spouse]-(s:Person) WHERE id(target) == %d RETURN id(s); " +
    "...";

// 执行批量查询
nebulaUtil.executeBatch(Arrays.asList(queries));
```

### MySQL查询优化

确保索引存在：

```sql
-- 关系表索引
CREATE INDEX idx_user_id ON user_relation(user_id);
CREATE INDEX idx_related_user_id ON user_relation(related_user_id);
CREATE INDEX idx_relation_type ON user_relation(relation_type);
```

---

## ⚠️ 注意事项

### 1. NebulaGraph依赖

如果不使用NebulaGraph，必须注释掉配置：

```java
// config/NebulaConfig.java
// @Configuration  // 注释这行
public class NebulaConfig {
```

否则启动会失败。

### 2. 关系数据同步

确保MySQL和NebulaGraph数据一致：

- 新建关系时同步到NebulaGraph
- 使用 `GraphSyncService.syncRelationToGraph()`

### 3. 审核通知

建议添加消息通知：
- 挽联提交时通知审核人
- 审核完成时通知提交人
- 一票否决时通知提交人

### 4. 挽联内容审核

建议添加：
- 敏感词过滤
- 内容长度限制
- 图片上传（可选）

---

## 🎯 前端页面

### Web端

| 页面 | 路径 | 说明 |
|------|------|------|
| 提交挽联 | /eulogy/submit | 选择对象提交挽联 |
| 审核挽联 | /eulogy/review | 我的待审核列表 |
| 挽联墙 | /eulogy/wall/:userId | 展示已通过挽联 |

**特色**：
- 提交时显示审核机制说明
- 审核时显示一票否决警告
- 挽联墙瀑布流展示

### 鸿蒙APP

| 页面 | 文件 | 说明 |
|------|------|------|
| 提交挽联 | SubmitEulogyPage.ets | 提交功能 |
| 审核挽联 | ReviewEulogyPage.ets | 审核功能 |

---

## 📂 文件清单

### 后端新增

```
rootlink-backend/
├── init-mysql.sql                     ⭐ 14张表
└── src/main/java/com/rootlink/backend/
    ├── entity/
    │   ├── Eulogy.java               ⭐ 挽联实体
    │   └── EulogyReviewer.java       ⭐ 审核人实体
    ├── mapper/
    │   ├── EulogyMapper.java         ⭐
    │   └── EulogyReviewerMapper.java ⭐
    ├── service/
    │   └── EulogyService.java        ⭐ 核心（500+行）
    └── controller/
        └── EulogyController.java     ⭐ REST API
```

### 前端新增

```
rootlink-web/src/
├── api/
│   └── eulogy.js                     ⭐
└── views/eulogy/
    ├── SubmitEulogy.vue              ⭐
    ├── ReviewEulogy.vue              ⭐
    └── EulogyWall.vue                ⭐

rootlink-harmonyos/.../eulogy/
├── SubmitEulogyPage.ets              ⭐
└── ReviewEulogyPage.ets              ⭐
```

---

## 🔄 版本对比

| 功能模块 | v0.5.0 | v0.6.0 |
|---------|--------|--------|
| 用户系统 | ✅ 100% | ✅ 100% |
| 亲属关系 | ✅ 100% | ✅ 100% |
| 图数据库 | ✅ 100% | ✅ 100% |
| 生前遗言 | ✅ 100% | ✅ 100% |
| 离世状态 | ✅ 100% | ✅ 100% |
| 挽联缅怀 | ❌ 0% | ✅ 100% ⭐ |

**总进度**: 45% → **50%** ↑

---

## 📝 常见问题

### Q1: NebulaGraph查询失败怎么办？

A: 系统自动降级到MySQL查询，不影响功能。

### Q2: 如何调试直系两代算法？

A: 添加日志查看圈定结果：

```java
log.info("直系两代亲属: {}", relatives);
```

### Q3: 一票否决后能恢复吗？

A: 不能。被拒绝的挽联无法恢复，需重新提交。

### Q4: 审核人数为0怎么办？

A: 系统会拒绝提交："该用户无直系亲属，无法提交挽联"。

---

**最后更新**: 2026-02-09  
**当前版本**: v0.6.0  
**开发进度**: 50%
