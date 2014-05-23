akka-cluster-test
=================

Demonstration of a problem with the Akka Cluster and routees

How to test
===========

In Intellij IDEA there are 3 runtime configurations, which must be started in the following order:

- Scheduler - it will create "main" actor, which should manage creation of worker actors in the cluster, as well as manage task scheduling.
- Worker - the "worker" actor, which solves some simple task and reports the result back to the master actor
- Cli - the actor, which uses ClusterClient to communicate with the cluster. It should be started only after workers have been registered in the scheduler.

Observed behavior:
============

* Scheduler:

    2014-05-23 19:42:54,813 DEBUG  [Scheduler$SchedulerActor] Starting actor Actor[akka://HttpCluster/user/router_scheduler/c1#-2070511885] 
    2014-05-23 19:43:15,838 DEBUG  [Scheduler$SchedulerActor] Adding worker Actor[akka.tcp://HttpCluster@127.0.0.1:58627/remote/akka.tcp/HttpCluster@127.0.0.1:2551/user/router_scheduler/c1/router_chunkworker/c1#2139998507] 
    2014-05-23 19:43:15,839 DEBUG  [Scheduler$SchedulerActor] Added worker Actor[akka.tcp://HttpCluster@127.0.0.1:58627/remote/akka.tcp/HttpCluster@127.0.0.1:2551/user/router_scheduler/c1/router_chunkworker/c1#2139998507] 
    2014-05-23 19:43:23,523 WARN   [ConsistentHashingRoutingLogic] Message [akka.test.Messages$SchedulerMessage] must be handled by hashMapping, or implement [akka.routing.ConsistentHashingRouter$ConsistentHashable] or be wrapped in [akka.routing.ConsistentHashingRouter$ConsistentHashableEnvelope] 

* Worker:

    2014-05-23 19:43:03,869 INFO   [Worker$WorkerActor] Starting up 

* Cli

    2014-05-23 19:43:23,466 INFO   [Cli$Controller] Sending request: SchedulerMessage(74c08667-aa23-42b4-8256-06695e0173c4,Hey there!)
