package com.example.minispring;

/** 模拟业务层（Service），它依赖 UserRepository。 */
public class UserService {
    private final UserRepository userRepository;

    /** 构造器注入 Repository，依赖由容器负责提供。 */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 执行业务逻辑。 */
    public String getWelcomeMessage() {
        return "欢迎你，" + userRepository.findUserName();
    }
}
