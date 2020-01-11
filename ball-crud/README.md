# 开发规范
##
## 业务模块层次结构
```
<business_package>
├── ApplicationManager.java: 业务实现
├── entity: jpa entity and repository
│   ├── ApplicationEntity.java
│   ├── ApplicationEntityRepository.java
│   └── ApplicationFunctionEntity.java
├── model: business model
│   ├── ApplicationFunction.java
│   ├── ApplicationMapper.java
│   └── ApplicationRegisterInfo.java
├── service: 对接服务，如三方对接，中间件对接...
│   ├── ApplicationDubboService.java
│   ├── ApplicationSearchService.java
│   ├── dto---可选的需要的数据传输对象定义
└── web---
    └── ApplicationAction.java: web-api接口等面向前端逻辑
```
xxxManager：模块通用业务逻辑实现直接放置模块根目录下(是否需要接口+实现随意)
* 注解类型必须为@Service
* Manager不能暴露任何Entity，给调用端对Entity产生实际依赖，暴露出去的Entity属性必须包装为Model

xxxEntity：任何entity命名必须以Entity结尾
Model名称不必添加任何前后缀
任何基于属性注解（jpa,validation，序列化..）都必须定义在getter方法防止跨方法直接访问字段
    

