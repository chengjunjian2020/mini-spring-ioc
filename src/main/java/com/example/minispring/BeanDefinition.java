package com.example.minispring;

/**
 * Bean 的“设计图”。容器注册阶段只保存元数据，真正对象在首次 getBean 时才创建。
 */
public class BeanDefinition {
    /** Bean 的名字，便于日志和理解；本示例不按名称查找。 */
    private final String beanName;

    /** Bean 对应的具体实现类。 */
    private final Class<?> beanClass;

    /** 创建一个 Bean 定义。 */
    public BeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

    /** 返回 Bean 名称。 */
    public String getBeanName() {
        return beanName;
    }

    /** 返回 Bean 的 Class。 */
    public Class<?> getBeanClass() {
        return beanClass;
    }
}
