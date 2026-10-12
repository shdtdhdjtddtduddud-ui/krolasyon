#!/bin/sh
# daily backup of the database and uploaded files (run from the project folder, e.g. via cron)
set -e
mkdir -p backups
STAMP=$(date +%Y-%m-%d)
docker compose exec -T app node -e "require('better-sqlite3')('/data/site.db').backup('/data/backup.db').then(()=>console.log('db ok'))"
tar czf "backups/site-$STAMP.tar.gz" data/backup.db data/files data/media
rm -f data/backup.db
# keep the last 14 backups
ls -1t backups/site-*.tar.gz | tail -n +15 | xargs -r rm -f
echo "backup written: backups/site-$STAMP.tar.gz"
