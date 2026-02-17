package com.rootlink.backend.utils;

import com.vesoft.nebula.client.graph.data.PathWrapper;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * NebulaGraph 工具类
 *
 * Schema：
 *   TAG  Person(name string, gender int, life_status int)
 *   EDGE PARENT_OF(parent_gender int, child_gender int)   父/母→子/女
 *   EDGE SPOUSE_OF()                                       配偶（双向）
 *   EDGE SIBLING_OF()                                      兄弟姐妹（双向）
 */
@Slf4j
@Component
public class NebulaUtil {

    @Autowired(required = false)
    private NebulaPool nebulaPool;

    @Value("${nebula.graph.username:root}")
    private String username;

    @Value("${nebula.graph.password:nebula}")
    private String password;

    @Value("${nebula.graph.space:rootlink_family}")
    private String space;

    public boolean isAvailable() {
        return nebulaPool != null;
    }

    // ── Schema 初始化 ────────────────────────────────

    @PostConstruct
    public void initSchema() {
        if (!isAvailable()) return;
        try {
            // 1. 创建图空间
            executeOnMeta(String.format(
                "CREATE SPACE IF NOT EXISTS `%s`" +
                "(partition_num=1,replica_factor=1,vid_type=INT64)", space));
            
            // 2. 等待图空间就绪（最多重试 10 次，共 20 秒）
            boolean spaceReady = false;
            for (int i = 0; i < 10; i++) {
                Thread.sleep(2000);  // 每次等 2 秒
                try {
                    // 尝试切换图空间，成功则跳出循环
                    doExecute("USE " + space);
                    spaceReady = true;
                    log.info("图空间 {} 已就绪（等待 {}s）", space, (i+1)*2);
                    break;
                } catch (Exception e) {
                    if (i == 9) {
                        // 最后一次还是失败，抛出异常
                        throw new RuntimeException("图空间创建超时，请检查 NebulaGraph 服务状态");
                    }
                    // 否则继续等待
                }
            }
            
            if (!spaceReady) {
                throw new RuntimeException("图空间未就绪，Schema 初始化中止");
            }

            // 3. 创建 TAG 和 EDGE（此时图空间已就绪）
            execute("CREATE TAG IF NOT EXISTS Person(name string,gender int,life_status int)");
            execute("CREATE EDGE IF NOT EXISTS PARENT_OF(parent_gender int,child_gender int)");
            execute("CREATE EDGE IF NOT EXISTS SPOUSE_OF()");
            execute("CREATE EDGE IF NOT EXISTS SIBLING_OF()");
            Thread.sleep(1000);
            log.info("NebulaGraph Schema 初始化完成");
        } catch (Exception e) {
            log.error("NebulaGraph Schema 初始化失败：{}", e.getMessage(), e);
        }
    }

    // ── 基础执行 ─────────────────────────────────────

    public ResultSet executeQuery(String nGQL) {
        return doExecute(nGQL);
    }

    public void execute(String nGQL) {
        doExecute(nGQL);
    }

    private ResultSet doExecute(String nGQL) {
        if (!isAvailable()) throw new RuntimeException("NebulaGraph 未启用");
        Session session = null;
        try {
            session = nebulaPool.getSession(username, password, true);
            ResultSet useRs = session.execute("USE `" + space + "`");
            if (!useRs.isSucceeded())
                throw new RuntimeException("切换图空间失败: " + useRs.getErrorMessage());
            ResultSet rs = session.execute(nGQL);
            if (!rs.isSucceeded())
                log.warn("nGQL 执行警告: {} | 错误: {}", nGQL, rs.getErrorMessage());
            else
                log.debug("nGQL 执行成功: {}", nGQL);
            return rs;
        } catch (Exception e) {
            log.error("NebulaGraph 执行异常 nGQL={}", nGQL, e);
            throw new RuntimeException("图数据库操作失败: " + e.getMessage(), e);
        } finally {
            if (session != null) session.release();
        }
    }

    private void executeOnMeta(String nGQL) {
        if (!isAvailable()) return;
        Session session = null;
        try {
            session = nebulaPool.getSession(username, password, false);
            session.execute(nGQL);
        } catch (Exception e) {
            log.warn("Meta 语句执行失败: {}", e.getMessage());
        } finally {
            if (session != null) session.release();
        }
    }

    // ── 顶点操作 ─────────────────────────────────────

    public void insertPerson(Long userId, String name, Integer gender) {
        if (!isAvailable()) return;
        try {
            String safeName = name == null ? "" : name.replace("'", "\\'");
            int g = gender != null ? gender : 0;
            execute(String.format(
                "INSERT VERTEX IF NOT EXISTS Person(name,gender,life_status) VALUES %d:('%s',%d,0)",
                userId, safeName, g));
            log.info("Person 顶点写入: userId={}", userId);
        } catch (Exception e) {
            log.error("写入 Person 顶点失败 userId={}: {}", userId, e.getMessage());
        }
    }

    public void updateLifeStatus(Long userId, int lifeStatus) {
        if (!isAvailable()) return;
        try {
            execute(String.format(
                "UPDATE VERTEX ON Person %d SET life_status=%d", userId, lifeStatus));
        } catch (Exception e) {
            log.error("更新 Nebula 生命状态失败 userId={}: {}", userId, e.getMessage());
        }
    }

    // ── 边操作 ───────────────────────────────────────

    /**
     * 将已确认关系同步写入图数据库。
     * chain 含义：申请人（A）描述对方（B）是"我的 xxx"
     *   ["父"] → B 是 A 的父亲 → B PARENT_OF A
     *   ["子"] → B 是 A 的儿子 → A PARENT_OF B
     *   ["配偶"] → 双向 SPOUSE_OF
     *   ["同辈",...] → 双向 SIBLING_OF
     */
    public void syncRelationToGraph(List<String> chain, Long userAId, Long userBId,
                                    Integer userAGender, Integer userBGender) {
        if (!isAvailable() || chain == null || chain.isEmpty()) return;
        try {
            String first = chain.get(0);
            int gA = userAGender != null ? userAGender : 0;
            int gB = userBGender != null ? userBGender : 0;

            if ("父".equals(first) || "母".equals(first)) {
                // B 是 A 的父/母 → B PARENT_OF A
                execute(String.format(
                    "INSERT EDGE PARENT_OF(parent_gender,child_gender) VALUES %d->%d:(%d,%d)",
                    userBId, userAId, gB, gA));

            } else if ("子".equals(first) || "女".equals(first)) {
                // B 是 A 的子/女 → A PARENT_OF B
                execute(String.format(
                    "INSERT EDGE PARENT_OF(parent_gender,child_gender) VALUES %d->%d:(%d,%d)",
                    userAId, userBId, gA, gB));

            } else if ("配偶".equals(first)) {
                execute(String.format(
                    "INSERT EDGE SPOUSE_OF() VALUES %d->%d:(),%d->%d:()",
                    userAId, userBId, userBId, userAId));

            } else if ("同辈".equals(first)) {
                execute(String.format(
                    "INSERT EDGE SIBLING_OF() VALUES %d->%d:(),%d->%d:()",
                    userAId, userBId, userBId, userAId));
            }
            log.info("关系同步到 Nebula: A={} B={} chain={}", userAId, userBId, chain);
        } catch (Exception e) {
            log.error("关系同步 Nebula 失败 A={} B={}: {}", userAId, userBId, e.getMessage());
        }
    }

    /**
     * 删除指定用户的 Person 顶点及其所有关联边（用于账号注销）
     * NebulaGraph 3.x 中删顶点时需先删边，使用 DELETE VERTEX … WITH EDGE 语法
     */
    public void deletePersonVertex(Long userId) {
        if (!isAvailable()) return;
        try {
            // WITH EDGE 会级联删除该顶点的所有入边和出边
            execute(String.format("DELETE VERTEX %d WITH EDGE", userId));
            log.info("Nebula Person 顶点删除完成: userId={}", userId);
        } catch (Exception e) {
            log.error("Nebula 删除顶点失败 userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Nebula 删除顶点失败: " + e.getMessage(), e);
        }
    }

    /** 删除两人之间所有类型的边 */
    public void removeRelationFromGraph(Long aId, Long bId) {
        if (!isAvailable()) return;
        try {
            String tpl = "DELETE EDGE %s %d->%d; DELETE EDGE %s %d->%d;";
            for (String et : new String[]{"PARENT_OF", "SPOUSE_OF", "SIBLING_OF"}) {
                execute(String.format(tpl, et, aId, bId, et, bId, aId));
            }
        } catch (Exception e) {
            log.warn("Nebula 删除边失败 A={} B={}: {}", aId, bId, e.getMessage());
        }
    }

    // ── 图遍历推断 ───────────────────────────────────

    /**
     * 查询两人之间所有路径（最多 maxSteps 跳）
     */
    public List<PathWrapper> findPaths(Long fromId, Long toId, int maxSteps) {
        if (!isAvailable()) return Collections.emptyList();
        try {
            String nGQL = String.format(
                "FIND ALL PATH FROM %d TO %d OVER * UPTO %d STEPS YIELD path AS p",
                fromId, toId, maxSteps);
            ResultSet rs = executeQuery(nGQL);
            List<PathWrapper> paths = new ArrayList<>();
            for (int i = 0; i < rs.rowsSize(); i++) {
                ValueWrapper vw = rs.rowValues(i).get("p");
                if (vw.isPath()) paths.add(vw.asPath());
            }
            return paths;
        } catch (Exception e) {
            log.error("findPaths 失败 from={} to={}: {}", fromId, toId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 查询某人 N 跳以内的所有邻居节点 ID
     */
    public Set<Long> findNeighbors(Long userId, int maxSteps) {
        if (!isAvailable()) return Collections.emptySet();
        try {
            String nGQL = String.format(
                "GO 1 TO %d STEPS FROM %d OVER * BIDIRECT YIELD id($$) AS nid",
                maxSteps, userId);
            ResultSet rs = executeQuery(nGQL);
            Set<Long> result = new HashSet<>();
            for (int i = 0; i < rs.rowsSize(); i++) {
                long nid = rs.rowValues(i).get("nid").asLong();
                if (nid != userId) result.add(nid);
            }
            return result;
        } catch (Exception e) {
            log.error("findNeighbors 失败 userId={}: {}", userId, e.getMessage());
            return Collections.emptySet();
        }
    }
}
