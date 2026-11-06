# Make not debug
sed -i '' "s/isDebug=true/isDebug=false/g" ./local.properties

export JAVA_HOME="$(/usr/libexec/java_home -v 21)"

./gradlew clean
./gradlew packageDistributionForCurrentOS

# Make debug version again
sed -i '' "s/isDebug=false/isDebug=true/g" ./local.properties

open ./core/build/compose/binaries/main/dmg/Journal-1.0.0.dmg
