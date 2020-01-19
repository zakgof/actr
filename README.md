## actr
![Travis CI](https://travis-ci.org/zakgof/actr.svg?branch=release)
![https://bintray.com/zakgof/maven/actr](https://api.bintray.com/packages/zakgof/maven/actr/images/download.svg)

Simple actor model implementation for Java

- Simple API: sending _ask_ and _tell_ messages is just calling class methods
- POJOs as Actors: focus on business logics, no need to extend any frameworks classes
- Type safe: no need for instanceof/cast, compile time check prevents sending wrong message to a wrong actor
- High performance: lock-free implementation and lightweight actor creation

Actor code is guaranteed to be executed in thread-safe context:
- no concurrent calls for a particular actor (although subsequent calls may be dispatched to different threads)
- actor state can be safely read/written from actor code without any synchronized/volatile specifiers

#### Schedulers

Schedulers are available to be configured on per-actor or per-actor-system basis.

- Shared ForkJoinPool scheduler (the default)    
All actors share the common work stealing `ForkJoinPool`. This option is best for CPU-intensive actors.

- Thread per actor (pinned thread) scheduler    
Each actor owns a thread and all calls to the actor execute in that dedicated thread. It is useful, in particular, when wrapping non thread safe API.
**NEW !** JDK's project Loom Virtual Threads (aka Fibers) are also supported.

- Fixed thread pool scheduler    
Uses a pool of a predefined number or threads for scheduling actor calls. It might be beneficial compared to ForkJoinPools for actors involving some io when actor's CPU utilization is not maximum.

- Scheduler based on a user-provided ExecutorService for a more flexible option.   

It's easy to introduce your own fine-tuned scheduler by just implementing `IActorScheduler`.

#### Comparison to akka

Akka will require more boilerplate code. Code with Actr will be more concise.

|  | Akka | Actr |
| ------------- | ------------- | ------------- |
| Type safety  | No. Sending any message to any actor won't reveal any problems at compile time|  Type safe |
| Actor implementation class  | Must extend AbstractActor |  No constraints |
| Message | Must create a class for every message type | Message type is represented by a method |
| Message type dispatching | Must implement dispatching| Message type = method, no need for implementing dispatching |
| Passing multiple params | Must pack into a single message class | Message type = method, so supported  |

Compare the same example implemented with
[akka](https://github.com/akka/akka-quickstart-java.g8/tree/2.5.x/src/main/g8/src/main/java/com/lightbend/akka/sample) and [actr](https://github.com/zakgof/actr/tree/master/src/example/java/com/zakgof/actr/vsakka). (Note the difference in Printer implementation. With akka, the implementation is 41 lines long with only 1 line of business code (line 34))

#### Performance
Actr outperforms Akka on common actor operations. A complete opensource benchmark is available here: https://github.com/zakgof/akka-actr-benchmark

### Setup

#### Gradle
````groovy
compile 'com.github.zakgof:actr:0.3.2'
````

#### Maven
````xml
<dependency>
  <groupId>com.github.zakgof</groupId>
  <artifactId>actr</artifactId>
  <version>0.3.2</version>
</dependency>
````

### Usage

Having a POJO class
````java
private static class Printer {

    public void print(String s) {
        // This is called in Printer's thread
        System.err.println("[Printer] " + s);
    }
    public String getName() {
        // This is called in Printer's thread
        return "Printer-1";
    }
}
````

Create an actor

````java
final ActorRef<Printer> printerActor = ActorSystem.create("default").actorOf(Printer::new);
````

Call Printer from another actor
````java


    public void run() {
        // Tell: send text to print
        printerActor.tell(printer -> printer.print("Hello !"));
        
        // Ask: retrieve printer name. Printer#getName runs in Printer's thread, #printerNameReply runs in Caller's thread
        printerActor.ask(Printer::getName, this::printerNameReceived);
    }
    
    private void printerNameReceived(String printerName) {
       // Do smth with Printer's name
       // This is called in Caller's thread
    }
    

````

### More examples
https://github.com/zakgof/actr/tree/master/src/example/java/com/zakgof/actr/example



