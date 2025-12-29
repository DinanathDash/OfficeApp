#!/bin/bash
set -e

# --- Configuration ---
KEYSTORE_FILE="release.keystore"
ALIAS="release"
# Use Hex to avoid special characters causing issues in secrets
PASSWORD=$(openssl rand -hex 16)
# Auto-detect keytool path
KEYTOOL_PATH=""

# 1. Try macOS java_home
if [ -x "/usr/libexec/java_home" ]; then
  JAVA_PATH=$(/usr/libexec/java_home 2>/dev/null)
  if [ -n "$JAVA_PATH" ] && [ -f "$JAVA_PATH/bin/keytool" ]; then
    KEYTOOL_PATH="$JAVA_PATH/bin/keytool"
  fi
fi

# 2. Try common Android Studio paths
if [ -z "$KEYTOOL_PATH" ]; then
   studio_paths=(
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
    "/Applications/Android Studio.app/Contents/jre/Contents/Home/bin/keytool"
    "$HOME/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/*/Contents/jbr/Contents/Home/bin/keytool"
  )
  for path in "${studio_paths[@]}"; do
    for p in $path; do
      if [ -f "$p" ] && [ -x "$p" ]; then
        KEYTOOL_PATH="$p"
        break 2
      fi
    done
  done
fi

# 3. Fallback to system command
if [ -z "$KEYTOOL_PATH" ] && command -v keytool &> /dev/null; then
  KEYTOOL_PATH="keytool"
fi

if [ -z "$KEYTOOL_PATH" ]; then
  echo "❌ Error: Could not find 'keytool'. Please ensure Java or Android Studio is installed."
  exit 1
fi

echo "ℹ️  Using keytool at: $KEYTOOL_PATH"

echo "🚀 Starting Automated Release Setup..."

# 1. Generate Keystore (Force overwrite to ensure password matches)
if [ -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  Deleting existing $KEYSTORE_FILE to ensure fresh keys causing no password mismatches."
    rm "$KEYSTORE_FILE"
fi

echo "🔑 Generating Keystore..."
"$KEYTOOL_PATH" -genkeypair -v \
  -keystore $KEYSTORE_FILE \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias $ALIAS \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dname "CN=Android Release, OU=Development, O=Personal, L=City, S=State, C=US"
echo "✅ Keystore generated."

# --- VERIFICATION STEP ---
echo "🔍 Verifying Keystore integrity..."
# Encode to base64 (simulating what we send to GitHub)
BASE64_TEST=$(base64 < "$KEYSTORE_FILE" | tr -d '\n')
# Decode back (simulating what Gradle does)
echo "$BASE64_TEST" | base64 -d > verify.keystore

# Try to list keys using the password
if "$KEYTOOL_PATH" -list -keystore verify.keystore -storepass "$PASSWORD" > /dev/null 2>&1; then
    echo "✅ Verification SUCCESS: Keystore password works with base64 encoding."
    rm verify.keystore
else
    echo "❌ Verification FAILED: Password incorrect or encoding issue."
    rm verify.keystore
    exit 1
fi

# 2. Upload Secrets to GitHub
echo "🔐 Uploading Secrets to GitHub..."
gh secret set RELEASE_KEY_ALIAS --body "$ALIAS"
echo "   - RELEASE_KEY_ALIAS set"

gh secret set RELEASE_KEY_PASSWORD --body "$PASSWORD"
echo "   - RELEASE_KEY_PASSWORD set"

gh secret set RELEASE_KEYSTORE_PASSWORD --body "$PASSWORD"
echo "   - RELEASE_KEYSTORE_PASSWORD set"

gh secret set RELEASE_KEYSTORE_BASE64 --body "$BASE64_TEST"
echo "   - RELEASE_KEYSTORE_BASE64 set"

echo "✅ All secrets uploaded successfully."

echo ""
echo "🎉 SUCCESS! Everything is done."
echo "   1. Keystore generated."
echo "   2. Secrets uploaded to GitHub."
echo ""
echo "👉 You can now go to GitHub Actions and run the 'Build and Publish Release' workflow!"
