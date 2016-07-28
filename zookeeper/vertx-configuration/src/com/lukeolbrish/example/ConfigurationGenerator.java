package com.lukeolbrish.example;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import io.vertx.core.json.JsonObject;

public class ConfigurationGenerator implements org.apache.zookeeper.Watcher {
    private Boolean mutex = false;
    private AtomicInteger configVersion = new AtomicInteger(0);

    /**
     * Generate some miscellaneous data to represent configuration data as a JSON object.
     * 
     * @param version the current configuration version
     * @return a JsonObject representing a configuration for the vertx front-end servers.
     */
    private static JsonObject createConfiguration(int version) {
        return new JsonObject().put("version", version)
                               .put("timestamp", new Date().getTime())
                               .put("uuid1", UUID.randomUUID().toString())
                               .put("uuid2", UUID.randomUUID().toString())
                               .put("uuid3", UUID.randomUUID().toString());
    }

    private ConfigurationGenerator() {
        try {
            ZooKeeper zk = new ZooKeeper(Constants.ZOOKEEPER_CONNECTION_STRING, 5000, this);
            // Wait for the zookeeper servers to get ready... which can several seconds when
            // using docker
            while(zk.getState() != ZooKeeper.States.CONNECTED) {
                synchronized (mutex) {
                    mutex.wait(5000);
                }
            }

            // With docker we will always need to create the root, but with reconfiguration
            // you might first need to lookup the currently saved configuration in zookeeper
            Stat s = zk.exists(Constants.CONFIGURATION_PATH, false);
            int version = configVersion.get();
            if (s == null) {
                zk.create(Constants.CONFIGURATION_PATH,
                          Integer.toString(version).getBytes(),
                          Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            else {
                byte[] rawData = zk.getData(Constants.CONFIGURATION_PATH, false, s);
                configVersion.set(new Integer(new String(rawData)).intValue());
            }

            // Every 5-10 seconds, create a new configuration
            Random rand = new Random();
            while (true) {
                synchronized (mutex) {
                    mutex.wait(5000 + rand.nextInt(5000));
                }

                version = configVersion.incrementAndGet();
                System.out.println("ConfigurationGenerator: Updating version to " + version);
                // the order of these two calls is important
                zk.create(Constants.CONFIGURATION_PATH + "/" + version,
                          ConfigurationGenerator.createConfiguration(version).encode().getBytes(),
                          Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                zk.setData(Constants.CONFIGURATION_PATH,
                           Integer.toString(version).getBytes(),
                           version - 1);
                // Delete configuration versions while keeping one prior version.
                if(version > 2) {
                    zk.delete(Constants.CONFIGURATION_PATH + "/" + (version - 2), -1);
                }
            }
        } catch (IOException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        // Zookeeper will send an event when we get connected so we can notify
        // the loop waiting on connection.
        synchronized (mutex) {
            mutex.notify();
        }
    }

    public static void main(String[] args) {
        new ConfigurationGenerator();
    }
}
