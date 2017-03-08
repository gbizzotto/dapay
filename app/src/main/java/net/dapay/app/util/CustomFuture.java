package net.dapay.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by gabriel on 12/30/16.
 * Replacement for CompletableFuture, absent until API 24
 * Augmented with isOnItsWay, when completion do not depend on uncertain variables anymore.
 * For example, when a web service has returned its payload and some time consuming local processing is still needed.
 */

public class CustomFuture<V extends Object> implements Future {
    private boolean mCancelled = false;
    private boolean mOnItsWay = false;
    private boolean mDone = false;
    private V mResult;
    private V mTemporaryResult;

    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (mDone)
            return false;
        mCancelled = true;
        return true;
    }
    private synchronized boolean AddThread(Thread t) {
        if (mDone || mCancelled)
            return false;
        return true;
    }
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
    {
        long until = System.currentTimeMillis() + unit.toMillis(timeout);
        while (!isDone() && !isCancelled()) {
            if (timeout != -1 && System.currentTimeMillis() >= until)
                throw new TimeoutException();
            Thread.sleep(100);
        }
        if (isCancelled())
            throw new CancellationException();
        return mResult;
    }
    public V get() throws InterruptedException {
        try {
            return get(-1, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return mResult; // Will never happen
        }
    }

    public synchronized boolean isCancelled() {
        return mCancelled;
    }
    public synchronized boolean isDone() {
        return mDone;
    }
    public synchronized boolean isOnItsWay() {
        return mOnItsWay;
    }

    public synchronized boolean setIsOnItsWay() {
        if (mCancelled || mDone)
            return false;
        mOnItsWay = true;
        return true;
    }
    public synchronized boolean setDone(V result) {
        if (mCancelled || mDone)
            return false;
        mDone = true;
        mResult = result;
        return true;
    }

    public synchronized boolean setTemporaryValue(V temporary_result) {
        if (mCancelled || mDone)
            return false;
        mTemporaryResult = temporary_result;
        return true;
    }
    public synchronized V getTemporaryValue() {
        return mTemporaryResult;
    }
}
