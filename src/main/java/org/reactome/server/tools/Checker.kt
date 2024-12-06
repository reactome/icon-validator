package org.reactome.server.tools

import java.time.Duration
import java.time.Instant

interface Checker : Runnable {
    override fun run() {
        val start = Instant.now()
        process()
        println("${javaClass.simpleName} finished in ${Duration.between(start, Instant.now()).toString().substring(2)}")
    }

    fun process()

    fun getFailedChecks(): Int
    fun getTotalChecks(): Int
}
