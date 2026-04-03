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
        println("Example: ./krisp.exe test.kp")
        return
    }

    // 3. Validate file extension (case-insensitive)
    val allowedExtensions = listOf(".kp", ".krisp")
    val lowerName = filename.lowercase()

    if (allowedExtensions.none { lowerName.endsWith(it) }) {
        println("Error: Supported extensions are ${allowedExtensions.joinToString()}")
        return
    }

    // 4. Try to open the specific file provided by the user
    val file = fopen(filename, "r")
    if (file == null) {
        println("Error: Could not open file at '$filename'")
        return
    }

    try {
        // 5. Read the file content
        val content = StringBuilder()
        memScoped {
            val bufferSize = 4096
            val buffer = allocArray<ByteVar>(bufferSize)

            while (fgets(buffer, bufferSize, file) != null) {
                content.append(buffer.toKString())
            }
        }

        val input = content.toString()

        // 6. Run the Interpreter Pipeline
        val parser = Parser()
        val env = createGlobalEnv()
        val program = parser.produceAST(input)

        evaluate(program, env)

    } catch (e: Exception) {
        println("\n[Runtime Error]: ${e.message}")
    } finally {
        // 7. Always close the file handle
        fclose(file)
    }
}