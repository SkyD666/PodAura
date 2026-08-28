#Requires -Version 5.1

<#
.SYNOPSIS
Creates, signs, installs, and removes the PodAura development MSIX package.

.DESCRIPTION
Manages a local self-signed certificate for PodAura MSIX development. Signing
material is protected for the current Windows user and stored outside the
repository under LocalAppData. Actions that modify machine certificate stores
automatically request administrator access through UAC.

.PARAMETER Action
CreateCertificate creates and trusts signing material. Build creates a signed
MSIX. Install builds and installs it. Uninstall removes only the app package.
RemoveCertificate removes only signing trust and files, and refuses while the
app is installed. Cleanup uninstalls the app and removes signing material.
Status reports package, artifact, signature, and certificate state.

.PARAMETER ForceNewCertificate
Removes the tracked development certificate and creates a new one before the
selected action.

.PARAMETER Launch
Launches PodAura after a successful Install action.

.EXAMPLE
.\scripts\msix-dev.ps1 Build

.EXAMPLE
.\scripts\msix-dev.ps1 Install -Launch

.EXAMPLE
.\scripts\msix-dev.ps1 Cleanup
#>

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet(
        "CreateCertificate",
        "Build",
        "Install",
        "Uninstall",
        "RemoveCertificate",
        "Cleanup",
        "Status"
    )]
    [string]$Action = "Build",

    [switch]$ForceNewCertificate,

    [switch]$Launch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($env:OS -ne "Windows_NT") {
    throw "MSIX development signing is only supported on Windows."
}

$ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$GradleWrapper = Join-Path $ProjectRoot "gradlew.bat"
$MsixPath = Join-Path $ProjectRoot "shared\build\PodAura.msix"
$Publisher = "CN=A899BB3F-B2EE-4733-BFE7-45715FA85273"
$PackageIdentityName = "SkyD666.PodAura"
$ApplicationId = "PodAura"
$CertificateFriendlyName = "PodAura MSIX Development"
$SigningDirectory = Join-Path $env:LOCALAPPDATA "PodAura\MsixSigning"
$PfxPath = Join-Path $SigningDirectory "PodAura-development.pfx"
$CerPath = Join-Path $SigningDirectory "PodAura-development.cer"
$CredentialPath = Join-Path $SigningDirectory "PodAura-development.credential.xml"
$MetadataPath = Join-Path $SigningDirectory "PodAura-development.json"

function Write-Step {
    param([Parameter(Mandatory = $true)][string]$Message)

    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator
    )
}

function Invoke-Elevated {
    $switchArguments = ""
    if ($ForceNewCertificate) {
        $switchArguments += " -ForceNewCertificate"
    }
    if ($Launch) {
        $switchArguments += " -Launch"
    }

    Write-Host "Administrator access is required. Approve the Windows UAC prompt."
    $logId = [Guid]::NewGuid().ToString("N")
    $outputLog = Join-Path $env:TEMP "PodAura-msix-dev-$logId.log"
    $escapedScriptPath = $PSCommandPath.Replace("'", "''")
    $escapedOutputLog = $outputLog.Replace("'", "''")
    $elevatedCommand = @"
`$ErrorActionPreference = "Stop"
try {
    & '$escapedScriptPath' -Action '$Action'$switchArguments *>&1 |
        Out-File -LiteralPath '$escapedOutputLog' -Encoding UTF8
    exit 0
} catch {
    (`$_ | Out-String) |
        Add-Content -LiteralPath '$escapedOutputLog' -Encoding UTF8
    exit 1
}
"@
    $encodedCommand = [Convert]::ToBase64String(
        [Text.Encoding]::Unicode.GetBytes($elevatedCommand)
    )
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-EncodedCommand",
        $encodedCommand
    )

    try {
        $process = Start-Process `
            -FilePath "powershell.exe" `
            -Verb RunAs `
            -ArgumentList $arguments `
            -WorkingDirectory $ProjectRoot `
            -Wait `
            -PassThru

        if (Test-Path -LiteralPath $outputLog) {
            $output = Get-Content -Raw -LiteralPath $outputLog
            if (-not [string]::IsNullOrWhiteSpace($output)) {
                Write-Host $output
            }
        }

        $exitCode = $process.ExitCode
    } finally {
        if (Test-Path -LiteralPath $outputLog) {
            Remove-Item -LiteralPath $outputLog -Force
        }
    }

    exit $exitCode
}

function ConvertTo-PlainText {
    param(
        [Parameter(Mandatory = $true)]
        [Security.SecureString]$SecureString
    )

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function New-RandomSecureString {
    $bytes = New-Object byte[] 32
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }

    $password = [Convert]::ToBase64String($bytes)
    return ConvertTo-SecureString -String $password -AsPlainText -Force
}

function Get-StoredCredential {
    if (-not (Test-Path -LiteralPath $CredentialPath)) {
        return $null
    }

    $credential = Import-Clixml -LiteralPath $CredentialPath
    if ($credential -isnot [PSCredential]) {
        throw "Invalid signing credential file: $CredentialPath"
    }

    return $credential
}

function Get-CertificateFromPfx {
    param(
        [Parameter(Mandatory = $true)]
        [Security.SecureString]$Password
    )

    $plainPassword = ConvertTo-PlainText -SecureString $Password
    try {
        return [Security.Cryptography.X509Certificates.X509Certificate2]::new(
            $PfxPath,
            $plainPassword,
            [Security.Cryptography.X509Certificates.X509KeyStorageFlags]::Exportable
        )
    } finally {
        $plainPassword = $null
    }
}

function Get-TrackedCertificateThumbprints {
    $thumbprints = [Collections.Generic.HashSet[string]]::new(
        [StringComparer]::OrdinalIgnoreCase
    )

    if (Test-Path -LiteralPath $MetadataPath) {
        try {
            $metadata = Get-Content -Raw -LiteralPath $MetadataPath | ConvertFrom-Json
            if ($metadata.Thumbprint) {
                [void]$thumbprints.Add([string]$metadata.Thumbprint)
            }
        } catch {
            Write-Warning "Could not read certificate metadata: $($_.Exception.Message)"
        }
    }

    if ((Test-Path -LiteralPath $PfxPath) -and
        (Test-Path -LiteralPath $CredentialPath)) {
        try {
            $credential = Get-StoredCredential
            $certificate = Get-CertificateFromPfx -Password $credential.Password
            try {
                [void]$thumbprints.Add($certificate.Thumbprint)
            } finally {
                $certificate.Reset()
            }
        } catch {
            Write-Warning "Could not inspect the existing PFX: $($_.Exception.Message)"
        }
    }

    $store = [Security.Cryptography.X509Certificates.X509Store]::new(
        "My",
        [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
    )
    try {
        $store.Open([Security.Cryptography.X509Certificates.OpenFlags]::ReadOnly)
        foreach ($certificate in $store.Certificates) {
            if ($certificate.Subject -eq $Publisher -and
                $certificate.FriendlyName -eq $CertificateFriendlyName) {
                [void]$thumbprints.Add($certificate.Thumbprint)
            }
        }
    } finally {
        $store.Close()
    }

    return @($thumbprints | ForEach-Object { $_ })
}

function Remove-DevelopmentCertificates {
    param([string[]]$Thumbprints)

    if (@($Thumbprints).Count -eq 0) {
        return
    }

    $targets = @(
        [PSCustomObject]@{
            Name = "My"
            Location = [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
        },
        [PSCustomObject]@{
            Name = "TrustedPeople"
            Location = [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
        },
        [PSCustomObject]@{
            Name = "Root"
            Location = [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
        },
        [PSCustomObject]@{
            Name = "My"
            Location = [Security.Cryptography.X509Certificates.StoreLocation]::LocalMachine
        },
        [PSCustomObject]@{
            Name = "TrustedPeople"
            Location = [Security.Cryptography.X509Certificates.StoreLocation]::LocalMachine
        },
        [PSCustomObject]@{
            Name = "Root"
            Location = [Security.Cryptography.X509Certificates.StoreLocation]::LocalMachine
        }
    )

    foreach ($target in $targets) {
        $locationName = if ($target.Location -eq
            [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser) {
            "CurrentUser"
        } else {
            "LocalMachine"
        }
        $storePath = "Cert:\$locationName\$($target.Name)"

        foreach ($thumbprint in $Thumbprints) {
            $match = Get-ChildItem -LiteralPath $storePath -ErrorAction Stop |
                Where-Object { $_.Thumbprint -eq $thumbprint }
            if ($null -eq $match) {
                continue
            }

            $arguments = if ($locationName -eq "CurrentUser") {
                @("-user", "-delstore", $target.Name, $thumbprint)
            } else {
                @("-delstore", $target.Name, $thumbprint)
            }
            & certutil.exe @arguments | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "Could not remove certificate $thumbprint from $storePath."
            }
        }
    }
}

function Remove-SigningFiles {
    foreach ($path in @($PfxPath, $CerPath, $CredentialPath, $MetadataPath)) {
        if (Test-Path -LiteralPath $path) {
            Remove-Item -LiteralPath $path -Force
        }
    }

    if (Test-Path -LiteralPath $SigningDirectory) {
        $remainingFiles = @(Get-ChildItem -LiteralPath $SigningDirectory -Force)
        if ($remainingFiles.Count -eq 0) {
            Remove-Item -LiteralPath $SigningDirectory -Force
        }
    }
}

function New-SigningMaterial {
    Write-Step "Creating a self-signed MSIX development certificate"
    New-Item -ItemType Directory -Force -Path $SigningDirectory | Out-Null

    $securePassword = New-RandomSecureString
    $certificate = New-SelfSignedCertificate `
        -Type Custom `
        -Subject $Publisher `
        -FriendlyName $CertificateFriendlyName `
        -CertStoreLocation "Cert:\CurrentUser\My" `
        -KeyUsage DigitalSignature `
        -KeyExportPolicy Exportable `
        -KeyAlgorithm RSA `
        -KeyLength 2048 `
        -HashAlgorithm SHA256 `
        -NotAfter (Get-Date).AddYears(2) `
        -TextExtension @(
            "2.5.29.37={text}1.3.6.1.5.5.7.3.3",
            "2.5.29.19={text}"
        )

    Export-PfxCertificate `
        -Cert $certificate `
        -FilePath $PfxPath `
        -Password $securePassword | Out-Null
    Export-Certificate `
        -Cert $certificate `
        -FilePath $CerPath | Out-Null

    [PSCredential]::new("PodAura MSIX", $securePassword) |
        Export-Clixml -LiteralPath $CredentialPath
    [PSCustomObject]@{
        Subject = $certificate.Subject
        Thumbprint = $certificate.Thumbprint
        NotAfter = $certificate.NotAfter.ToString("o")
    } | ConvertTo-Json | Set-Content -LiteralPath $MetadataPath -Encoding UTF8

    return [PSCustomObject]@{
        Certificate = $certificate
        Credential = [PSCredential]::new("PodAura MSIX", $securePassword)
    }
}

function Get-SigningMaterial {
    $requiredFiles = @($PfxPath, $CerPath, $CredentialPath, $MetadataPath)
    $missingFiles = @($requiredFiles | Where-Object {
        -not (Test-Path -LiteralPath $_)
    })
    $hasAllFiles = $missingFiles.Count -eq 0

    if ($ForceNewCertificate -or -not $hasAllFiles) {
        $existingThumbprints = @(Get-TrackedCertificateThumbprints)
        Remove-DevelopmentCertificates -Thumbprints $existingThumbprints
        Remove-SigningFiles
        return New-SigningMaterial
    }

    $credential = Get-StoredCredential
    $certificate = Get-CertificateFromPfx -Password $credential.Password
    if ($certificate.Subject -ne $Publisher) {
        throw "The PFX publisher does not match the MSIX manifest. Use -ForceNewCertificate."
    }
    if ($certificate.NotAfter -le (Get-Date).AddDays(30)) {
        throw "The development certificate expires soon. Use -ForceNewCertificate."
    }

    return [PSCustomObject]@{
        Certificate = $certificate
        Credential = $credential
    }
}

function Add-DevelopmentCertificateTrust {
    param(
        [Parameter(Mandatory = $true)]
        [Security.Cryptography.X509Certificates.X509Certificate2]$Certificate
    )

    Write-Step "Trusting certificate $($Certificate.Thumbprint) on this development machine"
    foreach ($storeName in @("TrustedPeople", "Root")) {
        $storePath = "Cert:\LocalMachine\$storeName"
        $existing = Get-ChildItem -LiteralPath $storePath |
            Where-Object { $_.Thumbprint -eq $Certificate.Thumbprint }
        if ($null -eq $existing) {
            Import-Certificate `
                -FilePath $CerPath `
                -CertStoreLocation $storePath | Out-Null
        }
    }
}

function Find-SignTool {
    $fromPath = Get-Command "signtool.exe" -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) {
        return $fromPath.Source
    }

    $kitsRoot = ${env:ProgramFiles(x86)}
    $candidates = @()
    foreach ($kitVersion in @("10", "11")) {
        $binRoot = Join-Path $kitsRoot "Windows Kits\$kitVersion\bin"
        if (Test-Path -LiteralPath $binRoot) {
            $candidates += @(Get-ChildItem `
                -Path (Join-Path $binRoot "*\x64\signtool.exe") `
                -File `
                -ErrorAction SilentlyContinue)
        }
    }

    $selected = $candidates | Sort-Object {
        try {
            [Version]$_.Directory.Parent.Name
        } catch {
            [Version]"0.0"
        }
    } -Descending | Select-Object -First 1

    if ($null -eq $selected) {
        throw "signtool.exe was not found. Install the Windows SDK."
    }

    return $selected.FullName
}

function Invoke-SignedMsixBuild {
    param(
        [Parameter(Mandatory = $true)]
        $SigningMaterial
    )

    Write-Step "Building and signing PodAura.msix"
    $plainPassword = ConvertTo-PlainText -SecureString $SigningMaterial.Credential.Password
    $oldPfx = [Environment]::GetEnvironmentVariable(
        "MSIX_SIGN_PFX_BASE64",
        "Process"
    )
    $oldPassword = [Environment]::GetEnvironmentVariable(
        "MSIX_SIGN_PFX_PASSWORD",
        "Process"
    )

    try {
        [Environment]::SetEnvironmentVariable(
            "MSIX_SIGN_PFX_BASE64",
            [Convert]::ToBase64String([IO.File]::ReadAllBytes($PfxPath)),
            "Process"
        )
        [Environment]::SetEnvironmentVariable(
            "MSIX_SIGN_PFX_PASSWORD",
            $plainPassword,
            "Process"
        )

        # The plugin does not declare its signing environment variables as task inputs.
        if (Test-Path -LiteralPath $MsixPath) {
            Remove-Item -LiteralPath $MsixPath -Force
        }

        Push-Location $ProjectRoot
        try {
            & $GradleWrapper ":shared:createMsix" "--console=plain"
            if ($LASTEXITCODE -ne 0) {
                throw "Gradle MSIX build failed with exit code $LASTEXITCODE."
            }
        } finally {
            Pop-Location
        }
    } finally {
        [Environment]::SetEnvironmentVariable(
            "MSIX_SIGN_PFX_BASE64",
            $oldPfx,
            "Process"
        )
        [Environment]::SetEnvironmentVariable(
            "MSIX_SIGN_PFX_PASSWORD",
            $oldPassword,
            "Process"
        )
        $plainPassword = $null
    }

    if (-not (Test-Path -LiteralPath $MsixPath)) {
        throw "MSIX output was not created: $MsixPath"
    }

    $signTool = Find-SignTool
    & $signTool verify /pa $MsixPath
    if ($LASTEXITCODE -ne 0) {
        throw "MSIX signature verification failed with exit code $LASTEXITCODE."
    }

    Write-Host "Signed MSIX: $MsixPath" -ForegroundColor Green
}

function Remove-InstalledPackage {
    $packages = @(Get-AppxPackage -Name $PackageIdentityName -ErrorAction SilentlyContinue)
    foreach ($package in $packages) {
        Write-Step "Uninstalling $($package.PackageFullName)"
        Remove-AppxPackage -Package $package.PackageFullName -ErrorAction Stop
    }

    if ($packages.Count -eq 0) {
        Write-Host "PodAura MSIX is not installed."
    }
}

function Install-MsixPackage {
    Remove-InstalledPackage
    Write-Step "Installing PodAura.msix"
    Add-AppxPackage -Path $MsixPath -ErrorAction Stop

    $package = Get-AppxPackage -Name $PackageIdentityName -ErrorAction Stop
    Write-Host "Installed: $($package.PackageFullName)" -ForegroundColor Green

    if ($Launch) {
        Write-Step "Launching PodAura"
        Start-Process `
            -FilePath "explorer.exe" `
            -ArgumentList "shell:AppsFolder\$($package.PackageFamilyName)!$ApplicationId"
    }
}

function Remove-CertificateAndFiles {
    $thumbprints = @(Get-TrackedCertificateThumbprints)
    if ($thumbprints.Count -gt 0) {
        Write-Step "Removing PodAura development certificates"
        Remove-DevelopmentCertificates -Thumbprints $thumbprints
    } else {
        Write-Host "No tracked PodAura development certificates were found."
    }

    Remove-SigningFiles
    Write-Host "Removed signing files from $SigningDirectory" -ForegroundColor Green
}

function Show-Status {
    $thumbprints = @(Get-TrackedCertificateThumbprints)
    $package = Get-AppxPackage -Name $PackageIdentityName -ErrorAction SilentlyContinue
    $signature = if (Test-Path -LiteralPath $MsixPath) {
        Get-AuthenticodeSignature -LiteralPath $MsixPath
    } else {
        $null
    }

    [PSCustomObject]@{
        PackageInstalled = ($null -ne $package)
        PackageFullName = if ($null -ne $package) { $package.PackageFullName } else { $null }
        MsixExists = (Test-Path -LiteralPath $MsixPath)
        MsixSignatureStatus = if ($null -ne $signature) {
            $signature.Status.ToString()
        } else {
            $null
        }
        MsixSigner = if ($null -ne $signature -and
            $null -ne $signature.SignerCertificate) {
            $signature.SignerCertificate.Subject
        } else {
            $null
        }
        SigningFilesExist = (Test-Path -LiteralPath $PfxPath)
        CertificateThumbprints = $thumbprints -join ", "
        SigningDirectory = $SigningDirectory
    } | Format-List
}

$requiresElevation = $Action -in @(
    "CreateCertificate",
    "Build",
    "Install",
    "RemoveCertificate",
    "Cleanup"
)
if ($requiresElevation -and -not (Test-IsAdministrator)) {
    Invoke-Elevated
}

switch ($Action) {
    "CreateCertificate" {
        $material = Get-SigningMaterial
        Add-DevelopmentCertificateTrust -Certificate $material.Certificate
        Write-Host "Certificate ready: $($material.Certificate.Thumbprint)" -ForegroundColor Green
    }
    "Build" {
        $material = Get-SigningMaterial
        Add-DevelopmentCertificateTrust -Certificate $material.Certificate
        Invoke-SignedMsixBuild -SigningMaterial $material
    }
    "Install" {
        $material = Get-SigningMaterial
        Add-DevelopmentCertificateTrust -Certificate $material.Certificate
        Invoke-SignedMsixBuild -SigningMaterial $material
        Install-MsixPackage
    }
    "Uninstall" {
        Remove-InstalledPackage
    }
    "RemoveCertificate" {
        $installed = Get-AppxPackage -Name $PackageIdentityName -ErrorAction SilentlyContinue
        if ($null -ne $installed) {
            throw "PodAura is installed. Use the Cleanup action to uninstall it before removing trust."
        }
        Remove-CertificateAndFiles
    }
    "Cleanup" {
        Remove-InstalledPackage
        Remove-CertificateAndFiles
    }
    "Status" {
        Show-Status
    }
}
