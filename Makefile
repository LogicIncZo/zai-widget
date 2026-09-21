.PHONY: build test lint verify apk clean

build:
	./gradlew assembleDebug

test:
	./gradlew testDebugUnitTest

lint:
	./gradlew lintDebug

verify: test lint build

apk: verify
	@mkdir -p releases
	@cp app/build/outputs/apk/debug/app-debug.apk releases/zai-widget-latest.apk
	@echo "APK at releases/zai-widget-latest.apk"

clean:
	./gradlew clean
