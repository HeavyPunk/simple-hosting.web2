#!/bin/bash

set -eu
#Checking for variables exists

check_for_var () {
    echo $1 > /dev/null
}

check_for_var $DEPLOY_ARCHIVE
check_for_var $MACHINES_IPS
check_for_var $MACHINE_PRIVATE_SSH_KEY
check_for_var $MACHINE_USER

#Preparing

echo "[DEPLOY] Prepare for deployment..."

chmod 600 $MACHINE_PRIVATE_SSH_KEY

echo "[DEPLOY] Prepare completed"

#Package

echo "[DEPLOY] Package starting..."
echo "[DEPLOY] Will be packaged to the $DEPLOY_ARCHIVE file"

echo "[DEPLOY] Switching to PROD environment..."

rm ./conf/application.conf
rm ./conf/META-INF/persistence.xml

echo "[DEPLOY] Successfully switched"

echo "[DEPLOY] Preparing configs..."

envsubst < ./.deploy/application.conf > ./conf/application.conf
envsubst < ./.deploy/persistence.xml > ./conf/META-INF/persistence.xml

echo "[DEPLOY] Successfully prepared"
echo "[DEPLOY] Starting build..."

sbt dist

echo "[DEPLOY] Successfully builded"

#Deploy

IFS=';' read -ra v_ip_array <<< "$MACHINES_IPS"

echo "[DEPLOY] Deployment starting"

run_cmd_via_ssh () {
    yes | ssh -oStrictHostKeyChecking=no -i $MACHINE_PRIVATE_SSH_KEY $MACHINE_USER@$machine "$1"
}

for machine in ${v_ip_array[*]}; do
    echo "[DEPLOY] Deploying to machine..."
    ASSETS_PATH=/home/$MACHINE_USER/web-api/
    yes | scp -oStrictHostKeyChecking=no -P 22 -i $MACHINE_PRIVATE_SSH_KEY $DEPLOY_ARCHIVE $MACHINE_USER@$machine:$ASSETS_PATH
    run_cmd_via_ssh "unzip $ASSETS_PATH/$DEPLOY_ARCHIVE"
    run_cmd_via_ssh "screen -t web-api-auto -d -m echo Hello"
    echo "[DEPLOY] Deployed"
done

echo "[DEPLOY] Deployment completed successfully"
