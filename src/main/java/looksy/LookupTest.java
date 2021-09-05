package looksy;

import java.util.*;

public class LookupTest {
    class Tuple<E> {
        public E _1;
        public E _2;

        Tuple(E _1, E _2) {
            this._1 = _1;
            this._2 = _2;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No test name specified");
            System.exit(1);
        }
        LookupTest test = new LookupTest();

        Tuple<Long> tuple = null;
        switch (args[0]) {
            case "lookupArrayLinkedList":
                tuple = test.lookupArrayLinkedList();
                break;
            case "lookupLinkedList":
                tuple = test.lookupLinkedList();
                break;
            case "lookupArray":
                tuple = test.lookupArray();
                break;
            case "lookupArraySequential":
                tuple = test.lookupArraySequential();
                break;
            case "lookupList":
                tuple = test.lookUpList();
                break;
            case "lookupHashMap":
                tuple = test.lookupHashMap();
                break;
            case "lookupSprawlingLinkedList":
                tuple = test.lookupSprawlingLinkedList();
                break;
            default:
                System.err.println("What?");
                System.exit(2);
        }
        System.out.printf("Duration: %d, count %d\n", tuple._1, tuple._2);
    }

    Tuple<Long> lookupArray() {
        Random r = new Random(3545652656L);
        int arraySize = 2500000;
        long[] bigArray = new long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            bigArray[i] = Math.abs(r.nextLong()) + 1;
        }

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1500000000; i++) {
            int index = (int)(Math.abs(r.nextLong()) % arraySize);
            long valueAtIndex = bigArray[index];
            assert(valueAtIndex > 0);
            counter++;
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

    long missCount = 0;
    private int findUnusedSlot(Random r, long[] array) {
        int entryCount = array.length / 2;
        // get random slot in entry array
        int startSlotIndex = (int)(Math.abs(r.nextInt()) % entryCount);
        for (int i = 0; i < entryCount; i++) {
            int candidate = (startSlotIndex + i) % entryCount;
            int candidateIndex = candidate * 2;
            if (array[candidateIndex] == -1) {
                return candidateIndex;
            }
            missCount++;
        }
        throw new RuntimeException("Unexpectedly ran out of slots!");
    }

    private int createLinkedList(Random r, long[] array) {
        int head = -1;
        int current = -1;
        for (int i = 0; i < 500; i++) {
            int next = findUnusedSlot(r, array);
            array[next] = Math.abs(r.nextLong()) + 1;
            if (head == -1) {
                head = next;
            } else {
                array[current + 1] = next;
            }
            current = next;
        }
        return head;
    }

    private Iterator<Long> getIter(long[] array, int head) {
        return new Iterator<Long>() {
            int next = head;

            public boolean hasNext() {
                return next != -1;
            }

            public Long next() {
                long value = array[next];
                next = (int) array[next + 1];
                return value;
            }
        };
    }

    Tuple<Long> lookupArrayLinkedList() {
        Random r = new Random(3545652656L);
        int entryCount = 2500000;
        long[] bigArray = new long[entryCount*2];
        for (int i = 0; i < bigArray.length; i++) {
            bigArray[i] = -1L;
        }

        int[] headArray = new int[5000];
        // create 5000 linked lists, each of length 500 with non-contiguous entries in bigArray.
        for (int index = 0; index < headArray.length; index++) {
            headArray[index] = createLinkedList(r, bigArray);
        }
        System.err.printf("Missed count is %d\n", missCount);

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 3000000; i++) {
            int index = (int) (Math.abs(r.nextLong()) % headArray.length);
            int head = headArray[index];
            Iterator<Long> valueIt = getIter(bigArray, head);
            while (valueIt.hasNext()) {
                long value = valueIt.next();
                assert(value > 0);
                counter++;
            }

            /* int next = headArray[index];
            while (next != -1) {
                long value = bigArray[next];
                assert(value > 0);
                counter++;
                next = (int) bigArray[next + 1];
            } */
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

    class Node {
        long value;
        Node next;

        Node(long value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    class SimpleLinkedList {
        Node head;
        int size = 0;

        void add(long value) {
            head = new Node(value, head);
            size++;
        }

        Iterator<Long> iterator() {
            return new Iterator<Long>() {
                Node next = head;

                public boolean hasNext() {
                    return next != null;
                }

                public Long next() {
                    long value = next.value;
                    next = next.next;
                    return value;
                }
            };
        }
    }

    Tuple<Long> lookupLinkedList() {
        Random r = new Random(3545652656L);
        SimpleLinkedList[] listArray = new SimpleLinkedList[5000];
        for (int i = 0; i < 2500000; i++) {
            // As much as possible, avoid appending to the same linked list
            // in sequence. We don't want the sequential nodes of a particular
            // linked list to be adjacent in the heap.
            // When we later traverse a linked list, we want to bounce
            // around (although we probably will do that within a limited
            // area of the heap, since we're only allocating 2500000 nodes).
            // Also, append only to lists that have less than 500 values.
            boolean fullList = true;
            while (fullList) {
                int index = (int) (Math.abs(r.nextLong()) % listArray.length);
                SimpleLinkedList list = listArray[index];
                if (list == null) {
                    list = new SimpleLinkedList();
                    listArray[index] = list;
                }
                if (list.size < 500) {
                    list.add(Math.abs(r.nextLong()) + 1);
                    fullList = false;
                }
            }
        }
        // check that there are no null entries
        for (int index = 0; index < listArray.length; index++) {
            if (listArray[index] == null) {
                SimpleLinkedList list = new SimpleLinkedList();
                listArray[index] = list;
                for (int j = 0; j < 500; j++) {
                    list.add(Math.abs(r.nextLong()) + 1);
                }
            }
        }

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 3000000; i++) {
            int index = (int)(Math.abs(r.nextLong()) % listArray.length);
            SimpleLinkedList list = listArray[index];
            for (Iterator<Long> it = list.iterator(); it.hasNext();) {
                long value = it.next();
                assert(value > 0);
                counter++;
            }
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

    Tuple<Long> lookupArraySequential() {
        Random r = new Random(3545652656L);
        int arraySize = 2500000;
        Long[] bigArray = new Long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            bigArray[i] = Math.abs(r.nextLong()) + 1;
        }

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1500000000; i++) {
            int index = i % arraySize;
            // still call random. I am assuming that the compiler/JVM will
            // not optimize away this entire line, even though index2 is not used,
            // because of possible side effects of a method call.
            int index2 = (int)(Math.abs(r.nextLong()) % arraySize);
            long valueAtIndex = bigArray[index];
            assert(valueAtIndex > 0);
            counter++;
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

    Tuple<Long> lookUpList() {
        Random r = new Random(3545652656L);
        List<Long>[] listArray = new List[5000];
        for (int i = 0; i < 2500000; i++) {
            // As much as possible, avoid appending to the same linked list
            // in sequence. We don't want the sequential nodes of a particular
            // linked list to be adjacent in the heap.
            // When we later traverse a linked list, we want to bounce
            // around (although we probably will do that within a limited
            // area of the heap, since we're only allocating 2500000 nodes).
            int index = (int)(Math.abs(r.nextLong()) % listArray.length);
            List<Long> list = listArray[index];
            if (list == null) {
                list = new LinkedList<>();
                listArray[index] = list;
            }
            list.add(Math.abs(r.nextLong()) + 1);
        }
        // check that there are no null entries
        for (int index = 0; index < listArray.length; index++) {
            if (listArray[index] == null) {
                List<Long> list = new LinkedList<>();
                listArray[index] = list;
                for (int j = 0; j < 500; j++) {
                    list.add(Math.abs(r.nextLong()) + 1);
                }
            }
        }

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 3000000; i++) {
            int index = (int)(Math.abs(r.nextLong()) % listArray.length);
            List<Long> list = listArray[index];
            for (Iterator<Long> it = list.iterator(); it.hasNext();) {
                long value = it.next();
                assert(value > 0);
                counter++;
            }
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

    Tuple<Long> lookupHashMap() {
        Random r = new Random(3545652656L);
        int mapSize = 2500000;
        Map<Integer, Long> bigMap = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            bigMap.put(i, Math.abs(r.nextLong()) + 1);
        }

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1500000000; i++) {
            int index = (int)(Math.abs(r.nextLong()) % mapSize);
            long valueAtIndex = bigMap.get(index);
            assert(valueAtIndex > 0);
            counter++;
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

    Tuple<Long> lookupSprawlingLinkedList() {
        class Node {
            Node next;
            long value;

            Node(long value) {
                this.value = value;
            }
        }

        Random r = new Random(3545652656L);
        int arraySize = 2500000;
        Node[] bigArray = new Node[arraySize];

        // bounce around the array, adding Nodes that we will later add to a linked list
        for (int i = 0; i < arraySize*3; i++) {
            int index = (int)(Math.abs(r.nextLong()) % arraySize);
            if (bigArray[index] == null) {
                bigArray[index] = new Node(Math.abs(r.nextLong()) + 1);
            }
        }
        // find all the null entries and fill them
        int nullCount = 0;
        for (int index = 0; index < arraySize; index++) {
            if (bigArray[index] == null) {
                nullCount++;
                bigArray[index] = new Node(Math.abs(r.nextLong()) + 1);
            }
        }
        System.err.printf("Null count was %d\n", nullCount);

        // iterate through the array, constructing a linked list
        Node head = bigArray[0];
        Node current = head;
        for (int index = 1; index < arraySize; index++) {
            current.next = bigArray[index];
            current = current.next;
        }

        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1500000000/arraySize; i++) {
            Node next = head;
            while (next != null) {
                long value = next.value;
                assert (value > 0);
                // we need to get a random number and do something with it
                int compareValue = (int)(Math.abs(r.nextLong()) % arraySize) * -1;
                if (value < compareValue) {
                    System.err.println("Impossible condition!");
                    System.exit(3);
                }
                counter++;
                next = next.next;
            }
        }
        long duration = System.currentTimeMillis() - startTime;

        return new Tuple(duration, counter);
    }

}
