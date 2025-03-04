package looksy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    String testName = args[0];
    boolean pause = false;
    if (args.length > 1 && args[1].equalsIgnoreCase("pause")) {
      pause = true;
    }
    LookupTest test = new LookupTest();

    Tuple<Long> tuple = null;
    switch (testName) {
      case "lookupArrayLinkedList":
        tuple = test.lookupArrayLinkedList(pause);
        break;
      case "lookupCompactedArrayLinkedList":
        tuple = test.lookupCompactedArrayLinkedList(pause);
        break;
      case "lookupLinkedList":
        tuple = test.lookupLinkedList(pause);
        break;
      case "lookupSequentialArrayLinkedList":
        tuple = test.lookupSequentialArrayLinkedList(pause);
        break;
      case "lookupSequentialLinkedList":
        tuple = test.lookupSequentialLinkedList(pause);
        break;
      case "lookupLinkedListShort":
        tuple = test.lookupLinkedListShort(pause);
        break;
      case "lookupArrayLinkedListShort":
        tuple = test.lookupArrayLinkedListShort(pause);
        break;
      default:
        System.err.println("What?");
        System.exit(2);
    }
    System.out.printf("Duration: %d, count %d\n", tuple._1, tuple._2);
  }

  private void prompt() {
    System.out.println("Pausing...");
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    try {
      reader.readLine();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
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

  class BlockOfLongs {
    long[] array;

    BlockOfLongs(long[] array) {
      this.array = array;
      for (int i = 0; i < this.array.length; i++) {
        this.array[i] = -1L;
      }
    }

    int cursor = 0;

    int allocate(int size) {
      int index = cursor;
      if (index >= array.length) {
        throw new RuntimeException("Ran out of space! " + index);
      }
      cursor += size;
      return index;
    }
  }

  Tuple<Long> lookupArrayLinkedList(boolean pause) {
    Random r = new Random(3545652656L);

    int entryCount = 2500000;
    long[] bigArray = new long[entryCount * 2];
    BlockOfLongs bol = new BlockOfLongs(bigArray);

    int[] headArray = new int[5000];
    for (int i = 0; i < headArray.length; i++) {
      headArray[i] = -1;
    }
    // create 5000 linked lists, each of length 500 with non-contiguous but
    // descending entries in bigArray.
    for (int i = 0; i < 500; i++) {
      for (int headIndex = 0; headIndex < headArray.length; headIndex++) {
        int index = bol.allocate(2);
        bigArray[index] = Math.abs(r.nextLong()) + 1;
        bigArray[index + 1] = headArray[headIndex];
        headArray[headIndex] = index;
      }
    }

    if (pause) {
      prompt();
    }
    int counter = 0;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 3000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % headArray.length);
      Iterator<Long> it = getIter(bigArray, headArray[index]);
      while (it.hasNext()) {
        long value = it.next();
        assert (value >= 2000000);
        counter++;
      }
    }
    long duration = System.currentTimeMillis() - startTime;

    return new Tuple(duration, counter);
  }

  Tuple<Long> lookupCompactedArrayLinkedList(boolean pause) {
    Random r = new Random(3545652656L);

    int entryCount = 2500000;
    long[] bigArray = new long[entryCount * 2];
    BlockOfLongs bol = new BlockOfLongs(bigArray);

    int[] headArray = new int[5000];
    for (int i = 0; i < headArray.length; i++) {
      headArray[i] = -1;
    }
    // create 5000 linked lists, each of length 500 with non-contiguous but
    // descending entries in bigArray.
    for (int i = 0; i < 500; i++) {
      for (int headIndex = 0; headIndex < headArray.length; headIndex++) {
        int index = bol.allocate(2);
        bigArray[index] = Math.abs(r.nextLong()) + 1;
        bigArray[index + 1] = headArray[headIndex];
        headArray[headIndex] = index;
      }
    }

    if (pause) {
      prompt();
    }

    long startTime = System.currentTimeMillis();

    // first, compact bigArray
    long[] oldBigArray = bigArray;
    bigArray = new long[entryCount * 2];
    bol = new BlockOfLongs(bigArray);
    for (int headIndex = 0; headIndex < headArray.length; headIndex++) {
      int next = headArray[headIndex];
      headArray[headIndex] = -1;
      while (next != -1) {
        long value = oldBigArray[next];
        int index = bol.allocate(2);
        bigArray[index] = value;
        bigArray[index + 1] = headArray[headIndex];
        headArray[headIndex] = index;
        next = (int) oldBigArray[next + 1];
      }
    }

    int counter = 0;
    for (int i = 0; i < 3000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % headArray.length);
      Iterator<Long> it = getIter(bigArray, headArray[index]);
      while (it.hasNext()) {
        long value = it.next();
        assert (value >= 2000000);
        counter++;
      }
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

  Tuple<Long> lookupLinkedList(boolean pause) {
    Random r = new Random(3545652656L);
    SimpleLinkedList[] listArray = new SimpleLinkedList[5000];
    // Avoid appending to the same linked list in sequence.
    // We don't want the sequential nodes of a particular
    // linked list to be adjacent in the heap.
    for (int i = 0; i < 500; i++) {
      for (int listIndex = 0; listIndex < listArray.length; listIndex++) {
        SimpleLinkedList list = listArray[listIndex];
        if (list == null) {
          list = new SimpleLinkedList();
          listArray[listIndex] = list;
        }
        list.add(Math.abs(r.nextLong()) + 1);
      }
    }

    if (pause) {
      prompt();
    }
    int counter = 0;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 3000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % listArray.length);
      SimpleLinkedList list = listArray[index];
      Iterator<Long> it = list.iterator();
      while (it.hasNext()) {
        long value = it.next();
        assert (value > 0);
        counter++;
      }
    }
    long duration = System.currentTimeMillis() - startTime;

    return new Tuple(duration, counter);
  }

  Tuple<Long> lookupSequentialArrayLinkedList(boolean pause) {
    Random r = new Random(3545652656L);

    int entryCount = 2500000;
    long[] bigArray = new long[entryCount * 2];
    BlockOfLongs bol = new BlockOfLongs(bigArray);

    int[] headArray = new int[5000];
    for (int i = 0; i < headArray.length; i++) {
      headArray[i] = -1;
    }
    // create 5000 linked lists, each of length 500 with contiguous and
    // descending entries in bigArray.
    for (int headIndex = 0; headIndex < headArray.length; headIndex++) {
      for (int i = 0; i < 500; i++) {
        int index = bol.allocate(2);
        bigArray[index] = Math.abs(r.nextLong()) + 1;
        bigArray[index + 1] = headArray[headIndex];
        headArray[headIndex] = index;
      }
    }

    // verify that each linked list has 500 entries and that they are relatively contigous
    System.out.println("Verify lists");
    for (int headIndex = 0; headIndex < headArray.length; headIndex++) {
      int next = headArray[headIndex];
      int count = 0;
      while (next != -1) {
        count++;
        int newNext = (int) bigArray[next + 1];
        if (newNext != -1 && newNext != next - 2) {
          System.err.printf("%d %d\n", newNext, next);
        }
        next = newNext;
      }
      if (count != 500) {
        throw new RuntimeException("Count is " + count);
      }
    }
    System.out.println("Done verifying lists");

    if (pause) {
      prompt();
    }
    int counter = 0;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 3000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % headArray.length);
      Iterator<Long> it = getIter(bigArray, headArray[index]);
      while (it.hasNext()) {
        long value = it.next();
        assert (value >= 2000000);
        counter++;
      }
    }
    long duration = System.currentTimeMillis() - startTime;

    return new Tuple(duration, counter);
  }

  Tuple<Long> lookupSequentialLinkedList(boolean pause) {
    System.out.println("lookupSequentialLinkedList");
    Random r = new Random(3545652656L);
    SimpleLinkedList[] listArray = new SimpleLinkedList[5000];
    for (int listIndex = 0; listIndex < listArray.length; listIndex++) {
      SimpleLinkedList list = new SimpleLinkedList();
      listArray[listIndex] = list;
      for (int i = 0; i < 500; i++) {
        list.add(Math.abs(r.nextLong()) + 1);
      }
    }

    if (pause) {
      prompt();
    }
    int counter = 0;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 3000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % listArray.length);
      SimpleLinkedList list = listArray[index];
      for (Iterator<Long> it = list.iterator(); it.hasNext(); ) {
        long value = it.next();
        assert (value > 0);
        counter++;
      }
    }
    long duration = System.currentTimeMillis() - startTime;

    return new Tuple(duration, counter);
  }

  // short linked lists
  Tuple<Long> lookupLinkedListShort(boolean pause) {
    System.out.println("lookupLinkedListShort");
    Random r = new Random(3545652656L);
    SimpleLinkedList[] listArray = new SimpleLinkedList[500000];
    // Avoid appending to the same linked list in sequence.
    // We don't want the sequential nodes of a particular
    // linked list to be adjacent in the heap.
    for (int i = 0; i < 2; i++) {
      for (int listIndex = 0; listIndex < listArray.length; listIndex++) {
        SimpleLinkedList list = listArray[listIndex];
        if (list == null) {
          list = new SimpleLinkedList();
          listArray[listIndex] = list;
        }
        list.add(Math.abs(r.nextLong()) + 1);
      }
    }

    System.out.printf("Length of list at element 200 is %d\n", listArray[200].size);

    if (pause) {
      prompt();
    }
    int counter = 0;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 750000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % listArray.length);
      SimpleLinkedList list = listArray[index];
      for (Iterator<Long> it = list.iterator(); it.hasNext(); ) {
        long value = it.next();
        assert (value > 0);
        counter++;
      }
    }
    long duration = System.currentTimeMillis() - startTime;

    return new Tuple(duration, counter);
  }

  Tuple<Long> lookupArrayLinkedListShort(boolean pause) {
    System.out.println("lookupArrayLinkedListShort");
    Random r = new Random(3545652656L);

    int entryCount = 2500000;
    long[] bigArray = new long[entryCount * 2];
    BlockOfLongs bol = new BlockOfLongs(bigArray);

    int[] headArray = new int[500000];
    for (int i = 0; i < headArray.length; i++) {
      headArray[i] = -1;
    }
    // create 5000 linked lists, each of length 500 with non-contiguous but
    // descending entries in bigArray.
    for (int i = 0; i < 2; i++) {
      for (int headIndex = 0; headIndex < headArray.length; headIndex++) {
        int index = bol.allocate(2);
        bigArray[index] = Math.abs(r.nextLong()) + 1;
        bigArray[index + 1] = headArray[headIndex];
        headArray[headIndex] = index;
      }
    }

    if (pause) {
      prompt();
    }
    int counter = 0;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 750000000; i++) {
      int index = (int) (Math.abs(r.nextLong()) % headArray.length);
      int next = headArray[index];
      while (next != -1) {
        long value = bigArray[next];
        assert (value >= 2000000);
        counter++;
        next = (int) bigArray[next + 1];
      }
    }
    long duration = System.currentTimeMillis() - startTime;

    return new Tuple(duration, counter);
  }
}
