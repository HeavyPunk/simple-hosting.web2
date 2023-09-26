set -eu

cd ..

echo "Building project..."
sbt dist
echo "Build is successfull"

PR_VERSION=$(sbt 'inspect actual version' | grep "Setting: java.lang.String" | cut -d '=' -f2 | tr -d ' ')
PR_NAME=$(sbt 'inspect actual name' | grep "Setting: java.lang.String" | cut -d '=' -f2 | tr -d ' ')
ARTIFACT_EXT=.zip
DISTR_NAME=$(echo $PR_NAME | tr . -)-$PR_VERSION

echo "Try to compute $DISTR_NAME sbt build artefact"

rm -rf .ci/build
mkdir .ci/build
unzip target/universal/$DISTR_NAME$ARTIFACT_EXT -d .ci/build/
mv .ci/build/$DISTR_NAME/* .ci/build/
rm -rf .ci/build/$DISTR_NAME
