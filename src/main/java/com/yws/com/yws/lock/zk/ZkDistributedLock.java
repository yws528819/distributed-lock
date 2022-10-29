package com.yws.com.yws.lock.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ZkDistributedLock implements Lock {

    private ZooKeeper zooKeeper;
    private String lockName;
    private static final String ROOT_PATH = "/locks";

    public ZkDistributedLock(ZooKeeper zooKeeper, String lockName) {
        this.zooKeeper = zooKeeper;
        this.lockName = lockName;
        try {
            if (zooKeeper.exists(ROOT_PATH, false) == null) {
                zooKeeper.create(ROOT_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lock() {
        tryLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        try {
            //创建zNode节点过程：为了防止zk客户端程序获取到锁之后，服务器宕机带来的死锁问题，这里创建的是临时节点
            zooKeeper.create(ROOT_PATH + "/" +  lockName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            tryLock();
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        try {
            zooKeeper.delete(ROOT_PATH + "/" + lockName, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
