package org.kairosdb.testing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


public class FakeScheduledExecutorService implements ScheduledExecutorService
{
    private boolean shutdown = false;

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
    {
        return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
    {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
    {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
    {
        return null;
    }

    @Override
    public void shutdown()
    {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow()
    {
        shutdown = true;
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown()
    {
        return shutdown;
    }

    @Override
    public boolean isTerminated()
    {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
    {
        return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task)
    {
        return null;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result)
    {
        return null;
    }

    @Override
    public Future<?> submit(Runnable task)
    {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
    {
        return Collections.emptyList();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    {
        return Collections.emptyList();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
    {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    {
        return null;
    }

    @Override
    public void execute(Runnable command)
    {
    }
}
