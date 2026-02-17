<template>
  <router-view v-slot="{ Component }">
    <transition name="page">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup></script>

<style>
/* ═══════════════════════════════════════════════════════
   全局 CSS 变量 & 重置
════════════════════════════════════════════════════════ */
:root {
  --c-bg:        #F1F5F9;
  --c-surface:   #FFFFFF;
  --c-border:    #E2E8F0;
  --c-sidebar:   #0C1730;
  --c-sidebar2:  #111E3A;
  --c-primary:   #5A67F2;
  --c-primary-h: #4451E0;
  --c-amber:     #F59E0B;
  --c-amber-h:   #D97706;
  --c-success:   #10B981;
  --c-danger:    #EF4444;
  --c-txt:       #1E293B;
  --c-txt-s:     #64748B;
  --c-txt-i:     #94A3B8;
  --radius-sm:   8px;
  --radius-md:   12px;
  --radius-lg:   18px;
  --shadow-sm:   0 1px 3px rgba(0,0,0,.08), 0 1px 2px rgba(0,0,0,.04);
  --shadow-md:   0 4px 20px rgba(0,0,0,.08), 0 1px 4px rgba(0,0,0,.04);
  --shadow-lg:   0 12px 40px rgba(0,0,0,.12), 0 4px 12px rgba(0,0,0,.06);
  --transition:  all .22s cubic-bezier(.4,0,.2,1);
}

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

html { font-size: 14px; scroll-behavior: smooth; }

body {
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Source Han Sans CN', system-ui, -apple-system, sans-serif;
  background: var(--c-bg);
  color: var(--c-txt);
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ── 页面切换动画 ── */
.page-enter-active,
.page-leave-active { transition: opacity .2s ease, transform .22s ease; }
.page-enter-from   { opacity: 0; transform: translateY(6px); }
.page-leave-to     { opacity: 0; transform: translateY(-4px); }

/* ── Element Plus 覆盖 ── */
.el-card {
  --el-card-border-radius: var(--radius-md);
  --el-card-border-color: var(--c-border);
  --el-card-box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border) !important;
}

.el-button--primary {
  --el-button-bg-color: var(--c-primary);
  --el-button-border-color: var(--c-primary);
  --el-button-hover-bg-color: var(--c-primary-h);
  --el-button-hover-border-color: var(--c-primary-h);
  border-radius: var(--radius-sm) !important;
  font-weight: 600;
  letter-spacing: .3px;
}

.el-button {
  border-radius: var(--radius-sm) !important;
  font-weight: 500;
  transition: var(--transition) !important;
}

.el-input__wrapper {
  border-radius: var(--radius-sm) !important;
  transition: var(--transition) !important;
}

.el-input__wrapper:hover,
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(90,103,242,.18) !important;
}

.el-tag { border-radius: 6px !important; font-weight: 500; font-size: 12px; }

/* ── 滚动条美化 ── */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 99px; }
::-webkit-scrollbar-thumb:hover { background: #94A3B8; }

/* ── 全局工具类 ── */
.rl-card {
  background: var(--c-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}
.rl-card:hover { box-shadow: var(--shadow-md); }

.rl-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px;
  background: var(--c-danger); color: #fff;
  border-radius: 99px; font-size: 11px; font-weight: 700;
}

/* ── 数字渐变装饰 ── */
.gradient-text {
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-amber) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ══ 全局手机端 El Plus 覆盖 ══ */
@media (max-width: 768px) {
  /* 对话框全屏 */
  .el-dialog { --el-dialog-margin-top: 0 !important; }
  .el-dialog__wrapper .el-dialog,
  .el-overlay-dialog .el-dialog {
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0 !important;
    position: fixed !important;
    bottom: 0 !important;
    left: 0 !important;
    right: 0 !important;
  }
  /* Drawer 全宽 */
  .el-drawer { width: 100% !important; border-radius: var(--radius-lg) var(--radius-lg) 0 0 !important; }
  .el-drawer.rtl  { border-radius: var(--radius-lg) var(--radius-lg) 0 0 !important; }
  /* 表格横滚 */
  .el-table { overflow-x: auto; }
  .el-table__body-wrapper { overflow-x: auto; }
  /* Tabs 横滚 */
  .el-tabs__nav-wrap { overflow-x: auto; }
  .el-tabs__nav-wrap::after { display: none; }
  /* 表单标签宽度自适应 */
  .el-form-item { flex-direction: column; }
  .el-form-item .el-form-item__label { text-align: left !important; width: auto !important; float: none !important; }
  .el-form-item .el-form-item__content { margin-left: 0 !important; }
  /* 按钮组响应 */
  .el-button-group { flex-wrap: wrap; }
}

</style>
