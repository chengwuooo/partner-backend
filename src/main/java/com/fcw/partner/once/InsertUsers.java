package com.fcw.partner.once;

import com.fcw.partner.mapper.UserMapper;
import com.fcw.partner.model.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.RandomUtils.*;


@Component
public class InsertUsers {
    @Resource
    private UserMapper userMapper;
    /**
     * 循环插入用户
     */

//    @Scheduled(initialDelay = 1000,fixedRate = Long.MAX_VALUE )
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 9000;
        for (int i = 0; i < INSERT_NUM; i++) {
            User user =new User();
            user.setUserName("是一个假数据");
            user.setUserAccount("fakeUser");
            user.setUserAvatar("https://img2.baidu.com/it/u=2920879629,2349199079&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1714323600&t=b083b814efa1d2b690ec0bab177b1370");
            user.setUserProfile("是一个假数据");
            user.setGender(nextInt(0, 2));
            user.setUserPassword("12345678");
            user.setPhone("123456789108");
            user.setEmail("fakeEmail@qq.com");
            user.setUserStatus(0);
            user.setUserRole("user");
            String[] tags={"咸鱼","吃瓜","搬砖","打工人","程序员","JAVA","C++","PYTHON","PHP"};
            //从tags数组中随机选取两个或者三个元素

            Random random = new Random();
            // 以一定概率决定抽取两个或三个标签
            int numTagsToPick = random.nextBoolean() ? 2 : 3;
            List<String> tagList = new ArrayList<>(Arrays.asList(tags));
            // 定义一个新列表，用于存放随机选取的标签
            List<String> selectedTags = new ArrayList<>();

            // 随机选取指定数量的不同标签
            for (int j = 0; j < numTagsToPick; j++) {
                int randomIndex = random.nextInt(tagList.size());
                selectedTags.add(tagList.remove(randomIndex));
            }
            String formattedTags = "[" + selectedTags.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", ")) + "]";

            // 将格式化后的字符串赋值给user对象的tags属性
            user.setTags(formattedTags);
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println( stopWatch.getLastTaskTimeMillis());

    }

    public static void main(String[] args) {
        InsertUsers insertUsers = new InsertUsers();
        insertUsers.doInsertUser();
    }
}

