/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa.storage;

import java.util.LinkedList;
import java.util.Queue;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class PrioritizableReentrantLockTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(PrioritizableReentrantLockTest.class);

    /**
     * Test of finalize method, of class PrioritizableReentrantLock.
     * {@code finalize} is tested in {@link #lock() } as well.
     * @throws Throwable
     */
    @Test
    public void testFinalize() throws Throwable {
        PrioritizableReentrantLock instance = new PrioritizableReentrantLock();
        instance.finalize();
        //nothing to assert, just checking for deadlock
    }

    /**
     * Test of lock method, of class PrioritizableReentrantLock.
     */
    @Test
    public void testLock() {
        PrioritizableReentrantLock instance = new PrioritizableReentrantLock();
        //test IllegalArgumentException on priority == -1
        try {
            instance.lock(-1);
            fail("IllegalArgumentException expected");
        }catch(IllegalArgumentException ex) {
            //expected
        }

        //test non-concurrent usage
        instance.lock(10);
        instance.unlock();
        instance.lock(10);
        instance.unlock();
        instance.lock(11);
        instance.unlock();
        instance.lock(10);
        instance.unlock();

        //test concurrent usage (test that next to 100 threads with low priority
        //one thread with high priority is executed not in order (other
        //concurrent test are difficult to implement)
        Queue<Thread> threadWaitQueue = new LinkedList<>();
        LinkedList<Thread> threadOrderQueue = new LinkedList<>();
        for(int i=0; i<100; i++) {
            Thread nonPriorityThread = new TestThread(instance,
                    3,
                    threadOrderQueue,
                    String.format("non-priority thread %d", i),
                    100 //sleepMillis
            );
            nonPriorityThread.start();
            threadWaitQueue.add(nonPriorityThread);
        }
        Thread priorityThread1 = new TestThread(
                instance,
                20,
                threadOrderQueue,
                "priority thread",
                0 //sleepMillis
        );
        Thread priorityThread2 = new TestThread(
                instance,
                21,
                threadOrderQueue,
                "priorty-thread-2",
                0 //sleepMillis
        );
        priorityThread1.start();
        priorityThread2.start();
        try {
            priorityThread1.join();
            priorityThread2.join();
            while(!threadWaitQueue.isEmpty()) {
                threadWaitQueue.poll().join();
            }
        }catch(InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        assertTrue(threadOrderQueue.contains(priorityThread1));
        assertTrue(threadOrderQueue.contains(priorityThread2));
        int priorityThread1OrderIndex = threadOrderQueue.indexOf(priorityThread1);
        int priorityThread2OrderIndex = threadOrderQueue.indexOf(priorityThread2);
        LOGGER.info(String.format("index of priority thread 1 is %d", priorityThread1OrderIndex));
        LOGGER.info(String.format("index of priority thread 2 is %d", priorityThread2OrderIndex));
        assertTrue(priorityThread1OrderIndex < threadOrderQueue.size()-1);
        assertTrue(priorityThread2OrderIndex < threadOrderQueue.size()-1);
    }

    private class TestThread extends Thread {
        private final PrioritizableReentrantLock instance;
        private final int priority;
        private final Queue<Thread> orderQueue;
        private final int sleepMillis;

        TestThread(PrioritizableReentrantLock instance,
                int priority,
                Queue<Thread> orderQueue,
                String name,
                int sleepMillis) {
            super(name);
            this.instance = instance;
            this.priority = priority;
            this.orderQueue = orderQueue;
            this.sleepMillis = sleepMillis;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepMillis);
                    //ensure that locking isn't too fast for priority thread to
                    //get priority
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            instance.lock(priority);
            synchronized(PrioritizableReentrantLockTest.this) {
                orderQueue.add(this);
            }
            instance.unlock();
        }
    }
}
