import router from '@ohos.router'
import { get, post } from '../utils/HttpUtil'
import { toast, toastErr } from '../utils/ToastUtil'

interface TestamentItem {
  id: number
  title: string
  testamentType: number
  unlockStatus: number
  visibility: number
  createTime: string
}

interface TypeOption {
  idx: number
  label: string
}

const TYPE_LABELS: string[] = ['', '文字遗言', '致特定人', '财产分配', '心愿清单']
const TYPE_COLORS: string[] = ['', '#667EEA', '#10B981', '#F59E0B', '#EF4444']
const TYPE_OPTIONS: TypeOption[] = [
  { idx: 1, label: '文字遗言' },
  { idx: 2, label: '致特定人' },
  { idx: 3, label: '财产分配' }
]

@Entry
@Component
struct Testament {
  @State list: TestamentItem[] = []
  @State loading: boolean = false
  @State showCreate: boolean = false
  @State newTitle: string = ''
  @State newContent: string = ''
  @State newType: number = 1
  @State creating: boolean = false

  aboutToAppear(): void { this.loadList() }

  async loadList(): Promise<void> {
    this.loading = true
    try {
      const data = await get<TestamentItem[]>('/v1/testament/my')
      this.list = data || []
    } catch (e) {
    } finally {
      this.loading = false
    }
  }

  async createTestament(): Promise<void> {
    if (!this.newTitle.trim()) { toastErr('请填写遗言标题'); return }
    if (!this.newContent.trim()) { toastErr('请填写遗言内容'); return }
    this.creating = true
    try {
      await post('/v1/testament/create', {
        title: this.newTitle, content: this.newContent,
        testamentType: this.newType, visibility: 0
      })
      toast('遗言已加密保存')
      this.showCreate = false
      this.newTitle = ''
      this.newContent = ''
      this.newType = 1
      this.loadList()
    } catch (e) {
      toastErr((e as Error).message)
    } finally {
      this.creating = false
    }
  }

  typeLabel(t: number): string {
    return (t >= 0 && t < TYPE_LABELS.length) ? TYPE_LABELS[t] : '其他'
  }

  typeColor(t: number): string {
    return (t >= 0 && t < TYPE_COLORS.length) ? TYPE_COLORS[t] : '#9CA3AF'
  }

  build() {
    Column() {
      Row() {
        Text('←').fontSize(22).fontColor('#1A1A2E').onClick(() => { router.back() })
        Text('生前遗言').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('+ 新建').fontSize(14).fontColor('#667EEA').fontWeight(FontWeight.Medium)
          .onClick(() => { this.showCreate = !this.showCreate })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      // 创建面板
      if (this.showCreate) {
        Column({ space: 12 }) {
          Text('创建新遗言').fontSize(15).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')

          Row({ space: 6 }) {
            ForEach(TYPE_OPTIONS, (item: TypeOption) => {
              Text(item.label)
                .fontSize(12)
                .fontColor(this.newType === item.idx ? Color.White : '#667EEA')
                .backgroundColor(this.newType === item.idx ? '#667EEA' : '#EEF0FF')
                .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                .borderRadius(8)
                .onClick(() => { this.newType = item.idx })
            })
          }

          TextInput({ placeholder: '遗言标题', text: this.newTitle })
            .height(44).borderRadius(10).backgroundColor('#F8F9FE').fontSize(14)
            .onChange((v: string) => { this.newTitle = v })

          TextArea({ placeholder: '写下您的遗言内容...', text: this.newContent })
            .height(100).borderRadius(10).backgroundColor('#F8F9FE').fontSize(14)
            .onChange((v: string) => { this.newContent = v })

          Row({ space: 10 }) {
            Button('取消', { type: ButtonType.Normal })
              .layoutWeight(1).height(42)
              .backgroundColor('#F5F5F5').fontColor('#6B7280').borderRadius(10)
              .onClick(() => { this.showCreate = false })
            Button(this.creating ? '保存中...' : '加密保存', { type: ButtonType.Normal })
              .layoutWeight(1).height(42)
              .linearGradient({ angle: 90, colors: [['#667EEA', 0], ['#764BA2', 1]] })
              .fontColor(Color.White).borderRadius(10)
              .enabled(!this.creating)
              .onClick(() => { this.createTestament() })
          }
        }
        .width('100%').padding(16)
        .backgroundColor(Color.White)
        .border({ width: { bottom: 1 }, color: '#F0F0F0' })
      }

      // 说明横幅
      Row({ space: 8 }) {
        Text('🔒').fontSize(16)
        Text('遗言内容加密存储，仅在离世确认后向指定接收人开放')
          .fontSize(12).fontColor('#6B7280').layoutWeight(1)
      }
      .width('100%').padding({ left: 16, right: 16, top: 10, bottom: 10 })
      .backgroundColor('#FAFBFF')

      if (this.loading) {
        Column() { LoadingProgress().width(44).height(44).color('#667EEA') }
          .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.list.length === 0) {
        Column({ space: 12 }) {
          Text('📝').fontSize(52)
          Text('还没有遗言').fontSize(16).fontColor('#9CA3AF')
          Text('在这里记录您的心愿与嘱托\n加密保存，安全可靠')
            .fontSize(13).fontColor('#C0C4CC').textAlign(TextAlign.Center)
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 12 }) {
          ForEach(this.list, (item: TestamentItem) => {
            ListItem() {
              Column({ space: 10 }) {
                Row({ space: 8 }) {
                  Text(this.typeLabel(item.testamentType))
                    .fontSize(11).fontColor(this.typeColor(item.testamentType))
                    .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                    .backgroundColor(this.typeColor(item.testamentType) + '1A').borderRadius(6)
                  Blank()
                  Row({ space: 4 }) {
                    Text(item.unlockStatus === 1 ? '🔓' : '🔒').fontSize(14)
                    Text(item.unlockStatus === 1 ? '已解锁' : '锁定中')
                      .fontSize(11).fontColor(item.unlockStatus === 1 ? '#10B981' : '#9CA3AF')
                  }
                }
                Text(item.title)
                  .fontSize(16).fontWeight(FontWeight.Medium).fontColor('#1A1A2E')
                Text(item.createTime?.substring(0, 10) || '')
                  .fontSize(11).fontColor('#C0C4CC')
              }
              .width('100%').padding(16)
              .backgroundColor(Color.White).borderRadius(16)
              .shadow({ radius: 6, color: '#0000000D', offsetX: 0, offsetY: 3 })
            }
          })
        }
        .padding({ left: 16, right: 16, top: 8 })
        .layoutWeight(1)
      }
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }
}
