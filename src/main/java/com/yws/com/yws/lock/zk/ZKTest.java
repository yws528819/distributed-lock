package com.yws.com.yws.lock.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZKTest {

    public static void main(String[] args) {
        ZooKeeper zooKeeper = null;
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper("192.168.0.105:2181", 30000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    Event.KeeperState state = event.getState();
                    if (Event.KeeperState.SyncConnected == state && Event.EventType.None == event.getType()) {
                        System.out.println("获取到链接了：" + event);
                        countDownLatch.countDown();
                    }else if (Event.KeeperState.Closed == state) {
                        System.out.println("关闭链接。。。");
                    }else {
                        //节点事件一般不写这里
                        // System.out.println("节点事件。。。");
                    }
                }
            });

            countDownLatch.await();

            //节点新增：永久、永久序列化、临时、临时序列化
            // zooKeeper.create("/yews/test1", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            // zooKeeper.create("/yews/test2", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            // zooKeeper.create("/yews/test2", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            // zooKeeper.create("/yews/test2", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            // zooKeeper.create("/yews/test3", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            // zooKeeper.create("/yews/test4", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            // zooKeeper.create("/yews/test4", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            // zooKeeper.create("/yews/test4", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

            //查询 判断节点是否存在 stat
            Stat stat = zooKeeper.exists("/yews", null);
            if (stat != null) {
                System.out.println("当前节点存在");
            }else {
                System.out.println("当前节点不存在");
            }

            //获取当前节点中的数据内容 get
            byte[] data = zooKeeper.getData("/yews", null, stat);
            System.out.println("当前节点的内容：" + new String(data));

            //获取当前节点的子节点 ls
            List<String> children = zooKeeper.getChildren("/yews", new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    System.out.println("节点的子节点发生变化。。。");
                }
            });
            System.out.println("当前节点的子节点：" + children);

            //更新：版本号必须与当前节点的版本号一致，否则更新失败。也可以指定-1，代表不关心版本号
            zooKeeper.setData("/yews", "hello new".getBytes(), stat.getVersion());

            //删除
            zooKeeper.delete("/yews/test1", -1);
            System.out.println("sssss");

            System.in.read();
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (zooKeeper != null) {
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
