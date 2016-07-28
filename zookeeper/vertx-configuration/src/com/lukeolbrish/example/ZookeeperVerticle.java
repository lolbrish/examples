package com.lukeolbrish.example;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class ZookeeperVerticle extends AbstractVerticle implements org.apache.zookeeper.Watcher {
    private ZooKeeper zk;
    private AtomicInteger configVersion = new AtomicInteger(0);

    private long timerId = -1;

    @Override
    public void start() {
        try {
            this.zk = new ZooKeeper(Constants.ZOOKEEPER_CONNECTION_STRING, 5000, this);
        } catch (IOException e) {
            System.err.println("ZookeeperVerticle.start exception");
            e.printStackTrace();
        }
        this.timerId = super.vertx.setPeriodic(3000, id -> {
            this.initialize();
        });
    }

    @Override
    public void stop() {

    }

    public void initialize() {
        if (this.zk.getState() != ZooKeeper.States.CONNECTED) {
            System.out.println("Waiting to check configuration until Zookeeper is connected...");
            return;
        }

        // cancel the timer once ZooKeeper connects
        super.vertx.cancelTimer(this.timerId);

        try {
            Stat s = this.zk.exists(Constants.CONFIGURATION_PATH, true);
            if (s != null) {
                this.zk.getData(Constants.CONFIGURATION_PATH, this, getVersionCallback(), this);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private DataCallback getVersionDataCallback() {
        return new DataCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] rawData, Stat s) {
                System.out.println("getVersionDataCallback.processResult - " + path + " - " + rc);
                if (rc != 0) {
                    System.out.println(KeeperException.Code.get(rc));
                    return;
                }
                ZookeeperVerticle zv = (ZookeeperVerticle) ctx;
                int version = -1;
                JsonObject data = new JsonObject(new String(rawData));
                synchronized (zv.configVersion) {
                    version = zv.configVersion.get();
                    System.out.println("getVersionDataCallback.processResult - " + path + " - "
                            + Constants.CONFIGURATION_PATH + "/" + version);
                    if (path.equals(Constants.CONFIGURATION_PATH + "/" + version)) {
                        synchronized (WebsiteMain.jsonObject) {
                            WebsiteMain.jsonObject.clear();
                            WebsiteMain.jsonObject.mergeIn(data);
                        }
                    }
                }
            }
        };
    }

    private DataCallback getVersionCallback() {
        return new DataCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] rawData, Stat s) {
                ZookeeperVerticle zv = (ZookeeperVerticle) ctx;
                int version = -1;
                synchronized (zv.configVersion) {
                    version = zv.configVersion.get();
                }
                int fetchedVersion = new Integer(new String(rawData)).intValue();
                if (fetchedVersion > version) {
                    synchronized (zv.configVersion) {
                        zv.configVersion.set(fetchedVersion);
                    }
                    zv.zk.getData(Constants.CONFIGURATION_PATH + "/" + fetchedVersion, false, getVersionDataCallback(),
                            zv);
                }
            }
        };
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("ZookeeperVerticle.WatchedEvent");
        System.out.println(event);

        if (event.getPath().equals(Constants.CONFIGURATION_PATH)) {
            this.zk.getData(Constants.CONFIGURATION_PATH, this, getVersionCallback(), this);
        }
    }
}
