package org.reactome.server.tools

import com.martiansoftware.jsap.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.reactome.server.tools.IconValidator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {
    Main.main(args)
}

@Suppress("unused")
object Main: CoroutineScope {
    private val errorLogger: Logger = LoggerFactory.getLogger("errorLogger")
    private val duplicateLogger: Logger = LoggerFactory.getLogger("duplicateLogger")
    private val executor = Executors.newCachedThreadPool() as ThreadPoolExecutor

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    fun main(args: Array<String>) {
        val jsap = SimpleJSAP(
            IconValidator::class.java.name,
            "Validates all the icon metadata before it is indexed during data release",
            arrayOf<Parameter>(
                FlaggedOption(
                    "directory",
                    JSAP.STRING_PARSER,
                    "../LIB",
                    JSAP.NOT_REQUIRED,
                    'd',
                    "directory",
                    "The place of icon XML to import"
                ).setList(true).setListSeparator(','),
                FlaggedOption(
                    "referencesfile",
                    JSAP.STRING_PARSER,
                    "references.txt",
                    JSAP.NOT_REQUIRED,
                    'r',
                    "referencesfile",
                    "A file containing references"
                ),
                FlaggedOption(
                    "categoriesfile",
                    JSAP.STRING_PARSER,
                    "categories.txt",
                    JSAP.NOT_REQUIRED,
                    'c',
                    "categoriesfile",
                    "A file containing categories"
                ),
                FlaggedOption(
                    "force",
                    JSAP.BOOLEAN_PARSER,
                    "false",
                    JSAP.NOT_REQUIRED,
                    'f',
                    "force",
                    "<< NOT RECOMMENDED >> Forces icon validator to pass and suppress errors."
                ),
                FlaggedOption(
                    "checkReferences",
                    JSAP.BOOLEAN_PARSER,
                    "true",
                    JSAP.NOT_REQUIRED,
                    'e',
                    "checkReferences",
                    "<< LONG PROCESS >> Check that all references are still active"
                ),
                FlaggedOption(
                    "iconNameCheck",
                    JSAP.BOOLEAN_PARSER,
                    "false",
                    JSAP.NOT_REQUIRED,
                    'i',
                    "iconNameCheck",
                    "<< LONG PROCESS >> Add warnings when the icon name cannot be found inside the reference link content"
                )
            )
        )

        val config = jsap.parse(args)

        if (jsap.messagePrinted()) exitProcess(1)

        val checkers: MutableList<Future<*>> = mutableListOf()
        checkers.add(executor.submit(DuplicateChecker(config)))
        checkers.add(executor.submit(ExtensionChecker(config)))
        val validator = IconValidator(config)
        checkers.add(executor.submit(validator))

        try {
            for (checker in checkers) {
                checker.get()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }

        if (validator.getFailedChecks() > 0 && !config.getBoolean("force")) {
            errorLogger.info("${validator.getFailedChecks()} errors are found in ${validator.getTotalChecks()} XML files.")
            exitProcess(1)
        }
        exitProcess(0)
    }
}
