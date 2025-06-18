./gradlew :app:installDebug

adb shell monkey -p com.ramitsuri.notificationjournal.debug -c android.intent.category.LAUNCHER 1

./gradlew :core:run