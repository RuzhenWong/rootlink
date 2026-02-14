import UIAbility from '@ohos.app.ability.UIAbility'
import hilog from '@ohos.hilog'
import AbilityDelegatorRegistry from '@ohos.app.ability.abilityDelegatorRegistry'
import { Hypium } from '@ohos/hypium'
import abilityTest from './test/Ability.test'

export default class TestAbility extends UIAbility {
  onCreate(): void {
    hilog.info(0x0000, 'testTag', 'TestAbility onCreate')
    const abilityDelegator = AbilityDelegatorRegistry.getAbilityDelegator()
    const abilityDelegatorArguments = AbilityDelegatorRegistry.getArguments()
    hilog.info(0x0000, 'testTag', 'start run testcase!!!')
    Hypium.hypiumTest(abilityDelegator, abilityDelegatorArguments, [abilityTest])
  }
}
