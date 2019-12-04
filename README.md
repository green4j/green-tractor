# Green CProc
Garbage-free (green) toolset to implement a concurrent Process with asynchronous API.

## Motivation/main goals
Multithreaded programming is a challenge. This section describes what problems this toolset tries to address to make the multithreaded programming easier for some cases at least.

### One single Worker
One of the main practical problems a developer faces while multithreaded programming is the visibility of data modified by threads for each other.
The developer should care about the visibility using appropriate memory barriers. Some memory barriers like StoreLoad, for example, are expensive and lead to significant performance degradation in case of high contantion.  

So, from performance's point of view, the most valuable design is the single threaded "share nothing (almost, of course)" architecture. This toolset makes all the job in one single Worker thread.

Such single threaded design can help to implement, for example, a custom Event Loop/Reactor pattern. With single threaded event loop it is easy to build FSM (Finite State Machine) based code.

### No explicit locks to call the API
There are two main strategies how two threads can interact to each other:
1. a thread modifies another thread's state or shared data under some locks (with a synchronized block, a ReadWriteLock etc.);
2. a thread sends a message/event to an inbound queue of another thread. There are two modifications of this strategy: CSP (Communicating Sequential Processing - [https://en.wikipedia.org/wiki/Communicating_sequential_processes](https://en.wikipedia.org/wiki/Communicating_sequential_processes)) and Actor model ([https://en.wikipedia.org/wiki/Actor_model](https://en.wikipedia.org/wiki/Communicating_sequential_processes)). One of the main differences between CSP and Actors is that CSP fundamentally involves a rendezvous between the threads (this can be implemented as a size limited queue with the size 1) and Actors don't synchronize on each other.

The first lock-based strategy has a very big disadvantage - it leads to deadlocks very often.

This toolset supports queue-based communication and it is tread-safe, so, a user shouldn't use any locks to call the API.
 
### Blocking or lock-free queue 
Depending on CPU limitations and latency requirements, the toolset can be configured to use one of available blocking or lock-free queue's implementations.

### Data and Commands
We assume that a Process receives two types of signals: Data events and control events (Commands). Commands should be delivered and processed ASAP, whereas Data events can be queued/buffered and processed later.

The toolset separates all the incoming signals to Data and Commands explicitly with its API and it delivers the Commands to the Worker thread in priority order. A CSP-like channel is used for Commands and a Ring Buffer for Data events.

### A Ring Buffer for Data
If the Worker may have its throughput degraded a bit from time to time, a buffer to collect incoming Data events may be required.
It is a common case when a data stream processing code has a Ring Buffer as its input. This toolset also provides the Ring Buffer to store the Data events until the Worker has taken them out to Process.

### Worker stopped notification
The Worker thread can be interrupted/stopped by `ConcurrentProcess.close()` method. If the Worker was stopped, it is convinient to automatically prevent the Data and Command producers from waiting on the queue. This is implemented with `ConcurrentProcessClosedException` which is thrown if a producer tryes to send a Data entry or a Command after the Process was closed.

### GC-free
The toolset is designed to be sutable for latency sensitivity applications. The code never recurses or allocates more memory than it needs. And it uses lock-free pools to reuse Data and Command objects.

### Simplicity
The source code is simple and consists of a few classes and interfaces, so, it's easy to just copy and past it into your own project. Don't forget unit tests! :)

### No extra dependencies
The code doesn't depend on any 3rd party library. The only dependency if the [Cab](https://github.com/anatolygudkov/green-cab) structure which can be also just copy-pasted into the project.

## Performance
Some synthetic tests for JMH can be found in the [jmh](https://github.com/anatolygudkov/green-cproc/tree/master/jmh/src/main/java/org/green/jmh/cproc) folder.

Data processing throughput with one and two producer's threads:
```
Benchmark                                          Mode  Cnt         Score         Error  Units
SendEntryBenchmark.singleSenderWithCabBackingOff  thrpt    9   6360199.474 ±  293874.572  ops/s
SendEntryBenchmark.singleSenderWithCabBlocking    thrpt    9   4370818.736 ± 1081026.232  ops/s
SendEntryBenchmark.singleSenderWithCabYielding    thrpt    9   6435405.987 ±  207263.422  ops/s
SendEntryBenchmark.twoSenderWithCabBlocking       thrpt    9   4336631.095 ±  211814.347  ops/s
SendEntryBenchmark.twoSendersWithCabBackingOff    thrpt    9   8602184.708 ±  196179.997  ops/s
SendEntryBenchmark.twoSendersWithCabYielding      thrpt    9  10270365.638 ±  399164.675  ops/s
```

Command processing throughput with one and two producer's threads:
```
Benchmark                                                         Mode  Cnt        Score        Error  Units
ExecuteCommandBenchmark.oneStartExecuteCallerWithCabBackingOff   thrpt    9  3449359.654 ±  39978.939  ops/s
ExecuteCommandBenchmark.oneStartExecuteCallerWithCabBlocking     thrpt    9   221733.428 ±   4006.002  ops/s
ExecuteCommandBenchmark.oneStartExecuteCallerWithCabYielding     thrpt    9  2259736.593 ±  33337.583  ops/s
ExecuteCommandBenchmark.twoStartExecuteCallersWithCabBackingOff  thrpt    9  3561216.670 ± 183056.779  ops/s
ExecuteCommandBenchmark.twoStartExecuteCallersWithCabBlocking    thrpt    9   135584.094 ±   1780.338  ops/s
ExecuteCommandBenchmark.twoStartExecuteCallersWithCabYielding    thrpt    9  2240264.340 ±  70082.217  ops/s

```

The tests were made on a laptop with:
```
Intel Core i7-8750H CPU @ 2.20GHz + DDR4 16GiB @ 2667MHz
Linux 5.0.0-27-generic #28-Ubuntu SMP Tue Aug 20 19:53:07 UTC 2019 x86_64 x86_64 x86_64 GNU/Linux
JMH version: 1.21
VM version: JDK 1.8.0_161, Java HotSpot(TM) 64-Bit Server VM, 25.161-b12
VM options: -Xmx3072m -Xms3072m -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant
```

## How to implement and use a custom Process

A sample how to implement and use a custom Process can be found in the [sample](https://github.com/anatolygudkov/green-cproc/tree/master/samples/src/main/java/org/green/samples/cproc/myproc) folder.

## License

The code is available under the terms of the [MIT License](http://opensource.org/licenses/MIT).
