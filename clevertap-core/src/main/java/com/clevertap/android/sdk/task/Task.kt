package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Definition of task is to execute some work & return success or failure callbacks
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class Task<TResult> internal constructor(
    @JvmField protected val config: CleverTapInstanceConfig?,
    @JvmField protected val executor: Executor,
    @JvmField protected val defaultCallbackExecutor: Executor,
    private val taskName: String?
) {

    protected enum class STATE { FAILED, SUCCESS, READY_TO_RUN, RUNNING }

    private val failureExecutables: MutableList<FailureExecutable<Exception>> = ArrayList()

    @JvmField
    protected var result: TResult? = null

    private val successExecutables: MutableList<SuccessExecutable<TResult>> = ArrayList()

    @JvmField
    protected var taskState: STATE = STATE.READY_TO_RUN

    /**
     * Register listener to get failure callbacks on the provided executor
     *
     * @param executor  - executor on which the failure callback will be called
     * @param listener  - failure listener
     * @return task
     */
    @Synchronized
    open fun addOnFailureListener(
        executor: Executor,
        listener: OnFailureListener<Exception>?
    ): Task<TResult> {
        if (listener != null) {
            failureExecutables.add(FailureExecutable(executor, listener))
        }
        return this
    }

    /**
     * Register listener to get failure callbacks on main thread
     *
     * @param listener  - failure listener
     * @return task
     */
    open fun addOnFailureListener(listener: OnFailureListener<Exception>): Task<TResult> {
        return addOnFailureListener(defaultCallbackExecutor, listener)
    }

    /**
     * Register listener to get success callbacks on the provided executor
     *
     * @param executor  - executor on which the success callback will be called
     * @param listener  - success listener
     * @return task
     */
    open fun addOnSuccessListener(
        executor: Executor,
        listener: OnSuccessListener<TResult>?
    ): Task<TResult> {
        if (listener != null) {
            successExecutables.add(SuccessExecutable(executor, listener))
        }
        return this
    }

    /**
     * Register listener to get success callbacks on main thread
     *
     * @param listener  - success listener
     * @return task
     */
    open fun addOnSuccessListener(listener: OnSuccessListener<TResult>): Task<TResult> {
        return addOnSuccessListener(defaultCallbackExecutor, listener)
    }

    /**
     * Simple method to execute the task
     *
     * @param logTag   - tag name to identify the task state in logs.
     * @param callable - piece of code to run
     */
    open fun execute(logTag: String?, callable: Callable<TResult>?) {
        executor.execute(newRunnableForTask(logTag, callable))
    }

    /**
     * Returns the state of task
     * Ref[STATE]
     */
    open val isSuccess: Boolean
        get() = taskState == STATE.SUCCESS

    /***
     * Removes the failure listener from the task.
     * @param listener - failure listener
     * @return task
     */
    @Suppress("unused")
    open fun removeOnFailureListener(listener: OnFailureListener<Exception>): Task<TResult> {
        val iterator = failureExecutables.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.failureListener === listener) {
                iterator.remove()
            }
        }
        return this
    }

    /***
     * Removes the Success listener from the task.
     * @param listener - success listener
     * @return task
     */
    @Suppress("unused")
    open fun removeOnSuccessListener(listener: OnSuccessListener<TResult>): Task<TResult> {
        val iterator = successExecutables.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.successListener === listener) {
                iterator.remove()
            }
        }
        return this
    }

    /**
     * Use this method in-case we need future task for the execution
     *
     * @param logTag   - tag name to identify the task state in logs.
     * @param callable - piece of code to run
     */
    open fun submit(logTag: String?, callable: Callable<TResult>?): Future<*> {
        if (executor !is ExecutorService) {
            throw UnsupportedOperationException(
                "Can't use this method without ExecutorService, Use Execute alternatively "
            )
        }
        return executor.submit(newRunnableForTask(logTag, callable))
    }

    /**
     * Submits piece of code to executor and returns result if code executes successfully within timeout or returns null
     *
     * @param logTag        tag name to identify logs.
     * @param callable      - piece of code to run
     * @param timeoutMillis - timeout for piece of code to run
     * @return result of callable or null
     */
    // TODO This method does not set state of the task correctly.
    open fun submitAndGetResult(logTag: String?, callable: Callable<TResult>?, timeoutMillis: Long): TResult? {
        if (executor !is ExecutorService) {
            throw UnsupportedOperationException(
                "Can't use this method without ExecutorService, Use Execute alternatively "
            )
        }
        var tResultFuture: Future<TResult>? = null
        try {
            tResultFuture = executor.submit(callable)
            return tResultFuture!!.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            logProperly("submitAndGetResult :: $logTag task timed out", e)
            if (tResultFuture != null && !tResultFuture.isCancelled) {
                tResultFuture.cancel(true)
            }
        } catch (e: Exception) {
            logProperly("submitAndGetResult :: $logTag task failed", e)
            if (tResultFuture != null && !tResultFuture.isCancelled) {
                tResultFuture.cancel(true)
            }
        }

        return null
    }

    internal fun onFailure(e: Exception) {
        setState(STATE.FAILED)
        for (failureExecutable in failureExecutables) {
            failureExecutable.execute(e)
        }
    }

    internal fun onSuccess(result: TResult) {
        setState(STATE.SUCCESS)
        setResult(result)
        for (successExecutable in successExecutables) {
            @Suppress("UNCHECKED_CAST")
            successExecutable.execute(this.result as TResult)
        }
    }

    private fun setResult(result: TResult) {
        this.result = result
    }

    private fun setState(taskState: STATE) {
        this.taskState = taskState
    }

    /**
     * Wraps the provided piece of code in runnable to execute on executor.
     *
     * @param logTag   - tag with which this task can be identified in the logs.
     * @param callable - piece of code to run.
     * @return Runnable
     */
    private fun newRunnableForTask(logTag: String?, callable: Callable<TResult>?): Runnable {
        return Runnable {
            try {
                setState(STATE.RUNNING)
                logProperly(
                    taskName + " Task: " + logTag + " starting on..." + Thread.currentThread().name,
                    null
                )
                val result = callable!!.call()
                logProperly(
                    taskName + " Task: " + logTag + " executed successfully on..." + Thread.currentThread().name,
                    null
                )
                onSuccess(result)
            } catch (e: Exception) {
                onFailure(e)
                logProperly(
                    taskName + " Task: " + logTag + " failed to execute on..." + Thread.currentThread().name,
                    e
                )
            }
        }
    }

    private fun logProperly(log: String, e: Exception?) {
        if (config != null) {
            config.logger.verbose(log, e)
        } else {
            Logger.v(log, e)
        }
    }
}
