package org.reactome.server.tools

import com.martiansoftware.jsap.JSAPResult
import org.apache.commons.collections4.ListUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ExtensionChecker(private val config: JSAPResult) : Checker {
    private val extensions = listOf(".svg", ".emf", ".png", ".xml")
    private val idsToExtensions: MutableMap<String, MutableList<String>> = HashMap()
    private var failedChecks: Int = 0

    override fun getFailedChecks(): Int {
        return failedChecks
    }

    override fun getTotalChecks(): Int {
        return idsToExtensions.size
    }

    override fun process() {
        val directory = config.getString("directory")

        val filesInDir = File(directory)

        val files = filesInDir.listFiles()
        if (files != null && files.size != 0) {
            for (file in files) {
                val fullName = file.name
                val id = fullName.substring(0, fullName.length - 4)
                if (!id.startsWith("R-ICO-")) continue
                val ext = fullName.substring(fullName.length - 4)
                idsToExtensions.compute(id) { key: String?, value: MutableList<String>? ->
                    var value = value
                    if (value == null) value = ArrayList()
                    value.add(ext)
                    value
                }
            }
        }

        for (id in idsToExtensions.keys) {
            val es: List<String> = idsToExtensions[id]!!
            if (!es.containsAll(extensions)) {
                ++failedChecks
                errorLogger.error(
                    "$id does not have ${extensions - es.toSet()} file(s)"
                )
            }
        }
    }

    companion object {
        private val errorLogger: Logger = LoggerFactory.getLogger("errorLogger")
    }
}
