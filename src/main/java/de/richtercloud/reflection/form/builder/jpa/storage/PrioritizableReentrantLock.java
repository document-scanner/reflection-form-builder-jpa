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
package de.richtercloud.reflection.form.builder.jpa.storage;

import de.richtercloud.message.handler.BugHandler;
import de.richtercloud.message.handler.ExceptionMessage;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
internal implementation notes:
- see internal implementation notes of ManagerQueueEntry for choice of
Lock/Condition/other monitor
- need to use one condition per Thread, not per priority in order to avoid all
priorities being executed at once
- In order to allow any sorting between different priorities it's necessary to
enqueue into the priority queue determining the thread with the highest priority
before waiting.
*/
/**
 * Maps priorities passed in {@link #lock(int) } to separate locks which are
 * managed in a priority queue where the topmost/head lock will be notified
 * after the last priority lock has been freed.
 *
 * @author richter
 */
public class PrioritizableReentrantLock extends ReentrantLock {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(PrioritizableReentrantLock.class);
    public final static int PRIORITY_DEFAULT = 10;
    @SuppressWarnings("PMD.AccessorMethodGeneration")
    private final PriorityBlockingQueue<ManagerQueueEntry> managerQueue = new PriorityBlockingQueue<>(100, (ManagerQueueEntry o1, ManagerQueueEntry o2) -> {
        return o2.priority.compareTo(o1.priority);
    });
    private final Thread managerThread;
    private final Lock conditionLock = new ReentrantLock();
    private final Condition locked = conditionLock.newCondition();
    private final BugHandler bugHandler;

    /**
     * Creates a {@code PrioritizableReentrantLock} with unfair policy for lock
     * requests with same priority.
     *
     * @param bugHandler the {@link BugHandler} to use for exceptions in the
     *     manager thread
     */
    public PrioritizableReentrantLock(BugHandler bugHandler) {
        this(false,
                bugHandler);
    }

    /**
     * Creates a {@code PrioritizableReentrantLock} with the given fairness
     * policy for requests lock requests with the same priority.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     * @param bugHandler the {@link BugHandler} to use for exceptions in the
     *     manager thread
     */
    @SuppressWarnings({"PMD.AvoidCatchingThrowable",
        "PMD.AccessorMethodGeneration"
    })
    public PrioritizableReentrantLock(boolean fair,
            BugHandler bugHandler) {
        super(fair);
        this.bugHandler = bugHandler;
        this.managerThread = new Thread(() -> {
            //can't control shutdown with boolean since managerQueue.take somehow
            //has to be brought to return (Poison Pill Shutdown
            //<ref>http://stackoverflow.com/questions/812342/how-to-interrupt-a-blockingqueue-which-is-blocking-on-take</ref>)
            while(true) {
                ManagerQueueEntry managerQueueHead;
                try {
                    managerQueueHead = managerQueue.take();
                }catch(InterruptedException ex) {
                    LOGGER.error("unexpected exception during dequeing element occured",
                            ex);
                    bugHandler.handleUnexpectedException(new ExceptionMessage(ex));
                    return;
                        //It's fine to return if the thread has been interrupted
                        //which is an unexpected condition anyway and can't be
                        //handled better than reporting an unexpected exception
                }
                if(managerQueueHead.condition == null) {
                    LOGGER.trace(String.format("received %s with condition null, meaning shutdown requested", ManagerQueueEntry.class));
                    break;
                }
                this.conditionLock.lock();
                try {
                    Condition managerQueueHeadCondition = managerQueueHead.condition;
                    LOGGER.trace(String.format("signaling condition with priority %d", managerQueueHead.priority));
                    managerQueueHeadCondition.signal();
                    try {
                        this.locked.await();
                    }catch(InterruptedException ex) {
                        LOGGER.error("unexpected exception during awaiting lock condition occured",
                                ex);
                        bugHandler.handleUnexpectedException(new ExceptionMessage(ex));
                        return;
                    }
                }catch(Throwable ex) {
                    LOGGER.error("unexpected exception during queue processing occured",
                            ex);
                    bugHandler.handleUnexpectedException(new ExceptionMessage(ex));
                }finally {
                    this.conditionLock.unlock();
                }
            }
        },
                "prioritizable-reentrant-lock-manager-thread");
        this.managerThread.start();
    }

    @Override
    protected void finalize() throws Throwable {
        LOGGER.trace(String.format("sending %s with condition null to manager queue in order to request shutdown", ManagerQueueEntry.class));
        managerQueue.offer(new ManagerQueueEntry(null, 0));
        managerThread.join();
        super.finalize();
    }

    @Override
    public void lock() {
        lock(PRIORITY_DEFAULT);
    }

    /*
    internal implementation notes:
    - There's no sense in waiting for anything other than the priority specific
    condition, lock or whatever because otherwise all incoming locking requests
    just end up waiting and no priorization can take place
    */
    /**
     * Waits for {@link ReentrantLock#lock() } until all locks with higher
     * priority have been acquired.
     * @param priority the desired priority (higher values causes earlier
     *     acquisition of lock)
     */
    @SuppressWarnings("PMD.AccessorMethodGeneration")
    public void lock(int priority) {
        if(priority < 0) {
            throw new IllegalArgumentException("priority has to be >= 0");
        }
        LOGGER.trace(String.format("locking with priority %d requested", priority));
        conditionLock.lock();
        try {
            //check whether a Condition already exists for the desired priority
            //(needs to run after conditionLock.lock)
            ManagerQueueEntry entry = new ManagerQueueEntry(conditionLock.newCondition(),
                        priority);
            managerQueue.offer(entry);
            LOGGER.trace(String.format("signaling manager queue condition with priority %d", priority));
            try {
                LOGGER.trace(String.format("waiting for condition with priority %d", priority));
                entry.condition.await();
            } catch (InterruptedException ex) {
                LOGGER.error("unexpected exception during awaiting lock condition occured",
                        ex);
                bugHandler.handleUnexpectedException(new ExceptionMessage(ex));
                return;
                    //It's fine to return if the thread has been interrupted
                    //which is an unexpected condition anyway and can't be
                    //handled better than reporting an unexpected exception
            }
            LOGGER.trace(String.format("locking with priority %d", priority));
            LOGGER.trace("locking thread");
            super.lock();
            LOGGER.trace("locked thread");
        }finally {
            conditionLock.unlock();
        }
    }

    @Override
    public void unlock() {
        super.unlock();
        LOGGER.trace("unlocked from thread");
        conditionLock.lock();
        try {
            LOGGER.trace("signaling manager queue condition");
            locked.signal();
        }finally {
            conditionLock.unlock();
        }
    }

    /*
    Internal implementation notes:
    - Using a Lock for the mapping to priority doesn't make sense because it's
    difficult to guarantee that the managerThread with successfully lock before
    threads invoking PrioritizableReentrantLock.lock(int) will -> need to use a
    Condition
    */
    /**
     * Encapsulates priority and an object to wait for in {@code managerQueue}.
     */
    private class ManagerQueueEntry {
        private final Condition condition;
        /**
         * Higher values cause higher priority, i.e. a place closer to the
         * queue's head.
         */
        private final Integer priority;

        /*
        internal implementation notes:
        - setting minimum of priority to 0 is for convenience (avoids
        confusion of relation to negative and positive priority values)
        */
        /**
         *
         * @param condition the condition to wait for
         * @param priority the priority
         * @throws IllegalArgumentException if {@code priority} is less than 0
         */
        protected ManagerQueueEntry(Condition condition, int priority) {
            //needs to allow condition == null for Poison Pill Shutdown (see
            //comment in managerThread)
            assert priority >= 0;
            this.condition = condition;
            this.priority = priority;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.condition);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ManagerQueueEntry other = (ManagerQueueEntry) obj;
            return Objects.equals(this.condition, other.condition);
        }
    }
}
