## actr
![Travis CI](https://travis-ci.org/zakgof/actr.svg?branch=release)
![https://bintray.com/zakgof/maven/actr](https://api.bintray.com/packages/zakgof/maven/actr/images/download.svg)

Simple actor model implementation for Java

- Simple API: sending _ask_ and _tell_ messages is just calling class methods
- POJOs as Actors: focus on business logics, no need to extend any frameworks classes
- Type safe: no need for instanceof/cast
- High performance: lock-free implementation and lightweight actor creation

Actor code in guaranteed to be executed in thread-safe context:
- no concurrent calls for a particular actor
- actor state can be safely read/written from actor code without any synchronized/volatile specifiers

#### Schedulers

Two schedulers are available for actors:

- Dedicated thread scheduler
Each actor owns a thread and all calls to the actor execute in that dedicated thread.

- Shared ForkJoinPool scheduler
All actors share a common work stealing ForkJoinPool

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
compile 'com.github.zakgof:actr:0.2.0'
````

#### Maven
````xml
<dependency>
  <groupId>com.github.zakgof</groupId>
  <artifactId>actr</artifactId>
  <version>0.2.0</version>
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
final ActorRef<Printer> printerActor = ActorSystem.dflt().actorOf(Printer::new);
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

### A Bigger example
https://github.com/zakgof/actr/blob/master/src/example/java/com/zakgof/actr/example/ActrExample.java



