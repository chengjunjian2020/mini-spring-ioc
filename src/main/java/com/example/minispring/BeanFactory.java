package com.example.minispring;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 最小 Bean 工厂：管理定义、单例对象，并演示构造器注入和字段注入。
 */
public class BeanFactory {
    /** 当前线程中的 Bean 创建调用层级，仅用于让递归日志更容易阅读。 */
    private final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    /** 避免循环依赖演示在栈溢出前输出过多重复日志。 */
    private static final int MAX_VERBOSE_LOG_DEPTH = 12;

    /** 当前顶层 getBean 调用是否已经输出过深层递归提示。 */
    private final ThreadLocal<Boolean> recursionNoticeLogged = ThreadLocal.withInitial(() -> false);

    /** 已注册的 Bean 定义。本例按 Class 作为查找键。 */
    private final Map<Class<?>, BeanDefinition> beanDefinitions = new HashMap<>();

    /** 一级单例缓存：一个 Class 在整个容器中只创建一个最终对象。 */
    private final Map<Class<?>, Object> singletonObjects = new HashMap<>();

    /** 二级缓存：三级工厂生成早期引用后，暂存字段注入中的半成品对象。 */
    private final Map<Class<?>, Object> earlySingletonObjects = new HashMap<>();

    /** 三级缓存：不直接保存对象，而是保存按需生成早期引用的工厂。 */
    private final Map<Class<?>, ObjectFactory<?>> singletonFactories = new HashMap<>();

    /** 正在创建的 Bean 类型，决定 getBean 是否允许查找二级和三级缓存。 */
    private final Set<Class<?>> singletonsCurrentlyInCreation = new HashSet<>();

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
     * 按类型取得 Bean。先查最终单例缓存，再查提前暴露缓存，均未命中才创建对象。
     *
     * @param requiredType 需要的 Bean 类型
     * @return 缓存中的对象或新创建的对象
     */
    public <T> T getBean(Class<T> requiredType) {
        int currentDepth = callDepth.get();
        if (currentDepth == 0) {
            recursionNoticeLogged.set(false);
        }
        logAtDepth(currentDepth, "[getBean] " + requiredType.getSimpleName());
        callDepth.set(currentDepth + 1);
        try {
            Object singleton = singletonObjects.get(requiredType);
            if (singleton != null) {
                logAtDepth(currentDepth, "[命中单例缓存] " + requiredType.getSimpleName());
                return requiredType.cast(singleton);
            }
            logAtDepth(currentDepth, "[一级缓存未命中] " + requiredType.getSimpleName());

            if (singletonsCurrentlyInCreation.contains(requiredType)) {
                Object earlySingleton = earlySingletonObjects.get(requiredType);
                if (earlySingleton != null) {
                    logAtDepth(currentDepth, "[命中二级早期引用缓存] " + requiredType.getSimpleName());
                    return requiredType.cast(earlySingleton);
                }
                logAtDepth(currentDepth, "[二级缓存未命中] " + requiredType.getSimpleName());

                ObjectFactory<?> singletonFactory = singletonFactories.get(requiredType);
                if (singletonFactory != null) {
                    logAtDepth(currentDepth, "[命中三级缓存] " + requiredType.getSimpleName());
                    logAtDepth(currentDepth, "[调用三级缓存工厂] " + requiredType.getSimpleName());
                    Object earlyReference = singletonFactory.getObject();
                    earlySingletonObjects.put(requiredType, earlyReference);
                    singletonFactories.remove(requiredType);
                    logAtDepth(currentDepth, "[早期引用转入二级缓存] " + requiredType.getSimpleName());
                    return requiredType.cast(earlyReference);
                }
            }

            BeanDefinition definition = beanDefinitions.get(requiredType);
            if (definition == null) {
                throw new IllegalArgumentException("没有找到 Bean 定义：" + requiredType.getName());
            }
            return requiredType.cast(createBean(definition));
        } finally {
            if (currentDepth == 0) {
                callDepth.remove();
                recursionNoticeLogged.remove();
            } else {
                callDepth.set(currentDepth);
            }
        }
    }

    /**
     * 预先创建所有已注册的单例 Bean。
     * <p>
     * getBean 本身已经包含“缓存优先、未命中则创建”的逻辑，因此这里不需要
     * 重复编写创建代码，只要逐个调用 getBean 即可。
     */
    public void preInstantiateSingletons() {
        System.out.println("\n--- 开始预实例化单例 Bean ---");
        for (BeanDefinition definition : beanDefinitions.values()) {
            Class<?> beanClass = definition.getBeanClass();
            System.out.printf("[预实例化] 获取 %s%n", beanClass.getSimpleName());
            getBean(beanClass);
        }
        System.out.println("--- 预实例化结束 ---");
    }

    /**
     * 用反射调用唯一构造器。构造器参数 Bean 递归解析依赖；无参且带
     * {@link Autowired} 字段的 Bean 则先提前暴露空对象，再完成字段注入。
     */
    private Object createBean(BeanDefinition definition) {
        Class<?> beanClass = definition.getBeanClass();
        int currentDepth = callDepth.get();
        logAtDepth(currentDepth, "[createBean] " + beanClass.getSimpleName());

        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException(beanClass.getSimpleName()
                    + " 必须且只能有一个构造器，本 mini 容器才能确定注入方式");
        }

        try {
            Constructor<?> constructor = constructors[0];
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();

            if (parameterTypes.length == 0 && hasAutowiredField(beanClass)) {
                Object bean = constructor.newInstance();
                logAtDepth(currentDepth, "[创建空对象] " + beanClass.getSimpleName());

                singletonsCurrentlyInCreation.add(beanClass);
                logAtDepth(currentDepth, "[标记正在创建] " + beanClass.getSimpleName());
                singletonFactories.put(beanClass, () -> {
                    logAtDepth(Math.max(0, callDepth.get() - 1), "[ObjectFactory.getObject] 返回原始空对象："
                            + beanClass.getSimpleName());
                    return bean;
                });
                logAtDepth(currentDepth, "[放入三级缓存] " + beanClass.getSimpleName());

                try {
                    for (Field field : beanClass.getDeclaredFields()) {
                        if (!field.isAnnotationPresent(Autowired.class)) {
                            continue;
                        }

                        Class<?> dependencyType = field.getType();
                        logAtDepth(currentDepth, "[注入字段] " + beanClass.getSimpleName()
                                + "." + field.getName() + " 需要 " + dependencyType.getSimpleName());
                        Object dependency = getBean(dependencyType);
                        field.setAccessible(true);
                        field.set(bean, dependency);
                        logAtDepth(currentDepth, "[字段注入完成] " + beanClass.getSimpleName()
                                + "." + field.getName());
                    }

                    earlySingletonObjects.remove(beanClass);
                    singletonObjects.put(beanClass, bean);
                    logAtDepth(currentDepth, "[完整 Bean 转入一级缓存] " + beanClass.getSimpleName());
                    return bean;
                } catch (ReflectiveOperationException | RuntimeException | Error e) {
                    throw e;
                } finally {
                    earlySingletonObjects.remove(beanClass);
                    singletonFactories.remove(beanClass);
                    singletonsCurrentlyInCreation.remove(beanClass);
                    logAtDepth(currentDepth, "[清理二级、三级缓存及创建中标记] "
                            + beanClass.getSimpleName());
                }
            }

            Object[] arguments = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> dependencyType = parameterTypes[i];
                logAtDepth(currentDepth, "[解析依赖] " + beanClass.getSimpleName()
                        + " 需要 " + dependencyType.getSimpleName());
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

    /** 判断 Bean 是否声明了字段注入点，用于选择字段注入创建流程。 */
    private boolean hasAutowiredField(Class<?> beanClass) {
        for (Field field : beanClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                return true;
            }
        }
        return false;
    }

    /** 输出带缩进的调用层级；深层重复递归只保留一条提示。 */
    private void logAtDepth(int depth, String message) {
        if (depth > MAX_VERBOSE_LOG_DEPTH) {
            if (!recursionNoticeLogged.get()) {
                System.out.println("  ".repeat(MAX_VERBOSE_LOG_DEPTH)
                        + "... 循环调用继续递归 ...");
                recursionNoticeLogged.set(true);
            }
            return;
        }
        System.out.println("  ".repeat(depth) + message);
    }
}
