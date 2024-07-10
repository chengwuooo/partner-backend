package com.fcw.partner.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Chengwu Fang
 * date 2024/7/7
 */
public class StringUtils {
    /**
     * 字符串json数组转Long类型set集合
     */
    public static Set<Long> stringJsonListToLongSet(String jsonList) {
        Set<Long> set = new Gson().fromJson(jsonList, new TypeToken<Set<Long>>() {
        }.getType());
        return Optional.ofNullable(set).orElse(new HashSet<>());
    }
}