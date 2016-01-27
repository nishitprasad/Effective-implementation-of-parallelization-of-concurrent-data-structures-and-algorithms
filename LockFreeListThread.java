import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

class Window {
    public Node pred, curr;
    Window(Node myPred, Node myCurr) {
        pred = myPred; curr = myCurr;
    }
}

public class LockFreeListThread {
    public static void main(String[] args) throws InterruptedException {
        long lStartTime = System.currentTimeMillis();
        LockFreeLinkedList LFList = new LockFreeLinkedList();
        int n=1000000;
        LFList.setOpCount(n);
        Operation t1 = new Operation(LFList,"I");
        Operation t2 = new Operation(LFList,"I");
        Operation t3 = new Operation(LFList,"I");
        Operation t4 = new Operation(LFList,"R");
        Operation t5 = new Operation(LFList,"R");
        
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
        
        /*Node temp = LFList.head.next;
        while(temp.next!=null){
            System.out.println("Node: " + temp.getItem().getValue()+ " Key " + temp.key);
            temp = temp.next;
        }*/
        System.out.println("Thread 1 Insert Count: " + LFList.count_Insert1);
        System.out.println("Thread 2 Insert Count: " + LFList.count_Insert2);
        System.out.println("Thread 3 Insert Count: " + LFList.count_Insert3);
        System.out.println("Thread 4 Remove Count: " + LFList.count_Remove4);
        System.out.println("Thread 5 Remove Count: " + LFList.count_Remove5);
        
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
        if (this.value == 0){
            return 0;
        }else{
        int hash = value + 1;
        return hash;            
        }
    }
}

class Node {
    T item;
    int key;
    AtomicMarkableReference<Node> next;
    ReentrantLock lock = new ReentrantLock();
    
    void lock(){
        this.lock.lock();
    }
    
    void unlock(){
        this.lock.unlock();
    }

    public Node() {
        this.item.setValue(0);
        this.next= null;
        this.key = item.hashCode();
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
    LockFreeLinkedList L;
    T[] item;
    int n=1000000;
		
    public Operation(LockFreeLinkedList L, String op) {
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

class LockFreeLinkedList {
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
    
    public LockFreeLinkedList() {
        head = new Node(new T(Integer.MIN_VALUE));
        head.next = new AtomicMarkableReference(new T(Integer.MAX_VALUE), false);
    }
       
    public boolean insert(T item) {
        int key = item.hashCode();
        while (true) {
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key == key) {
                return false;
            } else {
                Node node = new Node(item);
                node.next = new AtomicMarkableReference(curr, false);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    /*System.out.println("Pushed Item " + node.item.value);*/
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
				}
            }
        }
    }
       
    public Window find(Node head, int key) {
        Node pred = new Node(), curr = new Node(), succ = new Node();
        boolean[] marked = {false};
        boolean snip;
        retry: while (true) {
        pred = head;
        curr = (Node) pred.next.getReference();
            while (true) {
            succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) continue retry;
                        curr = succ;
                        succ = curr.next.get(marked);
                }
            if (curr.key >= key)
                return new Window(pred, curr);
            pred = curr;
            curr = succ;
            }
        }
    }

    public boolean remove(T item) {
        int key = item.hashCode();
        boolean snip;
        while (true) {
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key != key) {
                return false;
            } else {
                Node succ = curr.next.getReference();
                snip = curr.next.attemptMark(succ, true);
                if (!snip)
                    continue;
                pred.next.compareAndSet(curr, succ, false, false);
                /*System.out.println("Popped Item " + curr.item.value);*/
				if(Thread.currentThread().getId()==12){
                        count_Remove4++;
                }else{
                        count_Remove5++;
                }
                return true;
            }
        }
    }
    
public boolean contains(T item) {
        boolean[] marked = {false};
        int key = item.hashCode();
        Node curr = head;
        while (curr.key < key) {
        curr = curr.next.getReference();
        Node succ = curr.next.get(marked);
    }
    return (curr.key == key && !marked[0]);
    }
}