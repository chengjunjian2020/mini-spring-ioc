package com.example.minispring;

/** 程序入口：手动注册定义，再演示构造器注入和字段注入。 */
public class Application {
    /** 演示 IoC 容器的完整工作流程。 */
    public static void main(String[] args) {
        BeanFactory beanFactory = new BeanFactory();

        // 真实 Spring 通常由扫描或 @Bean 方法完成；本例故意手动注册。
        beanFactory.registerBeanDefinition(new BeanDefinition("userRepository", UserRepository.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("userService", UserService.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("userController", UserController.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("fieldAService", FieldAService.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("fieldBService", FieldBService.class));

        // 模拟 Spring 在容器启动阶段预先创建非懒加载的单例 Bean。
        beanFactory.preInstantiateSingletons();

        System.out.println("\n--- 预实例化后获取 UserController：直接命中单例缓存 ---");
        UserController controller = beanFactory.getBean(UserController.class);
        controller.handleRequest();

        System.out.println("\n--- 再次获取 UserController：仍然复用同一个单例 ---");
        UserController sameController = beanFactory.getBean(UserController.class);
        System.out.println("[验证] 两次是否同一个对象：" + (controller == sameController));

        System.out.println("\n--- 演示字段注入循环依赖：获取 FieldAService ---");
        FieldAService fieldAService = beanFactory.getBean(FieldAService.class);
        boolean circularReferenceResolved = fieldAService
                .getFieldBService()
                .getFieldAService() == fieldAService;
        System.out.println("[验证] FieldAService.fieldBService.fieldAService 是否指向原始 FieldAService："
                + circularReferenceResolved);

        beanFactory.registerBeanDefinition(new BeanDefinition("aService", AService.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("bService", BService.class));

        System.out.println("\n--- 演示构造器循环依赖：获取 AService ---");
        try {
            beanFactory.getBean(AService.class);
        } catch (StackOverflowError e) {
            System.out.print(e);
            System.out.println("构造器循环依赖导致无限递归，当前容器无法创建 Bean");
        }
    }
}
