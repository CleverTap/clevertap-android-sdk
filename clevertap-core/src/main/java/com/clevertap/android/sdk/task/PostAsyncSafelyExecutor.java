package com.clevertap.android.sdk.task;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Post async safely executor is nothing but a single thread pool executor
 */
class PostAsyncSafelyExecutor implements ExecutorService {

    private long EXECUTOR_THREAD_ID = 0;

    void setExecutor(final ExecutorService executor) {
        this.executor = executor;
    }

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(final Runnable task) {
        if (task == null) {
            throw new NullPointerException("PostAsyncSafelyExecutor#execute: task can't ne null");
        }
        final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;
        if (executeSync) {
            task.run();
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                    task.run();
                }
            });
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAll: This method is not supported");
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
            final TimeUnit unit)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAll: This method is not supported");
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAny: This method is not supported");
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAny: This method is not supported");
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        if (task == null) {
            throw new NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null");
        }
        Future<T> future = null;
        final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;
        if (executeSync) {
            try {
                task.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            future = executor.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                    return task.call();
                }
            });
        }
        return future;
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        if (task == null) {
            throw new NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null");
        }
        RunnableFuture<T> futureTask = new FutureTask<>(task, result);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public Future<?> submit(final Runnable task) {
        if (task == null) {
            throw new NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null");
        }
        RunnableFuture<Void> futureTask = new FutureTask<>(task, null);
        execute(futureTask);
        return futureTask;
    }
}