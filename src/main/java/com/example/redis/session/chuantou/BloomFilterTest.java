package com.example.redis.session.chuantou;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class BloomFilterTest {

    @Autowired
    private StringRedisTemplate cache;

    @Autowired
    private DataBaseDao storage;

    //初始化布隆过滤器
//1000：期望存入的数据个数，0.001：期望的误差率
    BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), 1000, 0.001);

    private String[] keys;
    //把所有数据存入布隆过滤器
    void init(){
        for (String key: keys) {
            bloomFilter.put(key);
        }
    }

   public String get(String key) {
        // 从布隆过滤器这一级缓存判断下key是否存在
        Boolean exist = bloomFilter.mightContain(key);
        if(!exist){
            return "";
        }
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
