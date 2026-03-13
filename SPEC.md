# GradlePretty - Android Build Log Formatter

## Project Overview
- **Project Name**: GradlePretty
- **Type**: Command-line tool (Kotlin)
- **Core Functionality**: Parses and formats Gradle build output into human-readable, colorized output similar to xcpretty for Xcode builds.

## Functionality Specification

### Core Features
1. **Log Parsing**: Read Gradle build output from stdin or file
2. **Output Formats**:
   - `--format=compact` - Single line per task, minimal output
   - `--format=simple` - Slightly more detailed, shows phases
   - `--format=pretty` - Colorized, detailed output with progress indicators
3. **File Reporting**: Generate test result reports in JSON and HTML formats
4. **Color Output**: Colored output for different log levels (errors=red, warnings=yellow, success=green)
5. **Timing**: Show build duration and task execution times
6. **Progress**: Real-time progress indicator for ongoing tasks

### User Interactions
- Pipe Gradle output: `./gradlew build | gradlepretty`
- Read from file: `gradlepretty --file build.log`
- Generate reports: `gradlepretty --format=json --output=report.json`

### Log Parsing
- Parse task start/completion
- Parse test results (passed, failed, skipped)
- Parse compilation errors with file:line references
- Parse warnings
- Parse build phases (configuring, executing, etc.)

### Options
- `--format` - Output format (compact/simple/pretty)
- `--color/--no-color` - Enable/disable colors
- `--file` - Read from file instead of stdin
- `--output` - Output file for reports
- `--report` - Report type (json/html)
- `--quiet` - Suppress non-error output

## Technical Design

### Architecture
- Entry point: `GradlePretty.kt` - main CLI handler
- Parser: `GradleOutputParser.kt` - parses raw Gradle output
- Formatters: `CompactFormatter.kt`, `SimpleFormatter.kt`, `PrettyFormatter.kt`
- Models: `BuildEvent.kt`, `TaskResult.kt`, `TestResult.kt`
- Reports: `JsonReportGenerator.kt`, `HtmlReportGenerator.kt`

### Dependencies
- Kotlin stdlib
- kotlinx-cli for argument parsing
- No external dependencies for core functionality

## Acceptance Criteria
1. Reads Gradle output from stdin or file
2. Formats output in compact, simple, and pretty modes
3. Colors errors in red, warnings in yellow, successes in green
4. Generates JSON test reports
5. Shows build timing information
6. Works as a pipe: `./gradlew build | gradlepretty`