import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedListThread {

    public static void main(String[] args) throws InterruptedException {
        long lStartTime = System.currentTimeMillis();
        LockBasedLinkedList LBList = new LockBasedLinkedList();
        int n=1000000;
        LBList.setOpCount(n);
        Operation t1 = new Operation(LBList,"I");
        Operation t2 = new Operation(LBList,"I");
        Operation t3 = new Operation(LBList,"I");
        Operation t4 = new Operation(LBList,"R");
        Operation t5 = new Operation(LBList,"R");
        
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
        
        /*Node temp = LBList.head.next;
        while(temp.next!=null){
            System.out.println("Node: " + temp.getItem().getValue()+ " Key " + temp.key);
            temp = temp.next;
        }*/
        System.out.println("Thread 1 Insert Count: " + LBList.count_Insert1);
        System.out.println("Thread 2 Insert Count: " + LBList.count_Insert2);
        System.out.println("Thread 3 Insert Count: " + LBList.count_Insert3);
        System.out.println("Thread 4 Remove Count: " + LBList.count_Remove4);
        System.out.println("Thread 5 Remove Count: " + LBList.count_Remove5);
        
        long lEndTime = System.currentTimeMillis();
        long difference = lEndTime - lStartTime;
	System.out.println("Elapsed milliseconds: " + difference);
    }
}

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
    
    @Override
    public int hashCode() {
        return value;
    }
}

class Node{
    T item;
    int key;
    Node next;
    ReentrantLock lock = new ReentrantLock();
    
    void lock(){
        this.lock.lock();
    }
    
    void unlock(){
        this.lock.unlock();
    }
    public Node() {

    }
    
    public Node(T item) {
        this.item = item;
        this.next= null;
        this.key = item.hashCode();
    }

    public T getItem() {
        return item;
    }
}

class Operation extends Thread
{
    String op;
    LockBasedLinkedList L;
    T[] item;
    int n=1000000;
		
    public Operation(LockBasedLinkedList L, String op) {
        this.L = L;
        this.op = op;
	item = new T[n];
    }

    @Override
    public void run() {
        if(op.contains("I")){
            while(L.Insert_Op.getAndDecrement()>0){
                    item[L.a.get()] = new T(L.a.get()+1);
                    L.insert(item[L.a.get()]);
                    L.a.getAndAdd(1);
            }
        } else {
                while(L.Remove_Op.getAndDecrement()>0){
                        Random rand = new Random();
                        int r = rand.nextInt(L.a.get());
                        L.remove(item[r]);
                }
        }
    }   
}

class LockBasedLinkedList {
    public Node head;
	int count_Insert1=0, count_Insert2=0, count_Insert3=0;
	int count_Remove4 =0, count_Remove5 =0;
	static int number_of_op;
	public AtomicInteger a = new AtomicInteger();
	public AtomicInteger Insert_Op = new AtomicInteger();
	public AtomicInteger Remove_Op = new AtomicInteger();
    public void setOpCount(int n){
        number_of_op = n;
        Insert_Op.set(number_of_op);
        Remove_Op.set(number_of_op);
    }
    
    
    public LockBasedLinkedList() {
        head = new Node(new T(Integer.MIN_VALUE));
        head.next = new Node(new T(Integer.MAX_VALUE));
    }       

    public boolean insert (T item){
        int key = item.hashCode();
        head.lock();
        Node pred = head;
        try {
            Node curr = pred.next;
            curr.lock();
            try {
                while(curr.key < key){
                    pred.unlock();
                    pred=curr;
                    curr = curr.next;
                    curr.lock();
                }
                if(curr.key==key){
                    return false;
                }
                Node newNode = new Node(item);
                /*System.out.println("Item " + item.getValue() + "Inserted");*/
                newNode.next = curr;
                pred.next = newNode;
                if(Thread.currentThread().getId()==9){
                    count_Insert1++;
                }
                else if(Thread.currentThread().getId()==10){
                    count_Insert2++;
                }
                else{
                    count_Insert3++;
                }
                return true;
            } finally {
                curr.unlock();
            }
        } finally {
            pred.unlock();
        }
    }

    public boolean remove(T item) {
        Node pred = null;
        Node curr = null;
        
        int key = item.hashCode();
        head.lock();
        try {
            pred = head;
            curr = pred.next;
            curr.lock();
            try {
                while(curr.key<key) {
                    pred.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock();
                }
                if(curr.key == key) {
                    /*System.out.println("Removed Node " + curr.item.getValue());*/
                    pred.next = curr.next;
                    System.out.println(Thread.currentThread().getId());
                    if(Thread.currentThread().getId()==12){
                        count_Remove4++;
                    }else{
                        count_Remove5++;
                   }
                    return true;
                }                
                return false;                    
            }
            finally {
                curr.unlock();
            }
        }
        finally {
            pred.unlock();
        }
    }
}
