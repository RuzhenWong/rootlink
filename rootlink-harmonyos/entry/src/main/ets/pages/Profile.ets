import router from '@ohos.router'
import { get, put } from '../utils/HttpUtil'
import { load, save } from '../utils/StorageUtil'
import { toast, toastErr } from '../utils/ToastUtil'
import { STORAGE_USER } from '../utils/Constants'

interface ProfileData {
  gender: number
  birthDate: string
  wechat: string
  qq: string
  workUnit: string
  maritalStatus: number
  bio: string
  avatarUrl: string
}

interface GenderOption {
  val: number
  label: string
}

const GENDER_OPTIONS: GenderOption[] = [
  { val: 0, label: '未设置' },
  { val: 1, label: '男' },
  { val: 2, label: '女' }
]

@Entry
@Component
struct Profile {
  @State userName: string = ''
  @State phone: string = ''
  @State gender: number = 0
  @State bio: string = ''
  @State wechat: string = ''
  @State qq: string = ''
  @State workUnit: string = ''
  @State birthDate: string = ''
  @State saving: boolean = false

  aboutToAppear(): void { this.loadProfile() }

  async loadProfile(): Promise<void> {
    try {
      const str = await load(getContext(this) as Context, STORAGE_USER)
      if (str) {
        const u = JSON.parse(str) as Record<string, ESObject>
        this.userName = (u['realName'] as string) || ''
        this.phone = (u['phone'] as string) || ''
      }
      const p = await get<ProfileData>('/v1/user/profile')
      if (p) {
        this.gender = p.gender || 0
        this.bio = p.bio || ''
        this.wechat = p.wechat || ''
        this.qq = p.qq || ''
        this.workUnit = p.workUnit || ''
        this.birthDate = p.birthDate || ''
      }
    } catch (e) {}
  }

  async saveProfile(): Promise<void> {
    this.saving = true
    try {
      await put('/v1/user/profile', {
        gender: this.gender, bio: this.bio, wechat: this.wechat,
        qq: this.qq, workUnit: this.workUnit, birthDate: this.birthDate
      })
      toast('资料已更新')
    } catch (e) {
      toastErr((e as Error).message)
    } finally {
      this.saving = false
    }
  }

  build() {
    Column() {
      // 导航栏
      Row() {
        Text('←').fontSize(22).fontColor('#1A1A2E').onClick(() => { router.back() })
        Text('个人资料').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text(this.saving ? '...' : '保存')
          .fontSize(14).fontColor('#667EEA').fontWeight(FontWeight.Medium)
          .onClick(() => { this.saveProfile() })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      Scroll() {
        Column({ space: 0 }) {
          // 头像区
          Column({ space: 8 }) {
            Stack() {
              Circle().width(80).height(80)
                .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
              Text(this.userName.charAt(0) || 'U')
                .fontSize(36).fontColor(Color.White).fontWeight(FontWeight.Bold)
            }
            Text(this.userName).fontSize(18).fontWeight(FontWeight.Bold).fontColor(Color.White)
            Text(this.phone).fontSize(13).fontColor('#E0D7FF')
          }
          .width('100%').padding({ top: 32, bottom: 28 })
          .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
          .alignItems(HorizontalAlign.Center)

          // 基本信息
          Column({ space: 0 }) {
            Text('基本信息')
              .fontSize(13).fontColor('#9CA3AF').fontWeight(FontWeight.Medium)
              .width('100%').padding({ left: 16, top: 16, bottom: 8 })

            this.InfoRow('👤', '姓名', this.userName, false, '')
            this.InfoRow('📱', '手机', this.phone, false, '')

            // 性别选择
            Row({ space: 12 }) {
              Text('⚧️').fontSize(18).width(28)
              Text('性别').fontSize(14).fontColor('#6B7280').width(70)
              Row({ space: 6 }) {
                ForEach(GENDER_OPTIONS, (g: GenderOption) => {
                  Text(g.label)
                    .fontSize(13)
                    .fontColor(this.gender === g.val ? Color.White : '#667EEA')
                    .backgroundColor(this.gender === g.val ? '#667EEA' : '#EEF0FF')
                    .padding({ left: 14, right: 14, top: 5, bottom: 5 })
                    .borderRadius(8)
                    .onClick(() => { this.gender = g.val })
                })
              }
              .layoutWeight(1)
            }
            .width('100%').height(56).padding({ left: 16, right: 16 })
            .backgroundColor(Color.White)
            .border({ width: { bottom: 0.5 }, color: '#F0F0F0' })

            this.InfoRow('🎂', '生日', this.birthDate, true, 'birthDate')
          }
          .backgroundColor(Color.White).borderRadius(16)
          .margin({ left: 16, right: 16, top: 8 })
          .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })

          // 联系方式
          Column({ space: 0 }) {
            Text('联系方式')
              .fontSize(13).fontColor('#9CA3AF').fontWeight(FontWeight.Medium)
              .width('100%').padding({ left: 16, top: 16, bottom: 8 })
            this.InfoRow('💬', '微信', this.wechat, true, 'wechat')
            this.InfoRow('🐧', 'QQ', this.qq, true, 'qq')
          }
          .backgroundColor(Color.White).borderRadius(16)
          .margin({ left: 16, right: 16, top: 12 })
          .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })

          // 其他信息
          Column({ space: 0 }) {
            Text('其他信息')
              .fontSize(13).fontColor('#9CA3AF').fontWeight(FontWeight.Medium)
              .width('100%').padding({ left: 16, top: 16, bottom: 8 })
            this.InfoRow('🏢', '工作单位', this.workUnit, true, 'workUnit')

            Row({ space: 12 }) {
              Text('✏️').fontSize(18).width(28)
              Text('简介').fontSize(14).fontColor('#6B7280').width(70)
              TextArea({ placeholder: '写点什么介绍自己...', text: this.bio })
                .fontSize(14).fontColor('#1A1A2E')
                .backgroundColor(Color.Transparent).borderWidth(0)
                .layoutWeight(1).height(80)
                .onChange((v: string) => { this.bio = v })
            }
            .width('100%').padding({ left: 16, right: 16, top: 10, bottom: 10 })
            .backgroundColor(Color.White)
          }
          .backgroundColor(Color.White).borderRadius(16)
          .margin({ left: 16, right: 16, top: 12 })
          .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })

          // 保存按钮
          Button(this.saving ? '保存中...' : '保存修改', { type: ButtonType.Normal })
            .width('100%').height(50)
            .fontSize(16).fontColor(Color.White)
            .linearGradient({ angle: 90, colors: [['#667EEA', 0], ['#764BA2', 1]] })
            .borderRadius(14)
            .margin({ left: 16, right: 16, top: 20 })
            .enabled(!this.saving)
            .onClick(() => { this.saveProfile() })

          Text('').height(32)
        }
        .width('100%')
      }
      .layoutWeight(1)
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }

  @Builder InfoRow(icon: string, label: string, val: string, editable: boolean, field: string) {
    Row({ space: 12 }) {
      Text(icon).fontSize(18).width(28)
      Text(label).fontSize(14).fontColor('#6B7280').width(70)
      if (editable) {
        TextInput({ placeholder: `请填写${label}`, text: val })
          .fontSize(14).fontColor('#1A1A2E').caretColor('#667EEA')
          .backgroundColor(Color.Transparent).borderWidth(0)
          .layoutWeight(1)
          .onChange((v: string) => {
            if (field === 'birthDate') { this.birthDate = v }
            else if (field === 'wechat') { this.wechat = v }
            else if (field === 'qq') { this.qq = v }
            else if (field === 'workUnit') { this.workUnit = v }
          })
      } else {
        Text(val || '未设置')
          .fontSize(14).fontColor(val ? '#1A1A2E' : '#C0C4CC').layoutWeight(1)
      }
    }
    .width('100%').height(52).padding({ left: 16, right: 16 })
    .backgroundColor(Color.White)
    .border({ width: { bottom: 0.5 }, color: '#F0F0F0' })
  }
}
