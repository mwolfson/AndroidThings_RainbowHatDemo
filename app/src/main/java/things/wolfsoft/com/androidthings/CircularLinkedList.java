package things.wolfsoft.com.androidthings;

/**
 * Created by huilin on 6/4/17.
 */


public class CircularLinkedList<T> {

    public int size =0;
    public Node head=null;
    public Node tail=null;

    //add a new node at the start of the linked list
    public void addNodeAtStart(Character data){
        Node n = new Node(data);
        if(size==0){
            head = n;
            tail = n;
            n.next = head;
        }else{
            Node temp = head;
            n.next = temp;
            head = n;
            tail.next = head;
        }
        size++;
    }

    public void addNodeAtEnd(Character data){
        if(size==0){
            addNodeAtStart(data);
        }else{
            Node n = new Node(data);
            tail.next =n;
            tail=n;
            tail.next = head;
            size++;
        }
    }

    public Node getNode(int index){
        Node temp = head;
        for (int i=0; i<index; i++){
            temp = temp.next;
        }
        return temp;
    }

    public int elementAt(int index){
        if(index>size){
            return -1;
        }
        Node n = head;
        while(index-1!=0){
            n=n.next;
            index--;
        }
        return n.data;
    }

    //get Size
    public int getSize(){
        return size;
    }

}
