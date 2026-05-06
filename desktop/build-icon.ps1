$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$resourcesDir = Join-Path $here 'src/main/resources'
$tempDir = Join-Path $here 'icon-build'
New-Item -ItemType Directory -Path $resourcesDir -Force | Out-Null
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

function New-IconPng([int]$size, [string]$path) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)

    # Background squircle (rounded square) with linear gradient
    $cornerRadius = [int]($size * 0.22)
    $bgPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $cornerRadius * 2
    $bgPath.AddArc(0, 0, $d, $d, 180, 90)
    $bgPath.AddArc($size - $d - 1, 0, $d, $d, 270, 90)
    $bgPath.AddArc($size - $d - 1, $size - $d - 1, $d, $d, 0, 90)
    $bgPath.AddArc(0, $size - $d - 1, $d, $d, 90, 90)
    $bgPath.CloseFigure()

    $startColor = [System.Drawing.Color]::FromArgb(255, 0x63, 0x66, 0xF1)
    $endColor   = [System.Drawing.Color]::FromArgb(255, 0xEC, 0x48, 0x99)
    $gradBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.PointF 0, 0),
        (New-Object System.Drawing.PointF ([float]$size), ([float]$size)),
        $startColor, $endColor
    )
    $g.FillPath($gradBrush, $bgPath)
    $bgPath.Dispose()
    $gradBrush.Dispose()

    # Map vector viewport (108) to pixel size
    $s = $size / 108.0

    # Geometry from ic_launcher_foreground.xml
    $cx = 26.5 * $s
    $r  = 6.5 * $s
    $rowY = @((39 * $s), (54 * $s), (69 * $s))
    $lineStartX = 46 * $s
    $lineEndX = @((70 * $s), (78 * $s), (67 * $s))
    $lineStroke = [Math]::Max(1.0, 3.1 * $s)
    $circleStroke = [Math]::Max(1.0, 2.2 * $s)
    $checkStroke = [Math]::Max(1.0, 2.0 * $s)
    $indigo = [System.Drawing.Color]::FromArgb(255, 0x63, 0x66, 0xF1)
    $whiteSoft = [System.Drawing.Color]::FromArgb(140, 0xFF, 0xFF, 0xFF)
    $white = [System.Drawing.Color]::White

    # Row 1: filled white circle + indigo check + soft white line
    $whiteFill = New-Object System.Drawing.SolidBrush $white
    $g.FillEllipse($whiteFill, [float]($cx - $r), [float]($rowY[0] - $r), [float]($r * 2), [float]($r * 2))
    $whiteFill.Dispose()

    $checkPen = [System.Drawing.Pen]::new($indigo, [float]$checkStroke)
    $checkPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $checkPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $checkPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $checkPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $checkPath.AddLine([float](29.5 * $s), [float](39.2 * $s), [float](32.2 * $s), [float](41.9 * $s))
    $checkPath.AddLine([float](32.2 * $s), [float](41.9 * $s), [float](36.2 * $s), [float](36 * $s))
    $g.DrawPath($checkPen, $checkPath)
    $checkPath.Dispose()
    $checkPen.Dispose()

    $softLinePen = [System.Drawing.Pen]::new($whiteSoft, [float]$lineStroke)
    $softLinePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $softLinePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($softLinePen, [float]$lineStartX, [float]$rowY[0], [float]$lineEndX[0], [float]$rowY[0])
    $softLinePen.Dispose()

    # Rows 2 and 3: hollow white circles + white line
    $hollowPen = [System.Drawing.Pen]::new($white, [float]$circleStroke)
    $linePen = [System.Drawing.Pen]::new($white, [float]$lineStroke)
    $linePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $linePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

    foreach ($i in 1..2) {
        $g.DrawEllipse($hollowPen, [float]($cx - $r), [float]($rowY[$i] - $r), [float]($r * 2), [float]($r * 2))
        $g.DrawLine($linePen, [float]$lineStartX, [float]$rowY[$i], [float]$lineEndX[$i], [float]$rowY[$i])
    }
    $hollowPen.Dispose()
    $linePen.Dispose()

    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

function New-IcoFile([string[]]$pngPaths, [string]$outIco) {
    $entries = @()
    foreach ($p in $pngPaths) {
        $bytes = [System.IO.File]::ReadAllBytes($p)
        $img = [System.Drawing.Image]::FromFile($p)
        $w = $img.Width; $h = $img.Height
        $img.Dispose()
        $entries += [pscustomobject]@{ Bytes = $bytes; W = $w; H = $h }
    }
    $stream = New-Object System.IO.MemoryStream
    $writer = New-Object System.IO.BinaryWriter $stream
    $writer.Write([uint16]0)
    $writer.Write([uint16]1)
    $writer.Write([uint16]$entries.Count)
    $offset = 6 + ($entries.Count * 16)
    foreach ($e in $entries) {
        $w = if ($e.W -ge 256) { [byte]0 } else { [byte]$e.W }
        $h = if ($e.H -ge 256) { [byte]0 } else { [byte]$e.H }
        $writer.Write([byte]$w)
        $writer.Write([byte]$h)
        $writer.Write([byte]0); $writer.Write([byte]0)
        $writer.Write([uint16]1); $writer.Write([uint16]32)
        $writer.Write([uint32]$e.Bytes.Length)
        $writer.Write([uint32]$offset)
        $offset += $e.Bytes.Length
    }
    foreach ($e in $entries) { $writer.Write($e.Bytes) }
    [System.IO.File]::WriteAllBytes($outIco, $stream.ToArray())
    $writer.Dispose(); $stream.Dispose()
}

$sizes = @(16, 32, 48, 64, 128, 256)
$paths = @()
foreach ($s in $sizes) {
    $p = Join-Path $tempDir "icon-$s.png"
    New-IconPng $s $p
    $paths += $p
}

$icoPath = Join-Path $resourcesDir 'icon.ico'
New-IcoFile $paths $icoPath

# Largest PNG for Linux distributions and runtime window icon (Compose can scale)
$largeSrc = Join-Path $tempDir 'icon-256.png'
$largeDst = Join-Path $resourcesDir 'icon.png'
Copy-Item $largeSrc $largeDst -Force

Write-Host "Generated: $icoPath"
Write-Host "Generated: $largeDst"
