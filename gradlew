#!/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# NepuTV gradlew bootstrap
#
# The gradle-wrapper.jar cannot be distributed in source repos.
# Run this script once to generate it, then use ./gradlew normally.
# ─────────────────────────────────────────────────────────────────────────────

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "⚠️  gradle-wrapper.jar not found. Bootstrapping..."

  # Check if gradle is installed
  if command -v gradle >/dev/null 2>&1; then
    gradle wrapper --gradle-version=8.4
    echo "✅ Wrapper generated! Re-running your command..."
    exec ./gradlew "$@"
  else
    echo ""
    echo "❌ Please do ONE of the following:"
    echo ""
    echo "  Option A — Open in Android Studio (it auto-generates the wrapper)"
    echo ""
    echo "  Option B — Install Gradle first:"
    echo "    brew install gradle     # macOS"
    echo "    sdk install gradle 8.4  # SDKMAN"
    echo "    choco install gradle    # Windows"
    echo "  Then run: gradle wrapper --gradle-version=8.4"
    echo ""
    echo "  Option C — Push to GitHub and let Actions build it (no local setup needed)"
    echo ""
    exit 1
  fi
fi

# Normal execution once wrapper exists
JAVA_OPTS="-Xmx2048m -Dfile.encoding=UTF-8"
exec java $JAVA_OPTS -jar "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
