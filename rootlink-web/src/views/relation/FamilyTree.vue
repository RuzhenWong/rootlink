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
                  修正性别错误（如母子被推断成父子）<br>
                  自动补全缺失的推断关系
                </div>
              </template>
              <el-button
                type="primary" size="small"
                :loading="reInferRunning"
                :disabled="reInferRunning"
                @click="startReInfer"
                style="border-radius:8px"
              >
                <el-icon v-if="!reInferRunning" style="margin-right:4px"><MagicStick /></el-icon>
                {{ reInferRunning ? '推断中...' : '重新推断' }}
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 全量重推进度条（内嵌卡片顶部） -->
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
              <el-progress
                v-if="reInferStatus.status === 'running'"
                :percentage="reInferStatus.progress || 0"
                :stroke-width="4"
                :show-text="false"
                style="margin-top:4px"
              />
              <div v-if="reInferStatus.status === 'done' && reInferStatus.result" class="reinfer-result">
                👥 {{ reInferStatus.result.networkSize }} 位成员 &nbsp;
                🔗 {{ reInferStatus.result.manualEdges }} 条原始关系 &nbsp;
                ✨ 新推断 {{ reInferStatus.result.newInferred }} 条
              </div>
            </div>
            <div class="reinfer-actions">
              <el-button
                v-if="reInferStatus.status === 'done'"
                type="primary" size="small" text
                @click="loadData(); reInferDone = false"
              >刷新树</el-button>
              <el-button size="small" text @click="dismissReInfer">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </transition>

      <div v-if="loading" v-loading="true" style="height:400px" />

      <el-empty
        v-else-if="directRelations.length === 0"
        description="暂无亲属关系，去「亲属关系」页面添加"
        :image-size="120"
      >
        <el-button type="primary" @click="$router.push('/relations')">去添加</el-button>
      </el-empty>

      <template v-else>
        <!-- =========== 关系图 =========== -->
        <div v-if="viewMode === 'tree'" class="tree-wrapper">
          <!-- 图例 -->
          <div class="legend-bar">
            <span v-for="(v,k) in COLORS" :key="k" class="legend-item">
              <span class="dot" :style="{ background: v.node }" />{{ k }}
            </span>
            <span class="legend-sep" />
            <span class="legend-tip">
              <span class="edge-solid" />实线=直系血亲/配偶 &nbsp;
              <span class="edge-dash" />虚线=推断关系
            </span>
          </div>

          <div class="svg-wrap" ref="svgWrap">
            <svg :width="svgW" :height="svgH" @click="deselect">
              <!-- ---- 连线层 ---- -->
              <g>
                <g v-for="(e, i) in edges" :key="'e' + i" class="edge-g">
                  <path
                    :d="e.d"
                    :stroke="e.color"
                    :stroke-width="e.width"
                    :stroke-dasharray="e.dash"
                    fill="none"
                    opacity="0.75"
                  />
                  <!-- 关系标注 -->
                  <text
                    v-if="e.label"
                    :x="e.mx" :y="e.my"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    :font-size="10"
                    :fill="e.color"
                    class="edge-label"
                    :transform="`rotate(${e.angle},${e.mx},${e.my})`"
                  >{{ e.label }}</text>
                </g>
              </g>

              <!-- ---- 节点层 ---- -->
              <g
                v-for="node in layoutNodes"
                :key="node.id"
                :transform="`translate(${node.x},${node.y})`"
                class="node-g"
                @click.stop="selectNode(node); openProfile(node.id, node.sublabel)"
              >
                <!-- 选中高亮圈 -->
                <circle
                  v-if="selectedId === node.id"
                  :r="node.r + 5"
                  fill="none"
                  stroke="#409eff"
                  stroke-width="2.5"
                  stroke-dasharray="4 2"
                />
                <!-- 主圆 -->
                <circle
                  :r="node.r"
                  :fill="node.fill"
                  :stroke="node.isMe ? '#fff' : 'none'"
                  :stroke-width="node.isMe ? 3 : 0"
                />
                <!-- 离世标记 -->
                <text v-if="node.lifeStatus === 3"
                  :x="node.r - 4" :y="-node.r + 8"
                  font-size="12" fill="#ff4d4f">✝</text>
                <!-- 姓名缩写 -->
                <text
                  x="0" y="1"
                  text-anchor="middle"
                  dominant-baseline="middle"
                  :font-size="node.isMe ? 13 : 12"
                  font-weight="700"
                  fill="#fff"
                  style="pointer-events:none; user-select:none"
                >{{ node.abbr }}</text>
                <!-- 节点下方：姓名 -->
                <text
                  x="0" :y="node.r + 15"
                  text-anchor="middle"
                  font-size="12"
                  :fill="node.anon ? '#bbb' : '#303133'"
                  style="pointer-events:none"
                >{{ node.label }}</text>
                <!-- 节点下方：与"我"的关系 -->
                <text
                  x="0" :y="node.r + 27"
                  text-anchor="middle"
                  font-size="10"
                  fill="#909399"
                  style="pointer-events:none"
                >{{ node.sublabel }}</text>
              </g>
            </svg>
          </div>

          <!-- 节点详情卡片 -->
          <transition name="slide-fade">
            <div v-if="selectedNode" class="detail-card">
              <div class="detail-header">
                <el-avatar :size="44" icon="UserFilled" :style="{ background: selectedNode.fill }" />
                <div>
                  <div class="detail-name">{{ selectedNode.label }}</div>
                  <el-tag size="small" :style="{ background: selectedNode.fill, color: '#fff', border: 'none' }">
                    {{ selectedNode.isMe ? '本人' : selectedNode.sublabel }}
                  </el-tag>
                  <el-tag
                    size="small"
                    :type="lifeTagType(selectedNode.lifeStatus)"
                    style="margin-left:4px"
                  >{{ lifeLabel(selectedNode.lifeStatus) }}</el-tag>
                </div>
              </div>
              <div class="detail-relations">
                <div class="detail-rel-title">与其他人的关系：</div>
                <div v-for="e in edgesOfSelected" :key="e.key" class="detail-rel-item">
                  <span class="rel-name">{{ e.otherLabel }}</span>
                  <el-tag size="small" type="info">{{ e.label }}</el-tag>
                </div>
                <div v-if="edgesOfSelected.length === 0" class="no-rel">暂无已标注的关系</div>
              </div>
              <div style="margin-top:10px; display:flex; gap:6px; flex-wrap:wrap">
                <el-button size="small" type="primary" plain
                  @click="openProfile(selectedNode.id, selectedNode.sublabel)">
                  查看详情
                </el-button>
                <el-button v-if="selectedNode.lifeStatus === 3"
                  size="small" type="info" plain
                  @click="$router.push('/eulogy/wall')">查看挽联</el-button>
              </div>
            </div>
          </transition>
        </div>

        <!-- =========== 列表视图 =========== -->
        <div v-else class="list-view">
          <div v-for="group in groupedRelations" :key="group.label" class="relation-group">
            <div class="group-title">{{ group.label }}（{{ group.items.length }}）</div>
            <div class="group-items">
              <div v-for="r in group.items" :key="r.relatedUserId" class="list-card" style="cursor:pointer" @click="openProfile(r.relatedUserId, r.relationDesc)">
                <el-avatar :size="36" icon="UserFilled" />
                <div class="list-info">
                  <div class="list-name">{{ r.realName || '未知' }}</div>
                  <el-tag size="small" type="primary">{{ r.relationDesc }}</el-tag>
                  <el-tag v-if="r.lifeStatus === 3" size="small" type="danger" style="margin-left:4px">已离世</el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-card>

    <!-- 亲属详情抽屉 -->
    <RelativeProfileDrawer v-model="profileDrawerVisible"
      :userId="profileTargetId" :relationDesc="profileRelationDesc" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, MagicStick, Loading, CircleCheck, CircleClose, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { relationApi } from '@/api/relation'
import { useAuthStore } from '@/stores/auth'
import RelativeProfileDrawer from '@/components/RelativeProfileDrawer.vue'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const directRelations = ref([])  // getMyRelations 返回的原始数据
const networkData     = ref({ nodes: [], edges: [] })  // 全网关系数据
const viewMode = ref('tree')
const selectedId = ref(null)
const svgWrap = ref(null)

// ================================================================
// 颜色配置
// ================================================================
const COLORS = {
  '本人':     { node: '#303133' },
  '长辈':     { node: '#667eea' },
  '配偶':     { node: '#e040fb' },
  '同辈':     { node: '#26c6da' },
  '晚辈':     { node: '#42a5f5' },
}

const EDGE_COLORS = {
  direct:   '#667eea',   // 直系（父母子女）
  spouse:   '#e040fb',   // 配偶
  sibling:  '#26c6da',   // 同辈
  inferred: '#fa8231',   // 推断关系
}

function nodeColor(desc) {
  if (!desc) return COLORS['同辈'].node
  const g = guessGen(desc)
  if (g > 0) return COLORS['长辈'].node
  if (g < 0) return COLORS['晚辈'].node
  if (desc === '配偶') return COLORS['配偶'].node
  return COLORS['同辈'].node
}

// ================================================================
// 代数判断
// ================================================================
function guessGen(desc) {
  if (!desc) return 0
  const up = new Set(['父亲','母亲','爷爷','奶奶','外公','外婆','曾祖父','曾祖母','外曾祖父','外曾祖母'])
  const down = new Set(['儿子','女儿','孙子','孙女','外孙','外孙女','曾孙','曾孙女'])
  const up2 = new Set(['爷爷','奶奶','外公','外婆'])
  const up3 = new Set(['曾祖父','曾祖母','外曾祖父','外曾祖母'])
  const down2 = new Set(['孙子','孙女','外孙','外孙女'])
  if (up3.has(desc))  return 3
  if (up2.has(desc))  return 2
  if (up.has(desc))   return 1
  if (down2.has(desc))return -2
  if (down.has(desc)) return -1
  return 0
}

function isParentDesc(desc) {
  return new Set(['父亲','母亲']).has(desc)
}
function isSiblingDesc(desc) {
  return new Set(['哥哥','弟弟','姐姐','妹妹','堂哥','堂弟','堂姐','堂妹','表哥','表弟','表姐','表妹']).has(desc)
}
function isChildDesc(desc) {
  return new Set(['儿子','女儿']).has(desc)
}
function isSpouseDesc(desc) {
  return desc === '配偶'
}

// ================================================================
// SVG 布局参数
// ================================================================
const SVG_PAD   = 60    // 四周边距
const NODE_R_ME = 32    // 我的半径
const NODE_R    = 26    // 普通节点半径
const GEN_H     = 130   // 代际垂直间距
const MIN_H_GAP = 120   // 同代节点最小水平间距
const ROW_BASE_Y = 90   // 第一行（最高代）Y 起始

const svgW = ref(860)
const svgH = ref(520)

// ================================================================
// 所有节点（含"我"）
// ================================================================
const allNodes = computed(() => {
  const me = authStore.userInfo
  const nodes = [{
    id: 'me',
    label: me?.realName || '我',
    abbr:  (me?.realName || '我').slice(0, 2),
    sublabel: '本人',
    lifeStatus: me?.lifeStatus ?? 0,
    generation: 0,
    fill: COLORS['本人'].node,
    r: NODE_R_ME,
    isMe: true,
    anon: false,
    phone: me?.phone,
  }]
  directRelations.value.forEach(r => {
    nodes.push({
      id: r.relatedUserId,
      label: r.realName || '未知',
      abbr:  (r.realName || '?').slice(0, 2),
      sublabel: r.relationDesc,
      lifeStatus: r.lifeStatus,
      generation: guessGen(r.relationDesc),
      fill: nodeColor(r.relationDesc),
      r: NODE_R,
      isMe: false,
      anon: false,
      phone: r.phone,
    })
  })
  return nodes
})

// ================================================================
// 布局计算
// ================================================================
const layoutNodes = computed(() => {
  // 按代分组
  const byGen = {}
  allNodes.value.forEach(n => {
    const g = n.generation
    if (!byGen[g]) byGen[g] = []
    byGen[g].push(n)
  })

  const gens = Object.keys(byGen).map(Number).sort((a, b) => b - a)
  const totalRows = gens.length
  const maxInRow = Math.max(...Object.values(byGen).map(a => a.length))

  // 计算 SVG 宽高
  const calcW = Math.max(860, maxInRow * MIN_H_GAP + SVG_PAD * 2)
  const calcH = Math.max(480, totalRows * GEN_H + SVG_PAD * 2 + 60)
  svgW.value = calcW
  svgH.value = calcH

  const centerX = calcW / 2
  const result = []

  gens.forEach((gen, rowIdx) => {
    const nodes = byGen[gen]
    const count = nodes.length
    const y = ROW_BASE_Y + rowIdx * GEN_H

    // 特殊排序：同代中，"我"居中，配偶放右，兄弟姐妹放左
    const sorted = sortSameGenNodes(nodes)

    const totalSpan = (count - 1) * MIN_H_GAP
    const startX = centerX - totalSpan / 2

    sorted.forEach((n, i) => {
      const x = count === 1 ? centerX : startX + i * MIN_H_GAP
      result.push({ ...n, x, y })
    })
  })

  return result
})

// 同辈节点排序：我居中，配偶紧靠右，同辈亲属靠左
function sortSameGenNodes(nodes) {
  const me      = nodes.filter(n => n.isMe)
  const spouses = nodes.filter(n => !n.isMe && isSpouseDesc(n.sublabel))
  const sibs    = nodes.filter(n => !n.isMe && isSiblingDesc(n.sublabel))
  const others  = nodes.filter(n => !n.isMe && !isSpouseDesc(n.sublabel) && !isSiblingDesc(n.sublabel))
  // 排列：同辈... | 我 | 配偶... | 其他
  return [...sibs, ...me, ...spouses, ...others]
}

// 通过 id 快速找节点坐标
function nodeById(id) {
  return layoutNodes.value.find(n => n.id === id) || null
}

// ================================================================
// 连线计算（核心：所有节点对之间的关系）
// ================================================================

/**
 * 计算两节点间直线中点坐标 + 角度（用于 label 旋转）
 */
function edgeMeta(from, to) {
  const mx = (from.x + to.x) / 2
  const my = (from.y + to.y) / 2
  const dx = to.x - from.x
  const dy = to.y - from.y
  let angle = Math.atan2(dy, dx) * 180 / Math.PI
  // 保持 label 正方向
  if (angle > 90 || angle < -90) angle += 180
  return { mx, my, angle }
}

/**
 * 两点之间的曲线路径（贝塞尔）
 */
function curvePath(from, to) {
  const dx = to.x - from.x
  const dy = to.y - from.y
  // 同代（水平线）：小弧度；跨代（垂直差大）：S 曲线
  if (Math.abs(dy) < 20) {
    // 同代：直线略弧
    const cx = (from.x + to.x) / 2
    const cy = Math.min(from.y, to.y) - 30
    return `M${from.x},${from.y} Q${cx},${cy} ${to.x},${to.y}`
  }
  const c1x = from.x, c1y = from.y + dy * 0.4
  const c2x = to.x,   c2y = to.y   - dy * 0.4
  return `M${from.x},${from.y} C${c1x},${c1y} ${c2x},${c2y} ${to.x},${to.y}`
}

const edges = computed(() => {
  const result     = []
  const seenPairs  = new Set()
  const myActualId = authStore.userInfo?.id   // 当前登录用户的真实 userId

  // 找 layout 节点（处理 'me' 特殊 id）
  function findNode(userId) {
    const uid = String(userId)
    const me  = layoutNodes.value.find(n => n.isMe)
    if (me && String(myActualId) === uid) return me
    return layoutNodes.value.find(n => String(n.id) === uid) || null
  }

  // 判断连线类型
  function edgeType(desc, inferStatus) {
    if (!desc) return 'inferred'
    if (isSpouseDesc(desc))  return 'spouse'
    if (isSiblingDesc(desc)) return 'sibling'
    // 直系血亲（含祖孙、曾祖等）+ 人工确认（inferStatus=0）均视为 direct 实线
    if (inferStatus === 0)   return 'direct'
    return 'inferred'
  }

  function addEdge(from, to, label, type, dash) {
    if (!from || !to) return
    const meta = edgeMeta(from, to)
    const colorMap = {
      direct:   EDGE_COLORS.direct,
      spouse:   EDGE_COLORS.spouse,
      sibling:  EDGE_COLORS.sibling,
      inferred: EDGE_COLORS.inferred,
    }
    result.push({
      d:     curvePath(from, to),
      color: colorMap[type] || EDGE_COLORS.inferred,
      width: (type === 'direct' || type === 'spouse') ? 2.0 : 1.4,
      dash,
      label,
      mx: meta.mx, my: meta.my, angle: meta.angle,
      fromId: from.id, toId: to.id,
      key: `${from.id}-${to.id}`,
    })
  }

  // ── 数据驱动：遍历后端返回的全网关系边 ──────────────────────────
  // networkData.edges 包含双向记录（A→B 和 B→A 各一条），
  // 用 seenPairs 去重，每对节点只画一条线。
  // 优先取 inferStatus=0（人工确认）的方向作为标签展示方向。
  const rawEdges = networkData.value?.edges || []

  // 先按 inferStatus 升序排（0=人工确认 优先），保证去重时优先保留人工确认边的标签
  const sortedEdges = [...rawEdges].sort((a, b) => a.inferStatus - b.inferStatus)

  for (const edge of sortedEdges) {
    const aId = edge.fromUserId
    const bId = edge.toUserId
    // 去重 key（无方向，双向只画一条）
    const pairKey = Math.min(aId, bId) + '-' + Math.max(aId, bId)
    if (seenPairs.has(pairKey)) continue
    seenPairs.add(pairKey)

    const fromNode = findNode(aId)
    const toNode   = findNode(bId)
    if (!fromNode || !toNode) continue   // 节点不在当前布局中（超出可见范围）

    const type = edgeType(edge.relationDesc, edge.inferStatus)
    // 虚线规则：配偶/同辈/直系确认 → 实线；推断 → 虚线
    const dash = (type === 'spouse' || type === 'sibling' || edge.inferStatus === 0) ? 'none' : '5 3'

    addEdge(fromNode, toNode, edge.relationDesc, type, dash)
  }

  return result
})
// ================================================================
// 工具方法
// ================================================================
function siblingLabel(desc) {
  const map = {
    '哥哥':'兄弟', '弟弟':'兄弟', '姐姐':'姐弟', '妹妹':'兄妹',
    '堂哥':'堂兄弟', '堂弟':'堂兄弟', '堂姐':'堂姐弟', '堂妹':'堂兄妹',
    '表哥':'表兄弟', '表弟':'表兄弟', '表姐':'表姐弟', '表妹':'表兄妹',
  }
  return map[desc] || '同辈'
}

// ================================================================
// 选中节点
// ================================================================
const selectedNode = computed(() => layoutNodes.value.find(n => n.id === selectedId.value) || null)
const selectNode = (n) => { selectedId.value = n.id === selectedId.value ? null : n.id }
const deselect = () => { selectedId.value = null }

// 选中节点的所有连线
const edgesOfSelected = computed(() => {
  if (!selectedNode.value) return []
  const id = selectedNode.value.id
  return edges.value
    .filter(e => e.fromId === id || e.toId === id)
    .map(e => {
      const otherId = e.fromId === id ? e.toId : e.fromId
      const other = layoutNodes.value.find(n => n.id === otherId)
      return { label: e.label, otherLabel: other?.label || '?', key: e.key }
    })
})

// ================================================================
// 列表分组
// ================================================================
const groupedRelations = computed(() => {
  const groups = [
    { label: '长辈',  test: n => guessGen(n.relationDesc) > 0 },
    { label: '配偶',  test: n => n.relationDesc === '配偶' },
    { label: '同辈',  test: n => guessGen(n.relationDesc) === 0 && n.relationDesc !== '配偶' },
    { label: '晚辈',  test: n => guessGen(n.relationDesc) < 0 },
  ]
  const result = []
  const used = new Set()
  groups.forEach(g => {
    const items = directRelations.value.filter(r => g.test(r))
    if (items.length) {
      result.push({ label: g.label, items })
      items.forEach(i => used.add(i.relatedUserId))
    }
  })
  const others = directRelations.value.filter(r => !used.has(r.relatedUserId))
  if (others.length) result.push({ label: '其他', items: others })
  return result
})

// ================================================================
// 工具标签
// ================================================================
const lifeTagType = s => ({ 0:'success',1:'info',2:'warning',3:'danger' })[s] || 'info'
const lifeLabel   = s => ({ 0:'活跃',1:'不活跃',2:'疑似离世',3:'已离世' })[s] || '未知'

// ================================================================
// 数据加载
// ================================================================
const loadData = async () => {
  loading.value = true
  try {
    const [relations, network] = await Promise.all([
      relationApi.getMyRelations(),
      relationApi.getRelationNetwork(),
    ])
    directRelations.value = relations || []
    networkData.value     = network  || { nodes: [], edges: [] }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

// ── 亲属详情抽屉 ─────────────────────────
const profileDrawerVisible = ref(false)
const profileTargetId = ref(null)
const profileRelationDesc = ref('')
const openProfile = (userId, relationDesc) => {
  if (!userId || userId === 'me') return
  profileTargetId.value = userId
  profileRelationDesc.value = relationDesc || ''
  profileDrawerVisible.value = true
}

// ================================================================
// 全量重推（重新分析家族网络关系）
// ================================================================
const reInferRunning  = ref(false)
const reInferDone     = ref(false)
const reInferStatus   = ref({ status: 'idle', progress: 0, message: '', result: null })
let   reInferJobId    = null
let   reInferTimer    = null

// 启动全量重推任务
const startReInfer = async () => {
  if (reInferRunning.value) return
  reInferRunning.value = true
  reInferDone.value    = false
  reInferStatus.value  = { status: 'running', progress: 0, message: '正在启动推断任务...' }
  reInferJobId         = null
  clearInterval(reInferTimer)

  try {
    const res = await relationApi.startFullReInfer()
    reInferJobId = res.jobId
    ElMessage.info('推断任务已启动，正在后台处理...')
    // 每 1.5 秒轮询一次进度
    reInferTimer = setInterval(pollReInferStatus, 1500)
  } catch (e) {
    reInferRunning.value = false
    reInferDone.value    = true
    reInferStatus.value  = { status: 'error', progress: 0, message: '启动失败：' + (e?.message || '网络错误') }
    ElMessage.error('推断任务启动失败')
  }
}

// 轮询进度
const pollReInferStatus = async () => {
  if (!reInferJobId) return
  try {
    const st = await relationApi.getReInferStatus(reInferJobId)
    reInferStatus.value = st

    if (st.status === 'done') {
      clearInterval(reInferTimer)
      reInferRunning.value = false
      reInferDone.value    = true
      ElMessage.success('关系推断完成！点击「刷新树」查看最新结果')
    } else if (st.status === 'error') {
      clearInterval(reInferTimer)
      reInferRunning.value = false
      reInferDone.value    = true
      ElMessage.error('推断过程中出现错误：' + st.message)
    }
  } catch (e) {
    // 网络抖动不停轮询，连续失败 3 次才放弃（简化：忽略单次失败）
    console.warn('轮询状态失败:', e)
  }
}

// 关闭进度条
const dismissReInfer = () => {
  clearInterval(reInferTimer)
  reInferRunning.value = false
  reInferDone.value    = false
  reInferStatus.value  = { status: 'idle', progress: 0, message: '' }
}

onMounted(loadData)
onBeforeUnmount(() => clearInterval(reInferTimer))
</script>

<style scoped>
.family-tree-page { max-width: 1060px; }

/* ── 全量重推进度条 ── */
.reinfer-bar {
  background: linear-gradient(135deg, rgba(90,103,242,.06) 0%, rgba(245,158,11,.04) 100%);
  border-bottom: 1px solid var(--c-border);
  padding: 10px 20px;
  margin: -20px -20px 16px;   /* 撑满 el-card__body 的 padding */
}
.reinfer-bar-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}
.reinfer-icon {
  width: 28px; height: 28px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  font-size: 16px;
}
.reinfer-icon.running { color: var(--c-primary); }
.reinfer-icon.done    { color: var(--c-success); }
.reinfer-icon.error   { color: var(--c-danger); }
.reinfer-info { flex: 1; min-width: 0; }
.reinfer-msg  { font-size: 13px; font-weight: 600; color: var(--c-txt); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.reinfer-result { font-size: 12px; color: var(--c-txt-s); margin-top: 4px; }
.reinfer-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }

/* 进度条展开动画 */
.slide-down-enter-active, .slide-down-leave-active {
  transition: max-height .3s ease, opacity .25s ease, padding .3s ease;
  overflow: hidden;
}
.slide-down-enter-from, .slide-down-leave-to {
  max-height: 0; opacity: 0; padding-top: 0; padding-bottom: 0;
}
.slide-down-enter-to, .slide-down-leave-from { max-height: 100px; opacity: 1; }

/* ── Card 覆盖 ── */
:deep(.el-card) {
  border-radius: var(--radius-md) !important;
  border: 1px solid var(--c-border) !important;
  box-shadow: var(--shadow-sm) !important;
}
:deep(.el-card__header) {
  background: #F8FAFC;
  border-bottom: 1px solid var(--c-border);
  padding: 14px 20px;
}

.card-header { display: flex; justify-content: space-between; align-items: center; }

/* ── 图例 ── */
.legend-bar {
  display: flex; flex-wrap: wrap; gap: 14px; align-items: center;
  margin-bottom: 14px; font-size: 12px; color: var(--c-txt-s);
  padding: 10px 14px; background: #F8FAFC; border-radius: var(--radius-sm);
  border: 1px solid var(--c-border);
}
.legend-item { display: flex; align-items: center; gap: 5px; font-weight: 500; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.legend-sep { width: 1px; height: 16px; background: var(--c-border); }
.legend-tip { display: flex; align-items: center; gap: 6px; color: var(--c-txt-i); }
.edge-solid { display: inline-block; width: 22px; height: 2px; background: #5A67F2; border-radius: 2px; }
.edge-dash  { display: inline-block; width: 22px; height: 0; border-top: 2px dashed #F59E0B; }

/* ── SVG 区域 ── */
.tree-wrapper { position: relative; }
.svg-wrap { overflow-x: auto; overflow-y: hidden; border-radius: var(--radius-sm); }
svg {
  display: block;
  background: linear-gradient(135deg, #F8FAFF 0%, #F1F5FF 100%);
  border-radius: var(--radius-sm);
  border: 1px solid var(--c-border);
}

/* ── 节点 ── */
.node-g { cursor: pointer; transition: opacity .15s; }
.node-g:hover { opacity: .88; }
.edge-label {
  font-weight: 700;
  paint-order: stroke;
  stroke: rgba(248,250,255,.9);
  stroke-width: 3px;
}

/* ── 详情卡片 ── */
.detail-card {
  position: absolute; right: 0; top: 0;
  width: 230px;
  background: rgba(255,255,255,.97);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-md);
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-lg);
  padding: 16px;
  z-index: 20;
}
.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.detail-name { font-size: 15px; font-weight: 800; color: var(--c-txt); line-height: 1.3; }
.detail-relations { margin-top: 10px; }
.detail-rel-title { font-size: 11px; color: var(--c-txt-s); font-weight: 600; margin-bottom: 6px; text-transform: uppercase; letter-spacing: .5px; }
.detail-rel-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 0; border-bottom: 1px dashed var(--c-border);
}
.rel-name { font-size: 12px; color: var(--c-txt); font-weight: 500; }
.no-rel { font-size: 12px; color: var(--c-txt-i); }

.slide-fade-enter-active, .slide-fade-leave-active { transition: all .22s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { opacity: 0; transform: translateX(14px); }

/* ── 列表视图 ── */
.list-view { display: flex; flex-direction: column; gap: 20px; }
.relation-group {}
.group-title {
  font-size: 12px; color: var(--c-txt-s); font-weight: 700;
  margin-bottom: 10px; padding-bottom: 6px;
  border-bottom: 2px solid var(--c-border);
  text-transform: uppercase; letter-spacing: .5px;
}
.group-items { display: flex; flex-wrap: wrap; gap: 10px; }
.list-card {
  display: flex; align-items: center; gap: 10px;
  background: var(--c-surface);
  border-radius: var(--radius-sm);
  padding: 12px 16px; min-width: 190px;
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}
.list-card:hover { box-shadow: var(--shadow-md); border-color: var(--c-primary); transform: translateY(-1px); }
.list-name { font-weight: 700; font-size: 13px; color: var(--c-txt); margin-bottom: 4px; }

@media (max-width: 768px) {
  .family-tree-page { max-width: 100%; }
  :deep(.el-card__header) { padding: 10px 12px; }
  :deep(.el-card__body) { padding: 12px; }
  /* 图例简化 */
  .legend-bar { gap: 8px; padding: 8px 10px; font-size: 11px; }
  .legend-sep, .legend-tip { display: none; }
  /* SVG 横滚 */
  .svg-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  /* 详情卡片变底部浮窗 */
  .detail-card {
    position: fixed !important;
    right: 0 !important;
    left: 0 !important;
    top: auto !important;
    bottom: calc(56px + env(safe-area-inset-bottom, 0px)) !important;
    width: 100% !important;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0 !important;
    max-height: 50vh;
    overflow-y: auto;
    box-shadow: 0 -8px 32px rgba(0,0,0,.15);
    padding: 16px;
    z-index: 90;
  }
  /* 列表视图 */
  .list-card { min-width: 0; width: 100%; flex: 1 1 calc(50% - 5px); box-sizing: border-box; }
  .group-items { flex-direction: row; flex-wrap: wrap; }
}
@media (max-width: 480px) {
  .list-card { flex: 1 1 100%; }
}

</style>