#
# CampusHub 本地数据库辅助脚本
#
# 这个脚本主要做三件事：
# 1. 找到你电脑里的 mysql 客户端
# 2. 执行建库建表脚本
# 3. 按需要导入或重置测试数据
#
# 常见用法：
#   .\scripts\init-db.ps1 -Username root -Password "你的密码"
#   .\scripts\init-db.ps1 -Mode reset-seed -Username root -Password "你的密码"
#

param(
    # 模式说明：
    # - init：初始化数据库，执行建表并导入初始测试数据
    # - reset-seed：清空当前开发测试数据，再重新导入固定测试数据
    [ValidateSet("init", "reset-seed")]
    [string]$Mode = "init",

    # mysql.exe 的完整路径。
    # 如果 MySQL 安装在常见位置，通常不用手动填写。
    [string]$MySqlExePath = "",

    # 数据库主机地址，本地开发一般就是 127.0.0.1。
    [string]$DbHost = "127.0.0.1",

    # MySQL 默认端口。
    [int]$Port = 3306,

    # MySQL 用户名，本地开发通常是 root。
    [string]$Username = "root",

    # MySQL 密码。
    # 如果你的本地 root 没有密码，这里可以留空。
    [string]$Password = "",

    # 存放数据库脚本的目录。
    # 一般不用改，默认就是仓库里的 database 目录。
    [string]$DatabaseDir = ""
)

# 脚本中只要有一步失败，就立刻停止，避免后面继续执行。
$ErrorActionPreference = "Stop"

function Resolve-MySqlExe {
    param([string]$Candidate)

    # 先尝试使用用户手动传入的 mysql.exe 路径。
    if ($Candidate -and (Test-Path -LiteralPath $Candidate)) {
        return (Resolve-Path -LiteralPath $Candidate).Path
    }

    # 再尝试从系统 PATH 环境变量里找 mysql.exe。
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    # 最后再尝试几个常见的 Windows 安装路径。
    $commonPaths = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe",
        "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
    )

    foreach ($path in $commonPaths) {
        if (Test-Path -LiteralPath $path) {
            return $path
        }
    }

    throw "mysql.exe was not found. Pass -MySqlExePath explicitly."
}

function Invoke-SqlFile {
    param(
        [string]$MySqlExe,
        [string]$SqlFile,
        [string]$DbHost,
        [int]$DbPort,
        [string]$DbUser
    )

    if (-not (Test-Path -LiteralPath $SqlFile)) {
        throw "SQL file was not found: $SqlFile"
    }

    # 把整个 SQL 文件一次性读出来，再交给 mysql 客户端执行。
    # 这样就不用每次手动打开 Workbench 去运行 SQL。
    Write-Host "Running: $SqlFile"
    Get-Content -LiteralPath $SqlFile -Raw | & $MySqlExe --default-character-set=utf8mb4 -h $DbHost -P $DbPort -u $DbUser

    if ($LASTEXITCODE -ne 0) {
        throw "mysql exited with code $LASTEXITCODE while running: $SqlFile"
    }
}

# 如果没有传数据库目录，就默认使用仓库里的 database 目录。
if (-not $DatabaseDir) {
    $DatabaseDir = Join-Path $PSScriptRoot "..\database"
}

# 生成这次要用到的 SQL 文件绝对路径。
$databaseDirResolved = (Resolve-Path -LiteralPath $DatabaseDir).Path
$schemaPath = Join-Path $databaseDirResolved "01-schema.sql"
$seedPath = Join-Path $databaseDirResolved "02-seed.sql"
$resetPath = Join-Path $databaseDirResolved "03-reset-dev-data.sql"

# 先找到 mysql.exe，后面执行 SQL 时直接复用。
$mysqlExe = Resolve-MySqlExe -Candidate $MySqlExePath

# 把密码临时放进环境变量 MYSQL_PWD。
# 这样 mysql 客户端执行时就不用再手动输入密码。
$env:MYSQL_PWD = $Password
try {
    Write-Host "Using mysql client: $mysqlExe"
    Write-Host "Mode: $Mode"

    switch ($Mode) {
        "init" {
            # 首次初始化：
            # 1. 建库建表
            # 2. 导入初始测试数据
            Invoke-SqlFile -MySqlExe $mysqlExe -SqlFile $schemaPath -DbHost $DbHost -DbPort $Port -DbUser $Username
            Invoke-SqlFile -MySqlExe $mysqlExe -SqlFile $seedPath -DbHost $DbHost -DbPort $Port -DbUser $Username
            Write-Host "Database init completed."
        }
        "reset-seed" {
            # 开发环境重置：
            # 1. 删除当前测试数据
            # 2. 重新导入固定的测试数据
            Invoke-SqlFile -MySqlExe $mysqlExe -SqlFile $resetPath -DbHost $DbHost -DbPort $Port -DbUser $Username
            Invoke-SqlFile -MySqlExe $mysqlExe -SqlFile $seedPath -DbHost $DbHost -DbPort $Port -DbUser $Username
            Write-Host "Database seed reset completed."
        }
    }
}
finally {
    # 脚本结束后，把临时密码环境变量清掉。
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
