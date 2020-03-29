package com.renwei.dubbo.registry.zookeeper;

import com.renwei.dubbo.loadbalance.LoadBalance;
import com.renwei.dubbo.loadbalance.RandomLoadBalance;
import com.renwei.dubbo.registry.RegistryService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.List;

public class ZookeeperRegistry implements RegistryService {

    private List<String> repos = new ArrayList<>();
    private CuratorFramework curatorFramework;

    {
        curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(ZkConfig.CONNECTION_ADDR).sessionTimeoutMs(4000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 10)).build();
        curatorFramework.start();
    }

    @Override
    public void registry(String serviceName, String serviceAddress) {
        String servicePath = ZkConfig.ZK_REGISTER_PATH.concat("/").concat(serviceName);

        try {
            // 向zk上注册服务名称节点， 持久节点
            if (curatorFramework.checkExists().forPath(servicePath) == null) {

                curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(servicePath, "0".getBytes());
            }
            // 向zk上服务名称节点下注册ip地址，临时节点
            String addressPath = servicePath.concat("/").concat(serviceAddress);
            String rsNode = curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(addressPath, "0".getBytes());
            System.out.println("服务注册成功，" + rsNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String discover(String serviceName) {
        String servicePath = ZkConfig.ZK_REGISTER_PATH.concat("/").concat(serviceName);

        try {
            repos = curatorFramework.getChildren().forPath(servicePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        registerWatch(servicePath);
        LoadBalance loadBalance = new RandomLoadBalance();
        return loadBalance.select(repos);
    }

    /**
     * 监听ZK节点内容刷新
     * @param path 路径
     */
    private void registerWatch(final String path) {
        PathChildrenCache childrenCache = new PathChildrenCache(curatorFramework, path, true);
        PathChildrenCacheListener childrenCacheListener = new PathChildrenCacheListener() {

            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                repos = curatorFramework.getChildren().forPath(path);
            }
        };
        childrenCache.getListenable().addListener(childrenCacheListener);
        try {
            childrenCache.start();
        } catch (Exception e) {
            throw new RuntimeException("注册Watch异常", e);
        }
    }
}
