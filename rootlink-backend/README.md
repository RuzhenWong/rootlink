# RootLink 后端服务

## 项目说明

RootLink家谱亲属关系应用后端服务,基于Spring Boot 3.2开发。

当前实现功能:
- ✅ 用户注册
- ✅ 用户登录/登出
- ✅ 短信验证码(Mock实现)
- ✅ 实名认证(Mock实现)
- ✅ JWT身份认证
- ✅ Redis缓存
- ✅ 统一响应封装
- ✅ 全局异常处理

## 技术栈

- Spring Boot 3.2.2
- MyBatis Plus 3.5.5
- MySQL 8.0
- Redis 7.0
- JWT 0.12.3
- Hutool 5.8.24

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+

### 2. 数据库初始化

执行SQL脚本创建数据库和表:

```sql
-- 创建数据库
CREATE DATABASE rootlink CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用之前提供的rootlink-database-design.sql创建表
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rootlink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的MySQL密码

  data:
    redis:
      host: localhost
      port: 6379
      password:  # 如果Redis设置了密码,在这里填写
```

### 4. 运行项目

```bash
# 进入项目目录
cd rootlink-backend

# 使用Maven编译
mvn clean install

# 运行
mvn spring-boot:run

# 或者直接运行主类
java -jar target/rootlink-backend-1.0.0.jar
```

服务启动后访问: http://localhost:8080/api

## API接口文档

### 1. 发送验证码

**接口**: `POST /api/v1/auth/sms/send`

**请求体**:
```json
{
  "phone": "13800138000",
  "type": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1707292800000
}
```

**说明**: 验证码会在控制台打印(Mock实现)

---

### 2. 用户注册

**接口**: `POST /api/v1/auth/register`

**请求体**:
```json
{
  "phone": "13800138000",
  "password": "Abc123456",
  "code": "123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null,
  "timestamp": 1707292800000
}
```

---

### 3. 用户登录

**接口**: `POST /api/v1/auth/login`

**请求体**:
```json
{
  "phone": "13800138000",
  "password": "Abc123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "realName": null,
    "realNameStatus": 0,
    "expireTime": 1707379200000
  },
  "timestamp": 1707292800000
}
```

---

### 4. 获取当前用户信息

**接口**: `GET /api/v1/user/current`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "13800138000",
    "email": null,
    "realName": null,
    "realNameStatus": 0,
    "status": 1,
    "lifeStatus": 0,
    "allowSearch": true,
    "createTime": "2024-02-07T10:00:00",
    "lastLoginTime": "2024-02-07T14:00:00"
  },
  "timestamp": 1707292800000
}
```

---

### 5. 提交实名认证

**接口**: `POST /api/v1/user/realname/submit`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "realName": "张三",
  "idCard": "440301198005150012"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "实名认证成功",
  "data": null,
  "timestamp": 1707292800000
}
```

**说明**: 当前使用Mock实现,仅做格式校验,实际不会调用第三方API

---

### 6. 查询实名状态

**接口**: `GET /api/v1/user/realname/status`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "realNameStatus": 2,
    "realName": "张三",
    "realNameTime": "2024-02-07T15:00:00"
  },
  "timestamp": 1707292800000
}
```

---

### 7. 登出

**接口**: `POST /api/v1/auth/logout`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": 1707292800000
}
```

## Mock服务说明

### 短信服务 (SmsService)

当前为Mock实现,验证码会在控制台打印。

**替换为真实服务**:

1. 选择短信服务商(阿里云/腾讯云/华为云等)
2. 引入对应SDK依赖
3. 修改 `SmsService.java` 中的TODO部分
4. 配置AccessKey等参数

### 实名认证服务 (RealNameVerifyService)

当前为Mock实现,仅做基础格式校验。

**替换为真实服务**:

1. 选择实名认证服务商(阿里云/腾讯云等)
2. 引入对应SDK依赖
3. 修改 `RealNameVerifyService.java` 中的TODO部分
4. 配置AppKey等参数

## 安全配置

⚠️ **生产环境部署前必须修改**:

1. `application.yml` 中的AES密钥:
```yaml
security:
  aes:
    key: RootLinkAES256!!  # 必须修改为32字节的强密钥
```

2. `application.yml` 中的JWT密钥:
```yaml
jwt:
  secret: RootLink@2026!SecretKey#Ultimate-Change-This-In-Production
```

3. 数据库密码

4. Redis密码(如有)

## 项目结构

```
src/main/java/com/rootlink/backend/
├── common/              # 公共类
│   ├── Result.java      # 统一响应
│   └── ErrorCode.java   # 错误码
├── config/              # 配置类
├── controller/          # 控制器
├── dto/                 # 数据传输对象
├── entity/              # 实体类
├── exception/           # 异常处理
├── interceptor/         # 拦截器
├── mapper/              # 数据访问层
├── service/             # 业务逻辑层
├── utils/               # 工具类
├── vo/                  # 视图对象
└── RootLinkApplication.java  # 启动类
```

## 测试建议

使用Postman或Apifox测试接口:

1. 发送验证码 → 查看控制台获取验证码
2. 使用验证码注册用户
3. 登录获取Token
4. 使用Token访问需要认证的接口
5. 提交实名认证

## 常见问题

### Q: 验证码收不到?
A: 当前是Mock实现,验证码在控制台打印,请查看IDEA控制台输出。

### Q: Token过期时间?
A: 默认24小时,可在application.yml中修改jwt.expire-time配置。

### Q: 如何对接真实短信服务?
A: 参考SmsService.java中的TODO注释,替换为真实API调用即可。

## 下一步开发

- [ ] 接入真实短信服务
- [ ] 接入真实实名认证API
- [ ] 实现文件上传功能
- [ ] 实现亲属关系模块
- [ ] 实现家族树功能
- [ ] ...更多功能

## 联系方式

如有问题,请查看项目文档或提Issue。
