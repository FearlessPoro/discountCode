param(
    [Parameter(Mandatory = $false)]
    [string] $IpAddress = "8.8.8.8"
)

$uri = "http://ip-api.com/json/$IpAddress" + "?fields=status,message,query,country,countryCode"

try {
    Invoke-RestMethod -Uri $uri -Method Get | ConvertTo-Json
}
catch {
    Write-Error "GeoIP lookup failed: $($_.Exception.Message)"
    exit 1
}
