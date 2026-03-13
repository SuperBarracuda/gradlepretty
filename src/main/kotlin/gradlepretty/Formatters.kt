package gradlepretty

interface OutputFormatter {
    fun format(event: BuildEvent): String?
    fun header(): String = ""
    fun footer(summary: BuildSummary): String = ""
}

class CompactFormatter(private val useColor: Boolean = true) : OutputFormatter {
    private val color = AnsiColors()
    
    override fun format(event: BuildEvent): String? {
        return when (event) {
            is BuildEvent.TaskStarted -> {
                if (useColor) "${color.cyan}> ${event.taskName}" else "> ${event.taskName}"
            }
            is BuildEvent.TaskCompleted -> {
                val symbol = if (event.skipped) "○" else "✓"
                val taskName = event.taskName
                if (useColor) {
                    if (event.skipped) {
                        "${color.dim}$symbol $taskName${color.reset}"
                    } else {
                        "${color.green}$symbol $taskName${color.reset}"
                    }
                } else {
                    "$symbol $taskName"
                }
            }
            is BuildEvent.TestPassed -> {
                if (useColor) "${color.green}  ✓ ${event.className}.${event.testName}${color.reset}" else "  ✓ ${event.className}.${event.testName}"
            }
            is BuildEvent.TestFailed -> {
                if (useColor) "${color.red}  ✗ ${event.className}.${event.testName}${color.reset}" else "  ✗ ${event.className}.${event.testName}"
            }
            is BuildEvent.TestSkipped -> {
                if (useColor) "${color.yellow}  ⊘ ${event.className}.${event.testName}${color.reset}" else "  ⊘ ${event.className}.${event.testName}"
            }
            is BuildEvent.CompilationError -> {
                if (useColor) "${color.red}error: ${event.file}:${event.line}: ${event.message}${color.reset}" else "error: ${event.file}:${event.line}: ${event.message}"
            }
            is BuildEvent.Warning -> {
                if (useColor) "${color.yellow}warning: ${event.message}${color.reset}" else "warning: ${event.message}"
            }
            is BuildEvent.BuildSuccessful -> {
                if (useColor) "${color.green}BUILD SUCCESSFUL${color.reset}" else "BUILD SUCCESSFUL"
            }
            is BuildEvent.BuildFailed -> {
                if (useColor) "${color.red}BUILD FAILED${color.reset}" else "BUILD FAILED"
            }
            else -> null
        }
    }
    
    override fun footer(summary: BuildSummary): String {
        val lines = mutableListOf<String>()
        lines.add("")
        lines.add("Build Summary:")
        lines.add("  Tasks: ${summary.tasks.size}")
        lines.add("  Tests: ${summary.passedTests} passed, ${summary.failedTests} failed, ${summary.skippedTests} skipped")
        lines.add("  Warnings: ${summary.warnings}")
        if (summary.duration > 0) {
            lines.add("  Duration: ${summary.duration / 1000}s")
        }
        return lines.joinToString("\n")
    }
}

class SimpleFormatter(private val useColor: Boolean = true) : OutputFormatter {
    private val color = AnsiColors()
    
    override fun format(event: BuildEvent): String? {
        return when (event) {
            is BuildEvent.BuildPhase -> {
                if (useColor) "${color.bold}${color.cyan}[${event.phase}]${color.reset}" else "[${event.phase}]"
            }
            is BuildEvent.TaskStarted -> {
                if (useColor) "  ${color.cyan}▶ ${event.project}:${event.taskName}${color.reset}" else "  ▶ ${event.project}:${event.taskName}"
            }
            is BuildEvent.TaskCompleted -> {
                val symbol = if (event.skipped) "○" else "✓"
                if (useColor) {
                    if (event.skipped) {
                        "  ${color.dim}$symbol ${event.project}:${event.taskName}${color.reset}"
                    } else {
                        "  ${color.green}$symbol ${event.project}:${event.taskName}${color.reset}"
                    }
                } else {
                    "  $symbol ${event.project}:${event.taskName}"
                }
            }
            is BuildEvent.TestPassed -> {
                if (useColor) "    ${color.green}✓ ${event.className}.${event.testName} (${event.duration}ms)${color.reset}" else "    ✓ ${event.className}.${event.testName} (${event.duration}ms)"
            }
            is BuildEvent.TestFailed -> {
                val msg = event.message.lines().firstOrNull() ?: "Test failed"
                if (useColor) "    ${color.red}✗ ${event.className}.${event.testName}${color.reset}\n      $msg" else "    ✗ ${event.className}.${event.testName}\n      $msg"
            }
            is BuildEvent.TestSkipped -> {
                if (useColor) "    ${color.yellow}⊘ ${event.className}.${event.testName}${color.reset}" else "    ⊘ ${event.className}.${event.testName}"
            }
            is BuildEvent.CompilationError -> {
                if (useColor) "${color.red}✗ ${event.file}:${event.line}: error: ${event.message}${color.reset}" else "✗ ${event.file}:${event.line}: error: ${event.message}"
            }
            is BuildEvent.Warning -> {
                val location = if (event.file != null) "${event.file}:${event.line}: " else ""
                if (useColor) "${color.yellow}⚠ $location${event.message}${color.reset}" else "⚠ $location${event.message}"
            }
            is BuildEvent.BuildSuccessful -> {
                if (useColor) "${color.green}═══════════════════════════════════════════════════\n  BUILD SUCCESSFUL\n═══════════════════════════════════════════════════${color.reset}" else "═══════════════════════════════════════════════════\n  BUILD SUCCESSFUL\n═══════════════════════════════════════════════════"
            }
            is BuildEvent.BuildFailed -> {
                if (useColor) "${color.red}═══════════════════════════════════════════════════\n  BUILD FAILED\n═══════════════════════════════════════════════════${color.reset}" else "═══════════════════════════════════════════════════\n  BUILD FAILED\n═══════════════════════════════════════════════════"
            }
            else -> null
        }
    }
    
    override fun footer(summary: BuildSummary): String {
        val lines = mutableListOf<String>()
        lines.add("")
        lines.add("═══════════════════════════════════════════════════")
        lines.add("Build Summary:")
        lines.add("  Tasks: ${summary.tasks.size} (${summary.tasks.count { it.skipped }} skipped)")
        lines.add("  Tests: ${summary.passedTests} passed, ${summary.failedTests} failed, ${summary.skippedTests} skipped")
        lines.add("  Warnings: ${summary.warnings}")
        lines.add("  Errors: ${summary.errors}")
        if (summary.duration > 0) {
            lines.add("  Total time: ${summary.duration / 1000}s")
        }
        return lines.joinToString("\n")
    }
}

class PrettyFormatter(private val useColor: Boolean = true) : OutputFormatter {
    private val color = AnsiColors()
    private var currentPhase: String = ""
    
    override fun format(event: BuildEvent): String? {
        return when (event) {
            is BuildEvent.BuildPhase -> {
                currentPhase = event.phase
                if (useColor) "${color.bold}${color.magenta}▸ ${event.phase}${color.reset}" else "▸ ${event.phase}"
            }
            is BuildEvent.TaskStarted -> {
                if (useColor) "  ${color.cyan}▶ ${event.project}:${color.bold}${event.taskName}${color.reset}" else "  ▶ ${event.project}:${event.taskName}"
            }
            is BuildEvent.TaskCompleted -> {
                val symbol = when {
                    event.skipped -> "○"
                    event.duration < 1000 -> "✓"
                    else -> "●"
                }
                val durationStr = if (event.duration > 0) " (${event.duration}ms)" else ""
                val taskName = event.project + if (event.project.isNotEmpty()) ":" else "" + event.taskName
                
                if (useColor) {
                    if (event.skipped) {
                        "  ${color.dim}$symbol $taskName${color.reset}$durationStr"
                    } else if (event.duration > 0) {
                        "  ${color.green}$symbol $taskName${color.reset}$durationStr"
                    } else {
                        "  ${color.red}$symbol $taskName${color.reset}"
                    }
                } else {
                    "  $symbol $taskName$durationStr"
                }
            }
            is BuildEvent.TestPassed -> {
                if (useColor) "    ${color.green}✓ ${event.className}.${color.bold}${event.testName}${color.reset} (${event.duration}ms)" else "    ✓ ${event.className}.${event.testName} (${event.duration}ms)"
            }
            is BuildEvent.TestFailed -> {
                val msg = event.message.lines().firstOrNull() ?: "Test failed"
                if (useColor) "    ${color.red}✗ ${event.className}.${color.bold}${event.testName}${color.reset}\n      ${color.red}$msg${color.reset}" else "    ✗ ${event.className}.${event.testName}\n      $msg"
            }
            is BuildEvent.TestSkipped -> {
                if (useColor) "    ${color.yellow}⊘ ${event.className}.${event.testName}${color.reset}" else "    ⊘ ${event.className}.${event.testName}"
            }
            is BuildEvent.CompilationError -> {
                if (useColor) "${color.red}✗ ${color.bold}${event.file}:${event.line}${color.reset}: ${color.red}error:${color.reset} ${event.message}" else "✗ ${event.file}:${event.line}: error: ${event.message}"
            }
            is BuildEvent.Warning -> {
                val location = if (event.file != null) "${color.yellow}${event.file}:${event.line}:${color.reset} " else ""
                if (useColor) "${color.yellow}⚠${color.reset} $location${event.message}" else "⚠ $location${event.message}"
            }
            is BuildEvent.BuildSuccessful -> {
                if (useColor) """
${color.green}╔══════════════════════════════════════════════════╗
║                    BUILD SUCCESSFUL                   ║
╚══════════════════════════════════════════════════╝${color.reset}
                """.trim() else "╔══════════════════════════════════════════════════╗\n║                    BUILD SUCCESSFUL                   ║\n╚══════════════════════════════════════════════════╝"
            }
            is BuildEvent.BuildFailed -> {
                if (useColor) """
${color.red}╔══════════════════════════════════════════════════╗
║                      BUILD FAILED                    ║
╚══════════════════════════════════════════════════╝${color.reset}
                """.trim() else "╔══════════════════════════════════════════════════╗\n║                      BUILD FAILED                    ║\n╚══════════════════════════════════════════════════╝"
            }
            else -> null
        }
    }
    
    override fun footer(summary: BuildSummary): String {
        val lines = mutableListOf("")
        lines.add("╔══════════════════════════════════════════════════╗")
        lines.add("║                   BUILD SUMMARY                   ║")
        lines.add("╠══════════════════════════════════════════════════╣")
        lines.add("║ Tasks:    ${summary.tasks.size.toString().padStart(4)} executed, ${summary.tasks.count { it.skipped }.toString().padStart(3)} skipped              ║")
        lines.add("║ Tests:    ${summary.passedTests.toString().padStart(4)} passed, ${summary.failedTests.toString().padStart(3)} failed, ${summary.skippedTests.toString().padStart(3)} skipped       ║")
        lines.add("║ Issues:   ${summary.warnings.toString().padStart(4)} warnings, ${summary.errors.toString().padStart(3)} errors               ║")
        if (summary.duration > 0) {
            val seconds = summary.duration / 1000.0
            lines.add("║ Time:      ${String.format("%.2f", seconds).padStart(6)}s                          ║")
        }
        lines.add("╚══════════════════════════════════════════════════╝")
        
        if (useColor) {
            return lines.joinToString("\n")
                .replace("Tasks:", "${color.bold}Tasks:${color.reset}")
                .replace("Tests:", "${color.bold}Tests:${color.reset}")
                .replace("Issues:", "${color.bold}Issues:${color.reset}")
                .replace("Time:", "${color.bold}Time:${color.reset}")
        }
        return lines.joinToString("\n")
    }
}

class AnsiColors {
    val reset = "\u001B[0m"
    val bold = "\u001B[1m"
    val dim = "\u001B[2m"
    val red = "\u001B[31m"
    val green = "\u001B[32m"
    val yellow = "\u001B[33m"
    val blue = "\u001B[34m"
    val magenta = "\u001B[35m"
    val cyan = "\u001B[36m"
    val white = "\u001B[37m"
}