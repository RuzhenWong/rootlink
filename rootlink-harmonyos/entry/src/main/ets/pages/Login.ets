import router from '@ohos.router'
import { post } from '../utils/HttpUtil'
import { save } from '../utils/StorageUtil'
import { toast, toastErr } from '../utils/ToastUtil'
import { STORAGE_TOKEN, STORAGE_USER } from '../utils/Constants'

interface LoginResp {
  token: string
  userInfo: object
}

@Entry
@Component
struct Login {
  @State phone: string = ''
  @State password: string = ''
  @State loading: boolean = false
  @State pwdVisible: boolean = false

  async doLogin(): Promise<void> {
    if (!this.phone || this.phone.length !== 11) { toastErr('请输入正确的手机号'); return }
    if (!this.password || this.password.length < 6) { toastErr('密码至少6位'); return }
    this.loading = true
    try {
      const res = await post<LoginResp>('/v1/auth/login', { phone: this.phone, password: this.password })
      await save(getContext(this) as Context, STORAGE_TOKEN, res.token)
      await save(getContext(this) as Context, STORAGE_USER, JSON.stringify(res.userInfo))
      router.replaceUrl({ url: 'pages/Index' })
    } catch (e) {
      toastErr((e as Error).message)
    } finally {
      this.loading = false
    }
  }

  @Builder InputRow(icon: ResourceStr, hint: string, val: string, type: InputType,
    onChange: (v: string) => void) {
    Row({ space: 12 }) {
      Text(icon).fontSize(18)
      TextInput({ placeholder: hint, text: val })
        .type(type).fontSize(15).fontColor('#1A1A2E')
        .caretColor('#667EEA')
        .backgroundColor(Color.Transparent).borderWidth(0)
        .layoutWeight(1)
        .onChange(onChange)
    }
    .width('100%').height(52)
    .backgroundColor('#F8F9FE').borderRadius(12)
    .padding({ left: 16, right: 16 })
  }

  build() {
    Column() {
      Column({ space: 10 }) {
        Stack() {
          Circle().width(72).height(72)
            .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
          Text('R').fontSize(38).fontWeight(FontWeight.Bold).fontColor(Color.White)
        }
        Text('RootLink').fontSize(26).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
        Text('家族根链').fontSize(13).fontColor('#9CA3AF').letterSpacing(2)
      }
      .width('100%').padding({ top: 70, bottom: 40 })
      .alignItems(HorizontalAlign.Center)

      Column({ space: 14 }) {
        // 手机号输入框
        Row({ space: 12 }) {
          Text('📱').fontSize(18)
          TextInput({ placeholder: '手机号', text: this.phone })
            .type(InputType.PhoneNumber).maxLength(11)
            .fontSize(15).fontColor('#1A1A2E').caretColor('#667EEA')
            .backgroundColor(Color.Transparent).borderWidth(0)
            .layoutWeight(1)
            .onChange((v: string) => { this.phone = v })
        }
        .width('100%').height(52)
        .backgroundColor('#F8F9FE').borderRadius(12)
        .padding({ left: 16, right: 16 })

        // 密码输入框
        Row({ space: 12 }) {
          Text('🔑').fontSize(18)
          TextInput({ placeholder: '密码', text: this.password })
            .type(this.pwdVisible ? InputType.Normal : InputType.Password)
            .fontSize(15).fontColor('#1A1A2E').caretColor('#667EEA')
            .backgroundColor(Color.Transparent).borderWidth(0)
            .layoutWeight(1)
            .onChange((v: string) => { this.password = v })
          Text(this.pwdVisible ? '🙈' : '👁').fontSize(18)
            .onClick(() => { this.pwdVisible = !this.pwdVisible })
        }
        .width('100%').height(52)
        .backgroundColor('#F8F9FE').borderRadius(12)
        .padding({ left: 16, right: 16 })

        Button(this.loading ? '登录中...' : '登 录', { type: ButtonType.Normal })
          .width('100%').height(52)
          .fontSize(16).fontWeight(FontWeight.Medium).fontColor(Color.White)
          .linearGradient({ angle: 90, colors: [['#667EEA', 0], ['#764BA2', 1]] })
          .borderRadius(12).enabled(!this.loading)
          .onClick(() => { this.doLogin() })

        Row({ space: 4 }) {
          Text('还没有账号？').fontSize(13).fontColor('#9CA3AF')
          Text('立即注册').fontSize(13).fontColor('#667EEA').fontWeight(FontWeight.Medium)
            .onClick(() => { router.pushUrl({ url: 'pages/Register' }) })
        }
        .width('100%').justifyContent(FlexAlign.Center).margin({ top: 4 })
      }
      .width('100%').padding({ left: 28, right: 28 })
    }
    .width('100%').height('100%').backgroundColor(Color.White)
  }
}
