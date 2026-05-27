param(
    [string]$BaseUrl = "http://localhost:8081",
    [int]$Repeat = 20,
    [string]$RedisCli = "redis-cli",
    [switch]$SkipRedisFlush
)

$ErrorActionPreference = "Stop"

function Get-RankKey {
    return "rank:note:daily:" + (Get-Date -Format "yyyy-MM-dd")
}

function Clear-RankCache {
    param(
        [string]$RedisCliPath
    )

    $rankKey = Get-RankKey
    try {
        & $RedisCliPath DEL $rankKey | Out-Null
    } catch {
        Write-Warning ("Failed to clear rank cache: " + $_.Exception.Message)
    }
}

function Invoke-RankRequest {
    param(
        [string]$Url
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing
    $stopwatch.Stop()

    return [PSCustomObject]@{
        HttpStatus = [int]$response.StatusCode
        Cache = $response.Headers["X-Rank-Cache"]
        DurationMs = [double]$response.Headers["X-Rank-Duration-Ms"]
        EndToEndMs = [double]$stopwatch.ElapsedMilliseconds
        ResultCount = [int]$response.Headers["X-Rank-Result-Count"]
    }
}

function Get-Stats {
    param(
        [array]$Items,
        [string]$Field
    )

    $values = @($Items | ForEach-Object { [double]($_.$Field) } | Sort-Object)
    if ($values.Count -eq 0) {
        return $null
    }

    $avg = ($values | Measure-Object -Average).Average
    $p95Index = [Math]::Ceiling($values.Count * 0.95) - 1
    if ($p95Index -lt 0) {
        $p95Index = 0
    }

    return [PSCustomObject]@{
        Avg = [Math]::Round($avg, 2)
        P95 = [Math]::Round($values[$p95Index], 2)
    }
}

function Measure-Scenario {
    param(
        [string]$Name,
        [string]$Url,
        [int]$Times
    )

    $records = @()
    for ($i = 1; $i -le $Times; $i++) {
        $records += Invoke-RankRequest -Url $Url
    }

    $durationStats = Get-Stats -Items $records -Field "DurationMs"
    $e2eStats = Get-Stats -Items $records -Field "EndToEndMs"
    $hitCount = @($records | Where-Object { $_.Cache -eq "HIT" }).Count
    $missCount = @($records | Where-Object { $_.Cache -eq "MISS" }).Count

    return [PSCustomObject]@{
        Scenario = $Name
        TotalRequests = $records.Count
        CacheHits = $hitCount
        CacheMisses = $missCount
        HitRate = $(if ($records.Count -gt 0) { [Math]::Round($hitCount * 100.0 / $records.Count, 2) } else { 0 })
        AvgDurationMs = $durationStats.Avg
        P95DurationMs = $durationStats.P95
        AvgEndToEndMs = $e2eStats.Avg
        P95EndToEndMs = $e2eStats.P95
        ResultCount = $(if ($records.Count -gt 0) { $records[-1].ResultCount } else { 0 })
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportDir = Join-Path $PSScriptRoot "reports"
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

$url = $BaseUrl.TrimEnd("/") + "/api/notes/ranklist"

if (-not $SkipRedisFlush) {
    Clear-RankCache -RedisCliPath $RedisCli
}

$coldRank = Measure-Scenario -Name "rank-cold" -Url $url -Times 1
$hotRank = Measure-Scenario -Name "rank-hot-repeat" -Url $url -Times $Repeat

$summary = @($coldRank, $hotRank)
$summaryPath = Join-Path $reportDir ("note-rank-measure-" + $timestamp + ".json")
$summary | ConvertTo-Json -Depth 4 | Set-Content -Path $summaryPath -Encoding UTF8

Write-Host ""
Write-Host "Note rank measurement summary"
Write-Host ("Report file: " + $summaryPath)
Write-Host ""
$summary | Format-Table -AutoSize
