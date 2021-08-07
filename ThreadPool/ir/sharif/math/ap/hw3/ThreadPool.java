package ir.sharif.math.ap.hw3;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

public class ThreadPool {
    private int threadNumbers;
    final ArrayList<Runnable> tasks;
    ArrayList<Worker> workers;
    HashMap<Runnable,Boolean> ended;
    HashMap<Runnable,Throwable> error;

    public ThreadPool(int threadNumbers) {
        error = new HashMap<>();
        ended = new HashMap<>();
        workers = new ArrayList<>();
        tasks = new ArrayList<>();
        this.threadNumbers = threadNumbers;
        addThread(threadNumbers);
    }

    public int getThreadNumbers() {
        return threadNumbers;
    }

    public void setThreadNumbers(int threadNumbers) {
       synchronized (tasks) {
           if (this.threadNumbers < threadNumbers) {
               addThread(threadNumbers - this.threadNumbers);
           } else {
               removeThread(this.threadNumbers - threadNumbers);
           }
           this.threadNumbers = threadNumbers;
       }
    }

    public void addThread(int x){
        synchronized (tasks) {
            for (int i = 0; i < x; i++) {
                Worker worker = new Worker();
                workers.add(worker);
                worker.start();
            }
        }
    }

    public void removeThread(int x){
        synchronized (tasks) {
            for (int i = 0; i < x; i++) {
                workers.get(0).delete();
                workers.remove(0);
            }
        }
    }

    public void invokeLater(Runnable runnable) {
        synchronized (tasks) {
            tasks.add(runnable);
            tasks.notifyAll();
        }
    }

    public void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
        synchronized (tasks) {
            tasks.add(runnable);
            tasks.notifyAll();
        }
        synchronized (tasks) {
            while (true) {
                tasks.wait();
                if (error.get(runnable) != null) {
                    InvocationTargetException Error = new InvocationTargetException(error.get(runnable));
                    throw Error;
                }
                if (ended.get(runnable) != null) {
                    if (ended.get(runnable)) break;
                }
            }
        }
    }


    public void invokeAndWaitUninterruptible(Runnable runnable) throws InvocationTargetException {
        synchronized (tasks) {
            tasks.add(runnable);
            tasks.notifyAll();
        }
        synchronized (tasks) {
            while (true) {
                try {
                    tasks.wait();
                    if (error.get(runnable) != null) {
                        InvocationTargetException Error = new InvocationTargetException(error.get(runnable));
                        throw Error;
                    }
                    if (ended.get(runnable) != null) {
                        if (ended.get(runnable)) break;
                    }
                } catch (InterruptedException e) {

                }
            }
        }
    }


    public class Worker extends Thread{
        boolean deleted = false;

        @Override
        public void run() {
            Runnable task;

            while (true){

                synchronized (tasks){
                    while (tasks.isEmpty()){
                        try {
                            tasks.wait();
                            if(deleted)break;
                        } catch (InterruptedException e) {
                        }
                    }
                    if(deleted)break;
                    task = tasks.get(tasks.size() - 1);
                    tasks.remove(tasks.size() - 1);
                }
                try {
                    task.run();
                }catch (Throwable t){
                    synchronized (tasks) {
                        error.put(task, t);
                        tasks.notifyAll();
                    }
                }
                synchronized (tasks) {
                    ended.put(task, true);
                    tasks.notifyAll();
                    if (deleted) break;
                }
            }
        }

        public void delete(){
            deleted = true;
            synchronized (tasks) {
                tasks.notifyAll();
            }
        }
    }
}