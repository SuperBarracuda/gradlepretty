package gradlepretty

sealed class BuildEvent {
    data class TaskStarted(val taskName: String, val project: String) : BuildEvent()
    data class TaskCompleted(val taskName: String, val project: String, val duration: Long, val skipped: Boolean = false) : BuildEvent()
    data class TestStarted(val testName: String, val className: String) : BuildEvent()
    data class TestPassed(val testName: String, val className: String, val duration: Long) : BuildEvent()
    data class TestFailed(val testName: String, val className: String, val message: String, val stackTrace: String) : BuildEvent()
    data class TestSkipped(val testName: String, val className: String) : BuildEvent()
    data class CompilationError(val file: String, val line: Int, val message: String) : BuildEvent()
    data class Warning(val message: String, val file: String? = null, val line: Int? = null) : BuildEvent()
    data class BuildPhase(val phase: String) : BuildEvent()
    data class BuildStarted(val project: String) : BuildEvent()
    data class BuildSuccessful(val duration: Long) : BuildEvent()
    data class BuildFailed(val message: String) : BuildEvent()
    data class Info(val message: String) : BuildEvent()
}

data class TaskResult(
    val name: String,
    val project: String,
    val duration: Long,
    val skipped: Boolean = false,
    val failed: Boolean = false,
    val error: String? = null
)

data class TestResult(
    val name: String,
    val className: String,
    val passed: Boolean,
    val skipped: Boolean = false,
    val duration: Long = 0,
    val message: String? = null,
    val stackTrace: String? = null
)

data class BuildSummary(
    val startTime: Long = 0,
    val endTime: Long = 0,
    val tasks: List<TaskResult> = emptyList(),
    val tests: List<TestResult> = emptyList(),
    val warnings: Int = 0,
    val errors: Int = 0
) {
    val duration: Long get() = if (endTime > 0 && startTime > 0) endTime - startTime else 0
    val passedTests: Int get() = tests.count { it.passed && !it.skipped }
    val failedTests: Int get() = tests.count { !it.passed && !it.skipped }
    val skippedTests: Int get() = tests.count { it.skipped }
}