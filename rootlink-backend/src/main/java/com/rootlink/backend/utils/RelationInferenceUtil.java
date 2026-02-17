package com.rootlink.backend.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 亲属关系推断工具
 * 核心逻辑：将关系链（如 ["父","母"]）转换为中文称谓（如"奶奶"）
 */
@Slf4j
@Component
public class RelationInferenceUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =============================================
    // 直系链称谓表（key = 链逗号拼接）
    // =============================================
    private static final Map<String, String> CHAIN_NAME_MAP = new LinkedHashMap<>();
    // 向上一代
    private static final Map<String, String> CHAIN_NAME_MAP_FEMALE = new LinkedHashMap<>();

    static {
        // ---- 上代 ----
        CHAIN_NAME_MAP.put("父", "父亲");
        CHAIN_NAME_MAP.put("母", "母亲");
        CHAIN_NAME_MAP.put("父,父", "爷爷");
        CHAIN_NAME_MAP.put("父,母", "奶奶");
        CHAIN_NAME_MAP.put("母,父", "外公");
        CHAIN_NAME_MAP.put("母,母", "外婆");
        CHAIN_NAME_MAP.put("父,父,父", "曾祖父");
        CHAIN_NAME_MAP.put("父,父,母", "曾祖母");
        CHAIN_NAME_MAP.put("母,父,父", "外曾祖父");
        CHAIN_NAME_MAP.put("母,父,母", "外曾祖母");
        CHAIN_NAME_MAP.put("母,母,父", "外曾祖父");
        CHAIN_NAME_MAP.put("母,母,母", "外曾祖母");
        CHAIN_NAME_MAP.put("父,母,父", "祖外公");
        CHAIN_NAME_MAP.put("父,母,母", "祖外婆");
        // ---- 下代 ----
        CHAIN_NAME_MAP.put("子", "儿子");
        CHAIN_NAME_MAP.put("女", "女儿");
        CHAIN_NAME_MAP.put("子,子", "孙子");
        CHAIN_NAME_MAP.put("子,女", "孙女");
        CHAIN_NAME_MAP.put("女,子", "外孙");
        CHAIN_NAME_MAP.put("女,女", "外孙女");
        CHAIN_NAME_MAP.put("子,子,子", "曾孙");
        CHAIN_NAME_MAP.put("子,子,女", "曾孙女");
        CHAIN_NAME_MAP.put("子,女,子", "外曾孙");
        CHAIN_NAME_MAP.put("子,女,女", "外曾孙女");
        CHAIN_NAME_MAP.put("女,子,子", "外孙的儿子");
        CHAIN_NAME_MAP.put("女,女,子", "外孙女的儿子");

        // ---- 配偶 ----
        CHAIN_NAME_MAP.put("配偶", "配偶");
        CHAIN_NAME_MAP.put("配偶,父", "岳父/公公");
        CHAIN_NAME_MAP.put("配偶,母", "岳母/婆婆");
        CHAIN_NAME_MAP.put("配偶,哥", "大舅子/大伯");
        CHAIN_NAME_MAP.put("配偶,弟", "小舅子/小叔");
        CHAIN_NAME_MAP.put("配偶,姐", "大姨子/大姑");
        CHAIN_NAME_MAP.put("配偶,妹", "小姨子/小姑");
        CHAIN_NAME_MAP.put("配偶,子", "继子");
        CHAIN_NAME_MAP.put("配偶,女", "继女");
        CHAIN_NAME_MAP.put("配偶,父,父", "配偶的爷爷");
        CHAIN_NAME_MAP.put("配偶,父,母", "配偶的奶奶");
        CHAIN_NAME_MAP.put("配偶,母,父", "配偶的外公");
        CHAIN_NAME_MAP.put("配偶,母,母", "配偶的外婆");
        CHAIN_NAME_MAP.put("配偶,父,哥", "配偶的伯父");
        CHAIN_NAME_MAP.put("配偶,父,弟", "配偶的叔叔");
        CHAIN_NAME_MAP.put("配偶,父,姐", "配偶的姑姑");
        CHAIN_NAME_MAP.put("配偶,父,妹", "配偶的姑姑");
        CHAIN_NAME_MAP.put("配偶,母,哥", "配偶的舅舅");
        CHAIN_NAME_MAP.put("配偶,母,弟", "配偶的舅舅");
        CHAIN_NAME_MAP.put("配偶,母,姐", "配偶的姨妈");
        CHAIN_NAME_MAP.put("配偶,母,妹", "配偶的姨妈");

        // ---- 旁系（父/母 → 兄弟姐妹） ----
        CHAIN_NAME_MAP.put("父,哥", "伯父");
        CHAIN_NAME_MAP.put("父,弟", "叔叔");
        CHAIN_NAME_MAP.put("父,姐", "姑姑");
        CHAIN_NAME_MAP.put("父,妹", "姑姑");
        CHAIN_NAME_MAP.put("母,哥", "舅舅");
        CHAIN_NAME_MAP.put("母,弟", "舅舅");
        CHAIN_NAME_MAP.put("母,姐", "姨妈");
        CHAIN_NAME_MAP.put("母,妹", "姨妈");
        CHAIN_NAME_MAP.put("父,配偶", "继母");
        CHAIN_NAME_MAP.put("母,配偶", "继父");

        // ---- 同辈之子（侄/甥） ----
        CHAIN_NAME_MAP.put("哥,子", "侄子");
        CHAIN_NAME_MAP.put("哥,女", "侄女");
        CHAIN_NAME_MAP.put("弟,子", "侄子");
        CHAIN_NAME_MAP.put("弟,女", "侄女");
        CHAIN_NAME_MAP.put("姐,子", "外甥");
        CHAIN_NAME_MAP.put("姐,女", "外甥女");
        CHAIN_NAME_MAP.put("妹,子", "外甥");
        CHAIN_NAME_MAP.put("妹,女", "外甥女");
        CHAIN_NAME_MAP.put("哥,配偶", "嫂子");
        CHAIN_NAME_MAP.put("弟,配偶", "弟媳");
        CHAIN_NAME_MAP.put("姐,配偶", "姐夫");
        CHAIN_NAME_MAP.put("妹,配偶", "妹夫");

        // ---- 子女之配偶 ----
        CHAIN_NAME_MAP.put("子,配偶", "儿媳");
        CHAIN_NAME_MAP.put("女,配偶", "女婿");
        CHAIN_NAME_MAP.put("子,子,配偶", "孙媳");

        // ---- 表堂（父母→兄弟姐妹→子女） ----
        CHAIN_NAME_MAP.put("父,哥,子", "堂兄弟");
        CHAIN_NAME_MAP.put("父,哥,女", "堂姐妹");
        CHAIN_NAME_MAP.put("父,弟,子", "堂兄弟");
        CHAIN_NAME_MAP.put("父,弟,女", "堂姐妹");
        CHAIN_NAME_MAP.put("父,姐,子", "表兄弟");
        CHAIN_NAME_MAP.put("父,姐,女", "表姐妹");
        CHAIN_NAME_MAP.put("父,妹,子", "表兄弟");
        CHAIN_NAME_MAP.put("父,妹,女", "表姐妹");
        CHAIN_NAME_MAP.put("母,哥,子", "表兄弟");
        CHAIN_NAME_MAP.put("母,哥,女", "表姐妹");
        CHAIN_NAME_MAP.put("母,弟,子", "表兄弟");
        CHAIN_NAME_MAP.put("母,弟,女", "表姐妹");
        CHAIN_NAME_MAP.put("母,姐,子", "表兄弟");
        CHAIN_NAME_MAP.put("母,姐,女", "表姐妹");
        CHAIN_NAME_MAP.put("母,妹,子", "表兄弟");
        CHAIN_NAME_MAP.put("母,妹,女", "表姐妹");
        // ---- 叔伯姑舅姨的配偶 ----
        CHAIN_NAME_MAP.put("父,哥,配偶", "伯母");
        CHAIN_NAME_MAP.put("父,弟,配偶", "婶婶");
        CHAIN_NAME_MAP.put("父,姐,配偶", "姑父");
        CHAIN_NAME_MAP.put("父,妹,配偶", "姑父");
        CHAIN_NAME_MAP.put("母,哥,配偶", "舅妈");
        CHAIN_NAME_MAP.put("母,弟,配偶", "舅妈");
        CHAIN_NAME_MAP.put("母,姐,配偶", "姨父");
        CHAIN_NAME_MAP.put("母,妹,配偶", "姨父");
        // ---- 曾祖（太）辈 ----
        CHAIN_NAME_MAP.put("父,父,父", "太爷爷");
        CHAIN_NAME_MAP.put("父,父,母", "太奶奶");
        CHAIN_NAME_MAP.put("母,母,父", "太外公");
        CHAIN_NAME_MAP.put("母,母,母", "太外婆");
        // ---- 重孙辈 ----
        CHAIN_NAME_MAP.put("子,子,子", "重孙子");
        CHAIN_NAME_MAP.put("子,子,女", "重孙女");
        // ---- 亲家 ----
        CHAIN_NAME_MAP.put("子,配偶,父", "亲家公");
        CHAIN_NAME_MAP.put("子,配偶,母", "亲家母");
        CHAIN_NAME_MAP.put("女,配偶,父", "亲家公");
        CHAIN_NAME_MAP.put("女,配偶,母", "亲家母");
        // ---- 爷爷的兄弟姐妹的子女 → 叔叔/姑姑（用户要求） ----
        CHAIN_NAME_MAP.put("父,父,哥,子", "叔叔（堂叔）");
        CHAIN_NAME_MAP.put("父,父,哥,女", "姑姑（堂姑）");
        CHAIN_NAME_MAP.put("父,父,弟,子", "叔叔（堂叔）");
        CHAIN_NAME_MAP.put("父,父,弟,女", "姑姑（堂姑）");
        // ---- 奶奶的兄弟姐妹的子女 → 表叔/表姑 ----
        CHAIN_NAME_MAP.put("父,母,哥,子", "表叔");
        CHAIN_NAME_MAP.put("父,母,哥,女", "表姑");
        CHAIN_NAME_MAP.put("父,母,弟,子", "表叔");
        CHAIN_NAME_MAP.put("父,母,弟,女", "表姑");
        CHAIN_NAME_MAP.put("父,母,姐,子", "表叔");
        CHAIN_NAME_MAP.put("父,母,姐,女", "表姑");
        CHAIN_NAME_MAP.put("父,母,妹,子", "表叔");
        CHAIN_NAME_MAP.put("父,母,妹,女", "表姑");
    }

    /**
     * 同辈称谓：(中间关系, 排行) -> 称谓
     */
    private static final Map<String, String> SIBLING_NAME_MAP = new HashMap<>();
    static {
        // 直属同辈
        SIBLING_NAME_MAP.put("直属,哥", "哥哥");
        SIBLING_NAME_MAP.put("直属,弟", "弟弟");
        SIBLING_NAME_MAP.put("直属,姐", "姐姐");
        SIBLING_NAME_MAP.put("直属,妹", "妹妹");
        // 伯伯/叔叔（父的兄弟）→ 堂
        for (String conn : new String[]{"伯伯", "叔叔"}) {
            SIBLING_NAME_MAP.put(conn + ",哥", "堂哥");
            SIBLING_NAME_MAP.put(conn + ",弟", "堂弟");
            SIBLING_NAME_MAP.put(conn + ",姐", "堂姐");
            SIBLING_NAME_MAP.put(conn + ",妹", "堂妹");
        }
        // 姑姑（父的姐妹）/舅舅（母的兄弟）/姨妈（母的姐妹）→ 表
        for (String conn : new String[]{"姑姑", "舅舅", "姨妈"}) {
            SIBLING_NAME_MAP.put(conn + ",哥", "表哥");
            SIBLING_NAME_MAP.put(conn + ",弟", "表弟");
            SIBLING_NAME_MAP.put(conn + ",姐", "表姐");
            SIBLING_NAME_MAP.put(conn + ",妹", "表妹");
        }
    }

    /**
     * 将关系链 JSON 转换为中文称谓
     * @param chainJson  JSON 数组字符串，如 ["父","母"]；同辈用 ["同辈","直属","哥"]
     * @return 中文称谓
     */
    public String chainToName(String chainJson) {
        try {
            List<String> chain = MAPPER.readValue(chainJson, new TypeReference<>() {});
            return resolveChain(chain);
        } catch (Exception e) {
            log.warn("关系链解析失败: {}", chainJson, e);
            return "亲属";
        }
    }

    public String resolveChain(List<String> chain) {
        if (chain == null || chain.isEmpty()) return "亲属";

        // 兼容旧的"同辈"前缀模式
        if ("同辈".equals(chain.get(0))) {
            if (chain.size() < 3) return "亲属";
            String key = chain.get(1) + "," + chain.get(2);
            return SIBLING_NAME_MAP.getOrDefault(key, "亲属");
        }

        // 单步：直接兄弟姐妹（新模式，哥/弟/姐/妹 作为第一个元素）
        if (chain.size() == 1) {
            String s = chain.get(0);
            switch (s) {
                case "哥": return "哥哥";
                case "弟": return "弟弟";
                case "姐": return "姐姐";
                case "妹": return "妹妹";
                default: return CHAIN_NAME_MAP.getOrDefault(s, "亲属");
            }
        }

        // 多步链（直接查表）
        String key = String.join(",", chain);
        if (CHAIN_NAME_MAP.containsKey(key)) return CHAIN_NAME_MAP.get(key);

        // 4步链查表（在 CHAIN_NAME_MAP 中已有 4步 key）
        return buildFallbackName(chain);
    }

    /**
     * 找不到精确匹配时的兜底名称（超过3层）
     */
    private String buildFallbackName(List<String> chain) {
        long upCount = chain.stream().filter(s -> "父".equals(s) || "母".equals(s)).count();
        long downCount = chain.stream().filter(s -> "子".equals(s) || "女".equals(s)).count();
        if (upCount > 0 && downCount == 0) return upCount + "代祖先";
        if (downCount > 0 && upCount == 0) return downCount + "代后裔";
        return "亲属";
    }

    // =============================================
    // 关系推断：已知 A→B 和 B→C，推出 A→C
    // =============================================

    /**
     * 推断传递关系
     * @param chainA2B A 到 B 的链（B 是 A 的什么人）
     * @param chainB2C B 到 C 的链（C 是 B 的什么人）
     * @return A 到 C 的链，无法推断时返回 null
     */
    public List<String> inferChain(List<String> chainA2B, List<String> chainB2C) {
        if (chainA2B == null || chainB2C == null) return null;
        if (chainA2B.isEmpty() || chainB2C.isEmpty()) return null;

        // ---- 同辈特殊处理 ----
        boolean a2bSibling = "同辈".equals(chainA2B.get(0));
        boolean b2cSibling = "同辈".equals(chainB2C.get(0));

        if (a2bSibling && !b2cSibling) {
            // A 和 B 是同辈，B->C 是直系：A->C 与 B->C 相同（同辈共享父母）
            String conn = chainA2B.size() >= 2 ? chainA2B.get(1) : "直属";
            if ("直属".equals(conn)) {
                return new ArrayList<>(chainB2C);
            }
            // 堂/表亲通过伯叔姑舅姨，父母关系需要修正
            return null; // 复杂情况暂不推断
        }

        if (!a2bSibling && b2cSibling) {
            // A->B 是直系，B 和 C 是同辈
            // 说明 C 也是 A 的亲属，但推断比较复杂，暂只处理直属同辈
            String conn = chainB2C.size() >= 2 ? chainB2C.get(1) : "直属";
            if ("直属".equals(conn)) {
                return new ArrayList<>(chainA2B);
            }
            return null;
        }

        if (a2bSibling && b2cSibling) {
            // 同辈的同辈，无法简单推断
            return null;
        }

        // ---- 纯直系推断 ----
        // 判断方向：up=[父/母], down=[子/女]
        boolean a2bUp = chainA2B.stream().allMatch(s -> "父".equals(s) || "母".equals(s));
        boolean a2bDown = chainA2B.stream().allMatch(s -> "子".equals(s) || "女".equals(s));
        boolean b2cUp = chainB2C.stream().allMatch(s -> "父".equals(s) || "母".equals(s));
        boolean b2cDown = chainB2C.stream().allMatch(s -> "子".equals(s) || "女".equals(s));

        // 配偶情况
        if ("配偶".equals(chainA2B.get(0))) {
            // A 是 B 的配偶，B->C 的关系 A 也有相同关系（共同子女、共同父母）
            return new ArrayList<>(chainB2C);
        }
        if ("配偶".equals(chainB2C.get(0))) {
            // B->C 是配偶，则 A->C 与 A->B 对应的配偶关系（不推断，太复杂）
            return null;
        }

        if (a2bUp && b2cUp) {
            // 双向上代：直接拼接，但不超过3层
            if (chainA2B.size() + chainB2C.size() > 3) return null;
            List<String> merged = new ArrayList<>(chainA2B);
            merged.addAll(chainB2C);
            return merged;
        }

        if (a2bDown && b2cDown) {
            // 双向下代
            if (chainA2B.size() + chainB2C.size() > 3) return null;
            List<String> merged = new ArrayList<>(chainA2B);
            merged.addAll(chainB2C);
            return merged;
        }

        if (a2bUp && b2cDown) {
            // ★ 核心逻辑：上行 N 步 + 下行 M 步
            int upLen   = chainA2B.size();
            int downLen = chainB2C.size();
            int net     = upLen - downLen;

            if (net == 0) {
                // 代数相消 → 同辈关系
                if (upLen == 1) {
                    // 直系同辈（共同父母），称谓待性别确认，先存 ["同辈","直属","弟"]
                    return Arrays.asList("同辈", "直属", "弟");
                }
                if (upLen == 2) {
                    // 共同祖父母 → 堂/表兄弟姐妹，暂不细分
                    return Arrays.asList("同辈", "直属", "弟");
                }
                return null; // 三代以上不推断
            }

            if (net == 1) {
                // 上行比下行多1步：B看C是比自己高一辈的亲属（叔/舅/姑等）
                // 例：B->A=["父","母"]（奶奶）, A->C=["子"]（A的儿子，即B的父辈）
                // 不处理旁系长辈
                return null;
            }

            if (net == -1) {
                // 下行比上行多1步：B看C是晚辈（侄子/外甥等）
                return null;
            }

            return null;
        }

        if (a2bDown && b2cUp) {
            // A的子女B，B往上看C（C是B的父母）→ C可能是A自己或A的配偶
            // 不推断（防止误推）
            return null;
        }

        return null;
    }

    /**
     * 将链转为 JSON 字符串
     */
    public String chainToJson(List<String> chain) {
        try {
            return MAPPER.writeValueAsString(chain);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 将 JSON 字符串转为链列表
     */
    public List<String> jsonToChain(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取逆向链（A->B 的逆向是 B->A）- 无性别修正版（兼容旧调用）
     */
    public List<String> reverseChain(List<String> chain) {
        return reverseChainWithGender(chain, null, null);
    }

    /**
     * 获取逆向链（带性别修正）
     * @param chain       A->B 的关系链（"B是A的什么人"）
     * @param myGender    A 的性别（1-男 2-女 null-未知）
     * @param otherGender B 的性别（1-男 2-女 null-未知）
     *
     * 规则：
     *  - 链中 子/女 → 逆向是 父(A男) 或 母(A女)：取决于 A(我) 的性别
     *  - 链中 父/母 → 逆向是 子(B男) 或 女(B女)：取决于 B(对方) 的性别
     */
    /**
     * 带性别修正的逆向链
     *
     * 核心规则（只对第一步/直接关系应用性别修正）：
     *  - 链第一步是 子/女（我说"对方是我的子/女"）
     *    → 逆向第一步是 父(我男) 或 母(我女)，由 myGender 决定
     *  - 链第一步是 父/母（我说"对方是我的父/母"）
     *    → 逆向第一步是 子(我男) 或 女(我女)，由 myGender 决定
     *  - 中间步骤（祖父母级）不知道性别，默认用 子/父（最常见）
     *
     * 示例：
     *  A(女) 说 B是我的子 → chain=["子"] → reverse=["母"] ✓
     *  A(女) 说 B是我的父 → chain=["父"] → reverse=["女"] ✓
     *  A(女) 说 B是我的父的母 → chain=["父","母"] → reverse=["子","女"] ✓
     *    即：A是B的儿子的女儿(孙女)
     */
    public List<String> reverseChainWithGender(List<String> chain, Integer myGender, Integer otherGender) {
        if (chain == null || chain.isEmpty()) return new ArrayList<>();

        // 单步配偶才对称返回；多步配偶链（["配偶","父"]/["配偶","母"]等）走通用逆向
        if (chain.size() == 1 && "配偶".equals(chain.get(0))) return new ArrayList<>(chain);

        // 同辈：逆向时用"我"(myGender)的性别确定称谓，用原链的长幼关系确定排行
        if ("同辈".equals(chain.get(0))) {
            List<String> rev = new ArrayList<>(chain);
            if (chain.size() >= 3) {
                String sib = chain.get(2);
                // 原链描述"目标人"的长幼：弟/妹=目标是年轻，哥/姐=目标是年长
                boolean targetIsYounger = "弟".equals(sib) || "妹".equals(sib);
                // 逆向后：我是"目标人"的另一方，我的长幼关系取反
                boolean iAmYounger = !targetIsYounger;
                if (iAmYounger) {
                    rev.set(2, myGender != null && myGender == 2 ? "妹" : "弟");
                } else {
                    rev.set(2, myGender != null && myGender == 2 ? "姐" : "哥");
                }
            }
            return rev;
        }

        // 直系链逆向（核心规则：反转每一步的方向和性别）
        List<String> rev = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            String s = chain.get(i);
            if (i == 0) {
                // 第一步：「我」与对方的直接关系，用 myGender 决定逆向称谓
                switch (s) {
                    case "子": case "女":
                        // B是A的孩子 → 逆向：A是B的父(男)/母(女)
                        rev.add(0, myGender != null && myGender == 2 ? "母" : "父");
                        break;
                    case "父": case "母":
                        // B是A的父母 → 逆向：A是B的子(男)/女(女)
                        rev.add(0, myGender != null && myGender == 2 ? "女" : "子");
                        break;
                    case "哥": case "姐":
                        // B是A的兄/姐（B比A年长） → 逆向：A是B的弟(男)/妹(女)
                        rev.add(0, myGender != null && myGender == 2 ? "妹" : "弟");
                        break;
                    case "弟": case "妹":
                        // B是A的弟/妹（B比A年幼） → 逆向：A是B的哥(男)/姐(女)
                        rev.add(0, myGender != null && myGender == 2 ? "姐" : "哥");
                        break;
                    default:
                        rev.add(0, s);
                }
            } else {
                // 中间步骤：反转父子/祖孙方向；兄弟姐妹作为中间步骤保持原值
                switch (s) {
                    case "子": case "女":
                        rev.add(0, "父");  // 暂不细分父/母，由后续 gender 修正处理
                        break;
                    case "父": case "母":
                        // 需要知道"孩子"的性别：
                        // 若前一步是"配偶"，则孩子是 A 的配偶（与 A 异性）
                        // A女→配偶男→子；A男→配偶女→女
                        String prevStep = chain.get(i - 1);
                        if ("配偶".equals(prevStep)) {
                            rev.add(0, myGender != null && myGender == 2 ? "子" : "女");
                        } else {
                            rev.add(0, "子");  // 默认取子（最常见）
                        }
                        break;
                    default: rev.add(0, s); // 哥/弟/姐/妹/配偶 原样保留
                }
            }
        }
        return rev;
    }

    private String reverseSibling(String s) {
        switch (s) {
            case "哥": return "弟";
            case "弟": return "哥";
            case "姐": return "妹";
            case "妹": return "姐";
            default: return s;
        }
    }

    /**
     * 根据性别修正称谓（用于推断后的最终显示）
     */
    public String correctByGender(String name, Integer gender) {
        if (gender == null) return name;
        // 需要性别修正的称谓
        if (gender == 1) { // 男
            if ("父亲或母亲".equals(name)) return "父亲";
            if ("孙子或孙女".equals(name)) return "孙子";
            if ("曾孙或曾孙女".equals(name)) return "曾孙";
        } else if (gender == 2) { // 女
            if ("父亲或母亲".equals(name)) return "母亲";
            if ("孙子或孙女".equals(name)) return "孙女";
        }
        return name;
    }
}
