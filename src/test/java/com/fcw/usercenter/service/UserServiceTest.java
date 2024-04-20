package com.fcw.usercenter.service;
import java.util.Date;

import com.fcw.usercenter.model.domain.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;




@SpringBootTest
class UserServiceTest {
    @Resource
    private UserService userService;
    @Test
    void test_addUser() {
        User user = new User();
        user.setUserAccount("1465");
        user.setUsername("fcw");
        user.setAvatarUrl("https://images.zsxq.com/FiO_RyT97vME5GFsVWJRqtBb3uD1?imageMogr2/auto-orient/thumbnail/380x/format/jpg/blur/1x0/quality/75&e=1714492799&s=vtmymyyjmyjtyyy&token=kIxbL07-8jAj8w1n4s9zv64FuZZNEATmlU_Vm6zD:5dTMSgJHqVpKRkch5H-7yhWvHjU=");
        user.setGender(0);
        user.setUserPassword("123");
        user.setPhone("");
        user.setEmail("");
        boolean save = userService.save(user);
        Assertions.assertTrue(save);
    }
    @Test
    void userRegister() {
        String userAccount = "fcw";
        String userPassword = "12345678";
        String checkPassword = "12345678";
        String planetCode="12314";
        long l = userService.userRegister(userAccount, userPassword, checkPassword);
        System.out.println(l);
        Assertions.assertEquals(-61, l);
    }


    @Test
    void userLogin() {
        String userAccount = "fcw";
        String userPassword = "12345678";
        HttpServletRequest request=Mockito.mock(HttpServletRequest.class);
        request.getSession().setAttribute("userLoginState",new Date());
        User user = userService.userLogin(userAccount, userPassword,request);
        System.out.println(user);
    }
}