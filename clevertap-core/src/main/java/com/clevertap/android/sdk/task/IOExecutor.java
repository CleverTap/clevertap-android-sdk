package com.clevertap.android.sdk.task;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class IOExecutor implements ExecutorService {

    private final int numCores = Runtime.getRuntime().availableProcessors();

    public void setExecutorService(final ExecutorService executorService) {
        mExecutorService = executorService;
    }

    ExecutorService mExecutorService = new ThreadPoolExecutor(numCores * 2, numCores * 2,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return mExecutorService.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        mExecutorService.execute(command);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return mExecutorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
            final TimeUnit unit)
            throws InterruptedException {
        return mExecutorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws ExecutionException, InterruptedException {
        return mExecutorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        return mExecutorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return mExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return mExecutorService.isTerminated();
    }

    @Override
    public void shutdown() {
        mExecutorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return mExecutorService.shutdownNow();
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return mExecutorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return mExecutorService.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return mExecutorService.submit(task);
    }
}
