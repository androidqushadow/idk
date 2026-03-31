package org.Krisp.re

import frontend.Parser
import runtime.createGlobalEnv
import runtime.evaluate
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    // 1. Grab the path from the command line arguments
    val filename = args.firstOrNull()

    // 2. If no path was provided, show usage instructions and exit
    if (filename == null) {
        println("Krisp Interpreter")
        println("Usage: krisp.exe <path_to_file>")
        println("Example: ./krisp.exe ")
        return
    }

    // 3. Try to open the specific file provided by the user
    val file = fopen(filename, "r")
    if (file == null) {
        // This triggers if the path is wrong or file doesn't exist
        println("Error: Could not open file at '$filename'")
        return
    }

    try {
        // 4. Read the file content
        val content = StringBuilder()
        memScoped {
            val bufferSize = 4096 // Increased buffer for larger files
            val buffer = allocArray<ByteVar>(bufferSize)
            while (fgets(buffer, bufferSize, file) != null) {
                content.append(buffer.toKString())
            }
        }

        val input = content.toString()

        // 5. Run the Interpreter Pipeline
        val parser = Parser()
        val env = createGlobalEnv()
        val program = parser.produceAST(input)

        evaluate(program, env)

    } catch (e: Exception) {
        println("\n[Runtime Error]: ${e.message}")
    } finally {
        // 6. Always close the file handle
        fclose(file)
    }
}