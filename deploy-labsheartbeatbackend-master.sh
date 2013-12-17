#!/bin/bash
### LOCAL ###

### PRODUCTION ###
ROOT_DEPLOY_PATH="/home/bitnami/apps/django/django_projects/labsheartbeatbackend_deployment/"
TMP_DIR="tmp"
TMP_DEPLOY_PATH=$ROOT_DEPLOY_PATH$TMP_DIR"/"
GIT_REPO_URL="https://xxx:xxx@bitbucket.org/mcgarrybowen/labs-heartbeat-visualization-framework.git"
DEST_PATH="/home/bitnami/apps/django/django_projects/labsheartbeatbackend/"

echo "Are you sure you want to deploy this code?"
select yn in "Yes" "No"; do
	case $yn in
		Yes )
			echo "Deployment script started with the following parameters:";
			echo "ROOT_DEPLOY_PATH: "$ROOT_DEPLOY_PATH;
			echo "TMP_DEPLOY_PATH: "$TMP_DEPLOY_PATH;
			echo "DEST_PATH: "$DEST_PATH;
			echo "GIT_REPO_URL: "$GIT_REPO_URL;
			
			echo "Start with emply deploy directory...";
			rm -rf $ROOT_DEPLOY_PATH*;
			
			echo "Change to deploy directory...";
			cd $ROOT_DEPLOY_PATH;
			
			echo "Clone the repo...";
			git clone $GIT_REPO_URL $TMP_DIR;
			
			echo "Create the correct config files and delete the unused ones...";
			mv $TMP_DEPLOY_PATH"src/labsheartbeatbackend/labsheartbeatbackend/settings-PROD.py" $TMP_DEPLOY_PATH"src/labsheartbeatbackend/labsheartbeatbackend/settings.py";
			rm $TMP_DEPLOY_PATH"src/labsheartbeatbackend/labsheartbeatbackend/settings-DEV.py";
			rm $TMP_DEPLOY_PATH".gitignore";
			rm -rf $TMP_DEPLOY_PATH".git";
			
			echo "Rsync the contents of the deployment subdirectory to the live project root...";
			rsync -avz --delete $TMP_DEPLOY_PATH"src/labsheartbeatbackend/" $DEST_PATH
			
			echo "Make sure the permissions are set up correctly..."
			chmod -R 755 $DEST_PATH
			chmod -R 777 $DEST_PATH"cache";
			chmod -R 777 $DEST_PATH"../json-backup";
			chmod -R 777 $DEST_PATH"../uploaded_screenshots";
			
			echo "Clean up deploy directory...";
			rm -rf $ROOT_DEPLOY_PATH*;
			
			echo "Restarting Apache...";
			/opt/bitnami/ctlscript.sh restart apache;
			
			echo "Deployment finished. Exiting..."; exit;;
		No ) echo "No deployment performed. Exiting..."; exit;;
	esac
done
