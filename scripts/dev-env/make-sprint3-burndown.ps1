# Sprint 3 burndown generator (keep this file pure ASCII)
# Scope PB-31~PB-39 = 31 SP; actual remaining = 31 - SP merged to develop per day
param(
    [string]$Out = (Join-Path $PSScriptRoot "..\..\docs\scrum\sprint-3-burndown.png")
)


Add-Type -AssemblyName System.Drawing

$width = 1000
$height = 600
$left = 90
$top = 70
$right = $width - 60
$bottom = $height - 90
$max = 35

$dates = @("11-16 AM", "11-16 PM", "11-16 EOD", "11-17 AM")
$ideal = @(31, 20.7, 10.3, 0)
$actual = @(31, 18, 4, 0)

$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$g.Clear([System.Drawing.Color]::White)

$titleFont = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
$labelFont = New-Object System.Drawing.Font("Segoe UI", 10)
$smallFont = New-Object System.Drawing.Font("Segoe UI", 9)
$axisPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(80, 80, 80), 1.5)
$gridPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(225, 225, 225), 1)
$idealPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70, 120, 200), 2)
$idealPen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$actualPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(230, 140, 50), 3)
$pointBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, 140, 50))
$textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 40, 40))
$mutedBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(110, 110, 110))

$g.DrawString("Sprint 3 Burndown  (PB-31 ~ PB-39, 31 SP)", $titleFont, $textBrush, 90, 18)
$g.DrawString("Story points remaining", $labelFont, $mutedBrush, 22, 86)

for ($v = 0; $v -le 30; $v += 10) {
    $y = $bottom - ($v / $max) * ($bottom - $top)
    $g.DrawLine($gridPen, $left, $y, $right, $y)
    $g.DrawString([string]$v, $smallFont, $mutedBrush, $left - 32, $y - 8)
}

$g.DrawLine($axisPen, $left, $top, $left, $bottom)
$g.DrawLine($axisPen, $left, $bottom, $right, $bottom)

function X([int]$i) {
    return $left + $i * ($right - $left) / ($dates.Count - 1)
}
function Y([double]$v) {
    return $bottom - ($v / $max) * ($bottom - $top)
}

for ($i = 0; $i -lt $dates.Count; $i++) {
    $g.DrawString($dates[$i], $smallFont, $mutedBrush, (X $i) - 24, $bottom + 12)
}

for ($i = 1; $i -lt $dates.Count; $i++) {
    $g.DrawLine($idealPen, (X ($i - 1)), (Y $ideal[$i - 1]), (X $i), (Y $ideal[$i]))
    $g.DrawLine($actualPen, (X ($i - 1)), (Y $actual[$i - 1]), (X $i), (Y $actual[$i]))
}
for ($i = 0; $i -lt $dates.Count; $i++) {
    $g.FillEllipse($pointBrush, (X $i) - 4, (Y $actual[$i]) - 4, 8, 8)
    $g.DrawString([string]$actual[$i], $smallFont, $mutedBrush, (X $i) + 8, (Y $actual[$i]) - 18)
}

$g.DrawLine($idealPen, 700, 54, 740, 54)
$g.DrawString("Ideal", $smallFont, $mutedBrush, 748, 46)
$g.DrawLine($actualPen, 810, 54, 850, 54)
$g.DrawString("Actual", $smallFont, $mutedBrush, 858, 46)

$g.DrawString("Scope: PB-31~PB-39 (31 SP) | done = merged to develop (see sprint-3.md)", $smallFont, $mutedBrush, 90, $height - 42)

$dir = Split-Path -Parent $Out
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Saved $Out"
