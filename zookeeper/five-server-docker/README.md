# Using A Docker Zookeeper Image

_This is a simple example that sets up a five server Zookeeper cluster._

## Requirements

This example requires [docker version 1.11+](https://www.docker.com/) and
[docker-compose version 1.6+](https://docs.docker.com/compose/).

## Running

```shell
docker-compose up
```

_On a separate terminal, you can use the following line to connect to the
five server Zookeeper instance:_

```shell
docker-compose run --rm zkcli -server zookeeper3
```

_There is an intro to zkcli here: [Connecting To
ZooKeeper](https://zookeeper.apache.org/doc/r3.4.8/zookeeperStarted.html#sc_ConnectingToZooKeeper)._
