param (
    [string]$ItemName
)

$ItemsPath = "E:\L2 j interluce ct0\game\data\stats\items"

Write-Host ""
Write-Host "=== Searching for item name: $ItemName ===" -ForegroundColor Cyan
Write-Host ""

Get-ChildItem -Path $ItemsPath -Recurse -Filter *.xml | ForEach-Object {

    Select-String -Path $_.FullName -Pattern "<item.*name=.*$ItemName" | ForEach-Object {

        $line = $_.Line

        # Item ID
        $itemId = if ($line -match 'id="(\d+)"') { $matches[1] } else { "N/A" }

        # Item Name
        $itemNameFound = if ($line -match 'name="([^"]+)"') { $matches[1] } else { "N/A" }

        # Description
        $itemDesc = if ($line -match 'description="([^"]+)"') { $matches[1] } else { "No description" }

        Write-Host "  ID   :" -NoNewline -ForegroundColor Yellow
        Write-Host " $itemId" -ForegroundColor DarkGreen

        Write-Host "  Name :" -NoNewline -ForegroundColor Yellow
        Write-Host " $itemNameFound" -ForegroundColor Cyan

        Write-Host "  Desc :" -NoNewline -ForegroundColor DarkGray
        Write-Host " $itemDesc" -ForegroundColor DarkGray

        Write-Host "----------------------------------------" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "=== Search finished ===" -ForegroundColor Cyan
