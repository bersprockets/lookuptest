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
            default:
                System.err.println("What?");
                System.exit(2);
        }
        System.out.printf("Duration: %d, count %d\n", tuple._1, tuple._2);
    }

    Tuple<Long> lookupArray() {
        Random r = new Random(3545652656L);
        int arraySize = 2500000;
        Long[] bigArray = new Long[arraySize];
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
}
