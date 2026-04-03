Write-Host "Buildando narrative-api..." -ForegroundColor Cyan

mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0)
{
    Write-Host "ERRO: Maven falhou!" -ForegroundColor Red
    exit 1
}

docker build -t xcorpiiion/narrative-api:latest .

if ($LASTEXITCODE -eq 0)
{
    Write-Host "narrative-api buildada com sucesso!" -ForegroundColor Green
}
else
{
    Write-Host "ERRO: Docker build falhou!" -ForegroundColor Red
}