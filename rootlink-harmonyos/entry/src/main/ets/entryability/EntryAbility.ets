import UIAbility from '@ohos.app.ability.UIAbility'
import hilog from '@ohos.hilog'
import window from '@ohos.window'

export default class EntryAbility extends UIAbility {
  onCreate(): void {
    hilog.info(0x0000, 'RootLink', 'onCreate')
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    hilog.info(0x0000, 'RootLink', 'onWindowStageCreate')
    windowStage.loadContent('pages/Splash', (err) => {
      if (err.code) {
        hilog.error(0x0000, 'RootLink', 'loadContent failed: %{public}s', JSON.stringify(err))
      }
    })
  }

  onForeground(): void {}
  onBackground(): void {}
}
