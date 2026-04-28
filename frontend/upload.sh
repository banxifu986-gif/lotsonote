npm run build
scp -r ./dist "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}"
rm -rf ./dist
