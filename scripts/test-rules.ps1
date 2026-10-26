$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$testOutput = Join-Path $taskRoot 'build/rules-tests'
New-Item -ItemType Directory -Force -Path $testOutput | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -d $testOutput "$taskRoot/app/src/main/java/tw/edu/ctust/ncdphone/Rules.java" "$taskRoot/app/src/main/java/tw/edu/ctust/ncdphone/FlipDetector.java" "$taskRoot/tests/RulesTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Rules compilation failed' }
& "$env:JAVA_HOME/bin/java.exe" -cp $testOutput tw.edu.ctust.ncdphone.RulesTest
if ($LASTEXITCODE -ne 0) { throw 'Rules tests failed' }
