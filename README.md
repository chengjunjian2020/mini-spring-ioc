# mini-spring-ioc

一个只演示 IoC 核心流程的纯 Java 项目：Bean 定义注册、单例缓存、反射创建、构造器注入和字段注入。

运行方式（JDK 21 或其他可用 JDK）：

```powershell
cd E:\learningProject\mini-spring-ioc
$sources = Get-ChildItem src\main\java\com\example\minispring\*.java | ForEach-Object FullName
javac -encoding UTF-8 -d out $sources
java -cp out com.example.minispring.Application
```

刻意不包含：包扫描、AOP、三级缓存、自动配置。

`Application` 还包含一个构造器循环依赖演示：`AService -> BService -> AService`。
它会触发 `StackOverflowError`，并输出“构造器循环依赖导致无限递归，当前容器无法创建 Bean”。

同时包含一个字段注入循环依赖演示：`FieldAService <-> FieldBService`。
三级缓存分别保存最终单例、早期引用和延迟生成早期引用的 `ObjectFactory`。
当前工厂直接返回原始空对象，字段注入完成后转入最终单例缓存；未来可以在工厂中生成代理对象。
