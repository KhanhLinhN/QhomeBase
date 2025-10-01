@echo off
echo Stopping QhomeBaseApp services...
docker-compose -f docker-compose.yml down
echo All services stopped.
pause