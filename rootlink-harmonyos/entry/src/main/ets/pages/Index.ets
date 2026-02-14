import router from '@ohos.router'
import { get } from '../utils/HttpUtil'
import { load, clear } from '../utils/StorageUtil'
import { STORAGE_USER, STORAGE_TOKEN } from '../utils/Constants'

interface StatItem {
  label: string
  val: number
  icon: string
}

interface QuickItem {
  icon: string
  label: string
  color: string
  page: string
}

interface RelationItem {
  relationId: number
  relatedUserId: number
  relationDesc: string
  realName: string
  lifeStatus: number
  phone: string
}

interface TestamentItem {
  id: number
  title: string
  testamentType: number
  unlockStatus: number
  createTime: string
}

interface MenuItem {
  icon: string
  label: string
  page: string
}

// ─── 首页 Tab ─────────────────────────────────────
@Component
struct DashboardTab {
  @State userName: string = '家族成员'
  @State relCount: number = 0
  @State pendingCount: number = 0

  aboutToAppear(): void {
    this.loadUser()
  }

  async loadUser(): Promise<void> {
    try {
      const str = await load(getContext(this) as Context, STORAGE_USER)
      if (str) {
        const u = JSON.parse(str) as Record<string, string>
        this.userName = u['realName'] || '家族成员'
      }
    } catch (e) {}
  }

  build() {
    Scroll() {
      Column({ space: 0 }) {
        // 渐变头部
        Column({ space: 16 }) {
          Row() {
            Column({ space: 4 }) {
              Text(`你好，${this.userName}`)
                .fontSize(22).fontWeight(FontWeight.Bold).fontColor(Color.White)
              Text('欢迎回到 RootLink')
                .fontSize(13).fontColor('#E0D7FF')
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)

            Stack() {
              Circle().width(44).height(44).fill(Color.White).opacity(0.2)
              Text('🔔').fontSize(20)
            }
          }
          .width('100%')

          Row() {
            ForEach([
              { label: '亲属成员', val: this.relCount, icon: '👥' } as StatItem,
              { label: '待确认', val: this.pendingCount, icon: '⏳' } as StatItem,
              { label: '遗言', val: 0, icon: '📝' } as StatItem
            ], (item: StatItem) => {
              Column({ space: 4 }) {
                Text(item.icon).fontSize(20)
                Text(item.val.toString())
                  .fontSize(24).fontWeight(FontWeight.Bold).fontColor(Color.White)
                Text(item.label).fontSize(11).fontColor('#E0D7FF')
              }
              .layoutWeight(1)
              .alignItems(HorizontalAlign.Center)
            })
          }
          .width('100%')
        }
        .width('100%')
        .padding(20)
        .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })

        // 快捷入口
        Column({ space: 12 }) {
          Text('快捷操作')
            .fontSize(16).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
            .width('100%')
            .margin({ top: 4 })

          Grid() {
            ForEach([
              { icon: '➕', label: '添加亲属', color: '#667EEA', page: 'pages/Relations' } as QuickItem,
              { icon: '🌳', label: '关系树',   color: '#10B981', page: 'pages/FamilyTree' } as QuickItem,
              { icon: '✍️', label: '写遗言',   color: '#F59E0B', page: 'pages/Testament' } as QuickItem,
              { icon: '🏛️', label: '挽联墙',   color: '#EF4444', page: 'pages/EulogyWall' } as QuickItem
            ], (item: QuickItem) => {
              GridItem() {
                Column({ space: 8 }) {
                  Stack() {
                    Circle().width(48).height(48).fill(item.color).opacity(0.12)
                    Text(item.icon).fontSize(22)
                  }
                  Text(item.label)
                    .fontSize(13).fontColor('#1A1A2E').fontWeight(FontWeight.Medium)
                }
                .width('100%').height(88)
                .backgroundColor(Color.White).borderRadius(16)
                .justifyContent(FlexAlign.Center)
                .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })
                .onClick(() => { router.pushUrl({ url: item.page }) })
              }
            })
          }
          .columnsTemplate('1fr 1fr')
          .columnsGap(12).rowsGap(12)
          .width('100%')

          Column({ space: 10 }) {
            Text('RootLink 使命')
              .fontSize(14).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
            Text('用数字方式记录家族关系，传递亲情与记忆。支持亲属关系推断、生前遗言加密保管、挽联审核发布等功能。')
              .fontSize(13).fontColor('#6B7280').lineHeight(20)
          }
          .width('100%').padding(16)
          .backgroundColor(Color.White).borderRadius(16)
          .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 24 })
      }
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }
}

// ─── 关系 Tab ─────────────────────────────────────
@Component
struct RelationsTab {
  @State relations: RelationItem[] = []
  @State loading: boolean = false

  aboutToAppear(): void {
    this.loadData()
  }

  async loadData(): Promise<void> {
    this.loading = true
    try {
      const data = await get<RelationItem[]>('/v1/relation/my')
      this.relations = data || []
    } catch (e) {
    } finally {
      this.loading = false
    }
  }

  build() {
    Column() {
      Row() {
        Text('亲属关系')
          .fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
        Blank()
        Text('+ 添加')
          .fontSize(14).fontColor('#667EEA').fontWeight(FontWeight.Medium)
          .onClick(() => { router.pushUrl({ url: 'pages/Relations' }) })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      if (this.loading) {
        Column() {
          LoadingProgress().width(40).height(40).color('#667EEA')
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.relations.length === 0) {
        Column({ space: 12 }) {
          Text('🌱').fontSize(48)
          Text('还没有亲属关系').fontSize(16).fontColor('#9CA3AF')
          Text('点击「+ 添加」搜索并添加家族成员').fontSize(13).fontColor('#C0C4CC')
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 10 }) {
          ForEach(this.relations, (r: RelationItem) => {
            ListItem() {
              Row({ space: 12 }) {
                Stack() {
                  Circle().width(44).height(44)
                    .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
                  Text(r.realName?.charAt(0) || '?')
                    .fontSize(18).fontColor(Color.White)
                }
                Column({ space: 4 }) {
                  Row({ space: 6 }) {
                    Text(r.realName || '未知')
                      .fontSize(15).fontWeight(FontWeight.Medium).fontColor('#1A1A2E')
                    if (r.lifeStatus === 3) {
                      Text('已离世')
                        .fontSize(10).fontColor('#EF4444')
                        .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                        .backgroundColor('#FEE2E2').borderRadius(4)
                    }
                  }
                  Text(r.relationDesc || '亲属').fontSize(12).fontColor('#9CA3AF')
                }
                .layoutWeight(1).alignItems(HorizontalAlign.Start)

                Text('›').fontSize(18).fontColor('#C0C4CC')
              }
              .width('100%').padding(14)
              .backgroundColor(Color.White).borderRadius(14)
              .shadow({ radius: 4, color: '#0000000A', offsetX: 0, offsetY: 2 })
            }
          })
        }
        .padding({ left: 16, right: 16 })
        .layoutWeight(1)

        Row({ space: 8 }) {
          Text('🌳').fontSize(16)
          Text('查看完整关系树').fontSize(14).fontColor('#667EEA').fontWeight(FontWeight.Medium)
          Text('›').fontSize(14).fontColor('#667EEA')
        }
        .width('100%').height(48)
        .backgroundColor(Color.White)
        .justifyContent(FlexAlign.Center)
        .border({ width: { top: 1 }, color: '#F0F0F0' })
        .onClick(() => { router.pushUrl({ url: 'pages/FamilyTree' }) })
      }
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }
}

// ─── 遗言 Tab ─────────────────────────────────────
@Component
struct TestamentTab {
  @State testaments: TestamentItem[] = []
  @State loading: boolean = false

  aboutToAppear(): void {
    this.loadData()
  }

  async loadData(): Promise<void> {
    this.loading = true
    try {
      const data = await get<TestamentItem[]>('/v1/testament/my')
      this.testaments = data || []
    } catch (e) {
    } finally {
      this.loading = false
    }
  }

  typeLabel(t: number): string {
    const labels: string[] = ['', '文字遗言', '致特定人', '财产分配', '心愿清单']
    return t >= 0 && t < labels.length ? labels[t] : '其他'
  }

  typeColor(t: number): string {
    const colors: string[] = ['', '#667EEA', '#10B981', '#F59E0B', '#EF4444']
    return t >= 0 && t < colors.length ? colors[t] : '#9CA3AF'
  }

  build() {
    Column() {
      Row() {
        Text('生前遗言').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#1A1A2E')
        Blank()
        Text('+ 新建').fontSize(14).fontColor('#667EEA').fontWeight(FontWeight.Medium)
          .onClick(() => { router.pushUrl({ url: 'pages/Testament' }) })
      }
      .width('100%').height(56).padding({ left: 16, right: 16 })

      if (this.testaments.length === 0) {
        Column({ space: 12 }) {
          Text('📝').fontSize(48)
          Text('还没有遗言').fontSize(16).fontColor('#9CA3AF')
          Text('记录您的心愿与嘱托').fontSize(13).fontColor('#C0C4CC')
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 10 }) {
          ForEach(this.testaments, (t: TestamentItem) => {
            ListItem() {
              Column({ space: 8 }) {
                Row({ space: 8 }) {
                  Text(this.typeLabel(t.testamentType))
                    .fontSize(11).fontColor(this.typeColor(t.testamentType))
                    .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                    .backgroundColor(this.typeColor(t.testamentType) + '1A').borderRadius(6)
                  Blank()
                  Row({ space: 4 }) {
                    Text(t.unlockStatus === 1 ? '🔓' : '🔒').fontSize(14)
                    Text(t.unlockStatus === 1 ? '已解锁' : '锁定中')
                      .fontSize(11).fontColor(t.unlockStatus === 1 ? '#10B981' : '#9CA3AF')
                  }
                }
                Text(t.title).fontSize(16).fontWeight(FontWeight.Medium).fontColor('#1A1A2E')
                Text(t.createTime?.substring(0, 10) || '')
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

// ─── 我的 Tab ──────────────────────────────────────
@Component
struct ProfileTab {
  @State userName: string = '用户'
  @State phone: string = ''
  @State realNameStatus: number = 0

  aboutToAppear(): void {
    this.loadUser()
  }

  async loadUser(): Promise<void> {
    try {
      const str = await load(getContext(this) as Context, STORAGE_USER)
      if (str) {
        const u = JSON.parse(str) as Record<string, ESObject>
        this.userName = (u['realName'] as string) || '用户'
        const rawPhone = (u['phone'] as string) || ''
        this.phone = rawPhone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
        this.realNameStatus = (u['realNameStatus'] as number) || 0
      }
    } catch (e) {}
  }

  statusLabel(s: number): string {
    const labels: string[] = ['未实名', '审核中', '已实名', '审核失败']
    return s >= 0 && s < labels.length ? labels[s] : '未知'
  }

  async logout(): Promise<void> {
    await clear(getContext(this) as Context)
    router.replaceUrl({ url: 'pages/Login' })
  }

  build() {
    Scroll() {
      Column({ space: 0 }) {
        // 用户卡片
        Column({ space: 12 }) {
          Stack() {
            Circle().width(72).height(72)
              .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
            Text(this.userName.charAt(0))
              .fontSize(32).fontColor(Color.White).fontWeight(FontWeight.Bold)
          }
          Column({ space: 4 }) {
            Text(this.userName)
              .fontSize(20).fontWeight(FontWeight.Bold).fontColor(Color.White)
            Text(this.phone).fontSize(13).fontColor('#E0D7FF')
            Row({ space: 4 }) {
              Text(this.realNameStatus === 2 ? '✓' : '•')
                .fontSize(11).fontColor(this.realNameStatus === 2 ? '#10B981' : '#F59E0B')
              Text(this.statusLabel(this.realNameStatus))
                .fontSize(11).fontColor(this.realNameStatus === 2 ? '#86EFAC' : '#FDE68A')
            }
          }
        }
        .width('100%')
        .padding({ top: 40, bottom: 32 })
        .linearGradient({ angle: 135, colors: [['#667EEA', 0], ['#764BA2', 1]] })
        .alignItems(HorizontalAlign.Center)

        // 菜单列表
        Column({ space: 10 }) {
          ForEach([
            { icon: '👤', label: '个人资料',   page: 'pages/Profile' } as MenuItem,
            { icon: '🌳', label: '家族关系树', page: 'pages/FamilyTree' } as MenuItem,
            { icon: '🏛️', label: '挽联纪念墙', page: 'pages/EulogyWall' } as MenuItem,
            { icon: '🛡️', label: '实名认证',   page: '' } as MenuItem,
            { icon: '⚙️', label: '设置',       page: '' } as MenuItem
          ], (item: MenuItem) => {
            Row({ space: 14 }) {
              Text(item.icon).fontSize(20)
              Text(item.label).fontSize(15).fontColor('#1A1A2E').layoutWeight(1)
              Text('›').fontSize(18).fontColor('#C0C4CC')
            }
            .width('100%').height(52).padding({ left: 16, right: 16 })
            .backgroundColor(Color.White).borderRadius(14)
            .shadow({ radius: 2, color: '#0000000A', offsetX: 0, offsetY: 1 })
            .onClick(() => {
              if (item.page) { router.pushUrl({ url: item.page }) }
            })
          })

          Button('退出登录', { type: ButtonType.Normal })
            .width('100%').height(48)
            .fontSize(15).fontColor('#EF4444')
            .backgroundColor('#FEF2F2').borderRadius(14)
            .onClick(() => { this.logout() })
        }
        .width('100%').padding({ left: 16, right: 16, top: 16, bottom: 32 })
      }
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }
}

// ─── 主导航 ────────────────────────────────────────
@Entry
@Component
struct Index {
  @State currentTab: number = 0
  private readonly tabTitles: string[] = ['首页', '关系', '遗言', '我的']
  private readonly tabIcons: string[] = ['🏠', '🌳', '📝', '👤']

  build() {
    Column() {
      // 内容区 —— Builder 返回 void，必须包在容器里才能加 layoutWeight
      Column() {
        if (this.currentTab === 0) { DashboardTab() }
        else if (this.currentTab === 1) { RelationsTab() }
        else if (this.currentTab === 2) { TestamentTab() }
        else { ProfileTab() }
      }
      .layoutWeight(1)
      .width('100%')

      // 底部导航
      Row() {
        ForEach(this.tabTitles, (title: string, idx: number) => {
          Column({ space: 3 }) {
            Text(this.tabIcons[idx])
              .fontSize(this.currentTab === idx ? 24 : 22)
              .fontColor(this.currentTab === idx ? '#667EEA' : '#9CA3AF')
            Text(title)
              .fontSize(10)
              .fontColor(this.currentTab === idx ? '#667EEA' : '#9CA3AF')
              .fontWeight(this.currentTab === idx ? FontWeight.Medium : FontWeight.Normal)
          }
          .layoutWeight(1).height(56)
          .justifyContent(FlexAlign.Center)
          .onClick(() => { this.currentTab = idx })
        })
      }
      .width('100%')
      .backgroundColor(Color.White)
      .border({ width: { top: 1 }, color: '#F0F0F0' })
    }
    .width('100%').height('100%').backgroundColor('#F8F9FE')
  }
}
