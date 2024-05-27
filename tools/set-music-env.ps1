# Requires a music.key with the right password in the main folder
$scriptDir = Split-Path -Path $MyInvocation.MyCommand.Definition -Parent
$parentDir = Split-Path -Path $scriptDir -Parent
$musicKeyPath = Join-Path -Path $parentDir -ChildPath "music.key"
$content = Get-Content -Path $musicKeyPath -Raw
[Environment]::SetEnvironmentVariable("GROWSSETH_MUSIC_PW", $content, [EnvironmentVariableTarget]::User)
Write-Host Done!