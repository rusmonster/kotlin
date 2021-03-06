package org.jetbrains.kotlin.doc

import java.io.PrintStream
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
* Main for running the KDocCompiler
*/
fun main(args: Array<String>): Unit {
    CLICompiler.doMain(KDocCompiler(), args);
}

/**
 * A version of the [[K2JVMCompiler]] which includes the [[KDoc]] compiler plugin and allows
 * command line validation or for the configuration to be provided via [[KDocArguments]]
 */
class KDocCompiler() : K2JVMCompiler() {

    protected override fun configureEnvironment(configuration : CompilerConfiguration, arguments : K2JVMCompilerArguments) {
        super.configureEnvironment(configuration, arguments)
        configuration.add(CLIConfigurationKeys.COMPILER_PLUGINS, KDoc(arguments as KDocArguments))

        // Suppress all messages from the compiler, because KDoc is not a compiler
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    }

    protected override fun createArguments() : K2JVMCompilerArguments {
        return KDocArguments()
    }

    protected override fun usage(target: PrintStream, extraHelp: Boolean) {
        target.println("Usage: KDocCompiler -d [<directory>|<jar>] [-stdlib <path to runtime.jar>] [<path>|-module <module file>] [-include-runtime]");
    }
}

class KDocArguments() : K2JVMCompilerArguments() {

    public var docConfig: KDocConfig = KDocConfig()

    /**
     * Applies the command line arguments to the given KDoc configuration
     */
    fun apply(): KDocConfig {
        // TODO...
        return docConfig
    }
}
