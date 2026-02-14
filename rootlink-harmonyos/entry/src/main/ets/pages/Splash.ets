import router from '@ohos.router'
import { load } from '../utils/StorageUtil'
import { STORAGE_TOKEN } from '../utils/Constants'

@Entry
@Component
struct Splash {
  @State opacity: number = 0
  @State scaleVal: number = 0.85

  aboutToAppear(): void {
    animateTo({ duration: 600, curve: Curve.EaseOut }, () => {
      this.opacity = 1
      this.scaleVal = 1
    })
    setTimeout(async () => {
      const token = await load(getContext(this) as Context, STORAGE_TOKEN)
      router.replaceUrl({ url: token ? 'pages/Index' : 'pages/Login' })
    }, 1500)
  }

  build() {
    Stack() {
      Column()
        .width('100%').height('100%')
        .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })

      Column({ space: 20 }) {
        Stack() {
          Circle().width(96).height(96).fill(Color.White).opacity(0.15)
          Text('R').fontSize(52).fontWeight(FontWeight.Bold).fontColor(Color.White)
        }
        Text('RootLink')
          .fontSize(32).fontWeight(FontWeight.Bold)
          .fontColor(Color.White).letterSpacing(2)
        Text('连接家族 · 传承记忆')
          .fontSize(15).fontColor('#E0D7FF').letterSpacing(1)
      }
      .scale({ x: this.scaleVal, y: this.scaleVal })
      .opacity(this.opacity)
    }
    .width('100%').height('100%')
  }
}
