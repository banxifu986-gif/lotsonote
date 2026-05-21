param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$NoteKeyword = "Java interview",
    [string]$UserKeyword = "admin",
    [int]$Repeat = 20,
    [string]$RedisCli = "redis-cli",
    [switch]$SkipRedisFlush
)

$ErrorActionPreference = "Stop"

function Invoke-SearchRequest {
    param(
        [string]$Url
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing
    $stopwatch.Stop()

    $cacheHeader = $response.Headers["X-Search-Cache"]
    $durationHeader = $response.Headers["X-Search-Duration-Ms"]
    $resultCountHeader = $response.Headers["X-Search-Result-Count"]

    return [PSCustomObject]@{
        Url = $Url
        HttpStatus = [int]$response.StatusCode
        Cache = $(if ($cacheHeader) { $cacheHeader } else { "UNKNOWN" })
        DurationMs = $(if ($durationHeader) { [double]$durationHeader } else { [double]$stopwatch.ElapsedMilliseconds })
        EndToEndMs = [double]$stopwatch.ElapsedMilliseconds
        ResultCount = $(if ($resultCountHeader) { [int]$resultCountHeader } else { -1 })
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
    $min = ($values | Measure-Object -Minimum).Minimum
    $max = ($values | Measure-Object -Maximum).Maximum
    $p95Index = [Math]::Ceiling($values.Count * 0.95) - 1
    if ($p95Index -lt 0) {
        $p95Index = 0
    }

    return [PSCustomObject]@{
        Count = $values.Count
        Avg = [Math]::Round($avg, 2)
        Min = [Math]::Round($min, 2)
        Max = [Math]::Round($max, 2)
        P95 = [Math]::Round($values[$p95Index], 2)
    }
}

function Flush-SearchCache {
    param(
        [string]$RedisCliPath
    )

    try {
        $keys = & $RedisCliPath --scan --pattern "search:*"
        foreach ($key in $keys) {
            if ($null -ne $key -and $key.Trim().Length -gt 0) {
                & $RedisCliPath DEL $key | Out-Null
            }
        }
    } catch {
        Write-Warning ("Failed to clear Redis search cache: " + $_.Exception.Message)
        Write-Warning 'You can run: redis-cli --scan --pattern "search:*" and DEL each key manually.'
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
        $records += Invoke-SearchRequest -Url $Url
    }

    $hitCount = @($records | Where-Object { $_.Cache -eq "HIT" }).Count
    $missCount = @($records | Where-Object { $_.Cache -eq "MISS" }).Count
    $durationStats = Get-Stats -Items $records -Field "DurationMs"
    $e2eStats = Get-Stats -Items $records -Field "EndToEndMs"

    return [PSCustomObject]@{
        Scenario = $Name
        Url = $Url
        TotalRequests = $records.Count
        CacheHits = $hitCount
        CacheMisses = $missCount
        HitRate = $(if ($records.Count -gt 0) { [Math]::Round($hitCount * 100.0 / $records.Count, 2) } else { 0 })
        AvgDurationMs = $durationStats.Avg
        P95DurationMs = $durationStats.P95
        AvgEndToEndMs = $e2eStats.Avg
        P95EndToEndMs = $e2eStats.P95
        ResultCount = $(if ($records.Count -gt 0) { $records[-1].ResultCount } else { -1 })
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportDir = Join-Path $PSScriptRoot "reports"
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

$normalizedBaseUrl = $BaseUrl.TrimEnd("/")
$encodedNoteKeyword = [uri]::EscapeDataString($NoteKeyword)
$encodedUserKeyword = [uri]::EscapeDataString($UserKeyword)
$noteUrl = "{0}/api/search/notes?keyword={1}&page=1&pageSize=10" -f $normalizedBaseUrl, $encodedNoteKeyword
$userUrl = "{0}/api/search/users?keyword={1}&page=1&pageSize=10" -f $normalizedBaseUrl, $encodedUserKeyword

if (-not $SkipRedisFlush) {
    Flush-SearchCache -RedisCliPath $RedisCli
}

$coldNote = Measure-Scenario -Name "note-cold" -Url $noteUrl -Times 1
$hotNote = Measure-Scenario -Name "note-hot-repeat" -Url $noteUrl -Times $Repeat

if (-not $SkipRedisFlush) {
    Flush-SearchCache -RedisCliPath $RedisCli
}

$coldUser = Measure-Scenario -Name "user-cold" -Url $userUrl -Times 1
$hotUser = Measure-Scenario -Name "user-hot-repeat" -Url $userUrl -Times $Repeat

$summary = @($coldNote, $hotNote, $coldUser, $hotUser)
$summaryPath = Join-Path $reportDir ("search-measure-" + $timestamp + ".json")
$summary | ConvertTo-Json -Depth 4 | Set-Content -Path $summaryPath -Encoding UTF8

Write-Host ""
Write-Host "Search measurement summary"
Write-Host ("Report file: " + $summaryPath)
Write-Host ""
$summary | Format-Table -AutoSize
