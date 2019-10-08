# Green CProc
Garbage-free (green) toolset (a nano-framework) to implement a process with an asynchronous API.

## Motivation/Main goals
Multithreaded programming is a challenge. This section describes what problems this toolset tries to address to make multithreaded programming easier for some cases.

### One single Worker
One of the main practical problems a developer faces while multithreaded programming is the visibility of a data modified by threads for each other.
The developer should care about the visibility using appropriate memory barriers. Some memory barriers like StoreLoad, for example, are expensive and lead to significant performance degradation.  

So, the most valuable from performance's point of view design is the single threaded "share nothing" architecture. This toolset makes all the job in one single Worker thread.

Such single threaded design can help to implement a custom event loop. With an event loop it is easy to build some FSM (Finite State Machine) code.

### No explicit locks to call the API
There are two main strategies how threads interact to each other:
1. a thread modifies another thread's state with some locks (with a synchronized block or a ReadWriteLock, for instance);
2. a thread sends a message/event to an inbound queue of another thread. There are two modifications of this strategy: CSP (Communicating Sequential Processing - [https://en.wikipedia.org/wiki/Communicating_sequential_processes](https://en.wikipedia.org/wiki/Communicating_sequential_processes)) and Actor model ([https://en.wikipedia.org/wiki/Actor_model](https://en.wikipedia.org/wiki/Communicating_sequential_processes)). One of the main differences between CSP and Actors is that CSP fundamentally involves a rendezvous between the processes (with the size of the inbound queue = 1) and Actors don't synchronize on each other.

The first lock-based strategy has a very big disadvantage - it leads to deadlocks very often.

This toolset supports queue-based communication and it is tread-safe, so, a user shouldn't use any locks to call the API.
 
### Blocking or lock-free queue 
Depending on CPU limitations and latency requirements, the toolset can be configured to use one of available blocking or lock-free queue's implementations.

### Data and Commands
We assume that a process receives two types of signals: Data events and control events (Commands). Commands should be delivered and processed ASAP, whereas Data events can be queued/buffered and processed later.

The toolset separates all incoming signals to Data and Commands explicitly in its API and delivers the Commands to the Worker thread in priority order. A CSP-like channel is used for Commands and a Ring Buffer for Data events.

### A Ring Buffer for Data
If the Worker may have its throughput degraded periodically, a buffer to collect incoming Data events may be required.
Typically, a data stream processing code has a Ring Buffer as its input. This toolset also provides the Ring Buffer to store the Data events until the Worker has taken them out to process.

### GC-free
The toolset is designed to be sutable for latency sencivity applications. The code never recurses or allocates more memory than it needs. And it uses lock-free pools to reuse Data and Command objects.

## Performance

Some synthetic tests for JMH can be found in the [jmh](https://github.com/anatolygudkov/green-cproc/tree/master/jmh/src/main/java/org/green/jmh/cproc) folder.

Data processing throughput with one and two producer's threads:
```
Benchmark                                          Mode  Cnt         Score        Error  Units
SendEntryBenchmark.singleSenderWithCabBackingOff  thrpt    9   6352586.645 ± 418778.308  ops/s
SendEntryBenchmark.singleSenderWithCabBlocking    thrpt    9   3850352.401 ± 126849.460  ops/s
SendEntryBenchmark.singleSenderWithCabYielding    thrpt    9   6807172.943 ± 261038.076  ops/s
SendEntryBenchmark.twoSenderWithCabBlocking       thrpt    9   4019757.308 ± 184458.661  ops/s
SendEntryBenchmark.twoSendersWithCabBackingOff    thrpt    9   8068049.589 ± 449019.487  ops/s
SendEntryBenchmark.twoSendersWithCabYielding      thrpt    9  10272908.163 ± 149389.427  ops/s
```

Command processing throughput with one and two producer's threads:
```
Benchmark                                                         Mode  Cnt        Score        Error  Units
ExecuteCommandBenchmark.oneStartExecuteCallerWithCabBackingOff   thrpt    9  3323414.904 ± 105806.633  ops/s
ExecuteCommandBenchmark.oneStartExecuteCallerWithCabBlocking     thrpt    9   220031.222 ±   2494.798  ops/s
ExecuteCommandBenchmark.oneStartExecuteCallerWithCabYielding     thrpt    9  2291261.929 ±  66947.660  ops/s
ExecuteCommandBenchmark.twoStartExecuteCallersWithCabBackingOff  thrpt    9  3721093.587 ± 187742.507  ops/s
ExecuteCommandBenchmark.twoStartExecuteCallersWithCabBlocking    thrpt    9   135408.095 ±   3895.282  ops/s
ExecuteCommandBenchmark.twoStartExecuteCallersWithCabYielding    thrpt    9  2299532.213 ±  77271.120  ops/s
```

The tests were made on a laptop with:
```
Intel Core i7-8750H CPU @ 2.20GHz + DDR4 16GiB @ 2667MHz
Linux 5.0.0-27-generic #28-Ubuntu SMP Tue Aug 20 19:53:07 UTC 2019 x86_64 x86_64 x86_64 GNU/Linux
JMH version: 1.21
VM version: JDK 1.8.0_161, Java HotSpot(TM) 64-Bit Server VM, 25.161-b12
VM options: -Xmx3072m -Xms3072m -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant
```

## How to implement and use a custom process

A sample how to implement and use a custom process can be found in the [sample](https://github.com/anatolygudkov/green-cproc/tree/master/samples/src/main/java/org/green/samples/cproc/myproc) folder.
