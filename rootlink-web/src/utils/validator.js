export const validator = {
  // 手机号校验
  isPhone(phone) {
    return /^1[3-9]\d{9}$/.test(phone)
  },

  // 密码校验（6-20位，包含大小写字母和数字）
  isPassword(password) {
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{6,20}$/.test(password)
  },

  // 身份证号校验
  isIdCard(idCard) {
    return /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/.test(idCard)
  },

  // 验证码校验（6位数字）
  isCode(code) {
    return /^\d{6}$/.test(code)
  },
}
