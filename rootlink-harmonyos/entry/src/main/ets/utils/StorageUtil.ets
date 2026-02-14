import preferences from '@ohos.data.preferences'

const STORE_NAME = 'rl_prefs'

export async function save(ctx: Context, key: string, value: string): Promise<void> {
  const pref = await preferences.getPreferences(ctx, STORE_NAME)
  await pref.put(key, value)
  await pref.flush()
}

export async function load(ctx: Context, key: string): Promise<string> {
  const pref = await preferences.getPreferences(ctx, STORE_NAME)
  const val = await pref.get(key, '')
  return val as string
}

export async function remove(ctx: Context, key: string): Promise<void> {
  const pref = await preferences.getPreferences(ctx, STORE_NAME)
  await pref.delete(key)
  await pref.flush()
}

export async function clear(ctx: Context): Promise<void> {
  const pref = await preferences.getPreferences(ctx, STORE_NAME)
  await pref.clear()
  await pref.flush()
}
