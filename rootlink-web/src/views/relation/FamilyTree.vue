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
            <el-button text @click="loadData">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>
      </template>

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
  </div>
  <!-- 亲属详情抽屉 -->
  <RelativeProfileDrawer v-model="profileDrawerVisible"
    :userId="profileTargetId" :relationDesc="profileRelationDesc" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { relationApi } from '@/api/relation'
import { useAuthStore } from '@/stores/auth'
import RelativeProfileDrawer from '@/components/RelativeProfileDrawer.vue'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const directRelations = ref([])  // getMyRelations 返回的原始数据
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
  const result = []
  const me = layoutNodes.value.find(n => n.isMe)
  if (!me) return []

  // ——— 辅助：节点按关系分类 ———
  const parents  = layoutNodes.value.filter(n => !n.isMe && isParentDesc(n.sublabel))
  const siblings = layoutNodes.value.filter(n => !n.isMe && isSiblingDesc(n.sublabel))
  const spouses  = layoutNodes.value.filter(n => !n.isMe && isSpouseDesc(n.sublabel))
  const children = layoutNodes.value.filter(n => !n.isMe && isChildDesc(n.sublabel))

  function addEdge(from, to, label, type = 'inferred', dash = '5 3') {
    if (!from || !to) return
    const meta = edgeMeta(from, to)
    const colorMap = {
      direct:  EDGE_COLORS.direct,
      spouse:  EDGE_COLORS.spouse,
      sibling: EDGE_COLORS.sibling,
      inferred: EDGE_COLORS.inferred,
    }
    result.push({
      d: curvePath(from, to),
      color: colorMap[type] || EDGE_COLORS.inferred,
      width: type === 'direct' || type === 'spouse' ? 2.0 : 1.4,
      dash:  type === 'direct' || type === 'spouse' ? 'none' : dash,
      label,
      mx: meta.mx,
      my: meta.my,
      angle: meta.angle,
      fromId: from.id,
      toId: to.id,
      key: `${from.id}-${to.id}`,
    })
  }

  // ===== 1. 我 ↔ 父母（母子/父子）=====
  parents.forEach(p => {
    const label = p.sublabel === '母亲' ? '母子' : '父子'
    addEdge(me, p, label, 'direct', 'none')
  })

  // ===== 2. 我 ↔ 同辈（姐弟/兄弟等）=====
  siblings.forEach(s => {
    const label = siblingLabel(s.sublabel)
    addEdge(me, s, label, 'sibling', 'none')
  })

  // ===== 3. 我 ↔ 配偶（夫妻）=====
  spouses.forEach(sp => {
    addEdge(me, sp, '夫妻', 'spouse', 'none')
  })

  // ===== 4. 我 ↔ 子女（父女/父子）=====
  children.forEach(c => {
    const label = c.sublabel === '女儿' ? '父女' : '父子'
    addEdge(me, c, label, 'direct', 'none')
  })

  // ===== 5. 父母 ↔ 同辈（母女/父女等）=====
  parents.forEach(p => {
    siblings.forEach(s => {
      const label = p.sublabel === '母亲' ? '母女' : '父女'
      addEdge(p, s, label, 'inferred')
    })
  })

  // ===== 6. 配偶 ↔ 子女（母女/母子）=====
  spouses.forEach(sp => {
    children.forEach(c => {
      const label = c.sublabel === '女儿' ? '母女' : '母子'
      addEdge(sp, c, label, 'inferred')
    })
  })

  // ===== 7. 父母 ↔ 子女（祖孙）=====
  parents.forEach(p => {
    children.forEach(c => {
      addEdge(p, c, '祖孙', 'inferred', '4 4')
    })
  })

  // ===== 8. 父母 ↔ 配偶（婆媳/岳父母）=====
  parents.forEach(p => {
    spouses.forEach(sp => {
      const label = p.sublabel === '母亲' ? '婆媳' : '翁婿/岳父'
      addEdge(p, sp, label, 'inferred', '4 4')
    })
  })

  // ===== 9. 配偶 ↔ 同辈（姑嫂）=====
  spouses.forEach(sp => {
    siblings.forEach(s => {
      addEdge(sp, s, '姑嫂', 'inferred', '4 4')
    })
  })

  // ===== 10. 同辈 ↔ 子女（姨甥/叔侄/舅甥）=====
  siblings.forEach(s => {
    children.forEach(c => {
      const label = ['姐姐','妹妹'].includes(s.sublabel) ? '姨甥' : '叔侄'
      addEdge(s, c, label, 'inferred', '4 4')
    })
  })

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
  try { directRelations.value = await relationApi.getMyRelations() || [] }
  catch (e) { console.error(e) }
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

onMounted(loadData)
</script>

<style scoped>
.family-tree-page { max-width: 1000px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; align-items: center; }

/* 图例 */
.legend-bar {
  display: flex; flex-wrap: wrap; gap: 14px; align-items: center;
  margin-bottom: 12px; font-size: 12px; color: #606266;
}
.legend-item { display: flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.legend-sep { width: 1px; height: 16px; background: #e4e7ed; }
.legend-tip { display: flex; align-items: center; gap: 6px; color: #909399; }
.edge-solid { display: inline-block; width: 24px; height: 2px; background: #667eea; border-radius: 1px; }
.edge-dash  { display: inline-block; width: 24px; height: 0; border-top: 2px dashed #fa8231; }

/* SVG 区域 */
.tree-wrapper { position: relative; }
.svg-wrap { overflow-x: auto; overflow-y: hidden; }
.tree-svg, svg { display: block; background: #f8faff; border-radius: 8px; border: 1px solid #eef0f6; cursor: pointer; }

/* 节点 */
.node-g { cursor: pointer; transition: opacity 0.15s; }
.node-g:hover circle:first-child { filter: brightness(1.1); }

/* 边标注 */
.edge-label {
  font-weight: 600;
  paint-order: stroke;
  stroke: #f8faff;
  stroke-width: 3px;
}

/* 详情卡片 */
.detail-card {
  position: absolute; right: 0; top: 0;
  width: 220px; background: #fff;
  border-radius: 10px; border: 1px solid #eee;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  padding: 14px; z-index: 20;
}
.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.detail-name { font-size: 15px; font-weight: 700; line-height: 1.3; }
.detail-relations { margin-top: 10px; }
.detail-rel-title { font-size: 11px; color: #909399; margin-bottom: 6px; }
.detail-rel-item { display: flex; align-items: center; justify-content: space-between;
  padding: 3px 0; border-bottom: 1px dashed #f0f0f0; }
.rel-name { font-size: 12px; color: #303133; }
.no-rel { font-size: 12px; color: #c0c4cc; }
.slide-fade-enter-active, .slide-fade-leave-active { transition: all .2s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { opacity: 0; transform: translateX(12px); }

/* 列表视图 */
.list-view { display: flex; flex-direction: column; gap: 16px; }
.relation-group {}
.group-title {
  font-size: 12px; color: #909399; font-weight: 600;
  margin-bottom: 8px; padding-bottom: 4px; border-bottom: 1px dashed #eee;
}
.group-items { display: flex; flex-wrap: wrap; gap: 10px; }
.list-card {
  display: flex; align-items: center; gap: 10px;
  background: #fafafa; border-radius: 8px;
  padding: 10px 14px; min-width: 180px;
  border: 1px solid #f0f0f0;
}
.list-info {}
.list-name { font-weight: 600; font-size: 13px; margin-bottom: 3px; }
</style>
