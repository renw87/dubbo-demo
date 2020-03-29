package com.renwei.dubbo.registry;

public interface RegistryService {

    /**
     * 服务注册
     * @param serviceName
     * @param serviceAddress
     */
    void registry(String serviceName, String serviceAddress);

    /**
     * 服务发现
     * @param serviceName
     */
    String discover(String serviceName);
}
