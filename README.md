# falsework-rpc
基于grpc的分布式远程调用程序,目前涉及的内容有服务级别的服务注册发现,基于客户端负载均衡,链路追踪,以及应用级别的IOC与AOP,
仍在计划中的功能有服务的熔断与隔离,流量控制等,仅供学习参考使用
# 项目介绍
## falsework-census
链路追踪数据与统计数据收集展示模块,基于zipkin+mysql实现
## falsework-account
作为整个项目的客户端演示程序
## falsework-core
整个项目的核心模块,提供整个项目的核心功能
## falsework-jdbc
封装jdbc层