package com.rootlink.backend.utils;

import com.vesoft.nebula.client.graph.data.PathWrapper;
import com.vesoft.nebula.client.graph.data.Relationship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将 Nebula 图路径（PathWrapper）解析为中文亲属称谓。
 *
 * ─────────────────────────────────────────────────────
 * v0.8.3 修复：extractSteps 中 parentNodeId / childNodeId
 * PARENT_OF 边定义永远是 src=父/母，dst=子/女，
 * 与遍历方向无关。之前用 forward 选取导致 UP 步骤取了
 * 孩子性别 → 母亲推断成父亲的 BUG 已修正。
 * ─────────────────────────────────────────────────────
 */
@Slf4j
@Component
public class NebulaRelationResolver {

    private static final int MALE   = 1;
    private static final int FEMALE = 2;

    public String resolve(PathWrapper path, Long viewerId, Map<Long, Integer> genderMap) {
        if (path == null) return "亲属";
        List<Step> steps = extractSteps(path, viewerId, genderMap);
        if (steps == null || steps.isEmpty()) return "亲属";
        String result = mapToKinship(steps, viewerId, genderMap);
        log.debug("[称谓解析] viewer={} steps={} → {}", viewerId, steps, result);
        return result;
    }

    public String resolveBest(List<PathWrapper> paths, Long viewerId, Map<Long, Integer> genderMap) {
        if (paths == null || paths.isEmpty()) return null;
        paths.sort(Comparator.comparingInt(p -> p.getRelationships().size()));
        for (PathWrapper path : paths) {
            String name = resolve(path, viewerId, genderMap);
            if (name != null && !"亲属".equals(name)) return name;
        }
        return "亲属";
    }

    // ══ 内部结构 ══════════════════════════════════════════

    enum StepType { UP, DOWN, SPOUSE, SIBLING }

    static class Step {
        final StepType type;
        final Long     nextNodeId;
        final int      parentGender;  // PARENT_OF 中 parent 节点性别（srcId）
        final int      childGender;   // PARENT_OF 中 child  节点性别（dstId）

        Step(StepType type, Long nextNodeId, int parentGender, int childGender) {
            this.type = type; this.nextNodeId = nextNodeId;
            this.parentGender = parentGender; this.childGender = childGender;
        }
        @Override public String toString() { return type + "→" + nextNodeId; }
    }

    // ══ 步骤提取（核心修复在此）══════════════════════════════

    private List<Step> extractSteps(PathWrapper path, Long startId, Map<Long, Integer> genderMap) {
        List<Step> steps = new ArrayList<>();
        Long currentId = startId;
        try {
            for (Relationship rel : path.getRelationships()) {
                long srcId  = rel.srcId().asLong();
                long dstId  = rel.dstId().asLong();
                String edge = rel.edgeName();
                boolean fwd = (srcId == currentId);
                Long nextId = fwd ? dstId : srcId;

                int parentGender = 0, childGender = 0;

                if ("PARENT_OF".equals(edge)) {
                    // ───────────────────────────────────────────────────────
                    // 修复关键：PARENT_OF 的 src=父/母，dst=子/女，永远如此。
                    // 不能用 fwd 来决定——fwd 只是「遍历方向」，不改变边语义。
                    // ───────────────────────────────────────────────────────
                    long parentNodeId = srcId;   // 始终是父/母
                    long childNodeId  = dstId;   // 始终是子/女

                    try {
                        parentGender = (int) rel.properties().get("parent_gender").asLong();
                        childGender  = (int) rel.properties().get("child_gender").asLong();
                    } catch (Exception ignored) {}

                    // genderMap 覆盖（最准，来自 UserProfile）
                    int mapPG = genderMap.getOrDefault(parentNodeId, 0);
                    int mapCG = genderMap.getOrDefault(childNodeId,  0);
                    if (mapPG != 0) parentGender = mapPG;
                    if (mapCG != 0) childGender  = mapCG;
                }

                StepType type;
                switch (edge) {
                    case "PARENT_OF":
                        // fwd=true  → 当前在父/母，走向子/女 → DOWN
                        // fwd=false → 当前在子/女，走向父/母 → UP
                        type = fwd ? StepType.DOWN : StepType.UP;
                        break;
                    case "SPOUSE_OF":  type = StepType.SPOUSE;  break;
                    case "SIBLING_OF": type = StepType.SIBLING; break;
                    default: log.warn("未知边类型: {}", edge); return null;
                }
                steps.add(new Step(type, nextId, parentGender, childGender));
                currentId = nextId;
            }
        } catch (Exception e) {
            log.error("路径步骤提取失败: {}", e.getMessage());
            return null;
        }
        return steps;
    }

    // ══ 称谓映射 ══════════════════════════════════════════

    private String mapToKinship(List<Step> steps, Long viewerId, Map<Long, Integer> genders) {
        int myG  = genders.getOrDefault(viewerId, 0);
        int n    = steps.size();
        Step s1  = steps.get(0);
        int targG = genders.getOrDefault(steps.get(n - 1).nextNodeId, 0);

        // ── 1 跳 ──────────────────────────────────────────────
        if (n == 1) {
            switch (s1.type) {
                case UP:
                    // parentGender 现已正确来自 genderMap[srcId]（父/母节点）
                    return s1.parentGender == FEMALE ? "母亲" : "父亲";
                case DOWN:
                    return s1.childGender == FEMALE ? "女儿" : "儿子";
                case SPOUSE:
                    return "配偶";
                case SIBLING:
                    return targG == FEMALE ? "姐妹" : "兄弟";
            }
        }

        // ── 2 跳 ──────────────────────────────────────────────
        if (n == 2) {
            Step s2 = steps.get(1);
            StepType t1 = s1.type, t2 = s2.type;
            int midG = genders.getOrDefault(s1.nextNodeId, 0);

            // ① 祖父母 / 外祖父母
            if (t1 == StepType.UP && t2 == StepType.UP) {
                boolean viaFather = s1.parentGender == MALE
                        || (s1.parentGender == 0 && midG == MALE);
                int grandG = s2.parentGender != 0 ? s2.parentGender : targG;
                return viaFather
                        ? (grandG == FEMALE ? "奶奶" : "爷爷")
                        : (grandG == FEMALE ? "外婆" : "外公");
            }

            // ② 兄弟姐妹
            if (t1 == StepType.UP && t2 == StepType.DOWN) {
                return targG == FEMALE ? "姐妹" : "兄弟";
            }

            // ③ 孙子女 / 外孙子女
            if (t1 == StepType.DOWN && t2 == StepType.DOWN) {
                boolean viaSon = s1.childGender == MALE
                        || (s1.childGender == 0 && midG == MALE);
                return viaSon
                        ? (targG == FEMALE ? "孙女" : "孙子")
                        : (targG == FEMALE ? "外孙女" : "外孙");
            }

            // ④ 叔/伯/姑/舅/姨
            if (t1 == StepType.UP && t2 == StepType.SIBLING) {
                boolean viaFather = s1.parentGender == MALE
                        || (s1.parentGender == 0 && midG == MALE);
                return viaFather
                        ? (targG == FEMALE ? "姑姑" : "叔伯")
                        : (targG == FEMALE ? "姨妈" : "舅舅");
            }

            // ⑤ 侄 / 甥
            if (t1 == StepType.SIBLING && t2 == StepType.DOWN) {
                return myG == FEMALE
                        ? (targG == FEMALE ? "外甥女" : "外甥")
                        : (targG == FEMALE ? "侄女" : "侄子");
            }

            // ⑥ 配偶的父母
            if (t1 == StepType.SPOUSE && t2 == StepType.UP) {
                int parentG = s2.parentGender != 0 ? s2.parentGender : targG;
                return myG == MALE
                        ? (parentG == FEMALE ? "岳母" : "岳父")
                        : (parentG == FEMALE ? "婆婆" : "公公");
            }

            // ⑦ 配偶的兄弟姐妹
            if (t1 == StepType.SPOUSE && t2 == StepType.SIBLING) {
                return myG == MALE
                        ? (targG == FEMALE ? "大姨子/小姨子" : "大舅子/小舅子")
                        : (targG == FEMALE ? "大姑姐/小姑子" : "大伯子/小叔子");
            }

            // ⑧ 配偶的子女（继子女）
            if (t1 == StepType.SPOUSE && t2 == StepType.DOWN) {
                return targG == FEMALE ? "继女" : "继子";
            }

            // ⑨ 父母的配偶（继父母）
            if (t1 == StepType.UP && t2 == StepType.SPOUSE) {
                return midG == MALE ? "继母" : "继父";
            }

            // ⑩ 儿媳 / 女婿
            if (t1 == StepType.DOWN && t2 == StepType.SPOUSE) {
                int childG = s1.childGender != 0 ? s1.childGender : midG;
                return childG == FEMALE ? "女婿" : "儿媳";
            }

            // ⑪ 兄弟姐妹的父母（通常已有直接关系，兜底）
            if (t1 == StepType.SIBLING && t2 == StepType.UP) {
                return s2.parentGender == FEMALE ? "母亲" : "父亲";
            }

            if (t1 == StepType.DOWN && t2 == StepType.UP) return "亲属";
        }

        // ── 3 跳 ──────────────────────────────────────────────
        if (n == 3) {
            StepType t1 = steps.get(0).type;
            StepType t2 = steps.get(1).type;
            StepType t3 = steps.get(2).type;
            int mid1G = genders.getOrDefault(steps.get(0).nextNodeId, 0);

            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.UP)
                return targG == FEMALE ? "曾祖母" : "曾祖父";

            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.SIBLING)
                return targG == FEMALE ? "姑祖母" : "伯祖父";

            if (t1==StepType.UP && t2==StepType.SIBLING && t3==StepType.DOWN) {
                boolean viaFather = steps.get(0).parentGender == MALE
                        || (steps.get(0).parentGender == 0 && mid1G == MALE);
                return viaFather
                        ? (targG == FEMALE ? "堂姐妹" : "堂兄弟")
                        : (targG == FEMALE ? "表姐妹" : "表兄弟");
            }

            if (t1==StepType.SIBLING && t2==StepType.DOWN && t3==StepType.DOWN)
                return targG == FEMALE ? "侄孙女" : "侄孙";

            if (t1==StepType.DOWN && t2==StepType.DOWN && t3==StepType.SPOUSE) {
                int c2G = steps.get(1).childGender != 0
                        ? steps.get(1).childGender
                        : genders.getOrDefault(steps.get(1).nextNodeId, 0);
                return c2G == FEMALE ? "孙女婿" : "孙媳妇";
            }

            if (t1==StepType.DOWN && t2==StepType.SPOUSE && t3==StepType.UP)
                return targG == FEMALE ? "亲家母" : "亲家公";

            if (t1==StepType.DOWN && t2==StepType.DOWN && t3==StepType.DOWN)
                return targG == FEMALE ? "曾孙女" : "曾孙";

            if (t1==StepType.SPOUSE && t2==StepType.UP && t3==StepType.SIBLING)
                return "配偶的" + (targG == FEMALE ? "姑/姨" : "叔/舅");

            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.DOWN)
                return targG == FEMALE ? "堂/表姐妹" : "堂/表兄弟";
        }

        // ── 4 跳 ──────────────────────────────────────────────
        if (n == 4) {
            StepType t1=steps.get(0).type, t2=steps.get(1).type,
                     t3=steps.get(2).type, t4=steps.get(3).type;

            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.UP && t4==StepType.UP)
                return targG == FEMALE ? "高祖母" : "高祖父";

            if (t1==StepType.UP && t2==StepType.SIBLING && t3==StepType.DOWN && t4==StepType.DOWN)
                return targG == FEMALE ? "再从姐妹" : "再从兄弟";

            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.SIBLING && t4==StepType.DOWN)
                return targG == FEMALE ? "堂姑" : "堂叔";
        }

        return "亲属";
    }
}
