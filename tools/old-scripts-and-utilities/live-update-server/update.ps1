$scriptPath = split-path -parent $MyInvocation.MyCommand.Definition
$orig = $(Get-Location)
Set-Location "$scriptPath/remote-commands-server"

Write-Host "CLEAN"
.\gradlew clean
Write-Host "BUILD"
.\gradlew build
Write-Host "JAR"
.\gradlew jar
Write-Host "TAR"
.\gradlew distTar
Write-Host "Removing previous server"
ssh raspi "rm -rf cydonia-mc/remote-commands-server"
Write-Host "Copying and unpacking new server"
scp "build\distributions\remote-commands-server-*.tar" raspi:cydonia-mc/remote-commands-server.tar
ssh raspi "cd cydonia-mc; tar -xvf remote-commands-server.tar"
Write-Host "Renaming server folder"
ssh raspi "mv cydonia-mc/remote-commands-server-* cydonia-mc/remote-commands-server"
ssh raspi "rm cydonia-mc/remote-commands-server.tar"

Set-Location $orig