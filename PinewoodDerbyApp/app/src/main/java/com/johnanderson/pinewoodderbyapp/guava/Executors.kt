//From: https://github.com/vjames19/kotlin-futures
//MIT License
//
//Copyright (c) 2017 Victor J Reventos
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package com.johnanderson.pinewoodderbyapp.guava

import java.util.concurrent.*
import java.util.concurrent.Future

object DirectExecutor : ExecutorService {
    override fun execute(command: Runnable) {
        command.run()
    }

    override fun shutdown() {
    }

    override fun <T : Any?> submit(task: Callable<T>?): Future<T> {
        throw NotImplementedError()
    }

    override fun <T : Any?> submit(task: Runnable?, result: T): Future<T> {
        throw NotImplementedError()
    }

    override fun submit(task: Runnable?): Future<*> {
        throw NotImplementedError()
    }

    override fun shutdownNow(): MutableList<Runnable> {
        throw NotImplementedError()
    }

    override fun isShutdown(): Boolean {
        throw NotImplementedError()
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T {
        throw NotImplementedError()
    }

    override fun isTerminated(): Boolean {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): MutableList<Future<T>> {
        throw NotImplementedError()
    }
}

object ForkJoinExecutor : ExecutorService by ForkJoinPool.commonPool()
val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
val defaultExecutor: ExecutorService = DirectExecutor
