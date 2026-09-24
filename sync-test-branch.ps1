param(
    [Parameter(Position = 0)]
    [string]$Branch
)

$ErrorActionPreference = "Stop"

function Invoke-Git {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    & git @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($Arguments -join ' ')"
    }
}

& git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Run this script from inside the SocialViewer Git repository."
}

$repoRoot = (& git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

$changes = & git status --porcelain
if ($changes) {
    Write-Host ""
    Write-Host "Local changes detected. Nothing was changed." -ForegroundColor Yellow
    & git status --short
    Write-Host ""
    Write-Host "Commit, stash, or discard these changes before syncing a test branch." -ForegroundColor Yellow
    exit 1
}

if ([string]::IsNullOrWhiteSpace($Branch)) {
    $Branch = (& git branch --show-current).Trim()
    if ([string]::IsNullOrWhiteSpace($Branch)) {
        throw "Detached HEAD. Pass the branch explicitly, e.g. .\sync-test-branch.ps1 feature/example"
    }
}

Write-Host "Fetching origin..." -ForegroundColor Cyan
Invoke-Git fetch origin --prune

& git show-ref --verify --quiet "refs/remotes/origin/$Branch"
if ($LASTEXITCODE -ne 0) {
    throw "Remote branch 'origin/$Branch' does not exist."
}

& git show-ref --verify --quiet "refs/heads/$Branch"
$localBranchExists = $LASTEXITCODE -eq 0

if ($localBranchExists) {
    Invoke-Git switch $Branch
} else {
    Invoke-Git switch --track -c $Branch "origin/$Branch"
}

Invoke-Git pull --ff-only origin $Branch

$shortSha = (& git rev-parse --short HEAD).Trim()

Write-Host ""
Write-Host "Ready to test." -ForegroundColor Green
Write-Host "Branch: $Branch"
Write-Host "Commit: $shortSha"
Write-Host ""
Write-Host "Now run the app from Android Studio on your emulator/device."
