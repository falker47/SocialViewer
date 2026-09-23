$ErrorActionPreference = 'Stop'

$gradleVersion = '9.6.0'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$tempRoot = Join-Path $env:TEMP ("socialviewer-gradle-" + [Guid]::NewGuid().ToString('N'))
$zipPath = Join-Path $tempRoot "gradle-$gradleVersion-bin.zip"
$shaPath = "$zipPath.sha256"
$distUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
$shaUrl = "$distUrl.sha256"
$wrapperProject = Join-Path $tempRoot 'wrapper-project'

try {
    New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
    New-Item -ItemType Directory -Force -Path $wrapperProject | Out-Null

    Write-Host "[1/4] Download Gradle $gradleVersion..."
    Invoke-WebRequest -Uri $distUrl -OutFile $zipPath
    Invoke-WebRequest -Uri $shaUrl -OutFile $shaPath

    Write-Host '[2/4] Verify SHA-256...'
    $expected = ((Get-Content -Raw $shaPath).Trim() -split '\s+')[0].ToLowerInvariant()
    $actual = (Get-FileHash -Path $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        throw "Gradle checksum mismatch. Expected $expected, got $actual"
    }

    Write-Host '[3/4] Generate Gradle Wrapper...'
    Expand-Archive -Path $zipPath -DestinationPath $tempRoot -Force
    @"
tasks.named('wrapper', Wrapper) {
    gradleVersion = '$gradleVersion'
    distributionType = Wrapper.DistributionType.BIN
}
"@ | Set-Content -Path (Join-Path $wrapperProject 'build.gradle') -Encoding ASCII

    $gradleBat = Join-Path $tempRoot "gradle-$gradleVersion\bin\gradle.bat"
    & $gradleBat -p $wrapperProject wrapper --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle wrapper generation failed with exit code $LASTEXITCODE"
    }

    Write-Host '[4/4] Copy wrapper into Social Viewer...'
    Copy-Item -Path (Join-Path $wrapperProject 'gradlew') -Destination (Join-Path $projectRoot 'gradlew') -Force
    Copy-Item -Path (Join-Path $wrapperProject 'gradlew.bat') -Destination (Join-Path $projectRoot 'gradlew.bat') -Force
    New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'gradle\wrapper') | Out-Null
    Copy-Item -Path (Join-Path $wrapperProject 'gradle\wrapper\gradle-wrapper.jar') -Destination (Join-Path $projectRoot 'gradle\wrapper\gradle-wrapper.jar') -Force
    Copy-Item -Path (Join-Path $wrapperProject 'gradle\wrapper\gradle-wrapper.properties') -Destination (Join-Path $projectRoot 'gradle\wrapper\gradle-wrapper.properties') -Force

    Write-Host ''
    Write-Host 'Gradle Wrapper ready. Open this folder in Android Studio.' -ForegroundColor Green
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot -ErrorAction SilentlyContinue
    }
}


