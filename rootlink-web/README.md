# RootLink Web前端

## 项目说明

RootLink家谱亲属关系应用Web前端，基于Vue 3 + Vite开发。

当前实现功能：
- ✅ 用户注册
- ✅ 用户登录/登出
- ✅ 实名认证
- ✅ 个人中心
- ✅ 首页Dashboard
- ✅ 响应式布局

## 技术栈

- Vue 3.4
- Vite 5.0
- Vue Router 4.2
- Pinia 2.1 (状态管理)
- Element Plus 2.5 (UI组件库)
- Axios (HTTP客户端)
- Day.js (日期处理)

## 快速开始

### 1. 安装依赖

```bash
cd rootlink-web
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

构建产物在 `dist` 目录。

## 项目结构

```
src/
├── api/              # API接口
│   ├── auth.js       # 认证接口
│   └── user.js       # 用户接口
├── assets/           # 静态资源
├── components/       # 公共组件
├── layouts/          # 布局组件
│   └── MainLayout.vue
├── router/           # 路由配置
│   └── index.js
├── stores/           # Pinia状态管理
│   └── auth.js
├── utils/            # 工具函数
│   ├── request.js    # Axios封装
│   ├── storage.js    # 本地存储
│   └── validator.js  # 校验工具
├── views/            # 页面组件
│   ├── auth/         # 认证页面
│   │   ├── Login.vue
│   │   └── Register.vue
│   ├── dashboard/    # 首页
│   │   └── Dashboard.vue
│   └── user/         # 用户页面
│       ├── RealName.vue
│       └── Profile.vue
├── App.vue           # 根组件
└── main.js           # 入口文件
```

## 使用说明

### 1. 注册账号

1. 访问 http://localhost:3000/register
2. 输入手机号，点击"发送验证码"
3. **重要**: 验证码在后端控制台打印，查看后端日志获取验证码
4. 输入验证码和密码（6-20位，包含大小写字母和数字）
5. 点击"注册"

### 2. 登录

1. 访问 http://localhost:3000/login
2. 输入手机号和密码
3. 点击"登录"

### 3. 实名认证

1. 登录后，首页会提示"未完成实名认证"
2. 点击"立即认证"或访问左侧菜单"实名认证"
3. 输入真实姓名和身份证号
4. 点击"提交认证"
5. **注意**: 当前为Mock模式，仅做格式校验

### 4. 查看个人信息

点击左侧菜单"个人中心"查看详细信息。

## 功能演示流程

完整测试流程：

```
1. 注册 → 查看后端控制台获取验证码 → 完成注册
2. 登录 → 跳转到首页
3. 首页显示未实名提醒
4. 点击"立即认证" → 填写实名信息 → 提交
5. 返回首页 → 实名状态已更新
6. 查看个人中心 → 显示完整信息
7. 点击右上角头像 → 退出登录
```

## 配置说明

### API代理配置

在 `vite.config.js` 中配置了API代理：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true,
    },
  },
}
```

如果后端地址不是 `http://localhost:8080`，请修改此配置。

### 路由配置

路由配置在 `src/router/index.js`，包含路由守卫：

- 未登录访问需要认证的页面 → 跳转登录页
- 登录后自动获取用户信息
- 自动设置页面标题

## 状态管理

使用Pinia管理全局状态，主要包括：

- `authStore`: 认证状态（token、用户信息、登录状态）

## 样式说明

- 使用Element Plus提供的组件样式
- 自定义样式使用scoped避免污染
- 响应式设计，支持不同屏幕尺寸

## 注意事项

1. **验证码获取**: 当前为Mock实现，验证码在后端控制台打印
2. **实名认证**: 当前为Mock实现，仅做格式校验
3. **Token过期**: Token过期后会自动跳转登录页
4. **跨域问题**: 开发环境通过Vite代理解决，生产环境需配置Nginx

## 常见问题

### Q: 验证码收不到？
A: 验证码在后端控制台打印，请查看后端IDEA或终端的日志输出。

### Q: 登录后页面空白？
A: 检查浏览器控制台是否有错误，确认后端服务是否正常运行。

### Q: 实名认证提交后没反应？
A: 当前为Mock实现，提交后会立即成功，刷新页面查看实名状态。

### Q: 如何修改后端地址？
A: 修改 `vite.config.js` 中的 `proxy.target` 配置。

## 下一步开发

- [ ] 亲属关系管理页面
- [ ] 家族树可视化
- [ ] 生前遗言功能
- [ ] 挽联缅怀功能
- [ ] 请帖管理
- [ ] 消息中心

## 部署说明

### 开发环境
```bash
npm run dev
```

### 生产构建
```bash
npm run build
```

### 生产部署 (Nginx示例)

```nginx
server {
    listen 80;
    server_name rootlink.example.com;

    root /var/www/rootlink-web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 联系方式

如有问题，请查看项目文档或提Issue。
