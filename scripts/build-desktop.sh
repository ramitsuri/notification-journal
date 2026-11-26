#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

CONFIGURATION="release"
EXPLICIT_BUILD=false
RUN_APP=false
NO_INSTALL=false
CLEAN_BUILD=false

print_usage() {
    cat <<'EOF'
Usage: ./scripts/build-desktop.sh [OPTIONS]

Options:
  --release      Build release app (isDebug=false) and install to /Applications/Journal.app (default)
  --debug        Build local debug app (isDebug=true) without modifying /Applications/Journal.app
  --run, -r      Launch the built debug app after stopping any running Journal instance (debug only)
  --open         Alias for --run
  --no-install   Do not copy release app to /Applications/Journal.app
  --clean        Run ./gradlew clean before building (incremental by default)
  --help, -h     Show this help message

Note:
  Both --debug and --release build Journal.app with bundle ID com.ramitsuri.notificationjournal
  and bind port 8000, so they cannot run side by side. Installing --release or passing --run
  closes any running Journal instance first.
EOF
}

for arg in "$@"; do
    case "$arg" in
        --debug)
            CONFIGURATION="debug"
            EXPLICIT_BUILD=true
            ;;
        --release)
            CONFIGURATION="release"
            EXPLICIT_BUILD=true
            ;;
        --run|-r|--open)
            RUN_APP=true
            ;;
        --no-install)
            NO_INSTALL=true
            ;;
        --clean)
            CLEAN_BUILD=true
            ;;
        --help|-h)
            print_usage
            exit 0
            ;;
        *)
            echo "Error: Unknown option '$arg'." >&2
            echo "Run './scripts/build-desktop.sh --help' for usage." >&2
            exit 1
            ;;
    esac
done

# If --run is passed without an explicit --release flag, default to debug configuration
if [ "$RUN_APP" = true ] && [ "$CONFIGURATION" = "release" ] && [ "$EXPLICIT_BUILD" = false ]; then
    CONFIGURATION="debug"
fi

if [ "$RUN_APP" = true ] && [ "$CONFIGURATION" != "debug" ]; then
    echo "Error: --run is only supported for debug builds (--debug)." >&2
    echo "For release builds, launch explicitly with: open -a Journal" >&2
    exit 1
fi

if [ "$CONFIGURATION" = "debug" ]; then
    IS_DEBUG="true"
    INSTALL_APP=false
else
    IS_DEBUG="false"
    if [ "$NO_INSTALL" = false ]; then
        INSTALL_APP=true
    else
        INSTALL_APP=false
    fi
fi

JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export JAVA_HOME

APP_BUNDLE="core/build/compose/binaries/main/app/Journal.app"
INSTALL_PATH="/Applications/Journal.app"
BUNDLE_ID="com.ramitsuri.notificationjournal"

journal_pids() {
    local pids pid cmd
    pids="$(pgrep -x "Journal" 2>/dev/null || true)"
    for pid in ${pids}; do
        cmd="$(ps -p "${pid}" -ww -o args= 2>/dev/null || true)"
        case "${cmd}" in
            /System/*) ;;
            */Journal.app/Contents/MacOS/Journal)
                echo "${pid}"
                ;;
        esac
    done
}

stop_running_journal() {
    local pids
    pids="$(journal_pids)"
    if [ -z "${pids}" ]; then
        return 0
    fi

    echo "Stopping running Journal instance..."
    osascript -e "tell application id \"${BUNDLE_ID}\" to quit" >/dev/null 2>&1 || true

    local i
    for i in $(seq 1 50); do
        if [ -z "$(journal_pids)" ]; then
            return 0
        fi
        sleep 0.1
    done

    pids="$(journal_pids)"
    if [ -n "${pids}" ]; then
        echo "Journal did not quit gracefully; sending SIGTERM..."
        # shellcheck disable=SC2086
        kill ${pids} 2>/dev/null || true
        for i in $(seq 1 20); do
            if [ -z "$(journal_pids)" ]; then
                return 0
            fi
            sleep 0.1
        done
    fi

    pids="$(journal_pids)"
    if [ -n "${pids}" ]; then
        echo "Journal still running; forcing exit..."
        # shellcheck disable=SC2086
        kill -9 ${pids} 2>/dev/null || true
        sleep 0.2
    fi
}

echo "============================================="
echo " Building Journal (${CONFIGURATION}, isDebug=${IS_DEBUG})"
echo "============================================="

GRADLE_TASKS=()
if [ "$CLEAN_BUILD" = true ]; then
    GRADLE_TASKS+=(clean)
fi
GRADLE_TASKS+=(:core:createDistributable)

./gradlew "${GRADLE_TASKS[@]}" "-PisDebug=${IS_DEBUG}"

if [ ! -d "${APP_BUNDLE}" ]; then
    echo "Error: App bundle not found at ${APP_BUNDLE}" >&2
    exit 1
fi

if [ "$INSTALL_APP" = true ]; then
    stop_running_journal
    echo "Installing ${APP_BUNDLE} to ${INSTALL_PATH}..."
    rm -rf "${INSTALL_PATH}"
    ditto "${APP_BUNDLE}" "${INSTALL_PATH}"
    echo "Successfully installed to ${INSTALL_PATH}"
fi

echo "============================================="
echo " Build complete!"
if [ "$INSTALL_APP" = true ]; then
    echo " Installed to: ${INSTALL_PATH}"
    echo " Launch with:  open -a Journal"
else
    echo " Local bundle: ${REPO_ROOT}/${APP_BUNDLE}"
    echo " Launch with:  open ${APP_BUNDLE}"
fi
echo "============================================="

if [ "$RUN_APP" = true ]; then
    echo ""
    stop_running_journal
    echo "Launching ${APP_BUNDLE}..."
    open "${APP_BUNDLE}"
    echo "✓ Journal (${CONFIGURATION}) launched."
fi
