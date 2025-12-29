#!/bin/bash

# Configuration
KEYSTORE_FILE="release.keystore"
ALIAS="release"
# Generate specific passwords
PASSWORD=$(openssl rand -base64 12)

# Define the command string
CMD_STRING="keytool -genkeypair -v -keystore $KEYSTORE_FILE -keyalg RSA -keysize 2048 -validity 10000 -alias $ALIAS -storepass $PASSWORD -keypass $PASSWORD -dname \"CN=Android Release, OU=Development, O=Personal, L=City, S=State, C=US\""

echo "=========================================="
echo "      KEYSTORE GENERATION HELPER"
echo "=========================================="
echo ""
echo "Attempting to generate keystore automatically..."

# Try to find a working keytool
KEYTOOL_BIN=""

# 1. Try macOS java_home
if [ -x "/usr/libexec/java_home" ]; then
  JAVA_PATH=$(/usr/libexec/java_home 2>/dev/null)
  if [ -n "$JAVA_PATH" ] && [ -f "$JAVA_PATH/bin/keytool" ]; then
    KEYTOOL_BIN="$JAVA_PATH/bin/keytool"
  fi
fi

# 2. Try common Android Studio paths
if [ -z "$KEYTOOL_BIN" ]; then
   studio_paths=(
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
    "/Applications/Android Studio.app/Contents/jre/Contents/Home/bin/keytool"
    "$HOME/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/*/Contents/jbr/Contents/Home/bin/keytool"
  )
  for path in "${studio_paths[@]}"; do
    for p in $path; do
      if [ -f "$p" ] && [ -x "$p" ]; then
        KEYTOOL_BIN="$p"
        break 2
      fi
    done
  done
fi

# 3. Fallback to system command
if [ -z "$KEYTOOL_BIN" ] && command -v keytool &> /dev/null; then
  KEYTOOL_BIN="keytool"
fi

# Execute
SUCCESS=false
if [ -n "$KEYTOOL_BIN" ]; then
  echo "Using keytool at: $KEYTOOL_BIN"
  $KEYTOOL_BIN -genkeypair -v \
    -keystore $KEYSTORE_FILE \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -alias $ALIAS \
    -storepass $PASSWORD \
    -keypass $PASSWORD \
    -dname "CN=Android Release, OU=Development, O=Personal, L=City, S=State, C=US"
  
  if [ $? -eq 0 ]; then
    SUCCESS=true
  else
    echo "⚠️  Automatic generation failed."
  fi
else
  echo "⚠️  Could not find a working 'keytool' command."
fi

echo ""
echo "=========================================="
if [ "$SUCCESS" = true ]; then
  echo "✅ Keystore generated successfully: $KEYSTORE_FILE"
else
  echo "❌ AUTOMATIC GENERATION FAILED"
  echo "Don't worry! You can do it manually."
  echo ""
  echo "STEP 1: Copy and Run this command in Android Studio Terminal:"
  echo "-------------------------------------------------------------"
  echo "$CMD_STRING"
  echo "-------------------------------------------------------------"
fi

echo ""
echo "STEP 2: Add these secrets to GitHub (Settings > Secrets > Actions):"
echo "-------------------------------------------------------------"
echo "RELEASE_KEYSTORE_PASSWORD : $PASSWORD"
echo "RELEASE_KEY_ALIAS         : $ALIAS"
echo "RELEASE_KEY_PASSWORD      : $PASSWORD"
if [ "$SUCCESS" = true ]; then
  echo "RELEASE_KEYSTORE_BASE64   : (Run 'base64 -i $KEYSTORE_FILE')"
else
  echo "RELEASE_KEYSTORE_BASE64   : (Run 'base64 -i $KEYSTORE_FILE' after generating the file)"
fi
echo "=========================================="

