package com.rootlink.backend.utils;

import com.vesoft.nebula.client.graph.data.PathWrapper;
import com.vesoft.nebula.client.graph.data.Relationship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将 Nebula 图路径（PathWrapper）解析为中文亲属称谓。
 *
 * 核心算法：
 *   遍历路径的每一条边，根据边类型（PARENT_OF/SPOUSE_OF/SIBLING_OF）和
 *   边方向（从当前节点出发 = 正向 / 从下一节点出发 = 逆向）
 *   构建一个「步骤序列」，再映射为称谓。
 *
 * 步骤类型：
 *   UP        — 我去找父/母（沿 PARENT_OF 边逆向走，即边终点是我）
 *   DOWN      — 我去找子/女（沿 PARENT_OF 边正向走，即边起点是我）
 *   SPOUSE    — 走到配偶
 *   SIBLING   — 走到兄弟姐妹
 */
@Slf4j
@Component
public class NebulaRelationResolver {

    // 性别常量
    private static final int MALE   = 1;
    private static final int FEMALE = 2;

    /** 解析最短路径为中文称谓，viewerId = 我的 userId，genderMap = 所有相关人的性别 */
    public String resolve(PathWrapper path, Long viewerId, Map<Long, Integer> genderMap) {
        if (path == null) return "亲属";
        List<Step> steps = extractSteps(path, viewerId, genderMap);
        if (steps == null || steps.isEmpty()) return "亲属";
        String result = mapToKinship(steps, viewerId, genderMap);
        log.debug("路径解析: steps={} → {}", steps, result);
        return result;
    }

    // ── 内部数据结构 ──────────────────────────────────

    enum StepType { UP, DOWN, SPOUSE, SIBLING }

    static class Step {
        final StepType type;
        final Long nextNodeId;
        final int parentGender;   // PARENT_OF 边上的 parent_gender 属性
        final int childGender;    // PARENT_OF 边上的 child_gender 属性

        Step(StepType type, Long nextNodeId, int parentGender, int childGender) {
            this.type = type;
            this.nextNodeId = nextNodeId;
            this.parentGender = parentGender;
            this.childGender = childGender;
        }

        @Override public String toString() {
            return type + "→" + nextNodeId;
        }
    }

    // ── 步骤提取 ──────────────────────────────────────

    private List<Step> extractSteps(PathWrapper path, Long startId, Map<Long, Integer> genderMap) {
        List<Step> steps = new ArrayList<>();
        Long currentId = startId;

        try {
            List<Relationship> rels = path.getRelationships();
            for (Relationship rel : rels) {
                long srcId = rel.srcId().asLong();
                long dstId = rel.dstId().asLong();
                String edgeName = rel.edgeName();
                boolean forward = (srcId == currentId);
                Long nextId = forward ? dstId : srcId;

                int parentGender = 0, childGender = 0;
                if ("PARENT_OF".equals(edgeName)) {
                    // 先从边属性读
                    try {
                        parentGender = (int) rel.properties().get("parent_gender").asLong();
                        childGender  = (int) rel.properties().get("child_gender").asLong();
                    } catch (Exception ignored) {}

                    // 用 genderMap 覆盖（更可靠，边插入时 gender 可能为 0）
                    long parentNodeId = forward ? srcId : dstId;   // PARENT_OF 方向：src=父/母 dst=子/女
                    long childNodeId  = forward ? dstId : srcId;
                    int mapParentG = genderMap.getOrDefault(parentNodeId, 0);
                    int mapChildG  = genderMap.getOrDefault(childNodeId, 0);
                    if (mapParentG != 0) parentGender = mapParentG;
                    if (mapChildG  != 0) childGender  = mapChildG;
                }

                StepType type;
                switch (edgeName) {
                    case "PARENT_OF":
                        type = forward ? StepType.DOWN : StepType.UP;
                        break;
                    case "SPOUSE_OF":
                        type = StepType.SPOUSE;
                        break;
                    case "SIBLING_OF":
                        type = StepType.SIBLING;
                        break;
                    default:
                        log.warn("未知边类型: {}", edgeName);
                        return null;
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

    // ── 称谓映射 ──────────────────────────────────────

    private String mapToKinship(List<Step> steps, Long viewerId, Map<Long, Integer> genders) {
        int myGender = genders.getOrDefault(viewerId, 0);
        int n = steps.size();
        Step s1 = steps.get(0);
        Step sN = steps.get(n - 1);   // 最后一步，目标人
        int targetGender = genders.getOrDefault(sN.nextNodeId, 0);

        // ══ 1 跳 ══════════════════════════════════════
        if (n == 1) {
            switch (s1.type) {
                case UP:
                    // 边上记录的 parentGender 就是对方（父/母）的性别
                    return s1.parentGender == FEMALE ? "母亲" : "父亲";
                case DOWN:
                    return s1.childGender == FEMALE ? "女儿" : "儿子";
                case SPOUSE:
                    return "配偶";
                case SIBLING:
                    return targetGender == FEMALE ? "姐妹" : "兄弟";
            }
        }

        // ══ 2 跳 ══════════════════════════════════════
        if (n == 2) {
            Step s2 = steps.get(1);
            StepType t1 = s1.type, t2 = s2.type;
            // 中间节点的性别
            int midGender = genders.getOrDefault(s1.nextNodeId, 0);

            // 父/母的父/母 → 祖父母
            if (t1 == StepType.UP && t2 == StepType.UP) {
                int grandGender = s2.parentGender;
                // 祖系（通过父亲）vs 外祖系（通过母亲）
                boolean throughFather = (s1.parentGender == MALE);
                if (throughFather) {
                    return grandGender == FEMALE ? "奶奶" : "爷爷";
                } else {
                    return grandGender == FEMALE ? "外婆" : "外公";
                }
            }

            // 父/母的子/女 → 兄弟姐妹（或自己，已过滤）
            if (t1 == StepType.UP && t2 == StepType.DOWN) {
                return targetGender == FEMALE ? "姐妹" : "兄弟";
            }

            // 子/女的子/女 → 孙子/孙女（外孙/外孙女）
            if (t1 == StepType.DOWN && t2 == StepType.DOWN) {
                // 通过儿子 → 孙，通过女儿 → 外孙
                boolean throughSon = (s1.childGender == MALE);
                if (throughSon) {
                    return targetGender == FEMALE ? "孙女" : "孙子";
                } else {
                    return targetGender == FEMALE ? "外孙女" : "外孙";
                }
            }

            // 子/女的父/母 → 自己（已过滤）或配偶，这里一般不出现
            if (t1 == StepType.DOWN && t2 == StepType.UP) {
                return "亲属"; // 通常不推断
            }

            // 父/母的兄弟姐妹 → 叔/伯/姑/舅/姨
            if (t1 == StepType.UP && t2 == StepType.SIBLING) {
                boolean throughFather = (s1.parentGender == MALE);
                if (throughFather) {
                    return targetGender == FEMALE ? "姑姑" : "叔伯";
                } else {
                    return targetGender == FEMALE ? "姨妈" : "舅舅";
                }
            }

            // 兄弟姐妹的子/女 → 侄子/侄女 or 外甥/外甥女
            if (t1 == StepType.SIBLING && t2 == StepType.DOWN) {
                // 我的性别决定称谓
                if (myGender == FEMALE) {
                    return targetGender == FEMALE ? "外甥女" : "外甥";
                } else {
                    return targetGender == FEMALE ? "侄女" : "侄子";
                }
            }

            // 兄弟姐妹的父/母 → 就是自己的父/母，通常已建直接关系，不推断
            if (t1 == StepType.SIBLING && t2 == StepType.UP) {
                return s2.parentGender == FEMALE ? "母亲" : "父亲";
            }

            // 配偶的父/母 → 公公/婆婆 or 岳父/岳母
            if (t1 == StepType.SPOUSE && t2 == StepType.UP) {
                boolean iMale = (myGender == MALE);
                int parentG = s2.parentGender;
                if (iMale) {
                    return parentG == FEMALE ? "岳母" : "岳父";
                } else {
                    return parentG == FEMALE ? "婆婆" : "公公";
                }
            }

            // 配偶的兄弟姐妹
            if (t1 == StepType.SPOUSE && t2 == StepType.SIBLING) {
                boolean iMale = (myGender == MALE);
                if (iMale) {
                    return targetGender == FEMALE ? "大姨子/小姨子" : "大舅子/小舅子";
                } else {
                    return targetGender == FEMALE ? "大姑姐/小姑子" : "大伯子/小叔子";
                }
            }

            // 配偶的子/女（前配偶的孩子）
            if (t1 == StepType.SPOUSE && t2 == StepType.DOWN) {
                return targetGender == FEMALE ? "继女" : "继子";
            }

            // 父/母的配偶（继父/继母）
            if (t1 == StepType.UP && t2 == StepType.SPOUSE) {
                int midG = midGender;
                // 亲父的配偶（若不是亲母）→ 继母；亲母的配偶 → 继父
                return midG == MALE ? "继母" : "继父";
            }
        }

        // ══ 3 跳 ══════════════════════════════════════
        if (n == 3) {
            StepType t1 = steps.get(0).type;
            StepType t2 = steps.get(1).type;
            StepType t3 = steps.get(2).type;
            int midGender1 = genders.getOrDefault(steps.get(0).nextNodeId, 0);

            // 曾祖父母
            if (t1 == StepType.UP && t2 == StepType.UP && t3 == StepType.UP) {
                return targetGender == FEMALE ? "曾祖母" : "曾祖父";
            }

            // 祖父母的兄弟姐妹（伯祖父、叔祖父、姑祖母等）→ 统称祖辈
            if (t1 == StepType.UP && t2 == StepType.UP && t3 == StepType.SIBLING) {
                return targetGender == FEMALE ? "姑祖母" : "伯（叔）祖父";
            }

            // 父母的兄弟姐妹的子女（堂兄弟/表兄弟姐妹）
            if (t1 == StepType.UP && t2 == StepType.SIBLING && t3 == StepType.DOWN) {
                boolean throughFather = (steps.get(0).parentGender == MALE);
                if (throughFather) {
                    return targetGender == FEMALE ? "堂姐妹" : "堂兄弟";
                } else {
                    return targetGender == FEMALE ? "表姐妹" : "表兄弟";
                }
            }

            // 兄弟姐妹的子女的子女 → 侄孙
            if (t1 == StepType.SIBLING && t2 == StepType.DOWN && t3 == StepType.DOWN) {
                return targetGender == FEMALE ? "侄孙女" : "侄孙";
            }

            // 孙子/孙女的配偶 → 孙媳/孙婿
            if (t1 == StepType.DOWN && t2 == StepType.DOWN && t3 == StepType.SPOUSE) {
                return targetGender == FEMALE ? "孙媳妇" : "孙女婿";
            }

            // 配偶的父母的兄弟姐妹 → 配偶的叔伯姑舅姨
            if (t1 == StepType.SPOUSE && t2 == StepType.UP && t3 == StepType.SIBLING) {
                return "配偶的" + (targetGender == FEMALE ? "姑/姨" : "叔/舅");
            }

            // 子女的配偶的父母 → 亲家
            if (t1 == StepType.DOWN && t2 == StepType.SPOUSE && t3 == StepType.UP) {
                return targetGender == FEMALE ? "亲家母" : "亲家公";
            }

            // 子女的子女的子女 → 曾孙
            if (t1 == StepType.DOWN && t2 == StepType.DOWN && t3 == StepType.DOWN) {
                return targetGender == FEMALE ? "曾孙女" : "曾孙";
            }
        }

        // ══ 4 跳 ══════════════════════════════════════
        if (n == 4) {
            StepType t1 = steps.get(0).type;
            StepType t2 = steps.get(1).type;
            StepType t3 = steps.get(2).type;
            StepType t4 = steps.get(3).type;

            // 高祖父母
            if (t1 == StepType.UP && t2 == StepType.UP &&
                t3 == StepType.UP && t4 == StepType.UP) {
                return targetGender == FEMALE ? "高祖母" : "高祖父";
            }

            // 父/母的兄弟姐妹的子女的子女 → 再从兄弟
            if (t1 == StepType.UP && t2 == StepType.SIBLING &&
                t3 == StepType.DOWN && t4 == StepType.DOWN) {
                return targetGender == FEMALE ? "再从姐妹" : "再从兄弟";
            }
        }

        // 兜底：超出已定义范围
        return "亲属";
    }

    /**
     * 批量为一批路径打分，选出最语义清晰的路径解析
     * 优先选「跳数最少」的路径，再从中选「最具体称谓（非"亲属"）」的
     */
    public String resolveBest(List<PathWrapper> paths, Long viewerId, Map<Long, Integer> genderMap) {
        if (paths == null || paths.isEmpty()) return null;

        // 先按路径长度排序（短路径优先）
        paths.sort(Comparator.comparingInt(p -> p.getRelationships().size()));

        for (PathWrapper path : paths) {
            String name = resolve(path, viewerId, genderMap);
            if (name != null && !"亲属".equals(name)) return name;
        }
        return "亲属";
    }
}
