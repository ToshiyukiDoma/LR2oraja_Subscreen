param([string]$GameDirectory = (Resolve-Path "$PSScriptRoot\..\..\..").Path)
$ErrorActionPreference = 'Stop'
$java = Join-Path $GameDirectory 'jre\bin\java.exe'
$buildDirectory = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force $buildDirectory | Out-Null
& $java -jar "$PSScriptRoot\ecj.jar" -17 -encoding UTF-8 -cp "$GameDirectory\beatoraja.jar;$PSScriptRoot\asm.jar" -d $buildDirectory "$PSScriptRoot\Touch.java" "$PSScriptRoot\ProfileRestart.java" "$PSScriptRoot\TouchWidgets.java" "$PSScriptRoot\GameMonitor.java" "$PSScriptRoot\GameSettings.java" "$PSScriptRoot\Backgrounds.java" "$PSScriptRoot\SkinPanel.java" "$PSScriptRoot\AudioControl.java" "$PSScriptRoot\AudioHooks.java"
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jarPath = [IO.Path]::GetFullPath("$PSScriptRoot\..\lr2touch.jar")
$temporaryJar = "$jarPath.new"
if (Test-Path -LiteralPath $temporaryJar) { Remove-Item -LiteralPath $temporaryJar }
$zip = [IO.Compression.ZipFile]::Open($temporaryJar, [IO.Compression.ZipArchiveMode]::Create)
try {
 $entry = $zip.CreateEntry('META-INF/MANIFEST.MF')
 $writer = [IO.StreamWriter]::new($entry.Open())
 $writer.Write("Manifest-Version: 1.0`r`nPremain-Class: lr2touch.Touch`r`nMain-Class: lr2touch.Touch`r`n`r`n")
 $writer.Dispose()
 Get-ChildItem -LiteralPath "$buildDirectory\lr2touch" -Filter '*.class' | ForEach-Object {
  [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,('lr2touch/'+$_.Name)) | Out-Null
 }
 $asm = [IO.Compression.ZipFile]::OpenRead("$PSScriptRoot\asm.jar")
 try { foreach ($item in $asm.Entries) { if ($item.FullName.StartsWith('org/objectweb/asm/') -and $item.FullName.EndsWith('.class')) { $copy=$zip.CreateEntry($item.FullName); $input=$item.Open(); $output=$copy.Open(); try { $input.CopyTo($output) } finally { $input.Dispose(); $output.Dispose() } } } } finally { $asm.Dispose() }
} finally { $zip.Dispose() }
Move-Item -LiteralPath $temporaryJar -Destination $jarPath -Force
& $java -jar $jarPath --test
if ($LASTEXITCODE -ne 0) { throw 'Self-test failed' }
