package com.example.minispring;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * 最小 Bean 工厂：管理定义、单例对象，并通过构造器完成依赖注入。
 */
public class BeanFactory {
    /** 已注册的 Bean 定义。本例按 Class 作为查找键。 */
    private final Map<Class<?>, BeanDefinition> beanDefinitions = new HashMap<>();

    /** 一级单例缓存：一个 Class 在整个容器中只创建一个对象。 */
    private final Map<Class<?>, Object> singletonObjects = new HashMap<>();

    /**
     * 注册一个 Bean 定义；此时不创建实例，体现“定义”和“对象”分离。
     */
    public void registerBeanDefinition(BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        if (beanDefinitions.containsKey(beanClass)) {
            throw new IllegalArgumentException("Bean 已注册：" + beanClass.getName());
        }
        beanDefinitions.put(beanClass, beanDefinition);
        System.out.printf("[注册定义] name=%s, class=%s%n",
                beanDefinition.getBeanName(), beanClass.getSimpleName());
    }

    /**
     * 按类型取得 Bean。先查单例缓存，未命中才创建对象。
     *
     * @param requiredType 需要的 Bean 类型
     * @return 缓存中的对象或新创建的对象
     */
    public <T> T getBean(Class<T> requiredType) {
        Object singleton = singletonObjects.get(requiredType);
        if (singleton != null) {
            System.out.printf("[命中单例缓存] %s%n", requiredType.getSimpleName());
            return requiredType.cast(singleton);
        }

        BeanDefinition definition = beanDefinitions.get(requiredType);
        if (definition == null) {
            throw new IllegalArgumentException("没有找到 Bean 定义：" + requiredType.getName());
        }
        return requiredType.cast(createBean(definition));
    }

    /**
     * 用反射调用唯一构造器。构造器的每一个参数都会递归调用 getBean，
     * 因而先创建最底层依赖，再创建当前对象。
     */
    private Object createBean(BeanDefinition definition) {
        Class<?> beanClass = definition.getBeanClass();
        System.out.printf("[创建 Bean] %s%n", beanClass.getSimpleName());

        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException(beanClass.getSimpleName()
                    + " 必须且只能有一个构造器，本 mini 容器才能确定注入方式");
        }

        try {
            Constructor<?> constructor = constructors[0];
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> dependencyType = parameterTypes[i];
                System.out.printf("[解析依赖] %s 需要 %s%n",
                        beanClass.getSimpleName(), dependencyType.getSimpleName());
                arguments[i] = getBean(dependencyType);
            }

            Object bean = constructor.newInstance(arguments);
            singletonObjects.put(beanClass, bean);
            System.out.printf("[放入单例缓存] %s%n", beanClass.getSimpleName());
            return bean;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("创建 Bean 失败：" + beanClass.getName(), e);
        }
    }
}
