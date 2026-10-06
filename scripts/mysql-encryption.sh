#!/bin/sh
# Encryption at rest for the Diwan databases on the host MySQL (8.0.24+ / 8.4), run ON THE SERVER as root:
#
#   sudo sh scripts/mysql-encryption.sh            # dry run: checks and prints what would change
#   sudo sh scripts/mysql-encryption.sh --apply    # configures the keyring, restarts MySQL, encrypts tables
#
# What it does (with --apply):
#   1. Installs the component_keyring_file keyring (MySQL's master key lives in /var/lib/mysql-keyring).
#   2. Restarts MySQL (a few seconds of downtime: the services reconnect by themselves).
#   3. Turns on encryption of new tables, redo/undo logs and binary logs (SET PERSIST, survives restarts).
#   4. Encrypts every existing table of the diwan_* databases (ALTER TABLE ... ENCRYPTION='Y'; copies the table).
#
# What it protects: the data files, logs and file-level copies/backups of /var/lib/mysql. It does NOT protect
# against someone who can log in to MySQL (they see decrypted rows) or who copies the keyring file too - keep
# /var/lib/mysql-keyring out of file backups and back it up separately and safely: WITHOUT IT THE ENCRYPTED
# TABLES CANNOT BE OPENED. mysqldump output is plaintext; encrypt dumps (e.g. gpg -c) before storing them.
# Sensitive portfolio text (names, notes, addresses) is additionally encrypted by the application
# (DATA_ENCRYPTION_KEYS), which also protects it from database logins and dumps.
#
# Connections: MySQL 8 serves TLS by default (auto-generated certificates); the portfolio service uses
# sslMode=PREFERRED, which encrypts whenever the server offers TLS. After checking that
# "SHOW STATUS LIKE 'Ssl_cipher'" is non-empty for the app user, DB_SSL_MODE=REQUIRED makes it mandatory.
set -eu

APPLY=0
[ "${1:-}" = "--apply" ] && APPLY=1
# Admin login: Ubuntu's maintenance account if present, otherwise e.g. MYSQL_ARGS="--defaults-extra-file=/root/.my.cnf"
if [ -n "${MYSQL_ARGS:-}" ]; then
  MYSQL="mysql $MYSQL_ARGS"
elif [ -r /etc/mysql/debian.cnf ]; then
  MYSQL="mysql --defaults-file=/etc/mysql/debian.cnf"
else
  MYSQL="mysql --protocol=socket -uroot"
fi
KEYRING_DIR=/var/lib/mysql-keyring

run() {
  echo "+ $*"
  if [ "$APPLY" -eq 1 ]; then sh -c "$*"; fi
}
sql() {
  echo "+ SQL: $1"
  if [ "$APPLY" -eq 1 ]; then $MYSQL -e "$1"; fi
}

[ "$(id -u)" -eq 0 ] || { echo "run as root (sudo)"; exit 1; }
VERSION=$($MYSQL -N -e "SELECT VERSION()")
PLUGIN_DIR=$($MYSQL -N -e "SELECT @@plugin_dir")
PLUGIN_DIR=${PLUGIN_DIR%/}
APPARMOR_PROFILE=/etc/apparmor.d/usr.sbin.mysqld
APPARMOR_LOCAL=/etc/apparmor.d/local/usr.sbin.mysqld
MYSQLD=$(command -v mysqld || echo /usr/sbin/mysqld)
echo "MySQL $VERSION, plugin_dir=$PLUGIN_DIR, mysqld=$MYSQLD"

STATUS=$($MYSQL -N -e "SELECT STATUS_VALUE FROM performance_schema.keyring_component_status WHERE STATUS_KEY='Component_status'" 2>/dev/null || true)
if [ "$STATUS" = "Active" ]; then
  echo "keyring component already active"
else
  echo "== 1. keyring component"
  run "mkdir -p $KEYRING_DIR && chown mysql:mysql $KEYRING_DIR && chmod 750 $KEYRING_DIR"
  run "printf '%s\n' '{ \"components\": \"file://component_keyring_file\" }' > ${MYSQLD}.my"
  run "printf '%s\n' '{ \"path\": \"$KEYRING_DIR/component_keyring_file\", \"read_only\": false }' > $PLUGIN_DIR/component_keyring_file.cnf"
  run "chmod 644 ${MYSQLD}.my $PLUGIN_DIR/component_keyring_file.cnf"
  # Ubuntu confines mysqld with AppArmor: allow the manifest, the keyring config and the keyring directory
  if [ -f "$APPARMOR_PROFILE" ]; then
    echo "== 1b. AppArmor rules ($APPARMOR_LOCAL)"
    run "grep -q 'diwan keyring' $APPARMOR_LOCAL 2>/dev/null || printf '%s\n' '# diwan keyring (scripts/mysql-encryption.sh)' '${MYSQLD}.my r,' '$PLUGIN_DIR/component_keyring_file.cnf r,' '$KEYRING_DIR/ rw,' '$KEYRING_DIR/** rwk,' >> $APPARMOR_LOCAL"
    run "apparmor_parser -r $APPARMOR_PROFILE"
  fi
  echo "== 2. restart MySQL"
  run "systemctl restart mysql"
  if [ "$APPLY" -eq 1 ]; then
    STATUS=$($MYSQL -N -e "SELECT STATUS_VALUE FROM performance_schema.keyring_component_status WHERE STATUS_KEY='Component_status'" || true)
    if [ "$STATUS" != "Active" ]; then
      echo "keyring did not load. Recent MySQL errors and AppArmor denials:"
      tail -20 /var/log/mysql/error.log 2>/dev/null | grep -iE 'keyring|component|error' || true
      grep 'apparmor="DENIED"' /var/log/syslog 2>/dev/null | grep mysqld | tail -5 || true
      exit 1
    fi
    echo "keyring active"
  fi
fi

echo "== 3. encrypt new tables and logs"
sql "SET PERSIST default_table_encryption = ON"
sql "SET PERSIST innodb_redo_log_encrypt = ON"
sql "SET PERSIST innodb_undo_log_encrypt = ON"
sql "SET PERSIST binlog_encryption = ON"

echo "== 4. encrypt existing diwan_* tables"
for db in $($MYSQL -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME LIKE 'diwan\\_%'"); do
  sql "ALTER DATABASE \`$db\` DEFAULT ENCRYPTION = 'Y'"
  for t in $($MYSQL -N -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='$db' AND TABLE_TYPE='BASE TABLE' AND (CREATE_OPTIONS NOT LIKE '%ENCRYPTION=\"Y\"%' OR CREATE_OPTIONS IS NULL)"); do
    sql "ALTER TABLE \`$db\`.\`$t\` ENCRYPTION = 'Y'"
  done
done

echo "== check"
$MYSQL -e "SELECT TABLE_SCHEMA, COUNT(*) tables, SUM(CREATE_OPTIONS LIKE '%ENCRYPTION=\"Y\"%') encrypted FROM information_schema.TABLES WHERE TABLE_SCHEMA LIKE 'diwan\\_%' AND TABLE_TYPE='BASE TABLE' GROUP BY TABLE_SCHEMA"
[ "$APPLY" -eq 1 ] || echo "dry run only - nothing was changed. Re-run with --apply."
echo "BACK UP $KEYRING_DIR NOW (separately from the database backups)."
