package com.example.minispring;

/** 程序入口：手动注册定义，再从容器取得 Controller。 */
public class Application {
    /** 演示 IoC 容器的完整工作流程。 */
    public static void main(String[] args) {
        BeanFactory beanFactory = new BeanFactory();

        // 真实 Spring 通常由扫描或 @Bean 方法完成；本例故意手动注册。
        beanFactory.registerBeanDefinition(new BeanDefinition("userRepository", UserRepository.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("userService", UserService.class));
        beanFactory.registerBeanDefinition(new BeanDefinition("userController", UserController.class));

        System.out.println("\n--- 第一次获取 UserController：触发递归创建 ---");
        UserController controller = beanFactory.getBean(UserController.class);
        controller.handleRequest();

        System.out.println("\n--- 第二次获取 UserController：直接复用单例 ---");
        UserController sameController = beanFactory.getBean(UserController.class);
        System.out.println("[验证] 两次是否同一个对象：" + (controller == sameController));
    }
}
