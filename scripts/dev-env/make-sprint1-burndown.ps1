# Sprint 1 burndown generator (keep this file pure ASCII)
# Scope PB-06~PB-19 = 40 SP; actual remaining recomputed from product-backlog.md SP on 2026-11-19
# D0 40 -> D1 32 (-8: PB-11 5 + PB-15 3, PR #78) -> D2 12 (-20: PB-06/07/08/09/12/13/17, PR #86)
param(
    [string]$Out = (Join-Path $PSScriptRoot "..\..\docs\scrum\sprint-1-burndown.png")
)

Add-Type -AssemblyName System.Drawing

$width = 1000
$height = 640
$left = 90
$top = 100
$right = $width - 60
$bottom = $height - 96
$max = 40

$dates = @("11-03", "11-04", "11-05", "11-06", "11-09", "11-10", "11-11", "11-12", "11-13")
$days = @("D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8")
$ideal = @(40, 35, 30, 25, 20, 15, 10, 5, 0)
$actual = @(40, 32, 12, 12, 12, 12, 12, 12, 12)

$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$g.Clear([System.Drawing.Color]::White)

$titleFont = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
$subFont = New-Object System.Drawing.Font("Segoe UI", 10)
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
$noteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(190, 60, 60))

$g.DrawString("Sprint 1 Burndown  (PB-06 ~ PB-19, 40 SP)", $titleFont, $textBrush, 90, 12)
$g.DrawString("2026-11-03 ~ 2026-11-13, 9 working days | closed at 28/40 SP (70%)", $subFont, $mutedBrush, 90, 48)
$g.DrawString("Story points remaining", $labelFont, $mutedBrush, 20, 116)

for ($v = 0; $v -le 40; $v += 10) {
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
    $g.DrawString($dates[$i], $smallFont, $textBrush, (X $i) - 18, $bottom + 10)
    $g.DrawString($days[$i], $smallFont, $mutedBrush, (X $i) - 10, $bottom + 26)
}

for ($i = 1; $i -lt $dates.Count; $i++) {
    $g.DrawLine($idealPen, (X ($i - 1)), (Y $ideal[$i - 1]), (X $i), (Y $ideal[$i]))
    $g.DrawLine($actualPen, (X ($i - 1)), (Y $actual[$i - 1]), (X $i), (Y $actual[$i]))
}
for ($i = 0; $i -lt $dates.Count; $i++) {
    $g.FillEllipse($pointBrush, (X $i) - 4, (Y $actual[$i]) - 4, 8, 8)
    $g.DrawString([string]$actual[$i], $smallFont, $textBrush, (X $i) + 8, (Y $actual[$i]) - 20)
}

# callout for the single large drop on D2
$g.DrawString("-20 SP on 11-05 = PB-06/07/08/09/12/13/17 (PR #86)", $smallFont, $noteBrush, (X 2) - 46, (Y 12) + 14)
$g.DrawString("D3~D8 flat: no S1-scope card left to merge (see sprint-1.md section 3)", $smallFont, $noteBrush, (X 2) - 46, (Y 12) + 30)

$g.DrawLine($idealPen, 700, 52, 740, 52)
$g.DrawString("Ideal", $smallFont, $mutedBrush, 748, 44)
$g.DrawLine($actualPen, 800, 52, 840, 52)
$g.DrawString("Actual", $smallFont, $mutedBrush, 848, 44)

$g.DrawString("Scope: PB-06~PB-19 (40 SP) | done = merged to develop AND acceptance scenario passed (see sprint-1.md section 3)", $smallFont, $mutedBrush, 90, $height - 40)
$g.DrawString("Recomputed 2026-11-19 against product-backlog.md SP (old 35/25 chart retracted)", $smallFont, $mutedBrush, 90, $height - 22)

$dir = Split-Path -Parent $Out
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Saved $Out"
