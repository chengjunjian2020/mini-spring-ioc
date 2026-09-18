package com.example.minispring;

/** 模拟 Web 层（Controller），它依赖 UserService。 */
public class UserController {
    private final UserService userService;

    /** 构造器注入 Service，Controller 不再自己 new Service。 */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 模拟一次接口调用。 */
    public void handleRequest() {
        System.out.println("[业务结果] " + userService.getWelcomeMessage());
    }
}
