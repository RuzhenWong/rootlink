import router from '@ohos.router'
import { get } from '../utils/HttpUtil'

interface RelItem {
  relatedUserId: number
  relationDesc: string
  realName: string
  lifeStatus: number
}

interface TreeNode {
  id: string
  label: string
  sublabel: string
  generation: number
  fill: string
  x: number
  y: number
  r: number
  isMe: boolean
}

interface TreeEdge {
  x1: number
  y1: number
  x2: number
  y2: number
  color: string
  dash: boolean
}

const GEN_H: number = 130
const H_GAP: number = 110

function guessGen(desc: string): number {
  if (desc === '爷爷' || desc === '奶奶' || desc === '外公' || desc === '外婆') { return 2 }
  if (desc === '父亲' || desc === '母亲') { return 1 }
  if (desc === '儿子' || desc === '女儿') { return -1 }
  if (desc === '孙子' || desc === '孙女' || desc === '外孙' || desc === '外孙女') { return -2 }
  return 0
}

function nodeColor(desc: string): string {
  const g = guessGen(desc)
  if (g > 0) { return '#667EEA' }
  if (g < 0) { return '#42A5F5' }
  if (desc === '配偶') { return '#E040FB' }
  return '#26C6DA'
}

function buildNodes(relations: RelItem[]): TreeNode[] {
  const me: TreeNode = {
    id: 'me', label: '我', sublabel: '本人',
    generation: 0, fill: '#303133',
    x: 0, y: 0, r: 30, isMe: true
  }
  const others: TreeNode[] = relations.map((r: RelItem) => {
    return {
      id: r.relatedUserId.toString(),
      label: r.realName || '未知',
      sublabel: r.relationDesc || '',
      generation: guessGen(r.relationDesc),
      fill: nodeColor(r.relationDesc),
      x: 0, y: 0, r: 24, isMe: false
    } as TreeNode
  })
  const all: TreeNode[] = [me, ...others]

  // 按代分组
  const byGen: Map<number, TreeNode[]> = new Map()
  for (const n of all) {
    if (!byGen.has(n.generation)) { byGen.set(n.generation, []) }
    byGen.get(n.generation)!.push(n)
  }

  const gens: number[] = Array.from(byGen.keys()).sort((a: number, b: number) => b - a)
  const counts: number[] = Array.from(byGen.values()).map((v: TreeNode[]) => v.length)
  let maxCount: number = 1
  for (const c of counts) { if (c > maxCount) { maxCount = c } }
  const svgW: number = Math.max(360, maxCount * H_GAP + 60)
  const cx: number = svgW / 2

  const result: TreeNode[] = []
  for (let rowIdx = 0; rowIdx < gens.length; rowIdx++) {
    const gen: number = gens[rowIdx]
    const nodes: TreeNode[] = byGen.get(gen)!
    const count: number = nodes.length
    const y: number = 60 + rowIdx * GEN_H
    const totalW: number = (count - 1) * H_GAP
    const startX: number = cx - totalW / 2

    for (let i = 0; i < nodes.length; i++) {
      result.push({
        id: nodes[i].id, label: nodes[i].label, sublabel: nodes[i].sublabel,
        generation: nodes[i].generation, fill: nodes[i].fill,
        x: count === 1 ? cx : startX + i * H_GAP,
        y, r: nodes[i].r, isMe: nodes[i].isMe
      })
    }
  }
  return result
}

function buildEdges(nodes: TreeNode[]): TreeEdge[] {
  const me: TreeNode | undefined = nodes.find((n: TreeNode) => n.isMe)
  if (!me) { return [] }

  const result: TreeEdge[] = []

  const addEdge = (a: TreeNode, b: TreeNode, dash: boolean = false): void => {
    result.push({ x1: a.x, y1: a.y, x2: b.x, y2: b.y, color: dash ? '#FA8231' : a.fill, dash })
  }

  const parents  = nodes.filter((n: TreeNode) => !n.isMe && (n.sublabel === '父亲' || n.sublabel === '母亲'))
  const siblings = nodes.filter((n: TreeNode) => !n.isMe && (n.sublabel === '哥哥' || n.sublabel === '弟弟' || n.sublabel === '姐姐' || n.sublabel === '妹妹'))
  const spouses  = nodes.filter((n: TreeNode) => !n.isMe && n.sublabel === '配偶')
  const children = nodes.filter((n: TreeNode) => !n.isMe && (n.sublabel === '儿子' || n.sublabel === '女儿'))

  for (const p of parents)  { addEdge(me, p) }
  for (const s of siblings) { addEdge(me, s) }
  for (const sp of spouses) { addEdge(me, sp) }
  for (const c of children) { addEdge(me, c) }
  for (const p of parents)  { for (const s of siblings) { addEdge(p, s, true) } }
  for (const sp of spouses) { for (const c of children) { addEdge(sp, c, true) } }
  for (const p of parents)  { for (const c of children) { addEdge(p, c, true) } }
  for (const p of parents)  { for (const sp of spouses) { addEdge(p, sp, true) } }

  return result
}

@Entry
@Component
struct FamilyTree {
  @State relations: RelItem[] = []
  @State loading: boolean = false
  @State selectedId: string = ''
  @State nodes: TreeNode[] = []
  @State edges: TreeEdge[] = []
  private svgW: number = 360
  private svgH: number = 400

  aboutToAppear(): void { this.loadData() }

  async loadData(): Promise<void> {
    this.loading = true
    try {
      const data = await get<RelItem[]>('/v1/relation/my')
      this.relations = data || []
      this.rebuildLayout()
    } catch (e) {
    } finally {
      this.loading = false
    }
  }

  rebuildLayout(): void {
    this.nodes = buildNodes(this.relations)
    this.edges = buildEdges(this.nodes)
    const maxX: number = this.nodes.reduce((m: number, n: TreeNode) => Math.max(m, n.x), 360)
    const maxY: number = this.nodes.reduce((m: number, n: TreeNode) => Math.max(m, n.y), 400)
    this.svgW = Math.max(360, maxX + 80)
    this.svgH = Math.max(400, maxY + 80)
  }

  selectedNode(): TreeNode | undefined {
    return this.nodes.find((n: TreeNode) => n.id === this.selectedId)
  }

  build() {
    Column() {
      Row() {
        Text('←').fontSize(22).fontColor('#1A1A2E').onClick(() => { router.back() })
        Text('家族关系树').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('↻').fontSize(20).fontColor('#667EEA').onClick(() => { this.loadData() })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      if (this.loading) {
        Column() { LoadingProgress().width(48).height(48).color('#667EEA') }
          .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.relations.length === 0) {
        Column({ space: 14 }) {
          Text('🌱').fontSize(56)
          Text('暂无亲属关系').fontSize(16).fontColor('#9CA3AF')
          Button('去添加亲属', { type: ButtonType.Normal })
            .height(44).padding({ left: 24, right: 24 })
            .backgroundColor('#667EEA').fontColor(Color.White).borderRadius(10)
            .onClick(() => { router.pushUrl({ url: 'pages/Relations' }) })
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        // 图例
        Row({ space: 14 }) {
          this.LegendDot('#303133', '本人')
          this.LegendDot('#667EEA', '长辈')
          this.LegendDot('#E040FB', '配偶')
          this.LegendDot('#26C6DA', '同辈')
          this.LegendDot('#42A5F5', '晚辈')
        }
        .width('100%').padding({ left: 16, right: 16, bottom: 8 })

        // 关系图（使用 Scroll + Stack + Shape/绝对定位）
        Scroll() {
          Stack({ alignContent: Alignment.TopStart }) {
            // 连线层
            Shape() {
              ForEach(this.edges, (e: TreeEdge) => {
                Path()
                  .commands(`M${e.x1} ${e.y1} Q${(e.x1 + e.x2) / 2} ${Math.min(e.y1, e.y2) - 20} ${e.x2} ${e.y2}`)
                  .strokeWidth(e.dash ? 1.5 : 2.5)
                  .stroke(e.color)
                  .strokeDashArray(e.dash ? [5, 3] : [0])
                  .fill(Color.Transparent)
              })
            }
            .width(this.svgW).height(this.svgH)

            // 节点层
            ForEach(this.nodes, (n: TreeNode) => {
              Column({ space: 2 }) {
                Stack() {
                  if (this.selectedId === n.id) {
                    Circle().width(n.r * 2 + 10).height(n.r * 2 + 10).fill('#409EFF').opacity(0.25)
                  }
                  Circle().width(n.r * 2).height(n.r * 2).fill(n.fill)
                  Text(n.label.length > 2 ? n.label.slice(0, 2) : n.label)
                    .fontSize(n.isMe ? 13 : 12).fontColor(Color.White).fontWeight(FontWeight.Bold)
                }
                Text(n.label).fontSize(11).fontColor('#1A1A2E').maxLines(1)
                Text(n.sublabel).fontSize(10).fontColor('#9CA3AF').maxLines(1)
              }
              .position({ x: n.x - n.r - 20, y: n.y - n.r - 5 })
              .onClick(() => { this.selectedId = this.selectedId === n.id ? '' : n.id })
            })
          }
          .width(this.svgW).height(this.svgH)
        }
        .scrollable(ScrollDirection.Horizontal)
        .width('100%').layoutWeight(1)

        // 节点详情卡
        if (this.selectedId !== '') {
          if (this.selectedNode() !== undefined) {
            Row({ space: 12 }) {
              Stack() {
                Circle().width(40).height(40).fill(this.selectedNode()!.fill)
                Text(this.selectedNode()!.label.charAt(0))
                  .fontSize(18).fontColor(Color.White).fontWeight(FontWeight.Bold)
              }
              Column({ space: 2 }) {
                Text(this.selectedNode()!.label)
                  .fontSize(16).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
                Text(this.selectedNode()!.isMe ? '本人' : this.selectedNode()!.sublabel)
                  .fontSize(12).fontColor('#6B7280')
              }
              .layoutWeight(1).alignItems(HorizontalAlign.Start)

              Text('✕').fontSize(18).fontColor('#9CA3AF')
                .onClick(() => { this.selectedId = '' })
            }
            .width('100%').padding({ left: 16, right: 16, top: 12, bottom: 16 })
            .backgroundColor(Color.White)
            .border({ width: { top: 1 }, color: '#F0F0F0' })
          }
        }
      }
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }

  @Builder LegendDot(color: string, label: string) {
    Row({ space: 4 }) {
      Circle().width(10).height(10).fill(color)
      Text(label).fontSize(11).fontColor('#6B7280')
    }
  }
}
