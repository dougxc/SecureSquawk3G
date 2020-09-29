/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.util.*;
import com.sun.squawk.vm.SC;
import com.sun.squawk.vm.FP;
import com.sun.squawk.vm.HDR;

import java.util.Enumeration;


/**
 * A <i>thread</i> is a thread of execution in a program. The Java
 * Virtual Machine allows an application to have multiple threads of
 * execution running concurrently.
 * <p>
 * Every thread has a priority. Threads with higher priority are
 * executed in preference to threads with lower priority.
 * <p>
 * There are two ways to create a new thread of execution. One is to
 * declare a class to be a subclass of <code>Thread</code>. This
 * subclass should override the <code>run</code> method of class
 * <code>Thread</code>. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes
 * larger than a stated value could be written as follows:
 * <p><hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p><blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 * <p>
 * The other way to create a thread is to declare a class that
 * implements the <code>Runnable</code> interface. That class then
 * implements the <code>run</code> method. An instance of the class can
 * then be allocated, passed as an argument when creating
 * <code>Thread</code>, and started. The same example in this other
 * style looks like the following:
 * <p><hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p><blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 * <p>
 *
 *
 * @author  unascribed
 * @see     java.lang.Runnable
 * @see     java.lang.Runtime#exit(int)
 * @see     java.lang.Thread#run()
 * @since   JDK1.0
 */

public class Thread implements Runnable {

    /*-----------------------------------------------------------------------*\
     *                          Global VM variables                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Flag to help early VM bringup.
     */
    private final static boolean FATAL_MONITOR_ERRORS = false;

    /**
     * The initial size (in words) of a thread's stack.
     */
    private final static int INITIAL_STACK_SIZE = 168;

    /**
     * The minimum size (in words) of a thread's stack. This constant accounts for the
     * number of slots required for the meta-info slots at the beginning of the chunk
     * plus the slots required to successfully make the initial call to VM.do_callRun()
     */
    private final static int MIN_STACK_SIZE = SC.limit + FP.FIXED_FRAME_SIZE + 1;

    /**
     * The current executing thread.
     */
    private static Thread currentThread;

    /**
     * The thread to be executed after the next threadSwap().
     */
    private static Thread otherThread;

    /**
     * The service thread for GC etc.
     */
    private static Thread serviceThread;

    /**
     * The stack for the service thread. This address corresponds
     * with the object pointer to the chunk. That is, it is {@link HDR#arrayHeaderSize}
     * bytes past the memory chunk allocated for the stack. The native launcher
     * which allocated this chunk will have written the length of the stack
     * array header word. This will be used to subsequently format this block
     * as an object of type {@link Klass#LOCAL_ARRAY}.
     */
    private static Address serviceStack;

    /**
     * The queue of runnable threads.
     */
    private static ThreadQueue runnableThreads;

    /**
     * The queue of timed waiting threads.
     */
    private static TimerQueue timerQueue;

    /**
     * The 'name' of the next thread.
     */
    private static int nextThreadNumber;

    /**
     * Hashtable of threads waiting for an event.
     */
    private static EventHashtable events;


    /*-----------------------------------------------------------------------*\
     *                            The public API                             *
    \*-----------------------------------------------------------------------*/

    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

   /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;

   /**
     * Allocates a new <code>Thread</code> object.
     * <p>
     * Threads created this way must have overridden their
     * <code>run()</code> method to actually do anything.
     *
     * @see     java.lang.Runnable
     */
    public Thread() {
        this(null);
    }

    /**
     * Allocates a new <code>Thread</code> object with a
     * specific target object whose <code>run</code> method
     * is called.
     *
     * @param   target   the object whose <code>run</code> method is called.
     */
    public Thread(Runnable target) {
        this(target, INITIAL_STACK_SIZE);
    }

    /**
     * Allocates a new <code>Thread</code> object with a
     * specific target object whose <code>run</code> method
     * is called and a specified stack size
     *
     * @param   target     the object whose <code>run</code> method is called.
     * @param   stackSize  the size (in words/slots) of the stack allocated for this thread
     */
    Thread(Runnable target, int stackSize) {
        this.threadNumber = nextThreadNumber++;
        this.target       = target;
        this.state        = NEW;
        this.stackSize    = Math.max(stackSize, MIN_STACK_SIZE);
        if (target instanceof Isolate) {
            this.isolate  = (Isolate)target;
        } else {
            this.isolate  = VM.getCurrentIsolate();
        }
        if (currentThread != null) {
            priority = (byte)currentThread.getPriority();
        } else {
            priority = NORM_PRIORITY;
        }
    }

    /**
     * Return a reference to the currently executing thread object.
     *
     * @return the currently executing thread
     */
    public static Thread currentThread() {
        return currentThread;
    }

    /**
     * Set the daemonic state of the thread.
     */
    public void setDaemon(boolean value) {
        isDaemon = value;
    }

    /**
     * Get the daemon state of the thread.
     */
    public boolean isDaemon() {
        return isDaemon;
    }

    /**
     * Adds a given thread to the timer queue.
     *
     * Note: this method may assign globals and threads into local variables and
     * so should not be on the stack of a call that eventually calls 'reschedule'
     *
     * @param thread   the thread to add or null if the currentThread should be added
     * @param millis   the time to wait on the queue
     */
    private static void addToTimerQueue(Thread thread, long millis) {
        if (thread == null) {
            thread = currentThread;
        }
        timerQueue.add(thread, millis);
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds. The thread
     * does not lose ownership of any monitors.
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        java.lang.Object#notify()
     */
    public static void sleep(long millis) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("negative sleep time");
        }
        if (millis > 0) {
            startFinalizers();
            addToTimerQueue(null, millis);
            reschedule();
        }
    }

    /**
     * Adds a given thread to the queue of runnable threads.
     *
     * Note: this method may assign globals and threads into local variables and
     * so should not be on the stack of a call that eventually calls 'reschedule'
     *
     * @param thread   the thread to add or null if the currentThread should be added
     */
    private static void addToRunnableThreadsQueue(Thread thread) {
        if (thread == null) {
            thread = currentThread;
        }
        runnableThreads.add(thread);
    }


   /**
    * Causes the currently executing thread object to temporarily pause
    * and allow other threads to execute.
    */
    public static void yield() {
        startFinalizers();
        addToRunnableThreadsQueue(null);
        reschedule();
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the <code>run</code> method of this thread.
     * <p>
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * <code>start</code> method) and the other thread (which executes its
     * <code>run</code> method).
     *
     * @exception  IllegalThreadStateException  if the thread was already started.
     * @see        java.lang.Thread#run()
     */
    public void start() {

       /*
        * Check that the thread has not yet been started.
        */
        if (state != NEW) {
            throw new IllegalThreadStateException();
        }

        /*
         * Initialize the new thread and add it to the list of runnable threads.
         */
        baptiseThread();
    }


    /**
     * If this thread was constructed using a separate
     * <code>Runnable</code> run object, then that
     * <code>Runnable</code> object's <code>run</code> method is called;
     * otherwise, this method does nothing and returns.
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     *
     * @see     java.lang.Thread#start()
     * @see     java.lang.Runnable#run()
     */
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    public final boolean isAlive() {
        return state == ALIVE;
    }

    /**
     * Changes the priority of this thread.
     *
     * @param newPriority priority to set this thread to
     * @exception  IllegalArgumentException  If the priority is not in the
     *             range <code>MIN_PRIORITY</code> to
     *             <code>MAX_PRIORITY</code>.
     * @see        #getPriority
     * @see        java.lang.Thread#getPriority()
     * @see        java.lang.Thread#MAX_PRIORITY
     * @see        java.lang.Thread#MIN_PRIORITY
     */
    public final void setPriority(int newPriority) {
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        priority = (byte)newPriority;
    }

    /**
     * Returns this thread's priority.
     *
     * @return  this thread's name.
     * @see     #setPriority
     * @see     java.lang.Thread#setPriority(int)
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Returns the current number of active threads in the VM.
     *
     * @return the current number of active threads
     */
    public static int activeCount() {
        return runnableThreads.size() + 1;
    }

    /**
     * Waits for this thread to die.
     *
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    public final void join() throws InterruptedException {
        if (this != currentThread && isAlive()) {
            Assert.that(currentThread.nextThread == null);
            currentThread.nextThread = this.joiners;
            this.joiners = currentThread;
            currentThread.setInQueue(Thread.JOIN);
            reschedule();
        }
    }

    /**
     * Waits for an isolate to stop.
     *
     * @param isolate the isolate to wait for
     */
    final static void isolateJoin(Isolate isolate) {
        if (currentThread.isolate == isolate) {
            throw new RuntimeException("Isolate cannot join itself");
        }
        if (!isolate.isHibernated()) {
            Assert.that(currentThread.nextThread == null);
            currentThread.setInQueue(Thread.ISOLATEJOIN);
            isolate.addJoiner(currentThread);
            reschedule();
        }
    }

    /**
     * Hibernate all the threads in the isolate and start the threads waiting for this event.
     *
     * @param isolate the isolate
     */
    private static void hibernateIsolate0(Isolate isolate) {

        /*
         * Enable all the threads waiting for the isolate to stop.
         */
        Thread list = isolate.getJoiners();
        startJoiners(list, Thread.ISOLATEJOIN);

        /*
         * Prune the runnable threads and add them to the isolate.
         */
        runnableThreads.prune(isolate);

        /*
         * Prune the timer threads and add them to the isolate.
         */
        timerQueue.prune(isolate);

        /*
         * Iterate through the events
         */
        events.prune(isolate);
    }

    /**
     * Visit method for EventHashtable visitor.
     *
     * @param key the key
     * @param value the value
     */
/*
    public void visitIntHashtable(int key, Object value, Object context) {
        Isolate isolate = (Isolate)context;
        Thread t = (Thread)value;
        if (t.isolate == isolate) {
            Thread t2 = findEvent(key);
            Assert.that(t == t2);
            Assert.that(t.nextThread == null);
            t.setInQueue(Thread.HIBERNATEDRUN);
            isolate.addToHibernatedRunThread(t);
        }
    }
*/
    /**
     * Hibernate all the threads in the isolate and start the threads waiting for this event.
     *
     * @param isolate the isolate
     */
    static void hibernateIsolate(Isolate isolate) {

        /*
         * Do things to other threads in separate function so that there are
         * no dangling references to other threads in this activation record.
         */
        hibernateIsolate0(isolate);

/*if[CHUNKY_STACKS]*/
        /*
         * Prune the orphan chunks and those owned by the isolate.
         */
        boolean trace = GC.isTracing(GC.TRACE_OBJECT_GRAPH_COPYING);
        GC.pruneStackChunks(trace, GC.PRUNE_ORPHAN, null);
        isolate.setHibernatedStackChunks(GC.pruneStackChunks(trace, GC.PRUNE_OWNED_BY_ISOLATE, isolate));
/*end[CHUNKY_STACKS]*/

        /*
         * Add the current thread if it is in this isolate.
         */
        if (currentThread.isolate == isolate) {
            Assert.that(currentThread.nextThread == null);
            currentThread.setInQueue(Thread.HIBERNATEDRUN);
            isolate.addToHibernatedRunThread(currentThread);
            reschedule();
        }
    }

/*if[CHUNKY_STACKS]*/

    /**
     * Unhibernate the isolate.
     *
     * @param isolate the isolate
     */
    final static void unhibernateIsolate(Isolate isolate) {

//int rcount = 0;
//int tcount = 0;
        /*
         * Add back the timer threads.
         */
        Thread threads = isolate.getHibernatedTimerThreads();
        while (threads != null) {
            Thread thread = threads;
//System.out.println("unhibernating timer thread "+thread);
//tcount++;
            threads = thread.nextTimerThread;
            thread.nextTimerThread = null;
            long time = thread.time;
            if (time == 0) {
                time = 1;
            }
            timerQueue.add(thread, time);
        }

        /*
         * Add back the runnable threads.
         */
        threads = isolate.getHibernatedRunThreads();
        while (threads != null) {
            Thread thread = threads;
            threads = thread.nextThread;
            thread.nextThread = null;
            thread.setNotInQueue(Thread.HIBERNATEDRUN);
//System.out.println("unhibernating run thread "+thread);
//rcount++;
            addToRunnableThreadsQueue(thread);
        }

//System.out.println("unhibernated "+rcount+" rthreads");
//System.out.println("unhibernated "+tcount+" tthreads");

        /*
         * Add back the hibernated stack chunks.
         */
        GC.appendStackChunks(isolate.takeHibernatedStackChunks());
    }

/*end[CHUNKY_STACKS]*/

    /**
     * Returns a string representation of this thread, including a unique number
     * that identifies the thread and the thread's priority.
     *
     * @return  a string representation of this thread.
     */
    public String toString() {
        /*
         * Avoid using StringBuffer so that can be called before synchronication is working.
         */
        String res = Klass.getInternalName(GC.getKlass(this)).concat("[");
        res = res.concat(String.valueOf(threadNumber));
        res = res.concat(" (pri=");
        res = res.concat(String.valueOf(getPriority()));
        res = res.concat(")]");
        return res;
    }


    /*-----------------------------------------------------------------------*\
     *                              Thread state                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Thread state values.
     */
    private final static byte NEW = 0, ALIVE = 1, DEAD = 2;

    /**
     * Queue names.
     */
    final static byte MONITOR = 1, CONDVAR = 2, RUN = 3, EVENT = 4, JOIN = 5, ISOLATEJOIN = 6, HIBERNATEDRUN = 7;

    /**
     * The Isolate under which the thread is running
     */
    private Isolate isolate;

    /**
     * The execution stack for the thread.
     */
    private Object stack;

    /**
     * The target to run (if run() is not overridden).
     */
    private Runnable target;

    /**
     * The state of the thread.
     */
    private byte state;

    /**
     * The size of the stack that will be created for this thread.
     */
    private final int stackSize;

    /**
     * The execution priority.
     */
    byte priority;

    /**
     * The queue the thread is in.
     */
    byte inqueue;

    /**
     * Reference used for enqueueing in the ready, monitor wait, condvar wait, or join queues.
     */
    Thread nextThread;

    /**
     * Flag to show if thread is a daemon.
     */
    boolean isDaemon;

    /**
     * Reference used for enqueueing in the timer queue.
     */
    Thread nextTimerThread;

    /**
     * Threads waiting for this thread to die.
     */
    Thread joiners;

    /**
     * Time to emerge from the timer queue.
     */
    long time;

/*if[SMARTMONITORS]*/
    /**
     * Saved monitor nesting depth.
     */
    short monitorDepth;

    /**
     * Maximum nesting depth.
     */
    private final static short MAXDEPTH = Short.MAX_VALUE;
/*else[SMARTMONITORS]*/
//  int monitorDepth;
//  private final static int   MAXDEPTH = Integer.MAX_VALUE;
/*end[SMARTMONITORS]*/

    /**
     * The monitor when the thread is in the condvar queue.
     */
    Monitor monitor;

    /**
     * The 'name' of the thread
     */
    private final int threadNumber;

    /*-----------------------------------------------------------------------*\
     *                           The implementation                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Initialize the threading system.
     */
    static void initializeThreading() {
        nextThreadNumber    = 0;
        runnableThreads     = new ThreadQueue();
        timerQueue          = new TimerQueue();
        events              = new EventHashtable();
        currentThread       = new Thread(); // Startup using a dummy thread
        serviceThread       = currentThread;

        /*
         * Convert the block of memory allocated for the service thread's stack into a
         * proper object of type Klass.LOCAL_ARRAY.
         */
        int length = Unsafe.getUWord(serviceStack, HDR.length).toInt();
        Assert.always(length > 0);
        GC.setHeaderClass(serviceStack, Klass.LOCAL_ARRAY);
        GC.setHeaderLength(serviceStack, length);
        serviceThread.stack = serviceStack.toObject();
    }

    /**
     * Get the isolate of the thread.
     *
     * @return the isolate
     */
    public Isolate getIsolate() {
        return isolate;
    }

    /**
     * Special thread starter that does reschedule the currently executing thread.
     */
    final void primitiveThreadStart() {
        start();
        rescheduleNext();
    }

    /**
     * Start any pending finalizers.
     */
    static void startFinalizers() {
        while (true) {
            Finalizer finalizer = currentThread.isolate.removeFinalizer();
            if (finalizer == null) {
                 break;
            }
            try {
                new Thread(finalizer).start();
            } catch(OutOfMemoryError ex) {
                currentThread.isolate.addFinalizer(finalizer); // Try again sometime later.
                return;
            }
        }
    }

    /**
     * Prepare a thread for execution.
     */
    private void baptiseThread() {
        Assert.that(currentThread != null);
        Assert.always(state == NEW);
        stack = newStack(stackSize, true);
        if (stack == null) {
            throw VM.getOutOfMemoryError();
        }

// VM.print("Thread::baptiseThread - stack size = "); VM.println(stackSize);
        state = ALIVE;

//VM.print("Thread::baptiseThread - owner of stack chunk "); VM.printAddress(stack); VM.print(" = "); VM.printAddress(Unsafe.getObject(stack, SC.owner)); VM.println();

        isolate.addThread(this);
        addToRunnableThreadsQueue(this);

        // Set the connection between the stack chunk and this thread which will
        // indicate to the garbage collector that the stack chunk is alive
        Unsafe.setObject(stack, SC.owner, this);
    }

    /**
     * End thread execution.
     */
    private void killThread(boolean nicely) {
        Assert.always(state == ALIVE);
        Thread list = joiners;
        joiners = null;
        startJoiners(list, Thread.JOIN);
        state = DEAD;

        // Remove the connection between the stack chunk and this thread which will
        // indicate to the garbage collector that the stack chunk is dead
        Unsafe.setObject(stack, SC.owner, null);

//VM.print("Thread::killThread - owner of stack chunk "); VM.printAddress(stack); VM.print(" = "); VM.printAddress(Unsafe.getObject(stack, SC.owner)); VM.println();
        if (nicely) {
            isolate.removeThread(this);
        }
        reschedule();
    }

    /**
     * Zero the pointer to the stack chunk.
     */
    void zeroStack() {
        stack = null;
    }

    /**
     * Start threads waiting for a join.
     *
     * @param list the list of waiting threads
     * @param queueName the queue name (JOIN, or ISOLATEJOIN)
     */
    private static void startJoiners(Thread list, byte queueName) {
        while (list != null) {
            Thread next = list.nextThread;
            list.nextThread = null;
            list.setNotInQueue(queueName);
            if (list.isolate.isAlive()) {
                addToRunnableThreadsQueue(list);   // Do we need this?
            }
            list = next;
        }
    }

    /**
     * Get the thread's stack
     *
     * @return the stack
     */
    final Object getStack() {
        return stack;
    }

    /**
     * Allocate a new stack.
     *
     * @param size the size of the stack in words
     * @param userThread true of call is made on a user mode thread
     * @return the stack or null if none could be allocated
     */
    private static Object newStack(int size, boolean userThread) {
        Object stack = GC.getExcessiveGC() ? null : GC.newStack(size);
        if (stack == null) {
            if (userThread) {
                VM.collectGarbage();
            } else {
                GC.collectGarbage();
            }
            stack = GC.newStack(size);
        }
        return stack;
    }

    /**
     * Extend the stack of the currently executing thread.
     * <p>
     * This code is called from the core VM using the GC stack and it is very important
     * that there are no non-null pointers in the activation record of this method when
     * the garbage might need collecting.
     *
     * @return false if the allocation failed
     */
    static boolean extendStack() {
/*if[CHUNKY_STACKS]*/
        /*
         * Stack extension will not work if java.lang.Klass has not been initialized as
         * the Klass.LOCAL static variable will be null.
         */
        Assert.always(VM.isCurrentIsolateInitialized(), "cannot extend stack until java.lang.Class is initialized");

        /*
         * Allocate a new stack and copy the contents of the old stack.
         */
        int extra = GC.getArrayLength(otherThread.stack);
        Object newStack = newStack(extra * 2, false);
        if (newStack == null) {
            return false;
        } else {
            Object oldStack = otherThread.stack;
            Address fp      = Address.fromObject(Unsafe.getObject(oldStack, SC.lastFP));
            int failedfp    = fp.diff(Address.fromObject(oldStack)).toInt() / com.sun.squawk.vm.HDR.BYTES_PER_WORD;
            GC.stackCopy(oldStack, newStack, failedfp);
            otherThread.stack = newStack;
            Address addr = Address.fromObject(newStack);
            addr = addr.add((failedfp+extra) * com.sun.squawk.vm.HDR.BYTES_PER_WORD);
            Unsafe.setAddress(newStack, SC.lastFP, addr);
            return true;
        }
/*else[CHUNKY_STACKS]*/
//      VM.fatalVMError();
//      return false;
/*end[CHUNKY_STACKS]*/
    }

    /**
     * Call the run() method of a thread. This is called by the VM when a new thread is started.
     * The call sequence is that Thread.start() calls Thread.reschedule() which calls VM.switchToThread()
     * which calls VM.do_callRun() which calls this function.
     */
    final void callRun() {
        try {
            try {
                run();
            } catch (OutOfMemoryError ex) {
                VM.println("Uncaught out of memory error");
                isolate.abort(999);
            } catch (Throwable ex) {
                System.err.println("Uncaught exception after " + VM.branchCount() + " branches");
                System.err.println(ex);
                ex.printStackTrace(System.err);
            }
            killThread(true);
        } catch (Throwable ex) {
        }
        isolate.abort(999); // Almost certainly another out of memory error
        killThread(false);
        VM.fatalVMError();
    }

    /**
     * Primitive method to choose the next executible thread.
     */
    private static void rescheduleNext() {
        Assert.that(GC.isSafeToSwitchThreads());
        Thread thread = null;

        /*
         * Loop until there is something to do.
         */
        while (true) {

            /*
             * Add any threads that are ready to be restarted.
             */
            while (true) {
                int event = VM.getEvent();
                if (event == 0) {
                    break;
                }
                signalEvent(event);
            }

            /*
             * Add any threads waiting for a certain time that are now due.
             */
            while ((thread = timerQueue.next()) != null) {
                Assert.that(thread.isAlive());
                Monitor monitor = thread.monitor;
                /*
                 * If the thread is wait()ing on a monitor then remove it
                 * from the conditional variable wait queue.
                 */
                if (monitor != null) {
                    monitor.removeCondvarWait(thread);
                    /*
                     * Reclaim the lock on the monitor. If this is available
                     * then the thread will be added to the run queue.
                     */
                    addMonitorWait(monitor, thread);
                } else {
                    /*
                     * Otherwise it is just waking up from a sleep() so it is now
                     * ready to run.
                     */
                    addToRunnableThreadsQueue(thread);
                }
            }

            /*
             * Break if there is something to do.
             */
            if ((thread = runnableThreads.next()) != null) {
                break;
            }

            /*
             * Wait for an event or until timeout.
             */
            long delta = timerQueue.nextDelta();
            if (delta > 0) {
                if (delta == Long.MAX_VALUE && events.size() == 0) {
                    /*
                     * This situation will usually only come about if the bootstrap
                     * isolate called System.exit() instead of VM.stopVM()
                     */
                    Assert.shouldNotReachHere("infinite wait on event queue");
                }
                VM.waitForEvent(delta);
            }
        }

        /*
         * Set the next thread.
         */
        Assert.that(thread != null);
        otherThread = thread;
    }

    /**
     * Context switch to another thread.
     */
    private static void reschedule() {
        fixupPendingMonitors();  // Convert any pending monitors to real ones
        rescheduleNext();        // Select the next thread
        VM.threadSwitch();       // and switch
    }

    /**
     * Get the 'other' thread.
     *
     * @return the other thread
     */
    static Thread getOtherThread() {
        return otherThread;
    }

    /**
     * Get the 'other' thread.
     *
     * @return the other thread
     */
    static Object getOtherThreadStack() {
        return otherThread.stack;
    }

    /**
     * Block a thread waiting for an event.
     *
     * Note: The bulk of the work is done in this function so that there are
     * no dangling references to other threads or globals in the activation record
     * that calls reschedule().
     *
     * @param event the event number to wait for
     */
    private static void waitForEvent0(int event) {
        startFinalizers();
        Thread t = currentThread;
        t.setInQueue(Thread.EVENT);
        events.put(event, t);
        Assert.that(t.nextThread == null);
    }

    /**
     * Block a thread waiting for an event.
     *
     * @param event the event number to wait for
     */
    static void waitForEvent(int event) {
        waitForEvent0(event);
        reschedule();
        Assert.that(!currentThread.inQueue(Thread.EVENT) || currentThread.nextThread == null);
    }

    /**
     * Restart a thread blocked on an event.
     *
     * @param event the event number to unblock
     */
    private static void signalEvent(int event) {
        Thread thread = findEvent(event);
        if (thread != null) {
            addToRunnableThreadsQueue(thread);
        }
    }

    /**
     * Find a thread blocked on an event.
     *
     * @param event the event number to unblock
     */
    static Thread findEvent(int event) {
        Thread thread = (Thread)events.remove(event);
        if (thread != null) {
            thread.setNotInQueue(Thread.EVENT);
            Assert.that(thread.nextThread == null);
        }
        return thread;
    }

    /**
     * throwBadMonitorStateException
     */
    private static void throwBadMonitorStateException() {
        if (FATAL_MONITOR_ERRORS) {
            VM.fatalVMError();
        }
        throw new IllegalMonitorStateException();
    }

    /**
     * Queue a thread onto a monitor until the monitor is not locked.
     *
     * @param monitor the monitor to queue onto.
     * @param thread the thread to queue.
     */
    private static void addMonitorWait(Monitor monitor, Thread thread) {
        Assert.that(thread.monitorDepth > 0);

        /*
         * Add to the wait queue.
         */
        monitor.addMonitorWait(thread);

        /*
         * If the wait queue has no owner then try and start a waiting thread.
         */
        if (monitor.owner == null) {
            removeMonitorWait(monitor);
        }
    }

    /**
     * Remove a thread from a monitor's wait queue and schedule it for execution.
     *
     * @param monitor the monitor
     */
    private static void removeMonitorWait(Monitor monitor) {

       /*
        * Try and remove a thread from the wait queue.
        */
        Thread waiter = monitor.removeMonitorWait();
        if (waiter != null /*&& waiter.isAlive()*/) {       // Is this right?
            Assert.that(waiter.isAlive());

            /*
             * Set the monitor's ownership and nesting depth.
             */
            monitor.owner = waiter;
            monitor.depth = waiter.monitorDepth;
            Assert.that(waiter.monitorDepth > 0);

            /*
             * Restart execution of the thread.
             */
            addToRunnableThreadsQueue(waiter);

        } else {

            /*
             * No thread is waiting for this monitor, so mark it as unused.
             */
            monitor.owner = null;
            monitor.depth = 0;
        }
    }

    /**
     * Create real monitors for objects with pending monitors.
     */
    static void fixupPendingMonitors() {
        Assert.that(currentThread != null);
        Object object = VM.removeVirtualMonitorObject();
        while (object != null) {
            Monitor monitor = GC.getMonitor(object);
            if (monitor.owner == null) {
                Assert.that(monitor.depth == 0);
                monitor.depth = 1;
                monitor.owner = currentThread;
            } else {
                Assert.that(monitor.owner == currentThread);
                Assert.that(monitor.depth > 0);
                monitor.depth++;
                Assert.always(monitor.depth < MAXDEPTH);
            }
            object = VM.removeVirtualMonitorObject();
        }
    }

    /**
     * Get a monitor.
     *
     * @param object the object to be synchronized upon
     */
    static Monitor getMonitor(Object object) {
        fixupPendingMonitors();  // Convert any pending monitors to real ones
        return GC.getMonitor(object);
    }

    /**
     * Enter a monitor.
     *
     * @param object the object to be synchronized upon
     */
    static void monitorEnter(Object object) {
        Monitor monitor = getMonitor(object);
        if (monitor.owner == null) {

            /*
             * Unowned monitor, make the current thread the owner.
             */
            monitor.owner = currentThread;
            monitor.depth = 1;

        } else if (monitor.owner == currentThread) {

            /*
             * Thread already owns the monitor, increment depth.
             */
            monitor.depth++;
            Assert.always(monitor.depth < MAXDEPTH);

        } else {

            /*
             * Add to the wait queue and set the depth for when thread is restarted.
             */
            currentThread.monitorDepth = 1;
            addMonitorWait(monitor, currentThread);
            reschedule();

            /*
             * Safety.
             */
            Assert.that(currentThread.isolate.isExited() || monitor.owner == currentThread);
            currentThread.monitor = null;
            currentThread.monitorDepth = 0;
        }
    }

    /**
     * Exit a monitor.
     *
     * @param object the object to be unsynchronized
     */
    static void monitorExit(Object object) {
        Monitor monitor = getMonitor(object);

        /*
         * Throw an exception if things look bad
         */
        if (monitor.owner != currentThread) {
            throwBadMonitorStateException();
        }

        /*
         * Safety.
         */
        Assert.that(monitor.depth > 0);

        /*
         * Try and restart a thread if the nesting depth is zero
         */
        if (--monitor.depth == 0) {
            removeMonitorWait(monitor);
        }

        /*
         * Check that the monitor's depth is zero if it is not in use.
         */
        Assert.that(monitor.owner != null || monitor.condvarQueue != null || monitor.monitorQueue != null || monitor.depth == 0);
/*if[SMARTMONITORS]*/
        /*
         * Remove the monitor if is was not used for a wait() operation.
         */
        if (monitor.owner == null && monitor.condvarQueue == null && monitor.monitorQueue == null) {
            Assert.that(monitor.depth == 0);
            GC.removeMonitor(object, !monitor.hasHadWaiter);
        }
/*end[SMARTMONITORS]*/
    }

    /**
     * Wait for an object to be notified.
     *
     * @param object the object to wait on
     * @param delta the timeout period
     */
    static void monitorWait(Object object, long delta) throws InterruptedException {
        Monitor monitor = getMonitor(object);

        /*
         * Throw an exception if things look bad
         */
        if (monitor.owner != currentThread) {
            throwBadMonitorStateException();
        }

/*if[SMARTMONITORS]*/
        /**
         * Record that the monitor was waited upon.
         */
        monitor.hasHadWaiter = true;
/*end[SMARTMONITORS]*/

        /*
         * Add to timer queue if time is > 0
         */
        if (delta > 0) {
            timerQueue.add(currentThread, delta);
        }

        /*
         * Save the nesting depth so it can be restored when it regains the monitor.
         */
        currentThread.monitorDepth = monitor.depth;

        /*
         * Add to the wait queue
         */
        monitor.addCondvarWait(currentThread);

        /*
         * Having relinquishing the monitor get the next thread off the wait queue.
         */
        removeMonitorWait(monitor);

        /*
         * Wait for a notify or a timeout.
         */
        Assert.that(monitor.condvarQueue != null);
        Assert.that(currentThread.monitor == monitor);
        reschedule();

        /*
         * Safety...
         */
        Assert.that(monitor.owner == currentThread);
        currentThread.monitor = null;
        currentThread.monitorDepth = 0;
    }

    /**
     * Notify an object.
     *
     * @param object the object be notified
     * @param notifyAll flag to notify all waiting threads
     */
    static void monitorNotify(Object object, boolean notifyAll) {

/*if[SMARTMONITORS]*/
        /*
         * Test to see if there is a real monitor object. If there is not
         * then the monitor lock must be on the pending monitor queue and
         * there cannot be another thread to notify.
         */
        if (VM.hasVirtualMonitorObject(object)) {
            Assert.that(!GC.hasRealMonitor(object));
            return;
        }
/*end[SMARTMONITORS]*/

        /*
         * Signal any waiting threads.
         */
        Monitor monitor = getMonitor(object);
        boolean success = false;

        /*
         * Throw an exception if the object is not owned by the current thread.
         */
        if (monitor.owner != currentThread) {
            throwBadMonitorStateException();
        }

        /*
         * Try and restart a thread.
         */
        do {
            /*
             * Get the next waiting thread.
             */
            Thread waiter = monitor.removeCondvarWait();
            if (waiter == null) {
                break;
            }

            /*
             * Note that a thread was found.
             */
            success = true;

            /*
             * Remove timeout if there was one and restart
             */
            timerQueue.remove(waiter);
            addMonitorWait(monitor, waiter);

        /*
         * Loop if it this is a notifyAll operation.
         */
        } while (notifyAll);

        /*
         * If the notify released something then yield.
         */
        if (success) {
            addToRunnableThreadsQueue(null);
            reschedule();
        }
    }

    /**
     * Test the thread to see if it is in a queue.
     */
    final boolean inQueue(byte name) {
        return inqueue == name;
    }

    /**
     * Declare the thread to be in a queue.
     */
    final void setInQueue(byte name) {
        Assert.that(inqueue == 0);
        inqueue = name;
    }

    /**
     * Declare the thread to be not in a queue.
     */
    final void setNotInQueue(byte name) {
        Assert.that(inqueue == name);
        inqueue = 0;
    }

}



/*=======================================================================*\
 *                                Monitor                                *
\*=======================================================================*/

final class Monitor {

    /**
     * The thread that owns the monitor.
     */
    Thread owner;

    /**
     * Queue of threads waiting to claim the monitor.
     */
    Thread monitorQueue;
    /**
     * Queue of threads waiting to claim the object.
     */
    Thread condvarQueue;

/*if[SMARTMONITORS]*/
    /**
     * Nesting depth.
     */
    short depth;

    /**
     * Flag to show if a wait occured.
     */
    boolean hasHadWaiter;
/*else[SMARTMONITORS]*/
//  int depth;
/*end[SMARTMONITORS]*/

    /*
     * Constructor
     */
    Monitor() {
    }

    /**
     * Add a thread to the monitor wait queue.
     *
     * @param thread the thread to add
     */
    void addMonitorWait(Thread thread) {
        thread.setInQueue(Thread.MONITOR);
        Assert.that(thread.nextThread == null);
        Thread next = monitorQueue;
        if (next == null) {
            monitorQueue = thread;
        } else {
            while (next.nextThread != null) {
                next = next.nextThread;
            }
            next.nextThread = thread;
        }
    }

    /**
     * Remove a thread from the monitor wait queue.
     *
     * @return a thread or null if there is none
     */
    Thread removeMonitorWait() {
        Thread thread = monitorQueue;
        if (thread != null) {
            monitorQueue = thread.nextThread;
            thread.setNotInQueue(Thread.MONITOR);
            thread.nextThread = null;
        }
        return thread;
    }

    /**
     * Add a thread to the conditional variable wait queue.
     *
     * @param thread the thread to add
     */
    void addCondvarWait(Thread thread) {
        thread.setInQueue(Thread.CONDVAR);
        thread.monitor = this;
        Assert.that(thread.nextThread == null);
        Thread next = condvarQueue;
        if (next == null) {
            condvarQueue = thread;
        } else {
            while (next.nextThread != null) {
                next = next.nextThread;
            }
            next.nextThread = thread;
        }
    }

    /**
     * Remove the next thread from the conditional variable wait queue.
     *
     * @return a thread or null if there is none
     */
    Thread removeCondvarWait() {
        Thread thread = condvarQueue;
        if (thread != null) {
            condvarQueue = thread.nextThread;
            thread.setNotInQueue(Thread.CONDVAR);
            thread.monitor = null;
            thread.nextThread = null;
        }
        return thread;
    }

    /**
     * Remove a specific thread from the conditional variable wait queue.
     *
     * @param thread the thread to remove
     */
    void removeCondvarWait(Thread thread) {
        if (thread.inQueue(Thread.CONDVAR)) {
            Thread next = condvarQueue;
            Assert.that(next != null);
            if (next == thread) {
                condvarQueue = thread.nextThread;
            } else {
                while (next.nextThread != thread) {
                    next = next.nextThread;
                    Assert.that(next != null);
                }
                if (next.nextThread == thread) {
                    next.nextThread = thread.nextThread;
                }
            }
            thread.setNotInQueue(Thread.CONDVAR);
            thread.monitor = null;
            thread.nextThread = null;
        }
    }
}


/*=======================================================================*\
 *                              ThreadQueue                              *
\*=======================================================================*/

final class ThreadQueue {

    /**
     * The first thread in the queue.
     */
    Thread first;

    /**
     * The count of threads in the queue.
     */
    int count;

    /**
     * Add a thread to the queue.
     *
     * @param thread the thread to add
     */
    void add(Thread thread) {
        Assert.that(thread.isAlive());
        thread.setInQueue(Thread.RUN);
        count++;
        if (first == null) {
            first = thread;
        } else {
            if (first.priority < thread.priority) {
                thread.nextThread = first;
                first = thread;
            } else {
                Thread last = first;
                while (last.nextThread != null && last.nextThread.priority >= thread.priority) {
                    last = last.nextThread;
                }
                thread.nextThread = last.nextThread;
                last.nextThread = thread;
            }
        }
    }

    /**
     * Get the number of elements in the queue.
     *
     * @return the count
     */
    int size() {
        return count;
    }

    /**
     * Get the next thread in the queue.
     *
     * @return a thread or null if there is none
     */
    Thread next() {
        Thread thread = first;
        if (thread != null) {
            thread.setNotInQueue(Thread.RUN);
            first = thread.nextThread;
            thread.nextThread = null;
            count--;
        }
        return thread;
    }

    /**
     * Remove all the threads in this queue that are owned by <code>isolate</code>
     * and add them to the queue of hibernated runnable threads in the isolate.
     *
     * @param isolate  the isolate whose runnable threads are to be removed
     */
    void prune(Isolate isolate) {
        Thread oldQueue = first;
        count = 0;
        first = null;
        while(oldQueue != null) {
            Thread thread = oldQueue;
            oldQueue = oldQueue.nextThread;
            thread.nextThread = null;
            thread.setNotInQueue(Thread.RUN);
            if (thread.getIsolate() != isolate) {
                add(thread);
            } else {
                thread.setInQueue(Thread.HIBERNATEDRUN);
                isolate.addToHibernatedRunThread(thread);
            }
        }
    }
}


/*=======================================================================*\
 *                               TimerQueue                              *
\*=======================================================================*/

final class TimerQueue {

    /**
     * The first thread in the queue.
     */
    Thread first;

    /**
     * Add a thread to the queue.
     *
     * @param thread the thread to add
     * @param delta the time period
     */
    void add(Thread thread, long delta) {
        Assert.that(thread.nextTimerThread == null);
        thread.time = System.currentTimeMillis() + delta;
        if (thread.time < 0) {

           /*
            * If delta is so huge that the time went negative then just make
            * it a very large value. The universe will end before the error
            * can be detected.
            */
            thread.time = Long.MAX_VALUE;
        }
        if (first == null) {
            first = thread;
        } else {
            if (first.time > thread.time) {
                thread.nextTimerThread = first;
                first = thread;
            } else {
                Thread last = first;
                while (last.nextTimerThread != null && last.nextTimerThread.time < thread.time) {
                    last = last.nextTimerThread;
                }
                thread.nextTimerThread = last.nextTimerThread;
                last.nextTimerThread = thread;
            }
        }
    }

    /**
     * Get the next thread in the queue that has reached its time.
     *
     * @return a thread or null if there is none
     */
    Thread next() {
        Thread thread = first;
        if (thread == null || thread.time > System.currentTimeMillis()) {
            return null;
        }
        first = first.nextTimerThread;
        thread.nextTimerThread = null;
        Assert.that(thread.time != 0);
        thread.time = 0;
        return thread;
    }

    /**
     * Remove a specific thread from the queue.
     *
     * @param thread the thread
     */
    void remove(Thread thread) {
        if (first == null) {
            Assert.that(thread.time == 0);
            return;
        }
        if (thread.time == 0) {
            return;
        }
        thread.time = 0;
        if (thread == first) {
            first = thread.nextTimerThread;
            thread.nextTimerThread = null;
            return;
        }
        Thread p = first;
        while (p.nextTimerThread != null) {
            if (p.nextTimerThread == thread) {
                p.nextTimerThread = thread.nextTimerThread;
                thread.nextTimerThread = null;
                return;
            }
            p = p.nextTimerThread;
        }
        VM.fatalVMError();
    }

    /**
     * Get the time delta to the next event in the queue.
     *
     * @return the time
     */
    long nextDelta() {
        boolean isTckTest = VM.getCurrentIsolate().isTckTest();
        if (first != null) {
            long now = System.currentTimeMillis();
            if (now >= first.time) {
                return 0;
            }
            long res = first.time - now;
            if (isTckTest && res > (1000*60)) {
                VM.print("Long wait in TCK ");
                VM.print(res);
                VM.println();
                VM.stopVM(99);
            }
            return res;
        } else {
            return Long.MAX_VALUE;
        }
    }


    /**
     * Remove all the threads in this queue that are owned by <code>isolate</code>
     * and add them to the queue of hibernated timer-blocked threads in the isolate.
     *
     * @param isolate  the isolate whose timer-blocked threads are to be removed
     */
    void prune(Isolate isolate) {
        start:
        while (true) {
            Thread t = first;
            while (t != null) {
                if (t.getIsolate() == isolate) {
                    long time = t.time - System.currentTimeMillis();
                    remove(t);
                    t.time = time;
                    isolate.addToHibernatedTimerThread(t);
                    continue start;
                }
                t = t.nextTimerThread;
            }
            break;
        }
    }

}

/**
 * Extension of IntHashtable that enables the pruning the threads of hibernated isolates.
 */
class EventHashtable extends IntHashtable implements IntHashtableVisitor {

    /**
     * The isolate being pruned.
     */
    private transient Isolate isolate;

    /**
     * Prune the isolates out of the event hash table.
     *
     * @param isolate the isolate remove
     */
    void prune(Isolate isolate) {
        this.isolate = isolate;
        visit(this);
        this.isolate = null;
    }

    /**
     * Visit method for EventHashtable visitor.
     *
     * @param key the key
     * @param value the value
     */
    public void visitIntHashtable(int key, Object value) {
        Thread t = (Thread)value;
        if (t.getIsolate() == isolate) {
            Thread t2 = Thread.findEvent(key);
            Assert.that(t == t2);
            Assert.that(t.nextThread == null);
            t.setInQueue(Thread.HIBERNATEDRUN);
            isolate.addToHibernatedRunThread(t);
        }
    }
}
