import router from '@ohos.router'
import { get, post } from '../utils/HttpUtil'
import { toast, toastErr } from '../utils/ToastUtil'

interface EulogyItem {
  id: number
  deceasedName: string
  eulogyTitle: string
  eulogyContent: string
  senderName: string
  publishTime: string
  lifeFrom: string
  lifeTo: string
}

@Entry
@Component
struct EulogyWall {
  @State eulogies: EulogyItem[] = []
  @State loading: boolean = false
  @State showCompose: boolean = false
  @State eulogyTitle: string = ''
  @State eulogyContent: string = ''
  @State senderAlias: string = ''
  @State submitting: boolean = false

  aboutToAppear(): void { this.loadWall() }

  async loadWall(): Promise<void> {
    this.loading = true
    try {
      const data = await get<EulogyItem[]>('/v1/eulogy/public/wall')
      this.eulogies = data || []
    } catch (e) {
    } finally {
      this.loading = false
    }
  }

  async submitEulogy(): Promise<void> {
    if (!this.eulogyTitle) { toastErr('请填写挽联标题'); return }
    if (!this.eulogyContent) { toastErr('请填写挽联内容'); return }
    this.submitting = true
    try {
      await post('/v1/eulogy/submit', {
        eulogyTitle: this.eulogyTitle,
        eulogyContent: this.eulogyContent,
        senderAlias: this.senderAlias
      })
      toast('挽联已提交，审核通过后将展示在纪念墙')
      this.showCompose = false
      this.eulogyTitle = ''
      this.eulogyContent = ''
      this.senderAlias = ''
    } catch (e) {
      toastErr((e as Error).message)
    } finally {
      this.submitting = false
    }
  }

  build() {
    Column() {
      // 导航栏
      Row() {
        Text('←').fontSize(22).fontColor('#1A1A2E').onClick(() => { router.back() })
        Text('挽联纪念墙').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('✍️').fontSize(20).onClick(() => { this.showCompose = !this.showCompose })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      // 顶部横幅
      Column({ space: 4 }) {
        Text('🕯️ 永恒的思念').fontSize(15).fontWeight(FontWeight.Bold).fontColor(Color.White)
        Text('在这里，用文字寄托对逝者的哀思与祝福')
          .fontSize(12).fontColor('#E0D7FF')
      }
      .width('100%').padding({ top: 14, bottom: 14 })
      .linearGradient({ angle: 135, colors: [['#4A5568', 0], ['#2D3748', 1]] })
      .alignItems(HorizontalAlign.Center)

      // 撰写面板
      if (this.showCompose) {
        Column({ space: 10 }) {
          Text('撰写挽联').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')

          TextInput({ placeholder: '挽联标题', text: this.eulogyTitle })
            .height(44).borderRadius(10).backgroundColor('#F8F9FE').fontSize(14)
            .onChange((v: string) => { this.eulogyTitle = v })

          TextArea({ placeholder: '挽联内容...', text: this.eulogyContent })
            .height(88).borderRadius(10).backgroundColor('#F8F9FE').fontSize(14)
            .onChange((v: string) => { this.eulogyContent = v })

          TextInput({ placeholder: '署名（选填，留空显示匿名）', text: this.senderAlias })
            .height(44).borderRadius(10).backgroundColor('#F8F9FE').fontSize(14)
            .onChange((v: string) => { this.senderAlias = v })

          Row({ space: 10 }) {
            Button('取消', { type: ButtonType.Normal })
              .layoutWeight(1).height(42)
              .backgroundColor('#F5F5F5').fontColor('#6B7280').borderRadius(10)
              .onClick(() => { this.showCompose = false })
            Button(this.submitting ? '提交中...' : '提交审核', { type: ButtonType.Normal })
              .layoutWeight(1).height(42)
              .linearGradient({ angle: 90, colors: [['#667EEA', 0], ['#764BA2', 1]] })
              .fontColor(Color.White).borderRadius(10)
              .enabled(!this.submitting)
              .onClick(() => { this.submitEulogy() })
          }
        }
        .width('100%').padding(16)
        .backgroundColor(Color.White)
        .border({ width: { bottom: 1 }, color: '#F0F0F0' })
      }

      // 挽联列表
      if (this.loading) {
        Column() { LoadingProgress().width(44).height(44).color('#667EEA') }
          .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.eulogies.length === 0) {
        Column({ space: 12 }) {
          Text('🕯️').fontSize(52)
          Text('纪念墙暂无内容').fontSize(16).fontColor('#9CA3AF')
          Text('成为第一个献上挽联的人').fontSize(13).fontColor('#C0C4CC')
          Button('撰写挽联', { type: ButtonType.Normal })
            .height(42).padding({ left: 24, right: 24 })
            .backgroundColor('#667EEA').fontColor(Color.White).borderRadius(10)
            .onClick(() => { this.showCompose = true })
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 14 }) {
          ForEach(this.eulogies, (item: EulogyItem) => {
            ListItem() {
              Column({ space: 10 }) {
                Row({ space: 10 }) {
                  Stack() {
                    Circle().width(44).height(44).fill('#4A5568')
                    Text(item.deceasedName?.charAt(0) || '?')
                      .fontSize(18).fontColor(Color.White)
                  }
                  Column({ space: 2 }) {
                    Text(item.deceasedName || '')
                      .fontSize(16).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
                    if (item.lifeFrom || item.lifeTo) {
                      Text(`${item.lifeFrom || '?'} — ${item.lifeTo || '?'}`)
                        .fontSize(11).fontColor('#9CA3AF')
                    }
                  }
                  .layoutWeight(1).alignItems(HorizontalAlign.Start)

                  Text('🕯️').fontSize(20)
                }

                Row() {
                  Divider()
                    .vertical(true)
                    .height(30)
                    .strokeWidth(3)
                    .color('#667EEA')
                  Text(item.eulogyTitle)
                    .fontSize(14).fontWeight(FontWeight.Medium).fontColor('#2D3748')
                    .lineHeight(20).padding({ left: 10 }).layoutWeight(1)
                }

                Text(item.eulogyContent)
                  .fontSize(13).fontColor('#6B7280').lineHeight(20)
                  .maxLines(3).textOverflow({ overflow: TextOverflow.Ellipsis })

                Row({ space: 6 }) {
                  Text(item.senderName ? `— ${item.senderName}` : '— 匿名')
                    .fontSize(12).fontColor('#9CA3AF').layoutWeight(1)
                  Text(item.publishTime?.substring(0, 10) || '')
                    .fontSize(11).fontColor('#C0C4CC')
                }
              }
              .width('100%').padding(16)
              .backgroundColor(Color.White).borderRadius(16)
              .shadow({ radius: 6, color: '#0000000D', offsetX: 0, offsetY: 3 })
            }
          })
        }
        .padding({ left: 16, right: 16, top: 12 })
        .layoutWeight(1)
      }
    }
    .width('100%').height('100%').backgroundColor('#F4F4F7')
  }
}
