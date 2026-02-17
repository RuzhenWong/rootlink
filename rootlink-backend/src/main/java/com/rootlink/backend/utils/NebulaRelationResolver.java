package com.rootlink.backend.utils;

import com.vesoft.nebula.client.graph.data.PathWrapper;
import com.vesoft.nebula.client.graph.data.Relationship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将 Nebula 图路径（PathWrapper）解析为中文亲属称谓。
 *
 * SIBLING_OF 边属性 seniority：
 *   在 src→dst 边上：1=src比dst年长(src是哥/姐), 2=src比dst年幼(src是弟/妹), 0=未知
 *
 * PARENT_OF 边：src=父/母，dst=子/女（永远如此，与遍历方向无关）
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
        final int      parentGender;   // PARENT_OF: parentNode 的性别
        final int      childGender;    // PARENT_OF: childNode 的性别
        // SIBLING_OF: nextNode 相对 currentNode 的年龄关系
        //   1=nextNode比currentNode年长(nextNode是哥/姐)
        //   2=nextNode比currentNode年幼(nextNode是弟/妹)
        //   0=未知
        final int      nextOlderThan;  

        Step(StepType type, Long nextNodeId, int parentGender, int childGender, int nextOlderThan) {
            this.type = type; this.nextNodeId = nextNodeId;
            this.parentGender = parentGender; this.childGender = childGender;
            this.nextOlderThan = nextOlderThan;
        }
        @Override public String toString() { return type + "→" + nextNodeId + "(pg=" + parentGender + ",cg=" + childGender + ",seniority=" + nextOlderThan + ")"; }
    }

    // ══ 步骤提取 ══════════════════════════════════════════

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

                int parentGender = 0, childGender = 0, nextOlderThan = 0;

                if ("PARENT_OF".equals(edge)) {
                    // PARENT_OF 的 src 永远是父/母，dst 永远是子/女
                    long parentNodeId = srcId;
                    long childNodeId  = dstId;
                    try {
                        parentGender = (int) rel.properties().get("parent_gender").asLong();
                        childGender  = (int) rel.properties().get("child_gender").asLong();
                    } catch (Exception ignored) {}
                    // genderMap 覆盖（来自 UserProfile，最准）
                    int mapPG = genderMap.getOrDefault(parentNodeId, 0);
                    int mapCG = genderMap.getOrDefault(childNodeId,  0);
                    if (mapPG != 0) parentGender = mapPG;
                    if (mapCG != 0) childGender  = mapCG;

                } else if ("SIBLING_OF".equals(edge)) {
                    // seniority: 在 src→dst 边上，1=src比dst年长，2=src比dst年幼，0=未知
                    int seniority = 0;
                    try { seniority = (int) rel.properties().get("seniority").asLong(); } catch (Exception ignored) {}
                    if (fwd) {
                        // 当前在 src，下一步是 dst
                        // seniority=1 → src(current)年长 → dst(next)年幼 → nextOlderThan=2
                        // seniority=2 → src(current)年幼 → dst(next)年长 → nextOlderThan=1
                        nextOlderThan = seniority == 1 ? 2 : (seniority == 2 ? 1 : 0);
                    } else {
                        // 当前在 dst，下一步是 src
                        // seniority=1 → src(next)年长 → nextOlderThan=1
                        // seniority=2 → src(next)年幼 → nextOlderThan=2
                        nextOlderThan = seniority;
                    }
                }

                StepType type;
                switch (edge) {
                    case "PARENT_OF":
                        // fwd=true → 从父母走向子女 → DOWN（往下辈）
                        // fwd=false → 从子女走向父母 → UP（往上辈）
                        type = fwd ? StepType.DOWN : StepType.UP;
                        break;
                    case "SPOUSE_OF":  type = StepType.SPOUSE;  break;
                    case "SIBLING_OF": type = StepType.SIBLING; break;
                    default: log.warn("未知边类型: {}", edge); return null;
                }
                steps.add(new Step(type, nextId, parentGender, childGender, nextOlderThan));
                currentId = nextId;
            }
        } catch (Exception e) {
            log.error("路径步骤提取失败: {}", e.getMessage());
            return null;
        }
        return steps;
    }

    // ══ 称谓映射（核心逻辑）══════════════════════════════

    private String mapToKinship(List<Step> steps, Long viewerId, Map<Long, Integer> genders) {
        int myG   = genders.getOrDefault(viewerId, 0);
        int n     = steps.size();
        Step s1   = steps.get(0);
        Long targId = steps.get(n - 1).nextNodeId;
        int targG   = genders.getOrDefault(targId, 0);

        // ────────────────────────── 1 跳 ──────────────────────────
        if (n == 1) {
            switch (s1.type) {
                case UP:
                    return s1.parentGender == FEMALE ? "母亲" : "父亲";
                case DOWN:
                    return s1.childGender == FEMALE ? "女儿" : "儿子";
                case SPOUSE:
                    return myG == FEMALE ? "丈夫" : (myG == MALE ? "妻子" : "配偶");
                case SIBLING:
                    // nextOlderThan: 1=next是哥/姐, 2=next是弟/妹, 0=未知
                    if (s1.nextOlderThan == 1) {
                        return targG == FEMALE ? "姐姐" : "哥哥";
                    } else if (s1.nextOlderThan == 2) {
                        return targG == FEMALE ? "妹妹" : "弟弟";
                    } else {
                        return targG == FEMALE ? "姐/妹" : "兄/弟";
                    }
            }
        }

        // ────────────────────────── 2 跳 ──────────────────────────
        if (n == 2) {
            Step s2 = steps.get(1);
            StepType t1 = s1.type, t2 = s2.type;
            Long midId = s1.nextNodeId;
            int midG   = genders.getOrDefault(midId, 0);

            // ① 祖父母 / 外祖父母（UP+UP）
            if (t1 == StepType.UP && t2 == StepType.UP) {
                boolean viaFather = (s1.parentGender == MALE) || (s1.parentGender == 0 && midG == MALE);
                int grandG = s2.parentGender != 0 ? s2.parentGender : targG;
                return viaFather
                        ? (grandG == FEMALE ? "奶奶" : "爷爷")
                        : (grandG == FEMALE ? "外婆" : "外公");
            }

            // ② 父母辈兄弟姐妹：叔伯姑舅姨（UP+SIBLING）
            if (t1 == StepType.UP && t2 == StepType.SIBLING) {
                boolean viaFather = (s1.parentGender == MALE) || (s1.parentGender == 0 && midG == MALE);
                // s2.nextOlderThan: 1=target比父/母年长, 2=target比父/母年幼
                if (viaFather) { // 父亲的兄弟姐妹
                    if (targG == MALE) {
                        return s2.nextOlderThan == 1 ? "伯父" : (s2.nextOlderThan == 2 ? "叔叔" : "叔/伯");
                    } else if (targG == FEMALE) {
                        return "姑姑";
                    }
                    return s2.nextOlderThan == 1 ? "伯父/姑姑" : "叔叔/姑姑";
                } else { // 母亲的兄弟姐妹
                    if (targG == MALE)   return "舅舅";
                    if (targG == FEMALE) return "姨妈";
                    return "舅舅/姨妈";
                }
            }

            // ③ 兄弟姐妹（共同父母）：UP+DOWN
            if (t1 == StepType.UP && t2 == StepType.DOWN) {
                // 通过父母找到同辈
                return targG == FEMALE ? "姐/妹" : "兄/弟";
            }

            // ④ 孙子女 / 外孙子女（DOWN+DOWN）
            if (t1 == StepType.DOWN && t2 == StepType.DOWN) {
                boolean viaSon = (s1.childGender == MALE) || (s1.childGender == 0 && midG == MALE);
                return viaSon
                        ? (targG == FEMALE ? "孙女" : "孙子")
                        : (targG == FEMALE ? "外孙女" : "外孙");
            }

            // ⑤ 侄子/外甥（SIBLING+DOWN）
            if (t1 == StepType.SIBLING && t2 == StepType.DOWN) {
                // 男性的兄弟姐妹的孩子 = 侄子/侄女; 女性的 = 外甥/外甥女
                if (myG == FEMALE) {
                    return targG == FEMALE ? "外甥女" : "外甥";
                } else {
                    return targG == FEMALE ? "侄女" : "侄子";
                }
            }

            // ⑥ 配偶的父母（SPOUSE+UP）
            if (t1 == StepType.SPOUSE && t2 == StepType.UP) {
                int parentG = s2.parentGender != 0 ? s2.parentGender : targG;
                if (myG == MALE) {
                    return parentG == FEMALE ? "岳母" : "岳父";
                } else if (myG == FEMALE) {
                    return parentG == FEMALE ? "婆婆" : "公公";
                }
                return parentG == FEMALE ? "岳母/婆婆" : "岳父/公公";
            }

            // ⑦ 配偶的兄弟姐妹（SPOUSE+SIBLING）
            if (t1 == StepType.SPOUSE && t2 == StepType.SIBLING) {
                // myG=男: 配偶的兄=大舅子, 弟=小舅子, 姐=大姨子, 妹=小姨子
                // myG=女: 配偶的兄=大伯子, 弟=小叔子, 姐=大姑子, 妹=小姑子
                if (myG == MALE) {
                    if (targG == MALE) {
                        return s2.nextOlderThan == 1 ? "大舅子" : (s2.nextOlderThan == 2 ? "小舅子" : "舅子");
                    } else {
                        return s2.nextOlderThan == 1 ? "大姨子" : (s2.nextOlderThan == 2 ? "小姨子" : "姨子");
                    }
                } else if (myG == FEMALE) {
                    if (targG == MALE) {
                        return s2.nextOlderThan == 1 ? "大伯子" : (s2.nextOlderThan == 2 ? "小叔子" : "叔/伯子");
                    } else {
                        return s2.nextOlderThan == 1 ? "大姑子" : (s2.nextOlderThan == 2 ? "小姑子" : "姑子");
                    }
                }
                return targG == MALE ? "配偶的兄弟" : "配偶的姐妹";
            }

            // ⑧ 配偶的子女（继子女）（SPOUSE+DOWN）
            if (t1 == StepType.SPOUSE && t2 == StepType.DOWN) {
                return targG == FEMALE ? "继女" : "继子";
            }

            // ⑨ 父母的配偶（继父母）（UP+SPOUSE）
            if (t1 == StepType.UP && t2 == StepType.SPOUSE) {
                // midG = 父/母的性别，配偶是异性
                return midG == MALE ? "继母" : (midG == FEMALE ? "继父" : "继父/继母");
            }

            // ⑩ 儿媳 / 女婿（DOWN+SPOUSE）
            if (t1 == StepType.DOWN && t2 == StepType.SPOUSE) {
                int childG = s1.childGender != 0 ? s1.childGender : midG;
                return childG == FEMALE ? "女婿" : "儿媳";
            }

            // ⑪ 兄弟姐妹的配偶（SIBLING+SPOUSE）
            if (t1 == StepType.SIBLING && t2 == StepType.SPOUSE) {
                // s1.nextOlderThan: 1=sibling比我年长(哥/姐), 2=sibling比我年幼(弟/妹)
                boolean sibOlder = s1.nextOlderThan == 1;
                boolean sibMale  = midG == MALE || (midG == 0 && targG == FEMALE); // 配偶性别暗示
                if (sibOlder && sibMale)   return "嫂子";
                if (sibOlder && !sibMale)  return "姐夫";
                if (!sibOlder && sibMale)  return "弟媳";
                if (!sibOlder && !sibMale) return "妹夫";
                return "兄弟/姐妹的配偶";
            }

            // ⑫ 兄弟姐妹的父母（兜底，通常有直接关系）
            if (t1 == StepType.SIBLING && t2 == StepType.UP) {
                return s2.parentGender == FEMALE ? "母亲" : "父亲";
            }

            if (t1 == StepType.DOWN && t2 == StepType.UP) return "亲属";
        }

        // ────────────────────────── 3 跳 ──────────────────────────
        if (n == 3) {
            StepType t1 = steps.get(0).type;
            StepType t2 = steps.get(1).type;
            StepType t3 = steps.get(2).type;
            Long mid1Id = steps.get(0).nextNodeId;
            Long mid2Id = steps.get(1).nextNodeId;
            int  mid1G  = genders.getOrDefault(mid1Id, 0);
            int  mid2G  = genders.getOrDefault(mid2Id, 0);

            // 曾祖父母（UP+UP+UP）
            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.UP) {
                return targG == FEMALE ? "太奶奶" : "太爷爷";
            }

            // 曾孙子女（DOWN+DOWN+DOWN）
            if (t1==StepType.DOWN && t2==StepType.DOWN && t3==StepType.DOWN) {
                return targG == FEMALE ? "重孙女" : "重孙子";
            }

            // 祖辈的兄弟姐妹（UP+UP+SIBLING）→ 曾祖伯/曾祖叔/曾祖姑等
            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.SIBLING) {
                // 爷爷奶奶的兄弟姐妹
                boolean viaGrFather = (steps.get(0).parentGender == MALE) || (steps.get(0).parentGender == 0 && mid1G == MALE);
                int grandG = steps.get(1).parentGender != 0 ? steps.get(1).parentGender : mid2G;
                boolean grandIsMale = grandG == MALE || (grandG == 0 && mid2G == MALE);
                // 爷爷奶奶的兄弟姐妹统一称：伯祖父/叔祖父/姑祖母等（实际中很少用，用通俗称呼）
                if (targG == MALE)   return grandIsMale ? "叔/伯祖父" : "舅祖父";
                if (targG == FEMALE) return grandIsMale ? "姑祖母" : "姨祖母";
                return "祖父辈亲属";
            }

            // 堂/表兄弟姐妹（UP+SIBLING+DOWN）
            if (t1==StepType.UP && t2==StepType.SIBLING && t3==StepType.DOWN) {
                boolean viaFather = (steps.get(0).parentGender == MALE) || (steps.get(0).parentGender == 0 && mid1G == MALE);
                // 通过父亲的兄弟(男方) → 堂；通过父亲的姐妹/母亲的任何兄弟姐妹 → 表
                boolean isDang = viaFather && (mid2G == MALE); // 父亲的兄弟
                String prefix = isDang ? "堂" : "表";
                // steps.get(2).nextOlderThan 或 targG 确定哥/弟/姐/妹
                // 由于不知道cousin相对我的年龄，只能按性别区分
                return prefix + (targG == FEMALE ? "姐/妹" : "兄/弟");
            }

            // 侄孙（SIBLING+DOWN+DOWN）
            if (t1==StepType.SIBLING && t2==StepType.DOWN && t3==StepType.DOWN) {
                return targG == FEMALE ? "侄孙女" : "侄孙";
            }

            // 孙媳/孙女婿（DOWN+DOWN+SPOUSE）
            if (t1==StepType.DOWN && t2==StepType.DOWN && t3==StepType.SPOUSE) {
                int c2G = steps.get(1).childGender != 0 ? steps.get(1).childGender : mid2G;
                return c2G == FEMALE ? "孙女婿" : "孙媳妇";
            }

            // 亲家（DOWN+SPOUSE+UP）
            if (t1==StepType.DOWN && t2==StepType.SPOUSE && t3==StepType.UP) {
                return targG == FEMALE ? "亲家母" : "亲家公";
            }

            // 叔伯/姑/舅/姨的配偶（UP+SIBLING+SPOUSE）
            if (t1==StepType.UP && t2==StepType.SIBLING && t3==StepType.SPOUSE) {
                boolean viaFather = (steps.get(0).parentGender == MALE) || (steps.get(0).parentGender == 0 && mid1G == MALE);
                boolean uncleIsMale = mid2G == MALE;
                if (viaFather) {
                    // 父亲的兄弟/姐妹的配偶
                    boolean isOlder = steps.get(1).nextOlderThan == 1;
                    if (uncleIsMale) return isOlder ? "伯母" : "婶婶"; // 伯父的妻子=伯母, 叔叔的妻子=婶婶
                    else             return "姑父";
                } else {
                    if (uncleIsMale) return "舅妈"; // 舅舅的妻子
                    else             return "姨父"; // 姨妈的丈夫
                }
            }

            // 叔伯/姑/舅/姨的子女（UP+SIBLING+DOWN）与 堂/表 共用，已在上面处理

            // 父/母辈配偶的兄弟姐妹（UP+SPOUSE+SIBLING）
            if (t1==StepType.UP && t2==StepType.SPOUSE && t3==StepType.SIBLING) {
                return targG == FEMALE ? "继母/父的姐妹" : "继父/母的兄弟";
            }

            // UP+UP+DOWN → 叔伯/姑/舅/姨（共同祖父母辈）的子女
            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.DOWN) {
                // 爷爷/外公的子女（即我父/母辈的兄弟姐妹）
                boolean viaFather = (steps.get(0).parentGender == MALE) || (steps.get(0).parentGender == 0 && mid1G == MALE);
                int grandG = steps.get(1).parentGender != 0 ? steps.get(1).parentGender : mid2G;
                boolean grandIsMale = grandG == MALE || (grandG == 0 && mid2G == MALE);
                if (viaFather && grandIsMale) {
                    // 爷爷的子女（我的叔伯/姑）
                    return targG == FEMALE ? "姑姑" : "叔/伯";
                }
                return targG == FEMALE ? "姨妈/姑姑" : "叔/舅";
            }

            // SIBLING+SPOUSE+UP → 嫂/弟媳/姐夫/妹夫的父母
            if (t1==StepType.SIBLING && t2==StepType.SPOUSE && t3==StepType.UP) {
                return targG == FEMALE ? "亲家母" : "亲家公";
            }
        }

        // ────────────────────────── 4 跳 ──────────────────────────
        if (n == 4) {
            StepType t1=steps.get(0).type, t2=steps.get(1).type,
                     t3=steps.get(2).type, t4=steps.get(3).type;
            Long mid1Id = steps.get(0).nextNodeId;
            Long mid2Id = steps.get(1).nextNodeId;
            Long mid3Id = steps.get(2).nextNodeId;
            int  mid1G  = genders.getOrDefault(mid1Id, 0);
            int  mid2G  = genders.getOrDefault(mid2Id, 0);
            int  mid3G  = genders.getOrDefault(mid3Id, 0);

            // 高祖父母（UP×4）
            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.UP && t4==StepType.UP)
                return targG == FEMALE ? "高祖母" : "高祖父";

            // 玄孙子女（DOWN×4）
            if (t1==StepType.DOWN && t2==StepType.DOWN && t3==StepType.DOWN && t4==StepType.DOWN)
                return targG == FEMALE ? "玄孙女" : "玄孙";

            // 表叔/表姑（奶奶的兄弟/姐妹的子女：UP+UP+SIBLING+DOWN）
            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.SIBLING && t4==StepType.DOWN) {
                boolean viaGrandMother = (steps.get(1).parentGender == FEMALE) || (steps.get(1).parentGender == 0 && mid2G == FEMALE);
                if (viaGrandMother) {
                    return targG == FEMALE ? "表姑" : "表叔";
                }
                // 爷爷的兄弟/姐妹的子女 → 用户要求直接称叔叔/姑姑
                return targG == FEMALE ? "姑姑" : "叔叔";
            }

            // 再从兄弟姐妹（UP+SIBLING+DOWN+DOWN）
            if (t1==StepType.UP && t2==StepType.SIBLING && t3==StepType.DOWN && t4==StepType.DOWN)
                return targG == FEMALE ? "再从姐妹" : "再从兄弟";

            // 堂叔/堂姑（UP+UP+SIBLING+SPOUSE... 或其他）
            if (t1==StepType.UP && t2==StepType.UP && t3==StepType.DOWN && t4==StepType.DOWN) {
                // 爷爷奶奶的子女的子女 → 堂/表兄弟姐妹的子女（侄辈）
                return targG == FEMALE ? "堂/表侄女" : "堂/表侄";
            }

            // 叔伯/姑/舅/姨的孙辈（UP+SIBLING+DOWN+DOWN）
            if (t1==StepType.UP && t2==StepType.SIBLING && t3==StepType.DOWN && t4==StepType.DOWN) {
                boolean viaFather = (steps.get(0).parentGender == MALE) || (steps.get(0).parentGender == 0 && mid1G == MALE);
                boolean uncleIsMale = mid2G == MALE;
                String prefix = (viaFather && uncleIsMale) ? "堂" : "表";
                return prefix + (targG == FEMALE ? "侄女" : "侄子");
            }
        }

        return "亲属";
    }
}
