package gradlepretty

import java.io.File

fun main(args: Array<String>) {
    var format = "pretty"
    var useColor = System.console() != null
    var inputFile: String? = null
    var outputFile: String? = null
    var reportType: String? = null
    var quiet = false
    
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--format", "-f" -> {
                if (i + 1 < args.size) format = args[++i]
            }
            "--color" -> useColor = true
            "--no-color" -> useColor = false
            "--file", "-i" -> {
                if (i + 1 < args.size) inputFile = args[++i]
            }
            "--output", "-o" -> {
                if (i + 1 < args.size) outputFile = args[++i]
            }
            "--report", "-r" -> {
                if (i + 1 < args.size) reportType = args[++i]
            }
            "--quiet", "-q" -> quiet = true
            "--help", "-h" -> {
                printHelp()
                return
            }
        }
        i++
    }
    
    if (!useColor) {
        System.setProperty("sun.stdout.encoding", "UTF-8")
    }
    
    val input: List<String> = if (inputFile != null) {
        File(inputFile).bufferedReader().readLines()
    } else {
        System.`in`.bufferedReader().readLines()
    }
    
    val formatter: OutputFormatter = when (format) {
        "compact" -> CompactFormatter(useColor)
        "simple" -> SimpleFormatter(useColor)
        "pretty" -> PrettyFormatter(useColor)
        else -> PrettyFormatter(useColor)
    }
    
    val parser = GradleOutputParser()
    var lastFormattedLine = ""
    
    input.forEach { line ->
        val event = parser.parse(line)
        if (event != null) {
            val formatted = formatter.format(event)
            if (formatted != null) {
                if (!quiet || event is BuildEvent.BuildFailed || event is BuildEvent.CompilationError) {
                    println(formatted)
                    lastFormattedLine = formatted
                }
            }
        }
    }
    
    val summary = parser.processInput(input.asSequence())
    
    if (outputFile != null && reportType != null) {
        when (reportType) {
            "json" -> JsonReportGenerator().writeToFile(summary, outputFile)
            "html" -> HtmlReportGenerator().writeToFile(summary, outputFile)
            else -> System.err.println("Unknown report type: $reportType")
        }
    }
    
    val footer = formatter.footer(summary)
    if (footer.isNotEmpty()) {
        println(footer)
    }
}

fun printHelp() {
    println("""
GradlePretty - Format Gradle build output

Usage: gradlepretty [options]

Options:
  --format, -f <format>   Output format: compact, simple, pretty (default: pretty)
  --color                Enable colored output
  --no-color             Disable colored output
  --file, -i <file>      Read from file instead of stdin
  --output, -o <file>    Output file for report
  --report, -r <type>    Report type: json, html
  --quiet, -q            Suppress non-error output
  --help, -h             Show this help

Examples:
  ./gradlew build | gradlepretty
  ./gradlew build | gradlepretty --format=compact
  gradlepretty --file build.log --report=html --output=report.html
    """.trim())
}
