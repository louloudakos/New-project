param(
    [string]$SkillName
)

# Χρώματα
$colorID = "Yellow"
$colorName = "Cyan"
$colorType = "Green"
$colorLevels = "Magenta"
$colorDesc = "DarkGray"

Write-Host "`nSearching for '$SkillName'..." -ForegroundColor Cyan

# Βρες όλα τα XML αρχεία
$Files = Get-ChildItem -Path . -Filter *.xml

foreach ($file in $Files) {
    [xml]$xml = Get-Content $file

    foreach ($skill in $xml.SelectNodes("//skill[contains(@name,'$SkillName')]")) {

        $id = $skill.id
        $name = $skill.name
        $levels = $skill.levels

        # Διάκριση αν είναι buff ή skill
        $type = "Skill"
        if ($skill.set | Where-Object { $_.name -eq "abnormalType" }) {
            $type = "Buff"
        }

        # Μικρή περιγραφή: μόνο από το πρώτο comment του skill
        $descNode = $skill.ChildNodes | Where-Object { $_.NodeType -eq "Comment" } | Select-Object -First 1
        $description = ""
        if ($descNode) {
            $description = $descNode.Data.Trim()
        }

        # Εμφάνιση
        Write-Host "ID: $id" -ForegroundColor $colorID -NoNewline
        Write-Host " | Name: $name" -ForegroundColor $colorName -NoNewline
        Write-Host " | Type: $type" -ForegroundColor $colorType -NoNewline
        Write-Host " | Levels: $levels" -ForegroundColor $colorLevels
        if ($description -ne "") {
            Write-Host "Description: $description" -ForegroundColor $colorDesc
        }

        Write-Host "`n" # Κενή γραμμή για διαχωρισμό
    }
}

Write-Host "Search complete." -ForegroundColor Cyan
