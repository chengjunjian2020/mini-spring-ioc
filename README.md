# mini-spring-ioc

一个只演示 IoC 核心流程的纯 Java 项目：Bean 定义注册、单例缓存、反射创建和构造器注入。

运行方式（JDK 21 或其他可用 JDK）：

```powershell
cd E:\learningProject\mini-spring-ioc
$sources = Get-ChildItem src\main\java\com\example\minispring\*.java | ForEach-Object FullName
javac -encoding UTF-8 -d out $sources
java -cp out com.example.minispring.Application
```

刻意不包含：包扫描、循环依赖、AOP、三级缓存、自动配置。
