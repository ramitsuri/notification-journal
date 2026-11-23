#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

TARGET_NAME="WorkMacApp"
CERT_NAME="Ramit Suri (Local)"

CONFIGURATION="release"
CREATE_CERT=false
NO_INSTALL=false
EXPLICIT_BUILD=false
RUN_APP=false

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
        --create-cert)
            CREATE_CERT=true
            ;;
        --no-install)
            NO_INSTALL=true
            ;;
        --help|-h)
            echo "Usage: ./build.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --debug        Build local debug app (NotificationJournalWork-Debug.app)"
            echo "  --release      Build release app and install to /Applications (default)"
            echo "  --run, -r      Run the app after building, terminating previous instances (debug only)"
            echo "  --no-install   Do not copy release app to /Applications"
            echo "  --create-cert  Generate local signing certificate in Keychain"
            echo "  --help, -h     Show this help message"
            exit 0
            ;;
    esac
done

# If --run is passed without an explicit --release flag, default to debug configuration
if [ "$RUN_APP" = true ] && [ "$CONFIGURATION" = "release" ] && [ "$EXPLICIT_BUILD" = false ]; then
    CONFIGURATION="debug"
fi

if [ "$RUN_APP" = true ] && [ "$CONFIGURATION" != "debug" ]; then
    echo "Error: --run is only supported for debug builds (--debug)."
    echo "For release/production builds, launch explicitly with: open -a NotificationJournalWork"
    exit 1
fi

# Default behavior:
# - Release automatically installs to /Applications
# - Debug stays local in the project folder
if [ "$CONFIGURATION" = "release" ] && [ "$NO_INSTALL" = false ]; then
    INSTALL_APP=true
else
    INSTALL_APP=false
fi

if [ "$CONFIGURATION" = "debug" ]; then
    APP_NAME="NotificationJournalWork-Debug"
    BUNDLE_ID="com.ramitsuri.notificationjournal.work.debug"
    DISPLAY_NAME="NotificationJournal Work (Debug)"
else
    APP_NAME="NotificationJournalWork"
    BUNDLE_ID="com.ramitsuri.notificationjournal.work"
    DISPLAY_NAME="NotificationJournal Work"
fi

# Optional: Create local Keychain signing certificate if requested
if [ "$CREATE_CERT" = true ]; then
    echo "Creating self-signed Code Signing certificate: \"${CERT_NAME}\"..."
    KEY_FILE="/tmp/codesign_${$}.key"
    CRT_FILE="/tmp/codesign_${$}.crt"
    P12_FILE="/tmp/codesign_${$}.p12"
    LOGIN_KEYCHAIN="${HOME}/Library/Keychains/login.keychain-db"

    # Delete any prior incomplete or previous test certificate (and its trust settings) to avoid clutter
    security delete-certificate -t -c "Local Dev Signing" "${LOGIN_KEYCHAIN}" > /dev/null 2>&1 || true
    security delete-certificate -t -c "${CERT_NAME}" "${LOGIN_KEYCHAIN}" > /dev/null 2>&1 || true

    # Generate key and cert with explicit digitalSignature and codeSigning extensions
    openssl req -x509 -newkey rsa:2048 -keyout "${KEY_FILE}" -out "${CRT_FILE}" -days 3650 -nodes \
        -subj "/CN=${CERT_NAME}" \
        -addext "keyUsage = critical, digitalSignature" \
        -addext "extendedKeyUsage = codeSigning" > /dev/null 2>&1

    # Export to PKCS#12 bundle with descriptive friendly name
    openssl pkcs12 -export -out "${P12_FILE}" -inkey "${KEY_FILE}" -in "${CRT_FILE}" -name "${CERT_NAME}" -passout pass:temp > /dev/null 2>&1

    # Import PKCS#12 into the user login keychain allowing codesign access
    security import "${P12_FILE}" -k "${LOGIN_KEYCHAIN}" -P temp -T /usr/bin/codesign -A > /dev/null 2>&1

    # Trust for code signing in user domain
    security add-trusted-cert -d -r trustRoot -p codeSign -k "${LOGIN_KEYCHAIN}" "${CRT_FILE}" > /dev/null 2>&1 || true

    rm -f "${KEY_FILE}" "${CRT_FILE}" "${P12_FILE}"

    # Verify identity
    if security find-identity -v -p codesigning | grep -q "${CERT_NAME}"; then
        echo "Successfully installed and verified \"${CERT_NAME}\" in Keychain!"
    else
        echo "Identity imported. If 'security find-identity' still does not show it,"
        echo "you can also create one in 3 clicks via Keychain Access:"
        echo "  Keychain Access -> Certificate Assistant -> Create a Certificate..."
        echo "  Name: ${CERT_NAME}, Type: Code Signing"
    fi

    if [ "$EXPLICIT_BUILD" = false ]; then
        echo "Ready! You can now run './build.sh --release' to build and package."
        exit 0
    fi
fi

echo "============================================="
echo " Building ${APP_NAME} (${CONFIGURATION})"
echo "============================================="

# 1. Build Swift executable
# Standalone Command Line Tools environments require --build-system native due to
# SwiftPM issue #10561. If a future Swift release removes that flag, we seamlessly
# fall back to the default build system.
BUILD_ARGS=(-c "${CONFIGURATION}")
if (swift build --build-system nonexistent 2>&1 || true) | grep -q "'native'"; then
    BUILD_ARGS+=(--build-system native)
fi

swift build "${BUILD_ARGS[@]}"

BINARY_DIR="$(swift build "${BUILD_ARGS[@]}" --show-bin-path)"
EXECUTABLE_PATH="${BINARY_DIR}/${TARGET_NAME}"

if [ ! -f "${EXECUTABLE_PATH}" ]; then
    echo "Error: Executable not found at ${EXECUTABLE_PATH}"
    exit 1
fi

# 2. Package into .app bundle
APP_BUNDLE="${APP_NAME}.app"
CONTENTS_DIR="${APP_BUNDLE}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"

echo "Creating bundle layout: ${APP_BUNDLE}"
rm -rf "${APP_BUNDLE}"
mkdir -p "${MACOS_DIR}"
mkdir -p "${RESOURCES_DIR}"

cp "${EXECUTABLE_PATH}" "${MACOS_DIR}/${APP_NAME}"

# Generate customized Info.plist for this environment
cat <<EOF > "${CONTENTS_DIR}/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleIdentifier</key>
    <string>${BUNDLE_ID}</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundleDisplayName</key>
    <string>${DISPLAY_NAME}</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>
EOF

# 3. Code Signing
echo "Verifying code-signing certificate..."

if security find-identity -v -p codesigning | grep -q "${CERT_NAME}"; then
    echo "Found valid signing identity: \"${CERT_NAME}\". Signing bundle..."
    codesign --force --deep --sign "${CERT_NAME}" "${APP_BUNDLE}"
    echo "Successfully signed with \"${CERT_NAME}\"."
else
    echo "Notice: \"${CERT_NAME}\" valid signing identity not found in Keychain."
    echo "Signing ad-hoc (-) for now."
    echo "TIP: To create a local certificate, run:"
    echo "     ./build.sh --create-cert"
    codesign --force --deep --sign - "${APP_BUNDLE}"
fi

# 4. Install Release builds to /Applications
if [ "$INSTALL_APP" = true ]; then
    INSTALL_DIR="/Applications"
    echo "Installing ${APP_BUNDLE} to ${INSTALL_DIR}..."
    rm -rf "${INSTALL_DIR}/${APP_BUNDLE}"
    cp -R "${APP_BUNDLE}" "${INSTALL_DIR}/"
    echo "Successfully installed to ${INSTALL_DIR}/${APP_BUNDLE}"
fi

echo "============================================="
echo " Build & packaging complete!"
if [ "$INSTALL_APP" = true ]; then
    echo " Installed to: /Applications/${APP_BUNDLE}"
    echo " Launch with:  open -a NotificationJournalWork"
else
    echo " Local bundle: $(pwd)/${APP_BUNDLE}"
    echo " Launch with:  open ${APP_BUNDLE}"
fi
echo "============================================="

# 5. Optional Run (Debug only)
if [ "$RUN_APP" = true ]; then
    echo ""
    echo "Terminating any existing ${APP_NAME} instances..."
    killall "${APP_NAME}" 2>/dev/null || true
    sleep 0.5
    echo "Launching ${APP_BUNDLE}..."
    open "${APP_BUNDLE}"
    echo "✓ ${APP_NAME} launched."
fi
