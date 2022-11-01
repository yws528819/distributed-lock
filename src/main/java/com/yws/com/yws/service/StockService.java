package com.yws.com.yws.service;

import com.yws.com.yws.lock.DistributedLockClient;
import com.yws.com.yws.lock.DistributedRedisLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import com.yws.com.yws.lock.zk.ZkClient;
import com.yws.com.yws.lock.zk.ZkDistributedLock;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLockClient lockClient;

    @Autowired
    private RedissonClient redisClient;

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private ZkClient zkClient;


    public void deduct() {
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, "/curator/locks");
        try {
            mutex.acquire();

            //1.查询库存信息
            String stock = redisTemplate.opsForValue().get("stock");

            //2.判断库存是否充足
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    //3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }

            //测试可重入
            test(mutex);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            try {
                mutex.release();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * zookeeper锁（自旋锁）
     */
    public void deduct5() {

        ZkDistributedLock zkLock = zkClient.getLock("lock");
        zkLock.lock();

        //1.查询库存信息
        String stock = redisTemplate.opsForValue().get("stock");

        try {
            //2.判断库存是否充足
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    //3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }

            test2();

        } finally {
            zkLock.unlock();
        }
    }

    /**
     * redisson锁
     */
    public void deduct4() {

        RLock rLock = redisClient.getLock("lock");
        rLock.lock();

        //1.查询库存信息
        String stock = redisTemplate.opsForValue().get("stock");

        try {
            //2.判断库存是否充足
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    //3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }
        } finally {
            rLock.unlock();
        }
    }


    /**
     * redis锁（自己实现）
     */
    public void deduct3() {
        DistributedRedisLock redisLock = lockClient.getRedisLock("lock");
        redisLock.lock();

        //1.查询库存信息
        String stock = redisTemplate.opsForValue().get("stock");

        try {
            //2.判断库存是否充足
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    //3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }

            //测试可重入
            //test();
            //测试自动续期
            //try {
            //    TimeUnit.SECONDS.sleep(40);
            //} catch (InterruptedException e) {
            //    throw new RuntimeException(e);
            //}

        } finally {
            redisLock.unlock();
        }
    }


    public void test(InterProcessMutex mutex) throws Exception {
        mutex.acquire();
        System.out.println("测试curator可重入锁。。。");
        mutex.release();
    }

    public void test1() {
        ZkDistributedLock lock = zkClient.getLock("lock");
        lock.lock();
        System.out.println("测试zookeeper可重入锁。。。");
        lock.unlock();
    }

    public void test2() {
        DistributedRedisLock lock = lockClient.getRedisLock("lock");
        lock.lock();
        System.out.println("测试redisson可重入锁。。。");
        lock.unlock();
    }


    public void deduct2() {
        String uuid = UUID.randomUUID().toString();
        //尝试加锁，不存在才加锁
        while (!redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS)) {
            //加锁失败，睡一会，再获取锁
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            //1.查询库存信息
            String stock = redisTemplate.opsForValue().get("stock");

            //2.判断库存是否充足
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    //3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }
        } finally {
            //先判断是否是自己的锁，再解锁（使用lua脚本）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                    "   redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +
                    "end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
            //if (StringUtils.equals(redisTemplate.opsForValue().get("lock"), uuid)) {
            //    redisTemplate.delete("lock");
            //}
        }
    }


    /**
     * redis乐观锁
     */
    public void deduct1() {

        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //watch
                operations.watch("stock");

                //1.查询库存信息
                String stock = redisTemplate.opsForValue().get("stock");

                //2.判断库存是否充足
                if (stock != null && stock.length() > 0) {
                    Integer st = Integer.valueOf(stock);
                    if (st > 0) {
                        //multi
                        operations.multi();
                        //3.扣减库存
                        redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                    }
                }
                //exec 执行事务
                List exec = operations.exec();
                //如果执行事务的返回结果为空，则代表扣减库存失败，重试
                if (exec == null || exec.size() == 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    deduct();
                }

                return exec;
            }
        });
    }

    public void fairyLock(String id) {
        RLock fairylock = redisClient.getFairLock("fairylock");
        fairylock.lock();
        try{
            TimeUnit.SECONDS.sleep(10);
            System.out.println("测试公平锁=============" + id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            fairylock.unlock();
        }
    }

    public void readLock() {
        RReadWriteLock rwLock = redisClient.getReadWriteLock("rwLock");
        RLock rLock = rwLock.readLock();
        rLock.lock(10, TimeUnit.SECONDS);
        // TODO: 一顿读操作
        //rLock.unlock();
    }

    public void writeLock() {
        RReadWriteLock rwLock = redisClient.getReadWriteLock("rwLock");
        RLock writeLock = rwLock.writeLock();
        writeLock.lock(10, TimeUnit.SECONDS);
        // TODO: 一顿写操作
        //writeLock.unlock();
    }


    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(6);

        for (int i = 0; i < 6; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + "准备出门了。。。");
                try {
                    TimeUnit.SECONDS.sleep(new Random().nextInt(5));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "出门了。。。。。。");
                countDownLatch.countDown();
            }, i + "号同学").start();
        }

        countDownLatch.await();
        System.out.println(Thread.currentThread().getName() + "班长锁门");
    }

    private static void jucSemaphoreTest() {
        Semaphore semaphore = new Semaphore(3);

        for (int i = 0; i < 6; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName() + "抢到了停车位");
                    TimeUnit.SECONDS.sleep(new Random().nextInt(10));
                    System.out.println(Thread.currentThread().getName() + "停了一会开走了！");
                    semaphore.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }, i + "号车").start();

        }
    }

    public void semaphore() {
        RSemaphore semaphore = redisClient.getSemaphore("semaphore");
        semaphore.trySetPermits(3);//设置资源量 限流的线程数

        try {
            semaphore.acquire();//获取资源，获取资源成功的线程可以继续处理业务操作。否则会被阻塞
            redisTemplate.opsForList().rightPush("log", "10086获取了资源，开始业务逻辑处理。" + Thread.currentThread().getName());
            TimeUnit.SECONDS.sleep(10 + new Random().nextInt(10));
            redisTemplate.opsForList().rightPush("log", "10086处理完业务逻辑，资源释放===========" + Thread.currentThread().getName());
            semaphore.release();//手动释放资源，后续请求线程就可以获取该资源
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void latch() {
        RCountDownLatch cdl = redisClient.getCountDownLatch("cdl");
        cdl.trySetCount(5);

        try {
            cdl.await();
            // TODO: 一顿操作准备锁门
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void coutDown() {
        RCountDownLatch cdl = redisClient.getCountDownLatch("cdl");
        // TODO: 一顿操作出门
        cdl.countDown();

    }

    public void zkReadLock() {
        try {
            InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(curatorFramework, "/curator/rwlock");
            readWriteLock.readLock().acquire(10, TimeUnit.SECONDS);

            //readWriteLock.readLock().release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void zkWriteLock() {
        try {
            InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(curatorFramework, "/curator/rwlock");
            readWriteLock.writeLock().acquire(10, TimeUnit.SECONDS);

            //readWriteLock.writeLock().release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
