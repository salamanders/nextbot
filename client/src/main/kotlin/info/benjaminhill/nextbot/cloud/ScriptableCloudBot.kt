package info.benjaminhill.nextbot.cloud

import com.google.gson.GsonBuilder
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import kotlinx.coroutines.*
import mu.KLoggable
import javax.script.Invocable
import javax.script.ScriptException
import kotlin.coroutines.CoroutineContext


/**
 * Inputs: bot:Map<String, Any>
 * Outputs: set values in result:Map<String, (Double, String, Boolean)>
 * May also use `_history = { count: 0 }` to hold state across calls (eg. previous call time)
 * Example: `script = "result.motor0 = bot.motor0 * 0.9"`
 */
open class ScriptableCloudBot(uId: String) : RunnableCloudSyncDoc(uId), CoroutineScope {
    @Transient
    private var job = Job()

    // CoroutineScope also sneaks in an "isActive" that is annoying.
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    @Transient
    private var invocableEngine: Invocable? = null

    @Transient
    private var scriptJob: Job? = null

    var script by RunnableCloudSyncDelegate("") // Blank disables the overhead of scripting.

    var scriptError by RunnableCloudSyncDelegate("")

    private fun stop() {
        invocableEngine = null
        scriptJob?.isActive
        scriptJob?.cancel()
        scriptJob = null
    }

    override fun close() {
        super.close()
        stop()
    }

    private fun setCode() {

        if (script.isBlank()) {
            stop()
            return
        }

        val factory = NashornScriptEngineFactory()
        logger.info { "${factory.engineName} ${factory.engineVersion} ${factory.languageVersion}" }
        val engine = factory.getScriptEngine(arrayOf(
                "--language=es6",
                "-strict", // too much?
                "--no-java",
                //"--no-syntax-extensions",
                "--optimistic-types"), null) { className ->
            when {
                className == "java.lang.Thread" -> false
                className == "java.lang.Runnable" -> false
                className == "java.util.Timer" -> false
                className.startsWith("java.util.concurrency") -> false
                className.startsWith("javafx") -> false
                className.startsWith("javax.swing") -> false
                className.startsWith("java.awt") -> false
                else -> true
            }
        }
        val invocable = engine as Invocable

        val jsSource = """/*jshint esversion: 6 */
if (!Math.sign) { Math.sign = function (x) { return ((x > 0) - (x < 0)) || +x; }; } // polyfill
const _history = {
  '_count':0,
  '_previousTs': (new Date()).getTime()
};
function botLoop(jsonStr) {
  "use strict";
  _history._count++;
  _history._durationMs = (new Date()).getTime() - _history._previousTs;
  const bot = JSON.parse(jsonStr);
  const result = {};
  $script
  _history._previousTs = (new Date()).getTime();
  return result;
}"""
        logger.info { "Setting script to `$jsSource` and max 4x a second" }
        try {
            engine.eval(jsSource)
        } catch (e: ScriptException) {
            val message = e.message ?: "Unknown error compiling script"
            logger.warn { "Compiling script failed with '$message'" }
            scriptError = message
            stop()
            return
        }

        scriptJob = launch {
            // Keep running even if not online
            while (isActive) {
                try {
                    // Because GSON wasn't picking up delegates
                    val result = invocable.invokeFunction("botLoop", GSON.toJson(asSimpleMap()))
                    @Suppress("UNCHECKED_CAST")
                    (result as Map<String, Any>).entries.forEach { (key, value) ->
                        if (key == this@ScriptableCloudBot::script.name) {
                            logger.warn { "Tried to set script to a new value, no crazy self-modification please." }
                        } else {
                            logger.debug { "Script set $key=$value" }
                            this@ScriptableCloudBot[key] = value
                        }
                    }
                } catch (e: ScriptException) {
                    val message = e.message ?: "Unknown error executing script"
                    logger.warn(e) { message }
                    scriptError = message
                    // Maybe the error is temporary?  Keep trying.
                }

                yield()
                delay(250)
                yield()
            }
            logger.debug { "Exiting scriptJob loop normally." }
        }
    }

    companion object : KLoggable {
        override val logger = logger()

        // Serialize the inputs
        val GSON = GsonBuilder().create()!!
    }
}