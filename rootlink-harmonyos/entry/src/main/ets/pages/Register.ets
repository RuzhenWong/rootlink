import router from '@ohos.router'
import { post } from '../utils/HttpUtil'
import { save } from '../utils/StorageUtil'
import { toastErr, toast } from '../utils/ToastUtil'
import { STORAGE_TOKEN, STORAGE_USER } from '../utils/Constants'

interface RegisterResp {
  token: string
  userInfo: object
}

@Entry
@Component
struct Register {
  @State phone: string = ''
  @State password: string = ''
  @State confirm: string = ''
  @State realName: string = ''
  @State loading: boolean = false

  async doRegister(): Promise<void> {
    if (!this.realName) { toastErr('请输入真实姓名'); return }
    if (!this.phone || this.phone.length !== 11) { toastErr('手机号格式不正确'); return }
    if (this.password.length < 6) { toastErr('密码至少6位'); return }
    if (this.password !== this.confirm) { toastErr('两次密码不一致'); return }
    this.loading = true
    try {
      const res = await post<RegisterResp>('/v1/auth/register', {
        phone: this.phone, password: this.password,
        realName: this.realName, confirmPassword: this.confirm
      })
      await save(getContext(this) as Context, STORAGE_TOKEN, res.token)
      await save(getContext(this) as Context, STORAGE_USER, JSON.stringify(res.userInfo))
      toast('注册成功，欢迎加入 RootLink！')
      router.replaceUrl({ url: 'pages/Index' })
    } catch (e) {
      toastErr((e as Error).message)
    } finally {
      this.loading = false
    }
  }

  @Builder FieldItem(icon: string, hint: string, val: string, type: InputType,
    cb: (v: string) => void) {
    Row({ space: 12 }) {
      Text(icon).fontSize(18)
      TextInput({ placeholder: hint, text: val })
        .type(type).fontSize(15).fontColor('#1A1A2E')
        .caretColor('#667EEA')
        .backgroundColor(Color.Transparent).borderWidth(0)
        .layoutWeight(1)
        .onChange(cb)
    }
    .width('100%').height(52)
    .backgroundColor('#F8F9FE').borderRadius(12)
    .padding({ left: 16, right: 16 })
  }

  build() {
    Column() {
      Row() {
        Text('←').fontSize(22).fontColor('#1A1A2E').onClick(() => { router.back() })
        Text('创建账号').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('').width(32)
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      Scroll() {
        Column({ space: 14 }) {
          Text('填写基本信息，开始记录您的家族故事')
            .fontSize(13).fontColor('#9CA3AF').margin({ top: 8, bottom: 16 })

          // 姓名
          Row({ space: 12 }) {
            Text('👤').fontSize(18)
            TextInput({ placeholder: '真实姓名', text: this.realName })
              .type(InputType.Normal).fontSize(15).fontColor('#1A1A2E')
              .caretColor('#667EEA').backgroundColor(Color.Transparent)
              .borderWidth(0).layoutWeight(1)
              .onChange((v: string) => { this.realName = v })
          }
          .width('100%').height(52).backgroundColor('#F8F9FE')
          .borderRadius(12).padding({ left: 16, right: 16 })

          // 手机号
          Row({ space: 12 }) {
            Text('📱').fontSize(18)
            TextInput({ placeholder: '手机号', text: this.phone })
              .type(InputType.PhoneNumber).fontSize(15).fontColor('#1A1A2E')
              .caretColor('#667EEA').backgroundColor(Color.Transparent)
              .borderWidth(0).layoutWeight(1)
              .onChange((v: string) => { this.phone = v })
          }
          .width('100%').height(52).backgroundColor('#F8F9FE')
          .borderRadius(12).padding({ left: 16, right: 16 })

          // 密码
          Row({ space: 12 }) {
            Text('🔑').fontSize(18)
            TextInput({ placeholder: '设置密码（至少6位）', text: this.password })
              .type(InputType.Password).fontSize(15).fontColor('#1A1A2E')
              .caretColor('#667EEA').backgroundColor(Color.Transparent)
              .borderWidth(0).layoutWeight(1)
              .onChange((v: string) => { this.password = v })
          }
          .width('100%').height(52).backgroundColor('#F8F9FE')
          .borderRadius(12).padding({ left: 16, right: 16 })

          // 确认密码
          Row({ space: 12 }) {
            Text('🔒').fontSize(18)
            TextInput({ placeholder: '确认密码', text: this.confirm })
              .type(InputType.Password).fontSize(15).fontColor('#1A1A2E')
              .caretColor('#667EEA').backgroundColor(Color.Transparent)
              .borderWidth(0).layoutWeight(1)
              .onChange((v: string) => { this.confirm = v })
          }
          .width('100%').height(52).backgroundColor('#F8F9FE')
          .borderRadius(12).padding({ left: 16, right: 16 })

          Button(this.loading ? '注册中...' : '创建账号', { type: ButtonType.Normal })
            .width('100%').height(52)
            .fontSize(16).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .linearGradient({ angle: 90, colors: [['#667EEA', 0], ['#764BA2', 1]] })
            .borderRadius(12).enabled(!this.loading)
            .onClick(() => { this.doRegister() })
            .margin({ top: 8 })

          Row({ space: 4 }) {
            Text('已有账号？').fontSize(13).fontColor('#9CA3AF')
            Text('返回登录').fontSize(13).fontColor('#667EEA').fontWeight(FontWeight.Medium)
              .onClick(() => { router.back() })
          }
          .width('100%').justifyContent(FlexAlign.Center)
        }
        .width('100%').padding({ left: 28, right: 28, bottom: 40 })
      }
      .layoutWeight(1)
    }
    .width('100%').height('100%').backgroundColor(Color.White)
  }
}
