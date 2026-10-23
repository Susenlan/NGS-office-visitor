$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$toolRoot = Join-Path $projectRoot '.tools'
$jdkRoot = (Get-ChildItem (Join-Path $toolRoot 'jdk') -Directory | Select-Object -First 1).FullName
$sdkRoot = Join-Path $toolRoot 'android-sdk'
$androidJar = Join-Path $sdkRoot 'platforms\android-35\android.jar'
$buildTools = Join-Path $sdkRoot 'build-tools\35.0.0'
$zxing = Join-Path $toolRoot 'libs\zxing-core-3.5.3.jar'
$junit = Join-Path $toolRoot 'libs\junit-4.13.2.jar'
$hamcrest = Join-Path $toolRoot 'libs\hamcrest-core-1.3.jar'

$required = @(
    (Join-Path $jdkRoot 'bin\javac.exe'),
    $androidJar,
    (Join-Path $buildTools 'aapt2.exe'),
    (Join-Path $buildTools 'd8.bat'),
    (Join-Path $buildTools 'zipalign.exe'),
    (Join-Path $buildTools 'apksigner.bat'),
    $zxing,
    $junit,
    $hamcrest
)
foreach ($item in $required) {
    if (-not (Test-Path -LiteralPath $item)) {
        throw "Missing build dependency: $item"
    }
}

$buildRoot = Join-Path $toolRoot 'manual-build'
$resolvedToolRoot = (Resolve-Path $toolRoot).Path
if (Test-Path -LiteralPath $buildRoot) {
    $resolvedBuildRoot = (Resolve-Path $buildRoot).Path
    if (-not $resolvedBuildRoot.StartsWith($resolvedToolRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean build directory outside .tools: $resolvedBuildRoot"
    }
    Remove-Item -LiteralPath $resolvedBuildRoot -Recurse -Force
}

$compiledRes = Join-Path $buildRoot 'compiled-res.zip'
$generated = Join-Path $buildRoot 'generated'
$classes = Join-Path $buildRoot 'classes'
$testClasses = Join-Path $buildRoot 'test-classes'
$dex = Join-Path $buildRoot 'dex'
$unsignedApk = Join-Path $buildRoot 'app-unsigned.apk'
$alignedApk = Join-Path $buildRoot 'app-aligned.apk'
$debugApk = Join-Path $buildRoot 'NGS-Visitor-debug.apk'
$keystore = Join-Path $toolRoot 'debug.keystore'
$outputDir = Join-Path $projectRoot 'output\android'

New-Item -ItemType Directory -Force $buildRoot,$generated,$classes,$testClasses,$dex,$outputDir | Out-Null

$env:JAVA_HOME = $jdkRoot
$env:Path = (Join-Path $jdkRoot 'bin') + ';' + $env:Path

$aapt2 = Join-Path $buildTools 'aapt2.exe'
& $aapt2 compile --dir (Join-Path $projectRoot 'app\src\main\res') -o $compiledRes
if ($LASTEXITCODE -ne 0) { throw 'aapt2 resource compilation failed' }

& $aapt2 link `
    -o $unsignedApk `
    -I $androidJar `
    --manifest (Join-Path $projectRoot 'app\src\main\AndroidManifest.xml') `
    --java $generated `
    --min-sdk-version 26 `
    --target-sdk-version 35 `
    --version-code 1 `
    --version-name '1.0.0' `
    --auto-add-overlay `
    $compiledRes
if ($LASTEXITCODE -ne 0) { throw 'aapt2 resource linking failed' }

$sourceFiles = @(
    Get-ChildItem (Join-Path $projectRoot 'app\src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName
)
$sourceFiles += Get-ChildItem $generated -Recurse -Filter '*.java' | ForEach-Object FullName
$sourceList = Join-Path $buildRoot 'main-sources.txt'
$sourceFiles | ForEach-Object { '"' + $_.Replace('\', '/') + '"' } |
    Set-Content -LiteralPath $sourceList -Encoding UTF8

$javac = Join-Path $jdkRoot 'bin\javac.exe'
& $javac -encoding UTF-8 -source 17 -target 17 -classpath "$androidJar;$zxing" -d $classes "@$sourceList"
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }

$appJar = Join-Path $buildRoot 'app-classes.jar'
$jar = Join-Path $jdkRoot 'bin\jar.exe'
& $jar --create --file $appJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Class archive creation failed' }

$d8 = Join-Path $buildTools 'd8.bat'
& $d8 --lib $androidJar --min-api 26 --output $dex $appJar $zxing
if ($LASTEXITCODE -ne 0) { throw 'D8 dex compilation failed' }

Get-ChildItem $dex -Filter '*.dex' | ForEach-Object {
    & $jar --update --file $unsignedApk -C $dex $_.Name
    if ($LASTEXITCODE -ne 0) { throw "Failed to add $($_.Name) to APK" }
}

$zipalign = Join-Path $buildTools 'zipalign.exe'
& $zipalign -p -f 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed' }

if (-not (Test-Path -LiteralPath $keystore)) {
    $keytool = Join-Path $jdkRoot 'bin\keytool.exe'
    & $keytool -genkeypair -v -keystore $keystore -storepass android -alias androiddebugkey `
        -keypass android -dname 'CN=Android Debug,O=NGS,C=CN' -keyalg RSA -keysize 2048 `
        -validity 10000
    if ($LASTEXITCODE -ne 0) { throw 'Debug keystore creation failed' }
}

$apksigner = Join-Path $buildTools 'apksigner.bat'
& $apksigner sign --ks $keystore --ks-key-alias androiddebugkey --ks-pass pass:android `
    --key-pass pass:android --out $debugApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }

& $apksigner verify --verbose --print-certs $debugApk
if ($LASTEXITCODE -ne 0) { throw 'APK verification failed' }

$testFiles = Get-ChildItem (Join-Path $projectRoot 'app\src\test\java') -Recurse -Filter '*.java' |
    ForEach-Object FullName
$testList = Join-Path $buildRoot 'test-sources.txt'
$testFiles | ForEach-Object { '"' + $_.Replace('\', '/') + '"' } |
    Set-Content -LiteralPath $testList -Encoding UTF8
$testClasspath = "$classes;$androidJar;$zxing;$junit;$hamcrest"
& $javac -encoding UTF-8 -source 17 -target 17 -classpath $testClasspath -d $testClasses "@$testList"
if ($LASTEXITCODE -ne 0) { throw 'Unit test compilation failed' }

$java = Join-Path $jdkRoot 'bin\java.exe'
& $java -classpath "$testClasses;$testClasspath" org.junit.runner.JUnitCore `
    com.nordgas.ngsvisitor.VoucherTest `
    com.nordgas.ngsvisitor.ApiUrlTest `
    com.nordgas.ngsvisitor.UniFiApiClientTest `
    com.nordgas.ngsvisitor.PasswordHasherTest
if ($LASTEXITCODE -ne 0) { throw 'Unit tests failed' }

$finalApk = Join-Path $outputDir 'NGS-Visitor-debug.apk'
Copy-Item -LiteralPath $debugApk -Destination $finalApk -Force
Write-Output "APK: $finalApk"
