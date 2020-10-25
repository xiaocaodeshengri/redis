package com.example.redis.session.hot;

import com.example.redis.session.chuantou.DataBaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HotPot {


    @Autowired
    private StringRedisTemplate cache;

    @Autowired
    private DataBaseDao storage;

    String get(String key) {
        // 从Redis中获取数据
        String value = cache.opsForValue().get(key);
        // 如果value为空， 则开始重构缓存
        if (value == null) {
            // 只允许一个线程重建缓存， 使用nx， 并设置过期时间ex
            String mutexKey = "mutext:key:" + key;
            String clientId = UUID.randomUUID().toString();
            if (cache.opsForValue().setIfAbsent(mutexKey, clientId, 30, TimeUnit.SECONDS)) {
                // 从数据源获取数据
                value = storage.get(key);
                // 回写Redis， 并设置过期时间
                long timeout =3 *60;
                cache.expire(key, timeout, TimeUnit.MINUTES);
                // 删除key_mutex
                cache.delete(mutexKey);
            }// 其他线程休息50毫秒后重试
            else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                get(key);
            }
        }
        return value;
    }
}
