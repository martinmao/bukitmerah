# 开发规范
##
## 业务模块层次结构
```
<business_model_package>
├── ApplicationManager.java: 应用层管理器，负责事务边界，技术性参数校验，通过repository(不仅仅是数据库，其他中间件，三方调用统称为repository)的数据获取，调用业务单元执行业务，并将业务产生的状态变更通过repository持久化
├── entity: entity and repository
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

Repository&Entities说明
* Entity中所有Primitive属性必须都定义为Wrapping类型，避免因为java初始化默认值带来的二义性(0 or null,false or null)
* 关联查询尽可能小表驱动大表，尤其在大数据量情况下
```
class DepartmentEntity{
    @OneToMany
    List<UserEntity> users;
    ...
}
class UserEntity{
    @ManyToOne
    DepartmentEntity dept;
}

interface UserRepository<UserEntity> ...{
    //查询给定部门下所有用户，不建议的姿势
    Page<UserEntity> findByDept_Id(Long deptId);
}
interface DepartmentRepository<DepartmentEntity>  ...{
    //查询给定部门下所有用户，建议的姿势
    Page<UserEntity> findUserById(Long deptId);
}
```
## 持久层方法命名规范
方法前缀
```
save，保存
get，返回0或1个结果，如果可能为空尽可能返回Optional
find，返回0-多个结果(通常结果集是一个page)
findAll，返回0-全部结果
exists，返回一个Boolean，意义上表明是否存在
count，返回一个计数统计结果
sum，返回一个计数汇总统计结果
```
检索条件使用By作为统一的介词，后面跟条件属性：如
```
findByEmail
```
如果检索（更新）不是一个完整Entity，方法名必须明确包含被检索（更新）的信息：如
```
findNameAndEmailByUserId
updatePassword
```
更新操作尽可能以主键（唯一约束）作为条件，不建议其他业务含义字段作为更新条件（即便其具备业务唯一性，但因业务易变 还是不建议），基于此前提，更新不需要申明条件
```
updatePasswordByUsername，不建议
updatePassword(Long id)，建议
特例，如果明确在表中定义了唯一约束，则允许基于唯一约束进行更新，此时方法签名中必须明确包含更新条件
updatePasswordByEmail(String email),//UNIQUE INDEX EMAIL.
```
禁止持久层使用create作为前缀（其可能包含create&save的过程），但create的过程一定是在内存中，所以create操作应该在上层进行处理
```
create(UserDto user),错误的示范

UserEntity user=userMapper.mapForSave(UserDto user);
userRepository.save(user);
```
## 应用层规范
应用层使用的申明式校验框架（JSR-303）仅限用于技术性校验（包括国际通用的业务属性，如年龄，性别，Email,URL等校验），即确保程序可以正常执行（检查空值，空串，空集，类型兼容，大小兼容），不会出现NPE（NullPointException），NFE（NumberFormatException）CCE（ClassCastException）等错误
而业务规则性校验，必须完整在业务层代码中实现(提供完整清晰的规则视图)。且业务规则抛出的校验异常（类型，参数异常，规则异常）必须明确包含业务错误信息.
```
@Max("5")
@Min("1")
int orderState;
...

void pay(@Valid Order order){//错误的示范，不清楚1-5代表什么，1-5是订单几种状态，属于业务范畴

}
void pay(Order order){
    assertOrderStateValid(Order order);//orderState range in(1-5)...
    ....business rules
    changeOrderState(order,OrderState.PAYED);//check whether can change to 'PAYED' state.
}
```

    

