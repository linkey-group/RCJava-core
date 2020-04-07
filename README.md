# **RCJava-core**

*** 
#### 项目介绍
Java SDK for RepChain，供开发人员调用，构造交易、提交交易以及获取交易或块数据

#### 软件架构
- src/main目录下
    ```
    com
        └── rcjava
            ├── client
            ├── exception
            ├── protos
            ├── sign
            ├── tran
            └── util
    ```
    * **`com.rcjava.client`** 该package下的类，主要用来构造sdk 客户端，客户端可以用来构造交易、提交交易、获取交易或块数据<br>
   
        > - ChainInfoClient 用来获取链信息的客户端
        > - TranPostClient  用来提交签名交易的客户端
   
    * **`com.rcjava.sign`** 该package下的类，主要是加密相关的工具类，包括签名与Hash等
    * **`com.rcjava.protos`** protoBuf generated messages，交易与块的相关操作
    * **`com.rcjava.tran`** 用来构建签名交易
    * **`com.rcjava.util`** 该package下，主要是一些工具类<br>
    
        > - CertUtil 证书相关工具类
        > - KeyUtil  key操作相关的工具类
        > - PemUtil  Pem操作相关的工具类
- src/test
    * 主要是相关工具类的测试用例，用户可参考此示例代码来构造交易、提交交易以及使用相关工具类
    
#### 安装教程

1. 项目基于MAVEN构建，搭建好MAVEN环境
2. jdk1.8+
3. download/clone项目到本地开发环境
4. `mvn clean install` 将jar包install到本地maven仓库
5. 项目就可以引用该jar包了
