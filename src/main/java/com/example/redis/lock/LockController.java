package com.example.redis.lock;

import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class LockController {

    private static final Logger logger = LoggerFactory.getLogger(LockController.class);

    @Autowired
    private RedissonClient redisson;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonAutoConfiguration configuration;
    /**
     *  1、分析问题：
     *      1、如果程序挂了，但是锁还没有释放怎么办？redis会一直阻塞
     *      解决方案：添加超时时间
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("/deduct_stock1")
    public String deductStock1() throws InterruptedException {
        String lockKey = "product_001";
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "tracy"); //jedis.setnx(key,value)
            if (!result) {
                return "1001";
            }
            // 加锁，实现锁续命功能
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock")); // jedis.get("stock")
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + ""); // jedis.set(key,value)
                System.out.println("扣减成功，剩余库存:" + realStock + "");
            } else {
                System.out.println("扣减失败，库存不足");
            }
        }finally {

        }
        return "end";
    }
    /**
     *      2、如果30秒过后线程1还没有执行完成，已经释放锁，线程2此时获取了锁，
     *      到线程1在过10秒后执行完成，把这把锁释放了，此时线程3又获取了锁，但是线程2还在执行中
     *      线程1释放了线程2应该占有的锁
     *      解决方案：  添加唯一标识，谁占有谁释放的原则 ，同时在添加超时时需要采用原子操作
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("/deduct_stock2")
    public String deductStock2() throws InterruptedException {
        String lockKey = "product_001";
        String clientId = UUID.randomUUID().toString();
        try {
//           Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "tracy"); //jedis.setnx(key,value)
//           stringRedisTemplate.expire(lockKey,30, TimeUnit.SECONDS);
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, clientId, 30, TimeUnit.SECONDS);
            if (!result) {
                return "1001";
            }

            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock")); // jedis.get("stock")
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + ""); // jedis.set(key,value)
                System.out.println("扣减成功，剩余库存:" + realStock + "");
            } else {
                System.out.println("扣减失败，库存不足");
            }
        }finally {
        }
        return "end";
    }

    /**
     *    3、这个超时时间怎么去定义？如果我30秒不够用怎么处理呢？
     *       引入这个机制 redission的看门狗，每过10秒进行一次续命
     *
     *     但是还存在一个问题，如果此时redis此时挂掉了，存储的锁信息就没有了，此时程序还在执行中
     *     ，从节点也没有锁信息，怎么处理？
     *     平衡：RedLock 或者是zk的解决方案
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("/deduct_stock3")
    public String deductStock3() throws InterruptedException {
        String lockKey = "product_001";
        RLock redissonLock = redisson.getLock(lockKey);
        try {
            // 加锁，实现锁续命功能
            redissonLock.lock();
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock")); // jedis.get("stock")
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + ""); // jedis.set(key,value)
                System.out.println("扣减成功，剩余库存:" + realStock + "");
            } else {
                System.out.println("扣减失败，库存不足");
            }
        }finally {
            redissonLock.unlock();
        }
        return "end";
    }

    @RequestMapping("/redlock")
    public String redlock() throws InterruptedException {
        String lockKey = "product_001";
        //这里需要自己实例化不同redis实例的redisson客户端连接，这里只是伪代码用一个redisson客户端简化了
        RLock lock1 = redisson.getLock(lockKey);
        RLock lock2 = redisson.getLock(lockKey);
        RLock lock3 = redisson.getLock(lockKey);

        /**
         * 根据多个 RLock 对象构建 RedissonRedLock （最核心的差别就在这里）
         */
        RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);
        try {
            /**
             * 4.尝试获取锁
             * waitTimeout 尝试获取锁的最大等待时间，超过这个值，则认为获取锁失败
             * leaseTime   锁的持有时间,超过这个时间锁会自动失效（值应设置为大于业务处理的时间，确保在锁有效期内业务能处理完）
             */
            boolean res = redLock.tryLock(10, 30, TimeUnit.SECONDS);
            if (res) {
                //成功获得锁，在这里处理业务
            }
        } catch (Exception e) {
            throw new RuntimeException("lock fail");
        } finally {
            //无论如何, 最后都要解锁
            redLock.unlock();
        }

        return "end";
    }

}