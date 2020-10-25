package com.example.redis.session.chuantou;

import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SaveEmpty {


    @Autowired
    private StringRedisTemplate cache;

    @Autowired
    private DataBaseDao storage;

    /**
     * 以下是一段伪代码
     * @param key
     * @return
     */

    public String  save(String key){

        // 从缓存中获取数据
        String cacheValue = cache.opsForValue().get(key);
        // 缓存为空
        if (StringUtils.isBlank(cacheValue)) {
            // 从存储中获取
            String storageValue = storage.get(key);
            cache.opsForValue().set(key, storageValue);
            // 如果存储数据为空， 需要设置一个过期时间(300秒)
            if (storageValue == null) {
                cache.expire(key, 60 * 5, TimeUnit.MINUTES);
            }
            return storageValue;
        } else {
            // 缓存非空
            return cacheValue;
        }
    }
}
