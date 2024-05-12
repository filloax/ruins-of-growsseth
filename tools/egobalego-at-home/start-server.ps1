# Check if Python is installed
$pythonExists = $(get-command python -ErrorAction SilentlyContinue)
if (-not $pythonExists) {
    Write-Host "Could not find python command, please install Python 3.x: https://www.python.org/downloads/"
    exit 1
}

$venv = $PSScriptRoot + "\.venv"
if (!(Test-Path $venv)) {
    Write-Host "Virtual environment does not exist. Creating now."
    py -m venv $venv
}

& "$venv\Scripts\Activate.ps1"

# Install Flask if it's not already installed
pip install -r "$PSScriptRoot\requirements.txt"

$script = $PSScriptRoot + "\start-server.py"
echo $script

py $script --open
