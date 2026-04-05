$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$targetDir = Join-Path $projectRoot "sample-docs\public"

$sources = @(
    @{
        FileName = "系统操作手册（企业）.pdf"
        Url = "https://www.gov.cn/zhengce/zhengceku/202402/P020240228395138681223.pdf"
    },
    @{
        FileName = "全面数字化的电子发票常见问题即问即答（适用纳税人）.pdf"
        Url = "https://static.tpass.chinatax.gov.cn/znhd/znzsNsrd/gy/v1/download/upload/material/148/20241125/7B1361A9A2F64120B461A015D530C2F7.pdf"
    },
    @{
        FileName = "企业开办一网通办系统操作手册.pdf"
        Url = "https://qykb.gdzwfw.gov.cn/qcdzhdj/static/%E4%BC%81%E4%B8%9A%E5%BC%80%E5%8A%9E%E4%B8%80%E7%BD%91%E9%80%9A%E5%8A%9E%E6%93%8D%E4%BD%9C%E6%89%8B%E5%86%8C.pdf"
    }
)

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

foreach ($source in $sources) {
    $targetPath = Join-Path $targetDir $source.FileName
    Write-Host "Downloading $($source.FileName) ..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $source.Url -OutFile $targetPath
}

Write-Host "Public source documents have been restored to $targetDir" -ForegroundColor Green
