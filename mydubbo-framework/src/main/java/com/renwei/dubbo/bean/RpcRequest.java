package com.renwei.dubbo.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 6416663699595371173L;
    private String className;
    private String methodName;
    private Class<?>[] types;
    private Object[] params;

}
