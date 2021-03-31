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

/**
 * Executor service pool containing more threads to offload the IO tasks.
 */
class IOExecutor implements ExecutorService {

    private final int numCores = Runtime.getRuntime().availableProcessors();

    public void setExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    ExecutorService executorService = new ThreadPoolExecutor(numCores * 2, numCores * 2,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        executorService.execute(command);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
            final TimeUnit unit)
            throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws ExecutionException, InterruptedException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return executorService.submit(task);
    }
}
