import router from '@ohos.router'
import { get, post } from '../utils/HttpUtil'
import { toast, toastErr } from '../utils/ToastUtil'

interface RelationItem {
  relationId: number
  relatedUserId: number
  relationDesc: string
  realName: string
  lifeStatus: number
  phone: string
}

interface PendingItem {
  id: number
  applicantName: string
  relationDesc: string
  reason: string
}

interface SearchUser {
  userId: number
  realName: string
  phone: string
}

const REL_OPTIONS: string[] = ['儿子','女儿','父亲','母亲','配偶','哥哥','弟弟','姐姐','妹妹']
const REL_CHAINS: string[][] = [['子'],['女'],['父'],['母'],['配偶'],['同辈','直属','哥'],
  ['同辈','直属','弟'],['同辈','直属','姐'],['同辈','直属','妹']]

@Entry
@Component
struct Relations {
  @State tab: number = 0
  @State relations: RelationItem[] = []
  @State pending: PendingItem[] = []
  @State loading: boolean = false
  @State searchIdCard: string = ''
  @State searchResult: SearchUser | null = null
  @State searching: boolean = false
  @State showAdd: boolean = false
  @State relIdx: number = 0
  @State addReason: string = ''

  aboutToAppear(): void {
    this.loadData()
  }

  async loadData(): Promise<void> {
    this.loading = true
    try {
      const data = await get<RelationItem[]>('/v1/relation/my')
      this.relations = data || []
      const pd = await get<PendingItem[]>('/v1/relation/apply/pending')
      this.pending = pd || []
    } catch (e) {
    } finally {
      this.loading = false
    }
  }

  async searchUser(): Promise<void> {
    if (this.searchIdCard.length < 15) { toastErr('请输入完整身份证号'); return }
    this.searching = true
    try {
      const r = await get<SearchUser>(`/v1/relation/search?idCard=${this.searchIdCard}`)
      this.searchResult = r
    } catch (e) {
      toastErr((e as Error).message)
    } finally {
      this.searching = false
    }
  }

  async applyRelation(): Promise<void> {
    if (!this.searchResult) { return }
    try {
      await post('/v1/relation/apply', {
        targetUserId: this.searchResult.userId,
        relationChain: JSON.stringify(REL_CHAINS[this.relIdx]),
        reason: this.addReason
      })
      toast('申请已发送，等待对方确认')
      this.showAdd = false
      this.searchResult = null
      this.searchIdCard = ''
    } catch (e) {
      toastErr((e as Error).message)
    }
  }

  async handlePending(id: number, action: number): Promise<void> {
    try {
      await post(`/v1/relation/apply/${id}/handle`, { action })
      toast(action === 1 ? '已同意' : '已拒绝')
      this.loadData()
    } catch (e) {
      toastErr((e as Error).message)
    }
  }

  build() {
    Column() {
      // 标题栏
      Row() {
        Text('←').fontSize(22).fontColor('#1A1A2E').onClick(() => { router.back() })
        Text('亲属关系').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('+ 添加').fontSize(14).fontColor('#667EEA').fontWeight(FontWeight.Medium)
          .onClick(() => { this.showAdd = !this.showAdd })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      // 添加面板
      if (this.showAdd) {
        Column({ space: 10 }) {
          Row({ space: 8 }) {
            TextInput({ placeholder: '输入对方身份证号搜索', text: this.searchIdCard })
              .layoutWeight(1).height(44).borderRadius(10)
              .backgroundColor('#F8F9FE').fontSize(14)
              .onChange((v: string) => { this.searchIdCard = v })
            Button('搜索', { type: ButtonType.Normal })
              .height(44).padding({ left: 16, right: 16 })
              .backgroundColor('#667EEA').fontColor(Color.White)
              .borderRadius(10).fontSize(14)
              .onClick(() => { this.searchUser() })
          }

          if (this.searchResult !== null) {
            Column({ space: 8 }) {
              Row({ space: 10 }) {
                Stack() {
                  Circle().width(40).height(40).fill('#667EEA')
                  Text(this.searchResult!.realName?.charAt(0) || '?')
                    .fontSize(16).fontColor(Color.White)
                }
                Column({ space: 2 }) {
                  Text(this.searchResult!.realName).fontSize(15).fontWeight(FontWeight.Medium)
                  Text(this.searchResult!.phone).fontSize(12).fontColor('#9CA3AF')
                }
                .layoutWeight(1).alignItems(HorizontalAlign.Start)
              }

              // 关系选择
              Row({ space: 8 }) {
                Text('我的').fontSize(13).fontColor('#6B7280')
                Select(REL_OPTIONS.map(v => ({ value: v })))
                  .selected(this.relIdx)
                  .value(REL_OPTIONS[this.relIdx])
                  .font({ size: 14 }).fontColor('#1A1A2E')
                  .layoutWeight(1)
                  .onSelect((idx: number) => { this.relIdx = idx })
              }

              TextInput({ placeholder: '申请理由（选填）' })
                .height(44).borderRadius(10).backgroundColor('#F8F9FE').fontSize(14)
                .onChange((v: string) => { this.addReason = v })

              Button('发送申请', { type: ButtonType.Normal })
                .width('100%').height(44)
                .backgroundColor('#667EEA').fontColor(Color.White)
                .borderRadius(10).fontSize(14)
                .onClick(() => { this.applyRelation() })
            }
            .width('100%').padding(12)
            .backgroundColor(Color.White).borderRadius(12)
            .border({ width: 1, color: '#E5E7EB' })
          }
        }
        .width('100%').padding({ left: 16, right: 16, bottom: 10 })
        .backgroundColor(Color.White)
        .border({ width: { bottom: 1 }, color: '#F0F0F0' })
      }

      // Tab
      Row() {
        Column() {
          Text('我的亲属')
            .fontSize(14)
            .fontWeight(this.tab === 0 ? FontWeight.Medium : FontWeight.Normal)
            .fontColor(this.tab === 0 ? '#667EEA' : '#9CA3AF')
          if (this.tab === 0) {
            Divider().color('#667EEA').strokeWidth(2).width(40)
          }
        }
        .layoutWeight(1).height(44).justifyContent(FlexAlign.Center)
        .onClick(() => { this.tab = 0 })

        Column() {
          Text(`待处理(${this.pending.length})`)
            .fontSize(14)
            .fontWeight(this.tab === 1 ? FontWeight.Medium : FontWeight.Normal)
            .fontColor(this.tab === 1 ? '#667EEA' : '#9CA3AF')
          if (this.tab === 1) {
            Divider().color('#667EEA').strokeWidth(2).width(40)
          }
        }
        .layoutWeight(1).height(44).justifyContent(FlexAlign.Center)
        .onClick(() => { this.tab = 1 })
      }
      .width('100%').backgroundColor(Color.White)
      .border({ width: { bottom: 1 }, color: '#F0F0F0' })

      // 列表
      if (this.tab === 0) {
        if (this.relations.length === 0) {
          Column({ space: 12 }) {
            Text('🌱').fontSize(48)
            Text('还没有亲属关系').fontSize(15).fontColor('#9CA3AF')
          }
          .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
        } else {
          List({ space: 10 }) {
            ForEach(this.relations, (r: RelationItem) => {
              ListItem() {
                Row({ space: 12 }) {
                  Stack() {
                    Circle().width(48).height(48)
                      .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
                    Text(r.realName?.charAt(0) || '?').fontSize(18).fontColor(Color.White)
                  }
                  Column({ space: 4 }) {
                    Row({ space: 6 }) {
                      Text(r.realName || '未知')
                        .fontSize(15).fontWeight(FontWeight.Medium).fontColor('#1A1A2E')
                      if (r.lifeStatus === 3) {
                        Text('已离世').fontSize(10).fontColor('#EF4444')
                          .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                          .backgroundColor('#FEE2E2').borderRadius(4)
                      }
                    }
                    Text(r.relationDesc || '亲属').fontSize(12).fontColor('#9CA3AF')
                  }
                  .layoutWeight(1).alignItems(HorizontalAlign.Start)
                }
                .width('100%').padding(14)
                .backgroundColor(Color.White).borderRadius(14)
                .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })
              }
            })
          }
          .padding({ left: 16, right: 16, top: 10 })
          .layoutWeight(1)
        }
      } else {
        if (this.pending.length === 0) {
          Column({ space: 12 }) {
            Text('✅').fontSize(48)
            Text('没有待处理申请').fontSize(15).fontColor('#9CA3AF')
          }
          .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
        } else {
          List({ space: 10 }) {
            ForEach(this.pending, (p: PendingItem) => {
              ListItem() {
                Column({ space: 10 }) {
                  Row({ space: 8 }) {
                    Stack() {
                      Circle().width(40).height(40).fill('#E0E7FF')
                      Text(p.applicantName?.charAt(0) || '?').fontSize(16).fontColor('#667EEA')
                    }
                    Column({ space: 2 }) {
                      Text(p.applicantName)
                        .fontSize(15).fontWeight(FontWeight.Medium).fontColor('#1A1A2E')
                      Text(`申请成为您的「${p.relationDesc}」`)
                        .fontSize(12).fontColor('#6B7280')
                    }
                    .layoutWeight(1).alignItems(HorizontalAlign.Start)
                  }
                  if (p.reason) {
                    Text(`"${p.reason}"`)
                      .fontSize(12).fontColor('#9CA3AF').fontStyle(FontStyle.Italic)
                  }
                  Row({ space: 10 }) {
                    Button('拒绝', { type: ButtonType.Normal })
                      .layoutWeight(1).height(36)
                      .backgroundColor('#FEF2F2').fontColor('#EF4444').borderRadius(8).fontSize(14)
                      .onClick(() => { this.handlePending(p.id, 2) })
                    Button('同意', { type: ButtonType.Normal })
                      .layoutWeight(1).height(36)
                      .backgroundColor('#667EEA').fontColor(Color.White).borderRadius(8).fontSize(14)
                      .onClick(() => { this.handlePending(p.id, 1) })
                  }
                }
                .width('100%').padding(14)
                .backgroundColor(Color.White).borderRadius(14)
                .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })
              }
            })
          }
          .padding({ left: 16, right: 16, top: 10 })
          .layoutWeight(1)
        }
      }
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }
}
