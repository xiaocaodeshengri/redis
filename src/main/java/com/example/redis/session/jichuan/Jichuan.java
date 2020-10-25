package com.example.redis.session.jichuan;

import com.example.redis.session.chuantou.DataBaseDao;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Jichuan {

    @Autowired
    private StringRedisTemplate cache;

    @Autowired
    private DataBaseDao storage;

  public   String get(String key) {
        // 从缓存中获取数据
        String cacheValue = cache.opsForValue().get(key);
        // 缓存为空
        if (StringUtils.isBlank(cacheValue)) {
            // 从存储中获取
            String storageValue = storage.get(key);
            cache.opsForValue().set(key, storageValue);
            //设置一个过期时间(300到600之间的一个随机数)
            int expireTime = new Random().nextInt(300)  + 300;
            if (storageValue == null) {
                cache.expire(key, expireTime, TimeUnit.MINUTES);
            }
            return storageValue;
        } else {
            // 缓存非空
            return cacheValue;
        }
    }
}
