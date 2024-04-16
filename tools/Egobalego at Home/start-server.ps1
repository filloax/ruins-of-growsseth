# Check if Python is installed
$pythonExists = $(get-command python -ErrorAction SilentlyContinue)
if (-not $pythonExists) {
    Write-Host "Please install Python 3.x https://www.python.org/downloads/"
    exit 1
}
$python = python --version 2>&1
if ($python -notmatch "Python") {
    Write-Host "Please install Python 3.x https://www.python.org/downloads/"
    exit 1
}

$venv = $PSScriptRoot + "\.venv"
if (!(Test-Path $venv)) {
    Write-Host "Virtual environment does not exist. Creating now."
    python -m venv $venv
}

"$venv\Scripts\Activate.ps1"

# Install Flask if it's not already installed
pip install -r "$PSScriptRoot\requirements.txt"

$script = $PSScriptRoot + "\start-server.py"

python $script --open