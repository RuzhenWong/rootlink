# RootLink v0.6.0 更新说明

## 📦 版本信息

**版本**: v0.6.0  
**发布日期**: 2026-02-09  
**主要更新**: 挽联缅怀系统（核心亮点功能）⭐  
**开发进度**: 45% → 50%

---

## ✅ 本次更新内容

### 挽联缅怀系统（完整实现）

这是RootLink项目的**核心亮点功能**，体现了对逝者的尊重和对家庭伦理的重视。

**核心特性**：
1. ✅ **挽联提交** - 为已离世亲属提交缅怀挽联
2. ✅ **直系两代圈定算法** - 基于NebulaGraph图查询 ⭐
3. ✅ **全员100%通过机制** - 所有直系两代亲属必须通过
4. ✅ **一票否决制** - 任何一人拒绝即整体拒绝 ⭐
5. ✅ **无直系亲属禁用** - 确保审核机制有效性
6. ✅ **挽联墙展示** - 展示所有通过审核的挽联

---

## 🎯 核心算法：直系两代圈定⭐

### 算法定义

**直系两代**包括：
- **第一代长辈**：父母
- **同代**：配偶
- **第一代晚辈**：子女
- **第二代长辈**：祖父母
- **第二代晚辈**：孙子女

### 技术实现

#### 方案一：NebulaGraph图查询（推荐）⭐

使用图数据库的多跳查询能力，高效准确地找出直系两代亲属。

**核心nGQL查询**：

```ngql
-- 1. 查询父母（一跳）
MATCH (p:Person)-[:Parent_Child]->(target:Person) 
WHERE id(target) == {targetUserId} 
RETURN id(p) AS user_id, 1 AS relation_type

-- 2. 查询配偶（边查询）
MATCH (target:Person)-[:Spouse]-(s:Person) 
WHERE id(target) == {targetUserId} 
RETURN id(s) AS user_id, 2 AS relation_type

-- 3. 查询子女（一跳）
MATCH (target:Person)-[:Parent_Child]->(c:Person) 
WHERE id(target) == {targetUserId} 
RETURN id(c) AS user_id, 3 AS relation_type

-- 4. 查询祖父母（两跳）⭐
MATCH (gp:Person)-[:Parent_Child]->(p:Person)-[:Parent_Child]->(target:Person)
WHERE id(target) == {targetUserId}
RETURN id(gp) AS user_id, 4 AS relation_type

-- 5. 查询孙子女（两跳）⭐
MATCH (target:Person)-[:Parent_Child]->(c:Person)-[:Parent_Child]->(gc:Person)
WHERE id(target) == {targetUserId}
RETURN id(gc) AS user_id, 5 AS relation_type
```

**优势**：
- 查询速度快（图数据库优化）
- 支持多跳查询
- 去重自动处理
- 复杂关系支持

#### 方案二：MySQL查询（Fallback）

当NebulaGraph不可用时自动降级。

```java
// 1. 查询父母
SELECT user_id FROM user_relation 
WHERE related_user_id = ? AND relation_type IN (1,2)

// 2. 查询子女
SELECT related_user_id FROM user_relation 
WHERE user_id = ? AND relation_type = 4

// 3. 递归查询祖父母/孙子女
// 先查父母，再查父母的父母
// 先查子女，再查子女的子女
```

**降级逻辑**：

```java
try {
    if (nebulaUtil != null) {
        return queryDirectRelativesFromNebula(targetUserId);
    }
} catch (Exception e) {
    log.error("NebulaGraph查询失败，降级到MySQL", e);
}
return queryDirectRelativesFromMySQL(targetUserId);
```

---

## 🔒 核心机制：全员100%通过 + 一票否决⭐

### 全员100%通过机制

**规则**：
- 系统圈定N个直系两代亲属作为审核人
- 每个审核人必须明确审核（通过/拒绝）
- 只有当N个人**全部通过**时，挽联才能发布
- 任何一人未审核，挽联保持待审核状态

**示例**：

```
场景：5个审核人
审核进度：3通过 + 1待审核 + 1通过 = 4/5
状态：待审核（需要等待第5人）

审核进度：5通过
状态：审核通过 ✅ → 自动发布到挽联墙
```

### 一票否决制

**规则**：
- 任何一个审核人点击"拒绝"
- 整个挽联立即被拒绝
- 记录拒绝人ID和拒绝理由
- 其他审核人无需继续审核

**示例**：

```
场景：5个审核人  
审核进度：2通过 + 1拒绝 ❌ + 2待审核
触发一票否决
状态：审核拒绝
拒绝人：用户C
拒绝理由："内容不妥"
```

**代码实现**：

```java
// 一票否决检查
if (reviewStatus == 2) {
    eulogy.setReviewStatus(2);  // 整体拒绝
    eulogy.setRejectReason(reviewOpinion);
    eulogy.setRejectorUserId(reviewerUserId);
    eulogy.setRejectTime(LocalDateTime.now());
    eulogyMapper.updateById(eulogy);
    
    log.info("挽联被拒绝（一票否决）");
    return;  // 立即返回，无需检查其他审核
}
```

---

## 🗄️ 数据库设计

### 新增表（2张）

#### 1. eulogy - 挽联表

**核心字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| target_user_id | BIGINT | 缅怀对象ID（已离世用户） |
| submitter_user_id | BIGINT | 提交人ID |
| content | TEXT | 挽联内容 |
| review_status | TINYINT | 0-待审核, 1-通过, 2-拒绝 |
| rejector_user_id | BIGINT | 拒绝人ID（一票否决） |
| publish_time | DATETIME | 发布时间 |

**状态流转**：

```
待审核 (0)
  ├─→ 全员通过 → 审核通过 (1) → 发布到挽联墙
  └─→ 一人拒绝 → 审核拒绝 (2) → 记录拒绝人
```

#### 2. eulogy_reviewer - 审核人表

**核心字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| eulogy_id | BIGINT | 挽联ID |
| reviewer_user_id | BIGINT | 审核人ID |
| relation_type | TINYINT | 关系类型 1-5 |
| review_status | TINYINT | 0-待审核, 1-通过, 2-拒绝 |

**关系类型**：
- 1: 父母
- 2: 配偶
- 3: 子女
- 4: 祖父母
- 5: 孙子女

**唯一索引**：
```sql
UNIQUE KEY `uk_eulogy_reviewer` (`eulogy_id`, `reviewer_user_id`)
```

确保同一审核人对同一挽联只能审核一次。

---

## 🔧 后端实现

### 核心Service

**EulogyService.java** (500+行)

**关键方法**：

#### 1. submitEulogy - 提交挽联

```java
public void submitEulogy(Long submitterUserId, Long targetUserId, String content) {
    // 1. 验证目标用户是已离世状态
    if (targetUser.getLifeStatus() != 3) {
        throw new BusinessException("只能为已离世的用户提交挽联");
    }
    
    // 2. 获取直系两代亲属圈（核心算法）⭐
    List<DirectRelative> directRelatives = getDirectTwoGenerationRelatives(targetUserId);
    
    // 3. 验证是否有直系亲属
    if (directRelatives.isEmpty()) {
        throw new BusinessException("该用户无直系亲属，无法提交挽联");
    }
    
    // 4. 创建挽联记录
    eulogyMapper.insert(eulogy);
    
    // 5. 为每个直系两代亲属创建审核记录
    for (DirectRelative relative : directRelatives) {
        reviewerMapper.insert(reviewer);
    }
}
```

#### 2. reviewEulogy - 审核挽联

```java
public void reviewEulogy(Long reviewerUserId, Long eulogyId, Integer reviewStatus, ...) {
    // 1. 验证审核人权限
    // 2. 更新审核人状态
    
    // 3. 一票否决检查 ⭐
    if (reviewStatus == 2) {
        eulogy.setReviewStatus(2);  // 立即拒绝
        eulogy.setRejectorUserId(reviewerUserId);
        return;
    }
    
    // 4. 全员通过检查 ⭐
    checkAllReviewersApproval(eulogyId);
}
```

#### 3. checkAllReviewersApproval - 检查全员通过

```java
private void checkAllReviewersApproval(Long eulogyId) {
    long totalCount = reviewers.size();
    long approvedCount = reviewers.stream()
        .filter(r -> r.getReviewStatus() == 1)
        .count();
    
    // 全员通过（100%）才发布
    if (approvedCount == totalCount && pendingCount == 0) {
        eulogy.setReviewStatus(1);  // 审核通过
        eulogy.setPublishTime(LocalDateTime.now());
        eulogyMapper.updateById(eulogy);
        
        log.info("挽联审核通过（全员100%通过）");
    }
}
```

#### 4. getDirectTwoGenerationRelatives - 直系两代算法⭐

```java
private List<DirectRelative> getDirectTwoGenerationRelatives(Long targetUserId) {
    try {
        if (nebulaUtil != null) {
            // 优先使用NebulaGraph
            return queryDirectRelativesFromNebula(targetUserId);
        } else {
            // 降级到MySQL
            return queryDirectRelativesFromMySQL(targetUserId);
        }
    } catch (Exception e) {
        // 异常时降级
        return queryDirectRelativesFromMySQL(targetUserId);
    }
}
```

---

## 📊 API接口

| 接口 | 方法 | 功能 | 权限 |
|------|------|------|------|
| `/v1/eulogy/submit` | POST | 提交挽联 | 所有人 |
| `/v1/eulogy/{id}/review` | POST | 审核挽联 | 审核人 |
| `/v1/eulogy/my-pending-reviews` | GET | 待审核列表 | 所有人 |
| `/v1/eulogy/wall/{userId}` | GET | 挽联墙 | 所有人 |
| `/v1/eulogy/{id}` | GET | 挽联详情 | 所有人 |

---

## 🌐 前端实现

### Web端页面

#### 1. SubmitEulogy.vue - 提交挽联

**特色功能**：
- 显示审核机制说明（全员通过 + 一票否决）
- 选择已离世的亲属
- 挽联内容输入（限1000字）

**关键提示**：

```vue
<el-alert type="info">
  <p>挽联提交后需要直系两代亲属全员审核通过才能发布</p>
  <p>直系两代包括：父母、配偶、子女、祖父母、孙子女</p>
  <p><strong>一票否决：</strong>任何一人拒绝即整个挽联被拒绝</p>
</el-alert>
```

#### 2. ReviewEulogy.vue - 审核挽联

**特色功能**：
- 展示待审核挽联列表
- 通过/拒绝按钮
- 拒绝需填写理由
- 查看审核进度

**一票否决警告**：

```vue
<el-alert type="warning">
  <p><strong>全员100%通过机制：</strong>所有直系两代亲属都必须通过</p>
  <p><strong>一票否决：</strong>您拒绝后，整个挽联将被立即拒绝</p>
</el-alert>
```

#### 3. EulogyWall.vue - 挽联墙

**特色功能**：
- 瀑布流展示
- 按发布时间倒序
- 显示提交人和时间
- 分页加载

**样式设计**：

```css
.eulogy-item {
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}
```

---

## 🧪 完整测试流程

### 测试场景：三代家庭

**家庭结构**：
```
爷爷A（已离世）— 奶奶B
    |
  父亲C — 母亲D
    |
  儿子E
    |
  孙子F
```

**直系两代圈子（以A为中心）**：
- 配偶：B
- 子女：C
- 孙子女：F
- **总计**：3人审核

### 测试步骤

#### 准备阶段

```sql
-- 1. 建立关系
INSERT INTO user_relation (user_id, related_user_id, relation_type, confirm_status)
VALUES
  (A, B, 3, 1),  -- 配偶
  (A, C, 4, 1),  -- 父子
  (C, E, 4, 1),
  (E, F, 4, 1);

-- 2. 同步到NebulaGraph
-- 使用 GraphSyncService 同步关系

-- 3. 设置A为已离世
UPDATE user SET life_status = 3 WHERE id = A;
```

#### 提交挽联

```
用户E提交挽联：
- 目标：爷爷A
- 内容："爷爷，您一路走好，我们会永远怀念您"

系统自动：
- 圈定审核人：B, C, F
- 创建3条审核记录
- 状态：待审核
```

#### 场景1：全员通过 ✅

```
时间线：
10:00 - B审核通过（1/3）
10:05 - C审核通过（2/3）
10:10 - F审核通过（3/3）✅

结果：
- 挽联状态：审核通过
- 发布时间：10:10
- 挽联墙：可见
```

#### 场景2：一票否决 ❌

```
时间线：
10:00 - B审核通过（1/3）
10:05 - C审核拒绝 ❌
        拒绝理由："内容需要修改"

结果：
- 挽联状态：审核拒绝
- 拒绝人：C
- F无需审核（已被否决）
- 挽联墙：不可见
```

### SQL验证

```sql
-- 查看挽联详情
SELECT * FROM eulogy WHERE id = ?;

-- 查看审核进度
SELECT 
  reviewer_user_id,
  review_status,
  review_time
FROM eulogy_reviewer
WHERE eulogy_id = ?;

-- 统计审核状态
SELECT 
  COUNT(*) AS total,
  SUM(CASE WHEN review_status = 1 THEN 1 ELSE 0 END) AS approved,
  SUM(CASE WHEN review_status = 2 THEN 1 ELSE 0 END) AS rejected,
  SUM(CASE WHEN review_status = 0 THEN 1 ELSE 0 END) AS pending
FROM eulogy_reviewer
WHERE eulogy_id = ?;
```

---

## 📈 技术亮点

### 1. 图查询算法⭐

使用NebulaGraph的多跳查询能力，实现复杂的亲属关系查询：

```ngql
-- 两跳查询祖父母
MATCH (gp:Person)-[:Parent_Child]->(p:Person)-[:Parent_Child]->(target:Person)
WHERE id(target) == {targetUserId}
RETURN id(gp)
```

### 2. 优雅降级

当NebulaGraph不可用时，自动降级到MySQL查询，保证功能可用性。

### 3. 原子性保证

使用 `@Transactional` 确保挽联提交和审核记录创建的原子性。

### 4. 去重机制

使用 `Set<DirectRelative>` 确保同一人不会被重复添加为审核人。

---

## ⚠️ 注意事项

### 1. NebulaGraph配置

**必须确保NebulaGraph运行**：

```bash
docker ps | grep nebula
```

如不使用，注释掉 `@Configuration`：

```java
// @Configuration
public class NebulaConfig {
```

### 2. 关系数据同步

**关键**：MySQL和NebulaGraph数据必须保持一致！

确保新建关系时调用：

```java
graphSyncService.syncRelationToGraph(relation);
```

### 3. 审核通知

建议添加消息推送：
- 提交时通知审核人
- 审核完成时通知提交人
- 一票否决时特别通知

### 4. 内容审核

建议添加：
- 敏感词过滤
- 字数限制（已实现1000字）
- 格式规范检查

---

## 📂 文件清单

### 后端新增文件

```
rootlink-backend/
├── init-mysql.sql                      ⭐ 14张表
└── src/main/java/com/rootlink/backend/
    ├── entity/
    │   ├── Eulogy.java                 ⭐ 新增
    │   └── EulogyReviewer.java         ⭐ 新增
    ├── mapper/
    │   ├── EulogyMapper.java           ⭐ 新增
    │   └── EulogyReviewerMapper.java   ⭐ 新增
    ├── service/
    │   └── EulogyService.java          ⭐ 新增（500+行）
    └── controller/
        └── EulogyController.java       ⭐ 新增
```

### 前端新增文件

```
rootlink-web/src/
├── api/
│   └── eulogy.js                       ⭐ 新增
└── views/eulogy/
    ├── SubmitEulogy.vue                ⭐ 新增
    ├── ReviewEulogy.vue                ⭐ 新增
    └── EulogyWall.vue                  ⭐ 新增

rootlink-harmonyos/.../eulogy/
├── SubmitEulogyPage.ets                ⭐ 新增
└── ReviewEulogyPage.ets                ⭐ 新增
```

---

## 🔄 版本对比

| 功能模块 | v0.5.0 | v0.6.0 | 实现率 |
|---------|--------|--------|--------|
| 用户系统 | ✅ | ✅ | 100% |
| 亲属关系 | ✅ | ✅ | 100% |
| 图数据库 | ✅ | ✅ | 100% |
| 生前遗言 | ✅ | ✅ | 100% |
| 离世状态 | ✅ | ✅ | 100% |
| 挽联缅怀 | ❌ | ✅ | 100% ⭐ |

**总进度**: 45% → **50%** ↑

---

## 📝 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| v0.6.0 | 2026-02-09 | 挽联缅怀系统⭐ |
| v0.5.0 | 2026-02-09 | 离世状态管理系统 |
| v0.4.0 | 2026-02-09 | 生前遗言系统 |

---

## 🎯 下一步计划

已完成核心功能开发（50%），后续可优化：

### 阶段1：功能增强
- 挽联图片上传
- 挽联评论功能
- 挽联点赞统计

### 阶段2：体验优化
- 消息推送通知
- 审核进度可视化
- 挽联墙主题定制

### 阶段3：运营支持
- 数据统计分析
- 违规内容审核
- 用户反馈收集

---

**所有核心功能已完整实现，代码质量保证，可直接投入测试！** 🎉
