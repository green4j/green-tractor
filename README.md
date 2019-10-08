# green-cproc
Garbage-free (green) toolset (a nano-framework) to implement a process with an asynchronous API.

## The main goals
Multithreaded programming is a challenge. This section describes what problems this toolset tries to address to make multithreaded programming easier for some cases at least.

### One single Worker
One of the main practical problems a developer faces while multithreaded programming is the visibility of a data modified by threads for each other.
The developer should care about the visibility using appropriate memory barriers. Some memory barriers like StoreLoad, for example, are expensive and lead to significant performance degradation.  

So, the most valuable from performance's point of view design is the single threaded "share nothing" architecture. This toolset makes all the job in one single Worker thread.

Such single threaded design can help to implement a custom event loop. With an event loop it is easy to build some FSM (Finite State Machine) code.

### No explicit locks to call the API
There are two main strategies how threads interact to each other:
1. a thread modifies another thread's state with some locks (with a synchronized block or a ReadWriteLock, for instance);
2. a thread sends a message/event to an inbound queue of another thread. There are two modifications of this strategy: CSP (Communicating Sequential Processing [https://en.wikipedia.org/wiki/Communicating_sequential_processes]) and Actor model ([https://en.wikipedia.org/wiki/Actor_model]). One of the main differences between CSP and Actors is that CSP fundamentally involves a rendezvous between the processes (with the size of the inbound queue = 1) and Actors don't synchronize on each other.

The first lock-based strategy has a very big disadvantage - it leads to deadlocks very often.

This toolset supports queue-based communication and it is tread-safe, so, a user shouldn't use any locks to call the API.
 
### Blocking and lock-free queue 
Depending on CPU limitations and latency requirements, the toolset can be configured to use one of available blocking or lock-free queue's implementations.

### Data and Commands
We assume that a process receives two types of signals: Data events and control events (Commands). Commands should be delivered and processed ASAP, whereas Data events can be queued/buffered and processed later.

The toolset separates all incoming signals to Data and Commands explicitly in its API and delivers the Commands to the Worker thread in priority order. A CSP-like channel is used for Commands and a Ring Buffer for Data events.

### A buffer for Data
If the Worker may have its throughput degraded periodically, a buffer to collect incoming Data events may be required.
Typically, a data stream processing code has a Ring Buffer as its input. This toolset also provides the Ring Buffer to store the Data events until the Worker has taken them out to process.

## How to implement and use a custom process

TBD
