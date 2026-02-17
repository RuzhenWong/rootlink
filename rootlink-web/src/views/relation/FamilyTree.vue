<template>
  <div class="family-tree-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🌳 家族关系树</span>
          <div class="header-actions">
            <el-radio-group v-model="viewMode" size="small" style="margin-right:12px">
              <el-radio-button value="tree">关系图</el-radio-button>
              <el-radio-button value="list">列表</el-radio-button>
            </el-radio-group>
            <el-tooltip content="刷新显示" placement="top">
              <el-button text :loading="loading" @click="loadData" style="margin-right:2px">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip placement="top" :disabled="reInferRunning">
              <template #content>
                <div style="max-width:220px;line-height:1.6">
                  重新分析整个家族网络的关系称谓<br>
                  修正性别错误、自动补全缺失关系
                </div>
              </template>
              <el-button type="primary" size="small"
                :loading="reInferRunning" :disabled="reInferRunning"
                @click="startReInfer" style="border-radius:8px">
                <el-icon v-if="!reInferRunning" style="margin-right:4px"><MagicStick /></el-icon>
                {{ reInferRunning ? '推断中...' : '重新推断' }}
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 全量重推进度条 -->
      <transition name="slide-down">
        <div v-if="reInferRunning || reInferDone" class="reinfer-bar">
          <div class="reinfer-bar-inner">
            <div class="reinfer-icon" :class="reInferStatus.status">
              <el-icon v-if="reInferStatus.status === 'running'" class="is-loading"><Loading /></el-icon>
              <el-icon v-else-if="reInferStatus.status === 'done'"><CircleCheck /></el-icon>
              <el-icon v-else-if="reInferStatus.status === 'error'"><CircleClose /></el-icon>
            </div>
            <div class="reinfer-info">
              <div class="reinfer-msg">{{ reInferStatus.message || '正在处理...' }}</div>
              <el-progress v-if="reInferStatus.status === 'running'"
                :percentage="reInferStatus.progress || 0"
                :stroke-width="4" :show-text="false" style="margin-top:4px" />
              <div v-if="reInferStatus.status === 'done' && reInferStatus.result" class="reinfer-result">
                👥 {{ reInferStatus.result.networkSize }} 位成员 &nbsp;
                🔗 {{ reInferStatus.result.manualEdges }} 条原始关系 &nbsp;
                ✨ 新推断 {{ reInferStatus.result.newInferred }} 条
              </div>
            </div>
            <div class="reinfer-actions">
              <el-button v-if="reInferStatus.status === 'done'" type="primary" size="small" text
                @click="loadData(); reInferDone = false">刷新树</el-button>
              <el-button size="small" text @click="dismissReInfer">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </transition>

      <div v-if="loading" v-loading="true" style="height:400px" />

      <template v-else>
        <el-empty v-if="directRelations.length === 0"
          description="暂无亲属关系，去「亲属关系」页面添加" :image-size="120">
          <el-button type="primary" @click="$router.push('/relations')">去添加</el-button>
        </el-empty>

        <template v-else>
        <!-- ===== 关系图 ===== -->
        <div v-if="viewMode === 'tree'" class="tree-wrapper">
          <div class="legend-bar">
            <span v-for="(v, k) in COLORS" :key="k" class="legend-item">
              <span class="dot" :style="{ background: v }" />{{ k }}
            </span>
            <span class="legend-sep" />
            <span class="legend-tip">
              <span class="edge-solid" />实线=已确认 &nbsp;
              <span class="edge-dash" />虚线=推断关系
            </span>
          </div>

          <div class="svg-scroll">
            <svg :width="svgW" :height="svgH" @click="deselect">
              <!-- ── 连线层 ── -->
              <g>
                <!-- 父子连线 -->
                <path v-for="(e, i) in parentChildEdges" :key="'pc' + i"
                  :d="e.d" :stroke="e.color" :stroke-width="e.width"
                  :stroke-dasharray="e.dash" fill="none"
                  stroke-linecap="round" stroke-linejoin="round" opacity="0.7" />
                <!-- 夫妻连线 -->
                <path v-for="(e, i) in spouseEdges" :key="'sp' + i"
                  :d="e.d" :stroke="e.color" :stroke-width="e.width"
                  :stroke-dasharray="e.dash" fill="none"
                  stroke-linecap="round" opacity="0.7" />
              </g>

              <!-- ── 节点层 ── -->
              <g v-for="node in layoutNodes" :key="node.id"
                :transform="`translate(${node.x},${node.y})`"
                class="node-g"
                @click.stop="selectNode(node)">
                <!-- 选中高亮环 -->
                <circle v-if="selectedId === node.id"
                  :r="node.r + 6" fill="none"
                  stroke="#409eff" stroke-width="2.5" stroke-dasharray="4 2" />
                <!-- 主圆 -->
                <circle :r="node.r" :fill="node.fill"
                  :stroke="node.isMe ? '#fff' : 'rgba(255,255,255,0.25)'"
                  :stroke-width="node.isMe ? 3 : 1" />
                <!-- 离世标记 -->
                <text v-if="node.lifeStatus === 3"
                  :x="node.r - 2" :y="-node.r + 8" font-size="11" fill="#ff4d4f">✝</text>
                <!-- 推断标记小圆点 -->
                <circle v-if="node.isInferred"
                  :cx="node.r - 1" :cy="-node.r + 1" r="5" fill="#fa8231" />
                <!-- 缩写文字 -->
                <text x="0" y="1" text-anchor="middle" dominant-baseline="middle"
                  :font-size="node.isMe ? 13 : 11" font-weight="700" fill="#fff"
                  style="pointer-events:none; user-select:none">{{ node.abbr }}</text>
                <!-- 姓名（第1行） -->
                <text x="0" :y="node.r + 16"
                  text-anchor="middle" font-size="12" fill="#1a1a2e"
                  font-weight="600" style="pointer-events:none">{{ node.label }}</text>
                <!-- 我对TA的称谓（第2行），本人不显示 -->
                <text v-if="!node.isMe && node.myRelation" x="0" :y="node.r + 30"
                  text-anchor="middle" font-size="10"
                  :fill="node.isInferred ? '#fa8231' : '#5A67F2'"
                  font-weight="500" style="pointer-events:none">{{ node.myRelation }}</text>
              </g>
            </svg>
          </div>

          <!-- 节点详情卡 -->
          <transition name="slide-fade">
            <div v-if="selectedNode" class="detail-card">
              <button class="detail-close" @click="deselect">✕</button>
              <div class="detail-header">
                <el-avatar :size="44" icon="UserFilled"
                  :style="{ background: selectedNode.fill }" />
                <div>
                  <div class="detail-name">{{ selectedNode.label }}</div>
                  <el-tag v-if="!selectedNode.isMe && selectedNode.myRelation" size="small"
                    :style="{ background: selectedNode.fill, color:'#fff', border:'none' }">
                    {{ selectedNode.myRelation }}
                  </el-tag>
                  <el-tag v-else-if="selectedNode.isMe" size="small" type="info">本人</el-tag>
                  <el-tag size="small" :type="lifeTagType(selectedNode.lifeStatus)"
                    style="margin-left:4px">{{ lifeLabel(selectedNode.lifeStatus) }}</el-tag>
                  <el-tag v-if="selectedNode.isInferred" size="small" type="warning"
                    style="margin-left:4px">推断</el-tag>
                </div>
              </div>
              <div style="margin-top:10px; display:flex; gap:6px">
                <el-button size="small" type="primary" plain
                  @click="openProfile(selectedNode.id, selectedNode.myRelation)">
                  查看详情
                </el-button>
              </div>
            </div>
          </transition>
        </div>

        <!-- ===== 列表视图 ===== -->
        <div v-else class="list-view">
          <div v-for="group in groupedRelations" :key="group.label" class="relation-group">
            <div class="group-title">{{ group.label }}（{{ group.items.length }}）</div>
            <div class="group-items">
              <div v-for="r in group.items" :key="r.relatedUserId"
                class="list-card" style="cursor:pointer"
                @click="openProfile(r.relatedUserId, r.relationDesc)">
                <el-avatar :size="36" icon="UserFilled" />
                <div class="list-info">
                  <div class="list-name">{{ r.realName || '未知' }}</div>
                  <el-tag size="small" type="primary">{{ r.relationDesc }}</el-tag>
                  <el-tag v-if="r.inferStatus === 2" size="small" type="warning"
                    style="margin-left:4px">推断</el-tag>
                  <el-tag v-if="r.lifeStatus === 3" size="small" type="danger"
                    style="margin-left:4px">已离世</el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
        </template><!-- end: template v-else (有亲属时的内容) -->
      </template><!-- end: template v-else (非loading时) -->
    </el-card>

    <RelativeProfileDrawer v-model="profileDrawerVisible"
      :userId="profileTargetId" :relationDesc="profileRelationDesc" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Refresh, MagicStick, Loading, CircleCheck, CircleClose, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { relationApi } from '@/api/relation'
import { useAuthStore } from '@/stores/auth'
import RelativeProfileDrawer from '@/components/RelativeProfileDrawer.vue'

const authStore = useAuthStore()
const loading   = ref(false)
const directRelations = ref([])   // 我的直接关系列表（含我对每人的称谓）
const networkData     = ref({ nodes: [], edges: [] })  // 全网边数据
const viewMode  = ref('tree')
const selectedId = ref(null)

// ══ 颜色 ══════════════════════════════════════════════
const COLORS = {
  '本人': '#303133',
  '长辈': '#667eea',
  '配偶': '#e040fb',
  '同辈': '#26c6da',
  '晚辈': '#42a5f5',
  '其他': '#90a4ae',
}

// ══ 代际判断 ══════════════════════════════════════════
// 正数=长辈，负数=晚辈，0=同辈/配偶
function guessGen(desc) {
  if (!desc) return 0
  const up3 = new Set(['太爷爷','太奶奶','太外公','太外婆','曾祖父','曾祖母','外曾祖父','外曾祖母','高祖父','高祖母'])
  const up2 = new Set(['爷爷','奶奶','外公','外婆'])
  const up1 = new Set([
    '父亲','母亲','伯父','叔叔','姑姑','舅舅','姨妈','伯母','婶婶','姑父','舅妈','姨父',
    '继父','继母','公公','婆婆','岳父','岳母','岳父/公公','岳母/婆婆',
    '大舅子','小舅子','大姨子','小姨子','大伯子','小叔子','大姑子','小姑子', // 配偶兄弟算同辈
  ])
  const dn1 = new Set(['儿子','女儿','侄子','侄女','外甥','外甥女','继子','继女','儿媳','女婿'])
  const dn2 = new Set(['孙子','孙女','外孙','外孙女','孙媳','孙女婿'])
  const dn3 = new Set(['重孙子','重孙女','外曾孙','外曾孙女','曾孙','曾孙女'])
  if (up3.has(desc)) return 3
  if (up2.has(desc)) return 2
  if (up1.has(desc)) return 1
  if (dn3.has(desc)) return -3
  if (dn2.has(desc)) return -2
  if (dn1.has(desc)) return -1
  return 0
}

function nodeColorByGen(gen, isMe) {
  if (isMe) return COLORS['本人']
  if (gen > 0) return COLORS['长辈']
  if (gen < 0) return COLORS['晚辈']
  return COLORS['同辈']
}

// ══ 布局参数 ══════════════════════════════════════════
const NODE_R_ME = 30
const NODE_R    = 24
const GEN_H     = 160   // 代际行高（含节点下方文字空间）
const H_GAP     = 140   // 同代最小水平间距
const PAD_X     = 80
const PAD_TOP   = 70
const svgW = ref(900)
const svgH = ref(540)

// ══ 我对每个人的称谓 Map（id → {desc, inferStatus}） ══
const myRelationMap = computed(() => {
  const map = new Map()
  directRelations.value.forEach(r => {
    map.set(String(r.relatedUserId), {
      desc: r.relationDesc,
      inferStatus: r.inferStatus,
    })
  })
  return map
})

// ══ 当前用户真实 id（优先从 networkData 获取，fallback authStore）══
// authStore 可能异步未就绪，networkData.nodes 里的 isMe=true 节点更可靠
const myRealId = computed(() => {
  // 1. 优先从 networkData.nodes 找 isMe=true 的节点
  const meNode = (networkData.value?.nodes || []).find(n => n.isMe === true)
  if (meNode?.userId != null) return String(meNode.userId)
  // 2. fallback: authStore
  const uid = authStore.userInfo?.id
  if (uid != null) return String(uid)
  return null
})

// ══ 全部节点（含「我」）════════════════════════════════
const allNodes = computed(() => {
  const myId = myRealId.value
  if (!myId) return []   // 还没拿到我的 id，等待响应式更新

  // 从 networkData 建全量节点 map
  const nodeInfoMap = new Map()
  ;(networkData.value?.nodes || []).forEach(n => {
    nodeInfoMap.set(String(n.userId), n)
  })

  const nodes = []
  const seen  = new Set()

  // ── 「我」自己（优先用 networkData 里的真实姓名）──
  const meInfo = nodeInfoMap.get(myId)
  const meName = meInfo?.realName || authStore.userInfo?.realName || '我'
  nodes.push({
    id: myId,
    label: meName,
    abbr:  meName.slice(0, 2),
    myRelation: '',
    lifeStatus: meInfo?.lifeStatus ?? authStore.userInfo?.lifeStatus ?? 0,
    generation: 0,
    fill: COLORS['本人'],
    r: NODE_R_ME,
    isMe: true,
    isInferred: false,
  })
  seen.add(myId)

  // ── directRelations 里的亲属 ──
  directRelations.value.forEach(r => {
    const rid = String(r.relatedUserId)
    if (seen.has(rid)) return
    seen.add(rid)
    const gen = guessGen(r.relationDesc)
    nodes.push({
      id: rid,
      label: r.realName || '未知',
      abbr:  (r.realName || '?').slice(0, 2),
      myRelation: r.relationDesc,
      lifeStatus: r.lifeStatus,
      generation: gen,
      fill: nodeColorByGen(gen, false),
      r: NODE_R,
      isMe: false,
      isInferred: r.inferStatus === 2,
    })
  })

  // ── networkData 里存在但和「我」无直接关系的节点（如弟媳的父母等）──
  // 代际设 0，之后 genMap BFS 会通过边关系推算真实代际
  nodeInfoMap.forEach((n, uid) => {
    if (seen.has(uid)) return
    seen.add(uid)
    nodes.push({
      id: uid,
      label: n.realName || '未知',
      abbr:  (n.realName || '?').slice(0, 2),
      myRelation: '',
      lifeStatus: n.lifeStatus ?? 0,
      generation: 0,
      fill: COLORS['其他'],
      r: NODE_R,
      isMe: false,
      isInferred: false,
    })
  })

  return nodes
})

// ══ 配偶对 Map（全局，供布局和连线共用）════════════════════════════
const spouseMap = computed(() => {
  const m = new Map()
  const SPOUSE_SET = new Set(['配偶','妻子','丈夫'])
  ;(networkData.value?.edges || []).forEach(e => {
    if (SPOUSE_SET.has(e.relationDesc)) {
      m.set(String(e.fromUserId), String(e.toUserId))
      m.set(String(e.toUserId),   String(e.fromUserId))
    }
  })
  return m
})

// ══ 布局计算 ══════════════════════════════════════════
const layoutNodes = computed(() => {
  if (allNodes.value.length === 0) return []

  // 整理代际分组
  const byGen = {}
  allNodes.value.forEach(n => {
    const g = n.generation
    if (!byGen[g]) byGen[g] = []
    byGen[g].push(n)
  })
  const gens = Object.keys(byGen).map(Number).sort((a, b) => b - a)

  /**
   * 对一行节点排序，核心规则：
   * 1. 找出所有配偶对，将配偶对捆绑为一个单元（[丈夫, 妻子] 或 [妻子, 丈夫]）
   * 2. 「我」的单元放中间，我的配偶紧贴在我右侧
   * 3. 我的兄弟姐妹放在我单元左侧
   * 4. 其他人放右侧（父辈的配偶会紧贴在父辈旁）
   */
  function sortRow(nodes) {
    const nodeIds = new Set(nodes.map(n => String(n.id)))
    const used    = new Set()

    // 把节点按「配偶对」分组：先处理有配偶的，再处理单身节点
    const units = []  // 每个 unit 是 [node] 或 [node, spouseNode]

    // 我先放（带上我的配偶）
    const meNode = nodes.find(n => n.isMe)
    if (meNode) {
      used.add(String(meNode.id))
      const mySpouseId = spouseMap.value.get(String(meNode.id))
      const mySpouseNode = mySpouseId && nodeIds.has(mySpouseId)
        ? nodes.find(n => String(n.id) === mySpouseId)
        : null
      if (mySpouseNode) {
        used.add(String(mySpouseNode.id))
        units.push({ unit: [meNode, mySpouseNode], isMe: true })
      } else {
        units.push({ unit: [meNode], isMe: true })
      }
    }

    // 其他有配偶对的节点
    nodes.forEach(n => {
      const nid = String(n.id)
      if (used.has(nid)) return
      used.add(nid)
      const spId = spouseMap.value.get(nid)
      if (spId && nodeIds.has(spId) && !used.has(spId)) {
        const spNode = nodes.find(x => String(x.id) === spId)
        if (spNode) {
          used.add(spId)
          units.push({ unit: [n, spNode], isMe: false })
          return
        }
      }
      units.push({ unit: [n], isMe: false })
    })

    // 把 unit 排序：我的unit在中间，兄弟姐妹在左，其他在右
    const SIBLING_SET = new Set(['哥哥','姐姐','弟弟','妹妹',
      '堂哥','堂姐','堂弟','堂妹','表哥','表姐','表弟','表妹'])
    const meUnit  = units.filter(u => u.isMe)
    const sibUnits = units.filter(u => !u.isMe && u.unit.some(n => SIBLING_SET.has(n.myRelation)))
    const othUnits = units.filter(u => !u.isMe && !u.unit.some(n => SIBLING_SET.has(n.myRelation)))

    const orderedUnits = [...sibUnits, ...meUnit, ...othUnits]
    // 展平 unit 为节点列表
    return orderedUnits.flatMap(u => u.unit)
  }

  const maxInRow = Math.max(...Object.values(byGen).map(a => a.length), 1)
  const calcW    = Math.max(900, maxInRow * H_GAP + PAD_X * 2)
  const calcH    = Math.max(540, gens.length * GEN_H + PAD_TOP + 80)
  svgW.value = calcW
  svgH.value = calcH
  const centerX = calcW / 2

  const result = []
  gens.forEach((gen, rowIdx) => {
    const sorted = sortRow(byGen[gen])
    const count  = sorted.length
    const y      = PAD_TOP + rowIdx * GEN_H
    const span   = (count - 1) * H_GAP
    const startX = centerX - span / 2
    sorted.forEach((n, i) => {
      result.push({ ...n, x: count === 1 ? centerX : startX + i * H_GAP, y })
    })
  })
  return result
})

// ══ 节点 id → 布局位置映射 ══════════════════════════
const nodeMap = computed(() => {
  const m = new Map()
  layoutNodes.value.forEach(n => m.set(String(n.id), n))
  return m
})

// ══ 连线类型判断（直接父子、配偶） ══════════════════
// 仅用 DIRECT 一代关系（不跨代）
const DIRECT_PARENT_DESCS = new Set(['父亲','母亲'])
const DIRECT_CHILD_DESCS  = new Set(['儿子','女儿'])

// ══ 代际 Map：id → generation ══════════════════════════════════════
// 以 directRelations 的代际为基础，再对 networkData.edges 做 BFS 补全：
// 若某节点 A 代际已知（genA），且 A 与 B 之间有关系，则可以推算 B 的代际
const genMap = computed(() => {
  const map = new Map()

  // Step1: 从 directRelations（已布局节点）初始化
  layoutNodes.value.forEach(n => map.set(String(n.id), n.generation))

  // Step2: 对 networkData.edges 做 BFS，利用关系描述推算未知节点代际
  // 关系方向：fromId 对 toId 的称谓
  const rawEdges = networkData.value?.edges || []

  // 建邻接表
  const adjMap = new Map()  // id → [{toId, desc, inferStatus}]
  for (const e of rawEdges) {
    const a = String(e.fromUserId), b = String(e.toUserId)
    if (!adjMap.has(a)) adjMap.set(a, [])
    adjMap.get(a).push({ id: b, desc: e.relationDesc })
  }

  // 已知：a 的代际 genA，a 说 b 是 desc → 推算 b 的代际 genB
  function inferGen(genA, desc) {
    const g = guessGen(desc)
    // desc 是 a 对 b 的称谓，gen>0 表示 b 比 a 年长
    // 所以 b 的代际 = genA + g
    return genA + g
  }

  // BFS
  const queue = []
  map.forEach((gen, id) => queue.push(id))
  let head = 0
  while (head < queue.length) {
    const id  = queue[head++]
    const gen = map.get(id)
    const neighbors = adjMap.get(id) || []
    for (const nb of neighbors) {
      if (map.has(nb.id)) continue   // 已有代际，不覆盖
      const nbGen = inferGen(gen, nb.desc)
      map.set(nb.id, nbGen)
      queue.push(nb.id)
    }
  }

  return map
})

// ══ 连线集合（亲子 + 配偶） ════════════════════════════════════════
/**
 * 连线规则：
 * 1. 【亲子线】只用真实亲子关系（父/母/儿/女）
 *    - 若孩子同时有父和母，且父母是夫妻：从父-母连线中点引出一条竖线到孩子
 *    - 若孩子只有一方父/母：直接从该父/母节点竖线连到孩子
 * 2. 【夫妻线】同代且关系是配偶/妻子/丈夫：水平短线连接
 * 3. 【兄弟姐妹线】不连（位置相邻已能体现关系，避免线太乱）
 *
 * 姑侄/伯侄等旁系关系：代际差=1但不是直接亲子，不画线
 */

const SPOUSE_DESCS = new Set(['配偶','妻子','丈夫'])
// 从 edge.fromUserId 视角看，edge.toUserId 是"我的孩子"
const IS_MY_CHILD  = new Set(['儿子','女儿'])
// 从 edge.fromUserId 视角看，edge.toUserId 是"我的父母"
const IS_MY_PARENT = new Set(['父亲','母亲'])

// 构建：childId → Set<parentId>（只含直接亲子关系）
const childParentsMap = computed(() => {
  const m = new Map()  // childId(str) → Set of parentId(str)
  const rawEdges = networkData.value?.edges || []
  for (const e of rawEdges) {
    const fromId = String(e.fromUserId)
    const toId   = String(e.toUserId)
    if (IS_MY_CHILD.has(e.relationDesc)) {
      // fromId 的孩子是 toId
      if (!m.has(toId)) m.set(toId, new Set())
      m.get(toId).add(fromId)
    } else if (IS_MY_PARENT.has(e.relationDesc)) {
      // fromId 的父母是 toId → fromId 是 toId 的孩子
      if (!m.has(fromId)) m.set(fromId, new Set())
      m.get(fromId).add(toId)
    }
  }
  return m
})

// ══ 亲子连线 ════════════════════════════════════════════════════════
const parentChildEdges = computed(() => {
  const result      = []
  const seenChild   = new Set()   // 已处理过的孩子节点（避免重复）
  const nm          = nodeMap.value
  const cpMap       = childParentsMap.value

  cpMap.forEach((parentIds, childId) => {
    if (seenChild.has(childId)) return
    seenChild.add(childId)

    const childNode = nm.get(childId)
    if (!childNode) return

    // 过滤：只保留在布局中存在的父节点
    const parentNodes = [...parentIds]
      .map(pid => nm.get(pid))
      .filter(Boolean)
    if (parentNodes.length === 0) return

    // 判断两个父节点是否互为配偶（若是，画"∩"型联合线）
    let drewJoint = false
    if (parentNodes.length >= 2) {
      for (let i = 0; i < parentNodes.length; i++) {
        for (let j = i + 1; j < parentNodes.length; j++) {
          const pA = parentNodes[i]
          const pB = parentNodes[j]
          const pAId = String(pA.id), pBId = String(pB.id)

          // 检查 pA 和 pB 是否是夫妻（spouseMap 里互相记录）
          if (spouseMap.value.get(pAId) !== pBId) continue

          // ── 父母联合连线 ──
          // 父母连线（水平段）的左右端点
          const leftP  = pA.x < pB.x ? pA : pB
          const rightP = pA.x < pB.x ? pB : pA
          const coupleY = (pA.y + pB.y) / 2
          const junctionX = (pA.x + pB.x) / 2   // 联结点 X（父母中点）
          const junctionY = coupleY               // 联结点 Y

          // 孩子顶部
          const cx = childNode.x
          const cy = childNode.y - childNode.r - 5

          // 竖线从联结点向下到孩子
          const dropY = junctionY + (cy - junctionY) * 0.5  // 折角高度

          // 路径：从联结点直接垂直到孩子（正交）
          let d
          if (Math.abs(cx - junctionX) < 3) {
            d = `M${junctionX},${junctionY} L${cx},${cy}`
          } else {
            d = `M${junctionX},${junctionY} L${junctionX},${dropY} L${cx},${dropY} L${cx},${cy}`
          }

          // 判断是否推断（取两条亲子边中任一为推断则标记）
          const isInferred = [...(networkData.value?.edges || [])]
            .filter(e => {
              const f = String(e.fromUserId), t = String(e.toUserId)
              return (f === pAId || f === pBId) && t === childId && IS_MY_CHILD.has(e.relationDesc)
                  || (f === childId) && (t === pAId || t === pBId) && IS_MY_PARENT.has(e.relationDesc)
            })
            .some(e => e.inferStatus === 2)

          result.push({
            d,
            color: isInferred ? '#fa8231' : '#667eea',
            width: isInferred ? 1.4 : 2,
            dash:  isInferred ? '5 3' : 'none',
            type: 'joint',
          })
          drewJoint = true
          // 标记这两个父节点已联合处理这个孩子
        }
      }
    }

    // 若没有找到配偶对，对每个父节点单独画竖线
    if (!drewJoint) {
      for (const pNode of parentNodes) {
        const px = pNode.x, py = pNode.y + pNode.r + 5
        const cx = childNode.x, cy = childNode.y - childNode.r - 5
        const midY = py + (cy - py) * 0.5
        const d = Math.abs(px - cx) < 3
          ? `M${px},${py} L${cx},${cy}`
          : `M${px},${py} L${px},${midY} L${cx},${midY} L${cx},${cy}`

        // 找对应 edge 的 inferStatus
        const matchEdge = (networkData.value?.edges || []).find(e => {
          const f = String(e.fromUserId), t = String(e.toUserId)
          return (f === String(pNode.id) && t === childId && IS_MY_CHILD.has(e.relationDesc))
              || (f === childId && t === String(pNode.id) && IS_MY_PARENT.has(e.relationDesc))
        })
        const isInferred = matchEdge?.inferStatus === 2

        result.push({
          d,
          color: isInferred ? '#fa8231' : '#667eea',
          width: isInferred ? 1.4 : 2,
          dash:  isInferred ? '5 3' : 'none',
          type: 'single',
        })
      }
    }
  })
  return result
})

// ══ 夫妻（同代）连线 ════════════════════════════════════════════════
// 规则：networkData.edges 中两端代际相同且关系是配偶类
const spouseEdges = computed(() => {
  const result   = []
  const seenPair = new Set()
  const rawEdges = networkData.value?.edges || []
  const gm       = genMap.value

  for (const edge of rawEdges) {
    if (!SPOUSE_DESCS.has(edge.relationDesc)) continue
    const aId  = String(edge.fromUserId)
    const bId  = String(edge.toUserId)
    const genA = gm.get(aId)
    const genB = gm.get(bId)
    if (genA === undefined || genB === undefined) continue
    if (Math.abs(genA - genB) > 0) continue  // 严格同代才连横线

    const pairKey = [aId, bId].sort().join('|')
    if (seenPair.has(pairKey)) continue
    seenPair.add(pairKey)

    const aNode = nodeMap.value.get(aId)
    const bNode = nodeMap.value.get(bId)
    if (!aNode || !bNode) continue

    const leftNode  = aNode.x < bNode.x ? aNode : bNode
    const rightNode = aNode.x < bNode.x ? bNode  : aNode
    const lx = leftNode.x  + leftNode.r  + 4
    const rx = rightNode.x - rightNode.r - 4
    if (rx <= lx) continue  // 节点重叠时不画

    const lineY = (aNode.y + bNode.y) / 2
    const d = `M${lx},${lineY} L${rx},${lineY}`

    const isInferred = edge.inferStatus === 2
    result.push({
      d,
      color: isInferred ? '#fa8231' : '#e040fb',
      width: isInferred ? 1.4 : 2,
      dash:  isInferred ? '5 3' : '4 2',
    })
  }
  return result
})
// ══ 选中节点 ══════════════════════════════════════════
const selectedNode = computed(() =>
  layoutNodes.value.find(n => n.id === selectedId.value) || null)
const selectNode = n => { selectedId.value = n.id === selectedId.value ? null : n.id }
const deselect   = () => { selectedId.value = null }

// ══ 列表分组 ══════════════════════════════════════════
const groupedRelations = computed(() => {
  const groups = [
    { label: '长辈',  test: r => guessGen(r.relationDesc) > 0 },
    { label: '配偶',  test: r => ['配偶','妻子','丈夫'].includes(r.relationDesc) },
    { label: '同辈',  test: r => guessGen(r.relationDesc) === 0 && !['配偶','妻子','丈夫'].includes(r.relationDesc) },
    { label: '晚辈',  test: r => guessGen(r.relationDesc) < 0 },
  ]
  const result = [], used = new Set()
  groups.forEach(g => {
    const items = directRelations.value.filter(r => g.test(r))
    if (items.length) { result.push({ label: g.label, items }); items.forEach(i => used.add(i.relatedUserId)) }
  })
  const others = directRelations.value.filter(r => !used.has(r.relatedUserId))
  if (others.length) result.push({ label: '其他', items: others })
  return result
})

// ══ 工具 ══════════════════════════════════════════════
const lifeTagType = s => ({0:'success',1:'info',2:'warning',3:'danger'})[s] || 'info'
const lifeLabel   = s => ({0:'活跃',1:'不活跃',2:'疑似离世',3:'已离世'})[s] || '未知'

// ══ 亲属详情抽屉 ══════════════════════════════════════
const profileDrawerVisible = ref(false)
const profileTargetId      = ref(null)
const profileRelationDesc  = ref('')
const openProfile = (userId, desc) => {
  const myId = authStore.userInfo?.id
  if (!userId || String(userId) === String(myId)) return
  profileTargetId.value     = userId
  profileRelationDesc.value = desc || ''
  profileDrawerVisible.value = true
}

// ══ 数据加载 ══════════════════════════════════════════
const loadData = async () => {
  loading.value = true
  try {
    const [relations, network] = await Promise.all([
      relationApi.getMyRelations(),
      relationApi.getRelationNetwork(),
    ])
    directRelations.value = relations || []
    networkData.value     = network  || { nodes: [], edges: [] }


  } catch (e) { console.error('[FamilyTree] 加载失败:', e) }
  finally { loading.value = false }
}

// ══ 全量重推 ══════════════════════════════════════════
const reInferRunning = ref(false)
const reInferDone    = ref(false)
const reInferStatus  = ref({ status: 'idle', progress: 0, message: '', result: null })
let reInferJobId = null, reInferTimer = null

const startReInfer = async () => {
  if (reInferRunning.value) return
  reInferRunning.value = true
  reInferDone.value    = false
  reInferStatus.value  = { status: 'running', progress: 0, message: '正在启动推断任务...' }
  clearInterval(reInferTimer)
  try {
    const res = await relationApi.startFullReInfer()
    reInferJobId = res.jobId
    ElMessage.info('推断任务已启动，正在后台处理...')
    reInferTimer = setInterval(pollReInferStatus, 1500)
  } catch (e) {
    reInferRunning.value = false; reInferDone.value = true
    reInferStatus.value  = { status: 'error', message: '启动失败：' + (e?.message || '网络错误') }
    ElMessage.error('推断任务启动失败')
  }
}
const pollReInferStatus = async () => {
  if (!reInferJobId) return
  try {
    const st = await relationApi.getReInferStatus(reInferJobId)
    reInferStatus.value = st
    if (st.status === 'done' || st.status === 'error') {
      clearInterval(reInferTimer)
      reInferRunning.value = false; reInferDone.value = true
      st.status === 'done'
        ? ElMessage.success('关系推断完成！点击「刷新树」查看最新结果')
        : ElMessage.error('推断过程出现错误：' + st.message)
    }
  } catch (e) { console.warn('轮询失败:', e) }
}
const dismissReInfer = () => {
  clearInterval(reInferTimer)
  reInferRunning.value = false; reInferDone.value = false
  reInferStatus.value  = { status: 'idle' }
}

onMounted(loadData)
onBeforeUnmount(() => clearInterval(reInferTimer))
</script>

<style scoped>
.family-tree-page { max-width: 1060px; }

/* ── 重推进度条 ── */
.reinfer-bar {
  background: linear-gradient(135deg, rgba(90,103,242,.06), rgba(245,158,11,.04));
  border-bottom: 1px solid var(--c-border);
  padding: 10px 20px; margin: -20px -20px 16px;
}
.reinfer-bar-inner { display:flex; align-items:center; gap:12px; }
.reinfer-icon { width:28px; height:28px; flex-shrink:0; display:flex; align-items:center; justify-content:center; border-radius:50%; font-size:16px; }
.reinfer-icon.running { color: var(--c-primary); }
.reinfer-icon.done    { color: var(--c-success); }
.reinfer-icon.error   { color: var(--c-danger); }
.reinfer-info { flex:1; min-width:0; }
.reinfer-msg  { font-size:13px; font-weight:600; color:var(--c-txt); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.reinfer-result { font-size:12px; color:var(--c-txt-s); margin-top:4px; }
.reinfer-actions { display:flex; align-items:center; gap:4px; flex-shrink:0; }
.slide-down-enter-active, .slide-down-leave-active { transition: max-height .3s ease, opacity .25s ease, padding .3s ease; overflow:hidden; }
.slide-down-enter-from, .slide-down-leave-to { max-height:0; opacity:0; padding-top:0; padding-bottom:0; }
.slide-down-enter-to, .slide-down-leave-from { max-height:100px; opacity:1; }

/* ── Card ── */
:deep(.el-card) { border-radius:var(--radius-md) !important; border:1px solid var(--c-border) !important; box-shadow:var(--shadow-sm) !important; }
:deep(.el-card__header) { background:#F8FAFC; border-bottom:1px solid var(--c-border); padding:14px 20px; }
.card-header { display:flex; justify-content:space-between; align-items:center; }

/* ── 图例 ── */
.legend-bar {
  display:flex; flex-wrap:wrap; gap:14px; align-items:center;
  margin-bottom:14px; font-size:12px; color:var(--c-txt-s);
  padding:10px 14px; background:#F8FAFC;
  border-radius:var(--radius-sm); border:1px solid var(--c-border);
}
.legend-item { display:flex; align-items:center; gap:5px; font-weight:500; }
.dot { width:10px; height:10px; border-radius:50%; }
.legend-sep { width:1px; height:16px; background:var(--c-border); }
.legend-tip { display:flex; align-items:center; gap:6px; color:var(--c-txt-i); }
.edge-solid { display:inline-block; width:22px; height:2px; background:#667eea; border-radius:2px; }
.edge-dash  { display:inline-block; width:22px; height:0; border-top:2px dashed #F59E0B; }

/* ── SVG ── */
.tree-wrapper { position:relative; }
.svg-scroll { overflow:auto; max-height:70vh; border-radius:var(--radius-sm); border:1px solid var(--c-border); }
svg { display:block; background:linear-gradient(160deg, #F8FAFF 0%, #EEF2FF 100%); }
.node-g { cursor:pointer; transition:opacity .15s; }
.node-g:hover { opacity:.82; }

/* ── 详情卡 ── */
.detail-card {
  position:absolute; right:0; top:0; width:220px;
  background:rgba(255,255,255,.97); backdrop-filter:blur(12px);
  border-radius:var(--radius-md); border:1px solid var(--c-border);
  box-shadow:var(--shadow-lg); padding:16px; z-index:20;
}
.detail-close {
  position:absolute; right:10px; top:10px;
  background:none; border:none; cursor:pointer;
  font-size:14px; color:var(--c-txt-i); padding:2px 6px;
  border-radius:4px; line-height:1;
}
.detail-close:hover { background:#f0f0f0; color:var(--c-txt); }
.detail-header { display:flex; align-items:center; gap:10px; margin-bottom:10px; padding-right:20px; }
.detail-name { font-size:15px; font-weight:800; color:var(--c-txt); line-height:1.3; margin-bottom:4px; }
.slide-fade-enter-active, .slide-fade-leave-active { transition:all .22s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { opacity:0; transform:translateX(14px); }

/* ── 列表视图 ── */
.list-view { display:flex; flex-direction:column; gap:20px; }
.group-title { font-size:12px; color:var(--c-txt-s); font-weight:700; margin-bottom:10px; padding-bottom:6px; border-bottom:2px solid var(--c-border); text-transform:uppercase; letter-spacing:.5px; }
.group-items { display:flex; flex-wrap:wrap; gap:10px; }
.list-card { display:flex; align-items:center; gap:10px; background:var(--c-surface); border-radius:var(--radius-sm); padding:12px 16px; min-width:190px; border:1px solid var(--c-border); box-shadow:var(--shadow-sm); transition:var(--transition); }
.list-card:hover { box-shadow:var(--shadow-md); border-color:var(--c-primary); transform:translateY(-1px); }
.list-name { font-weight:700; font-size:13px; color:var(--c-txt); margin-bottom:4px; }

@media (max-width: 768px) {
  .family-tree-page { max-width:100%; }
  :deep(.el-card__header) { padding:10px 12px; }
  :deep(.el-card__body)   { padding:12px; }
  .legend-bar { gap:8px; padding:8px 10px; font-size:11px; }
  .legend-sep, .legend-tip { display:none; }
  .svg-scroll { max-height:55vh; }
  .detail-card { position:fixed !important; right:0 !important; left:0 !important; top:auto !important; bottom:calc(56px + env(safe-area-inset-bottom,0px)) !important; width:100% !important; border-radius:var(--radius-lg) var(--radius-lg) 0 0 !important; max-height:45vh; overflow-y:auto; box-shadow:0 -8px 32px rgba(0,0,0,.15); z-index:90; }
  .list-card { min-width:0; flex:1 1 calc(50% - 5px); box-sizing:border-box; }
}
@media (max-width: 480px) { .list-card { flex:1 1 100%; } }
</style>
