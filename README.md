## actr
Simple actor model implementation for Java

- Simple API
- POJOs as Actors
- Type safe

A thread is created for every Actor.
POJO object methods run in its Actor's thread when ActorRef API is used.
When an `ask` call is invoked, the response is transferred to the caller's actor thread.

#### Why not Akka ?

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

### Setup

#### Gradle
````groovy
compile 'com.github.zakgof:actr:0.0.1'
````

#### Maven
````xml
<dependency>
  <groupId>com.github.zakgof</groupId>
  <artifactId>actr</artifactId>
  <version>0.0.1</version>
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
final ActorRef<Printer> printerActor = ActorRef.from(Printer::new);
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

