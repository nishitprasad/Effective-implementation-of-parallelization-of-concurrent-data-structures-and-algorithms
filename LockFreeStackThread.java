import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.logging.Level;
import java.util.logging.Logger;

class T {
    public int value;

    public T(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
  
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final T other = (T) obj;
        return this.value == other.value;
    }
}

public class LockFreeStackThread {

    public static void main(String[] args) throws InterruptedException {
        long lStartTime = System.currentTimeMillis();
        LockFreeStackOp LFStack = new LockFreeStackOp();
        int n=10000000;
        LFStack.setOpCount(n);
                    
        Operation t1 = new Operation(LFStack,"Push");
        Operation t2 = new Operation(LFStack,"Push");
        Operation t3 = new Operation(LFStack,"Push");
        Operation t4 = new Operation(LFStack,"Pop");
        Operation t5 = new Operation(LFStack,"Pop");
        
        
        t1.start();
        t2.start();       
        t3.start();
        t4.start();       
        t5.start();
        
        try{
            t1.join();
            t2.join();
            t3.join();
            t4.join();
            t5.join();
        } catch( Exception e) {
            System.out.println("Interrupted");  
        }
        /*Node temp = LBStack.top;
        System.out.println("Final Stack Contents: ");
        while(temp!=null){
            System.out.println(" " + temp.getItem().getValue());
            temp = temp.next;
        }*/
        System.out.println("Thread 1 Push Count: " + LFStack.count_Push1);
        System.out.println("Thread 2 Push Count: " + LFStack.count_Push2);
        System.out.println("Thread 3 Push Count: " + LFStack.count_Push3);
        System.out.println("Thread 4 Pop Count: " + LFStack.count_Pop4);
        System.out.println("Thread 5 Pop Count: " + LFStack.count_Pop5);
        /*System.out.println("Array ");
        for(int i=0;i<n;i++){
            wait_th[i]=LBStack.getWaitList(i);
            if(wait_th[i]!=0){
                System.out.println("Thread " + i + " "+ wait_th[i]);                
            }
        }*/
        
        long lEndTime = System.currentTimeMillis();
        long difference = lEndTime - lStartTime;
	System.out.println("Elapsed milliseconds: " + difference);
        }
    }
    
class Operation extends Thread
{
    String op;
    LockFreeStackOp S;
    int n=10000000;
    T[] item;
    
    public Operation(LockFreeStackOp S, String op) {
        this.op = op;
        this.S = S;
        item = new T[n];
    }

    @Override
    public void run() {
        if(op.contains("Push")){
            while(S.Push_Op.getAndDecrement()>0){                
                item[S.a] = new T(S.a+1);
                try {
                    S.push(item[S.a]);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Operation.class.getName()).log(Level.SEVERE, null, ex);
                }
                S.a++;
                //S.Push_Op.decrementAndGet();
            }
            
    }   else{
            while(S.Pop_Op.getAndDecrement()>0){                
                try {
                    S.pop();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Operation.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
    }
}
}


class LockFreeStackOp {
    AtomicReference<Node> top = new AtomicReference<>(null);
    int number_of_op;
    static final int MIN_DELAY = 10;
    static final int MAX_DELAY = 100;
    static int a=0;
    public int count_Push1=0, count_Push2=0, count_Push3=0;
    int count_Pop4 =0, count_Pop5 =0;
    
    public AtomicInteger Push_Op = new AtomicInteger();
    public AtomicInteger Pop_Op = new AtomicInteger();

    public void setOpCount(int n){
        number_of_op = n;
        Push_Op.set(number_of_op);
        Pop_Op.set(number_of_op);
    }

    public int getCount_Push1() {
        return count_Push1;
    }

    public int getCount_Push2() {
        return count_Push2;
    }

    public int getCount_Push3() {
        return count_Push3;
    }

    public int getCount_Pop4() {
        return count_Pop4;
    }

    public int getCount_Pop5() {
        return count_Pop5;
    }
    
    Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);
    
    protected boolean tryPush(Node node){
        Node oldTop = top.get();
        node.next = oldTop;
        return(top.compareAndSet(oldTop, node));
    }
    
    public void push(T value) throws InterruptedException{
        Node node = new Node(value);
        while(true){
            if(tryPush(node)){                
                if(Thread.currentThread().getId()==9){
                    count_Push1++;
                }
                else if(Thread.currentThread().getId()==10){
                    count_Push2++;
                }
                else{
                    count_Push3++;
                }
                /*System.out.println("Pushed Item "+ node.value.value);*/
                return;
            }else{
                backoff.backoff();                
            }
        }
    }
    
    protected Node tryPop() throws InterruptedException {
        Node oldTop = top.get();
        if(oldTop == null){
            //System.out.println("Stack is Empty");
        }
        Node newTop = oldTop.getNext();
        if(top.compareAndSet(oldTop, newTop)){
            /*System.out.println("Popped Item "+ oldTop.getItem().value);*/
            return oldTop;
        }else{
            return null;
        }
    }
    
    public T pop() throws InterruptedException {
        while(true){
            Node returnNode = tryPop();
            if(returnNode != null){
                if(Thread.currentThread().getId()==12){
                        count_Pop4++;
                }else{
                        count_Pop5++;
                }
                return returnNode.getItem();
            }else{
                backoff.backoff();
            }
        }
    }
}

class Node {
    public T item;
    public Node next;
    
    public Node(T item){
        this.item = item;
        this.next = null;
    }

    public Node getNext() {
        return next;
    }
    
    public T getItem() {
        return item;
    }
}

class LockFreeExchanger<T> {
    static final int EMPTY = 0, WAITING = 1, BUSY = 2;
    
    AtomicStampedReference<T> slot = new AtomicStampedReference<T>(null, 0);
    
    public T exchange(T myItem, long timeout, TimeUnit unit) throws TimeoutException {
        long nanos = unit.toNanos(timeout);
        long timeBound = System.nanoTime()+ nanos;
        int[] stampHolder = {EMPTY};
        while(true){
            if(System.nanoTime()>timeBound){
                System.out.println("TIMEOUT");
            }
            T yrItem = slot.get(stampHolder);
            int stamp = stampHolder[0];
            switch(stamp) {
                case EMPTY:
                    if (slot.compareAndSet(yrItem, myItem, EMPTY, WAITING)) {
                    while (System.nanoTime() < timeBound){
                        yrItem = slot.get(stampHolder);
                        if (stampHolder[0] == BUSY) {
                             slot.set(null, EMPTY);
                                 return yrItem;
                         }
                     }
                    }
                    if (slot.compareAndSet(myItem, null, WAITING, EMPTY)) {
                        System.out.println("TIMEOUT");
                    }
                    else {
                        yrItem = slot.get(stampHolder);
                        slot.set(null, EMPTY);
                        return yrItem;
                    }
               break;
              case WAITING: 
                 if (slot.compareAndSet(yrItem, myItem, WAITING, BUSY))
                   return yrItem;
                break;
             case BUSY:
                break;
            }
           }
      }
   }
class EliminationArray<T> {
    private static final int duration = 2;
    LockFreeExchanger<T>[] exchanger;
    Random random;
    
    public EliminationArray(int capacity) {
        exchanger = (LockFreeExchanger<T>[]) new LockFreeExchanger[capacity];
        for (int i = 0; i < capacity; i++) {
            exchanger[i] = new LockFreeExchanger<T>();
        }
    random = new Random();
    }
    public T visit(T value, int range) throws TimeoutException {
        int slot = random.nextInt(range);
        return (exchanger[slot].exchange(value, duration,
        TimeUnit.MILLISECONDS));
   }
 }

class Backoff {
                final int minDelay, maxDelay;
                int limit;
                final Random random;
                public Backoff(int min, int max) {
                    minDelay = min;
                    maxDelay = max;
                    limit = minDelay;
                    random = new Random();
                }

                public void backoff() throws InterruptedException {
                    int delay = random.nextInt(limit);
                    limit = Math.min(maxDelay, 2*limit);
                    Thread.sleep(delay);
                }
    }