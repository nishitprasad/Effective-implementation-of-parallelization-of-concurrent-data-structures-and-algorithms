import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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

class Node {
    T item;
    int key;
    Node next;
    
    public Node() {

    }
    
    public Node(T item) {
        this.item = item;
        this.next= null;
    }

    public T getItem() {
        return item;
    }
}

public class LockBasedStack {

    public static void main(String[] args) throws InterruptedException {
        long lStartTime = System.currentTimeMillis();
        LockBasedStack LBStack = new LockBasedStack();
        int n=10000;
        LBStack.setOpCount(n);
                    
        Operation t1 = new Operation(LBStack,"Push");
        Operation t2 = new Operation(LBStack,"Push");
        Operation t3 = new Operation(LBStack,"Push");
        Operation t4 = new Operation(LBStack,"Pop");
        Operation t5 = new Operation(LBStack,"Pop");
        
        
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
        System.out.println("Thread 1 Push Count: " + LBStack.count_Push1);
        System.out.println("Thread 2 Push Count: " + LBStack.count_Push2);
        System.out.println("Thread 3 Push Count: " + LBStack.count_Push3);
        System.out.println("Thread 4 Pop Count: " + LBStack.count_Pop4);
        System.out.println("Thread 5 Pop Count: " + LBStack.count_Pop5);
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
    LockBasedStack S;
    int n=10000;
    T[] item;
    public Operation(LockBasedStack S, String op) {
        this.op = op;
        this.S = S;
        item = new T[n];
    }

    @Override
    public void run() {
        if(op.contains("Push")){
            while(S.Push_Op.getAndDecrement()>0){                
                item[S.a] = new T(S.a+1);
                S.push(item[S.a]);
                S.a++;
                //S.Push_Op.decrementAndGet();
            }
            
    }   else{
            while(S.Pop_Op.getAndDecrement()>0){                
                S.pop();
            }
    }
}
}

class LockBasedStack {
    Node top;
    ReentrantLock lock = new ReentrantLock();
    int count_Push1=0, count_Push2=0, count_Push3=0;
    int count_Pop4 =0, count_Pop5 =0;
    static int number_of_op;
    static int a=0;
    public AtomicInteger Push_Op = new AtomicInteger();
    public AtomicInteger Pop_Op = new AtomicInteger();
            
    
    public void setOpCount(int n){
        number_of_op = n;
        Push_Op.set(number_of_op);
        Pop_Op.set(number_of_op);
    }
      
    public LockBasedStack(){
        top = null;
    }
       
    void push(T item){
            lock.lock();
            try {
                Node curr = top;
                top = new Node(item);
                top.next = curr;
                /*System.out.println("Item Pushed " + top.item.getValue());*/ 
                if(Thread.currentThread().getId()==9){
                    count_Push1++;
                }
                else if(Thread.currentThread().getId()==10){
                    count_Push2++;
                }
                else{
                    count_Push3++;
                }              
            } finally {
            lock.unlock();
            }      
    }
    void pop(){
        lock.lock();
            try {
                if (top==null){
                /*System.out.println("Stack is empty");*/
                } else {
                /*System.out.println("Item Popped " + top.item.getValue());*/
                    top = top.next;
                    if(Thread.currentThread().getId()==12){
                        count_Pop4++;
                    }else{
                        count_Pop5++;
                   }
                }
            }finally {
                lock.unlock();
            }
    }
}