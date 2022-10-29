package com.yws.com.yws.lock.zk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class ZkClient {

    private ZooKeeper zooKeeper;


    @PostConstruct
    public void init() {
        try {
            zooKeeper = new ZooKeeper("192.168.0.105:2181", 30000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    Event.KeeperState state = event.getState();
                    if (Event.KeeperState.SyncConnected == state && Event.EventType.None == event.getType()) {
                        System.out.println("获取到链接了：" + event);
                    } else if (Event.KeeperState.Closed == state) {
                        System.out.println("关闭链接。。。");
                    }
                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void destroy() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public ZkDistributedLock getLock(String lockName) {
        return new ZkDistributedLock(zooKeeper, lockName);
    }


}
