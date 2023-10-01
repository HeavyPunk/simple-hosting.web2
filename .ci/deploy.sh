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

echo "[DEPLOY] Zipping binaries..."
zip -r $DEPLOY_ARCHIVE ./build/*
echo "[DEPLOY] Successfully zipped binaries"

#Deploy

IFS=';' read -ra v_ip_array <<< "$MACHINES_IPS"
for machine in ${v_ip_array[*]}; do
    echo "[DEPLOY] Deploying to machine..."
    BINARIES_PATH=/home/$MACHINE_USER/web-api
    yes | scp -oStrictHostKeyChecking=no -P 22 -i $MACHINE_PRIVATE_SSH_KEY $DEPLOY_ARCHIVE $MACHINE_USER@$machine:$BINARIES_PATH
    yes | ssh -oStrictHostKeyChecking=no -i $MACHINE_PRIVATE_SSH_KEY $MACHINE_USER@$machine "unzip $BINARIES_PATH/$DEPLOY_ARCHIVE"
    yes | ssh -oStrictHostKeyChecking=no -i $MACHINE_PRIVATE_SSH_KEY $MACHINE_USER@$machine "$BINARIES_PATH/$DEPLOY_ARCHIVE/bin/simple-hosting-web2 -J--add-opens -Jjava.base/java.lang=ALL-UNNAMED &"
    echo "[DEPLOY] Deployed"
done

echo "[DEPLOY] Deployment completed successfully"