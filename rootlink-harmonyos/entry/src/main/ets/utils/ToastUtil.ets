import promptAction from '@ohos.promptAction'

export function toast(msg: string, duration: number = 2000): void {
  promptAction.showToast({ message: msg, duration: duration })
}

export function toastErr(msg: string): void {
  toast(msg || '操作失败', 3000)
}
