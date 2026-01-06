# Used to prepare original wavs into oggs, keep here for reference

Get-ChildItem "<insert folder>" |
    Where-Object { $_.Name -match ".*(\.wav|\.mp3)" } |
    ForEach-Object {
        Write-Host "    Normalizing $($_.Name)..."
        pyenv exec ffmpeg-normalize $_.FullName `
            --preset streaming-video  `
            "-c:a" libvorbis `
            '-e="-q:a 8 -ac 1"'`
            --lower-only `
            -o (Join-Path $OutputDir "$($_.BaseName).ogg")
    }

Write-Host "Done."