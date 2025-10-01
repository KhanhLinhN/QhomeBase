@echo off
echo Starting QhomeBaseApp services...
docker-compose -f docker-compose.yml up --build -d
echo All services started.
pause