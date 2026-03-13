package gradlepretty

import java.util.regex.Pattern

class GradleOutputParser {
    private val events = mutableListOf<BuildEvent>()
    private val summary = BuildSummary()
    private var currentTask: String? = null
    private var currentProject: String? = null
    private var taskStartTime: Long = 0
    
    // Regex patterns for Gradle output
    private val taskStartPattern = Pattern.compile("""(:[a-zA-Z0-9_-]+)""")
    private val taskCompletedPattern = Pattern.compile("""(:[a-zA-Z0-9_-]+) (?:BUILD SUCCESSFUL|BUILD FAILED|FAILED) in ([\d.]+)(?:s|ms)?""")
    private val testStartedPattern = Pattern.compile("""Test (?:started|run) '(.*?)' \((.*?)\)""")
    private val testPassedPattern = Pattern.compile("""Test (?:passed|suceeded) '(.*?)' \((.*?)\) in ([\d.]+)ms?""")
    private val testFailedPattern = Pattern.compile("""Test (?:failed|error) '(.*?)' \((.*?)\)""")
    private val testSkippedPattern = Pattern.compile("""Test (?:skipped|ignored) '(.*?)' \((.*?)\)""")
    private val compilationErrorPattern = Pattern.compile("""(.+?):(\d+):\s*error:\s*(.+)""")
    private val warningPattern = Pattern.compile("""(.+?):(\d+):\s*warning:\s*(.+)""")
    private val buildPhasePattern = Pattern.compile("""(Configure|Execute|Build) (?:project|phase)[:\s]*(.*)""")
    private val buildStartedPattern = Pattern.compile("""Configure project :(\w+)""")
    private val buildSuccessPattern = Pattern.compile("""BUILD (SUCCESSFUL|FAILED) in ([\d.]+)(?:s|ms)?""")
    private val gradleStartPattern = Pattern.compile("""Gradle (?:Daemon|Build) (?:started|beginning)""")
    private val infoPattern = Pattern.compile("""\[INFO\] (.+)""")
    
    fun parse(line: String): BuildEvent? {
        val trimmed = line.trim()
        
        // Skip empty lines and ANSI codes
        if (trimmed.isEmpty() || trimmed.startsWith("\u001B")) return null
        
        // Check for task execution
        if (trimmed.startsWith("> Task :")) {
            return parseTaskLine(trimmed)
        }
        
        // Check for test results
        if (trimmed.contains("Test ") || trimmed.contains("tests")) {
            val testEvent = parseTestLine(trimmed)
            if (testEvent != null) return testEvent
        }
        
        // Check for compilation errors
        compilationErrorPattern.matcher(trimmed).let { m ->
            if (m.matches()) {
                return BuildEvent.CompilationError(m.group(1)!!, m.group(2)!!.toInt(), m.group(3)!!)
            }
        }
        
        // Check for warnings
        warningPattern.matcher(trimmed).let { m ->
            if (m.matches()) {
                return BuildEvent.Warning(m.group(3)!!, m.group(1), m.group(2).toIntOrNull())
            }
        }
        
        // Check for build phases
        if (trimmed.startsWith("Configure project") || trimmed.startsWith("Execute tasks") || trimmed.startsWith("Build tasks")) {
            return BuildEvent.BuildPhase(trimmed)
        }
        
        // Check for build completion
        if (trimmed.contains("BUILD")) {
            buildSuccessPattern.matcher(trimmed).let { m ->
                if (m.matches()) {
                    val duration = parseDuration(m.group(2)!!)
                    return if (m.group(1) == "SUCCESSFUL") {
                        BuildEvent.BuildSuccessful(duration)
                    } else {
                        BuildEvent.BuildFailed(trimmed)
                    }
                }
            }
        }
        
        // Check for info messages (skip standard Gradle info)
        if (trimmed.startsWith("[") && trimmed.contains("] ")) {
            return null // Skip standard Gradle output like [DEBUG], [INFO], etc.
        }
        
        return null
    }
    
    private fun parseTaskLine(line: String): BuildEvent? {
        // Example: "> Task :app:compileDebugKotlin UP-TO-DATE"
        // Example: "> Task :app:test UP-TO-DATE"
        // Example: "> Task :app:build FAILED"
        
        val taskMatch = Regex("""'> Task (:[\w:]+) (\w+(?:-[a-zA-Z]+)*)""").find(line)
        if (taskMatch != null) {
            val taskPath = taskMatch.groupValues[1]
            val taskName = taskMatch.groupValues[2]
            
            return when {
                line.contains("UP-TO-DATE") -> {
                    BuildEvent.TaskCompleted(taskPath.removePrefix(":"), "app", 0, skipped = true)
                }
                line.contains("FAILED") -> {
                    BuildEvent.TaskCompleted(taskPath.removePrefix(":"), "app", 0, skipped = false)
                }
                else -> {
                    BuildEvent.TaskStarted(taskName, taskPath.removePrefix(":"))
                }
            }
        }
        
        // Check for completed task
        if (line.contains(" SUCCESS ") || line.contains(" FAILED ")) {
            val match = Regex("""'> Task ([^ ]+) (SUCCESS|FAILED)(?: in ([\d.]+)(?:s|ms))?""").find(line)
            if (match != null) {
                val task = match.groupValues[1].removePrefix(":")
                val success = match.groupValues[2] == "SUCCESS"
                val duration = match.groupValues[3]?.let { parseDuration(it) } ?: 0L
                return BuildEvent.TaskCompleted(task, "", duration, skipped = !success)
            }
        }
        
        return null
    }
    
    private fun parseTestLine(line: String): BuildEvent? {
        testPassedPattern.matcher(line).let { m ->
            if (m.matches()) {
                return BuildEvent.TestPassed(m.group(1)!!, m.group(2)!!, m.group(3)!!.toLong())
            }
        }
        
        testFailedPattern.matcher(line).let { m ->
            if (m.matches()) {
                return BuildEvent.TestFailed(m.group(1)!!, m.group(2)!!, line, "")
            }
        }
        
        testSkippedPattern.matcher(line).let { m ->
            if (m.matches()) {
                return BuildEvent.TestSkipped(m.group(1)!!, m.group(2)!!)
            }
        }
        
        return null
    }
    
    private fun parseDuration(durationStr: String): Long {
        return try {
            if (durationStr.endsWith("s")) {
                (durationStr.dropLast(1).toDouble() * 1000).toLong()
            } else {
                durationStr.toLong()
            }
        } catch (e: NumberFormatException) {
            0L
        }
    }
    
    fun processInput(lines: Sequence<String>): BuildSummary {
        var startTime = 0L
        var endTime = 0L
        val tasks = mutableListOf<TaskResult>()
        val tests = mutableListOf<TestResult>()
        var warnings = 0
        var errors = 0
        
        var currentTaskName: String? = null
        var currentTaskProject: String? = null
        var currentTaskStart: Long = 0L
        
        lines.forEach { line ->
            val event = parse(line) ?: return@forEach
            
            when (event) {
                is BuildEvent.BuildStarted -> {
                    startTime = System.currentTimeMillis()
                }
                is BuildEvent.TaskStarted -> {
                    currentTaskName = event.taskName
                    currentTaskProject = event.project
                    currentTaskStart = System.currentTimeMillis()
                }
                is BuildEvent.TaskCompleted -> {
                    val duration = System.currentTimeMillis() - currentTaskStart
                    tasks.add(TaskResult(
                        name = event.taskName,
                        project = event.project,
                        duration = duration,
                        skipped = event.skipped,
                        failed = !event.skipped && event.duration == 0L
                    ))
                    currentTaskName = null
                    currentTaskProject = null
                }
                is BuildEvent.TestPassed -> {
                    tests.add(TestResult(event.testName, event.className, true, duration = event.duration))
                }
                is BuildEvent.TestFailed -> {
                    tests.add(TestResult(event.testName, event.className, false, message = event.message))
                    errors++
                }
                is BuildEvent.TestSkipped -> {
                    tests.add(TestResult(event.testName, event.className, false, skipped = true))
                }
                is BuildEvent.BuildSuccessful -> {
                    endTime = System.currentTimeMillis()
                }
                is BuildEvent.Warning -> {
                    warnings++
                }
                is BuildEvent.CompilationError -> {
                    errors++
                }
                else -> {}
            }
        }
        
        return BuildSummary(startTime, endTime, tasks, tests, warnings, errors)
    }
}