#!/bin/bash
set -e

# --- Configuration ---
KEYSTORE_FILE="release.keystore"
ALIAS="release"
PASSWORD=$(openssl rand -base64 12)
KEYTOOL_PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"

echo "🚀 Starting Automated Release Setup..."

# 1. Generate Keystore
if [ -f "$KEYSTORE_FILE" ]; then
    echo "ℹ️  $KEYSTORE_FILE already exists, skipping generation."
else
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
fi

# 2. Upload Secrets to GitHub
echo "🔐 Uploading Secrets to GitHub..."
gh secret set RELEASE_KEY_ALIAS --body "$ALIAS"
echo "   - RELEASE_KEY_ALIAS set"

gh secret set RELEASE_KEY_PASSWORD --body "$PASSWORD"
echo "   - RELEASE_KEY_PASSWORD set"

gh secret set RELEASE_KEYSTORE_PASSWORD --body "$PASSWORD"
echo "   - RELEASE_KEYSTORE_PASSWORD set"

# Base64 encode keystore (Strip newlines to ensure single line string)
BASE64_KEY=$(base64 < "$KEYSTORE_FILE" | tr -d '\n')
gh secret set RELEASE_KEYSTORE_BASE64 --body "$BASE64_KEY"
echo "   - RELEASE_KEYSTORE_BASE64 set"

echo "✅ All secrets uploaded successfully."


echo ""
echo "🎉 SUCCESS! Everything is done."
echo "   1. Keystore generated."
echo "   2. Secrets uploaded to GitHub."
echo ""
echo "👉 You can now go to GitHub Actions and run the 'Build and Publish Release' workflow!"
