# Toolbox.io Android App

A comprehensive security application for Android devices that helps protect your apps and data from unauthorized access.

## 🚀 Features

### 🔒 Security Features
- **App Locking**: Protect apps with fake crash messages to confuse intruders
- **Unlock Protection**: Alarm and photo capture on failed unlock attempts
- **Touch Protection**: Detect unauthorized physical access to your device
- **Biometric Authentication**: Support for fingerprint and face unlock

### 🛠️ Utility Features
- **Quick Settings Tiles**: Sleep mode and other system utilities
- **App Manager**: Share APKs, manage apps, and get technical information
- **Shortcuts**: Access to hidden system apps and settings
- **Notification History**: Track and manage app notifications

### 🔧 System Integration
- **Accessibility Service**: Seamless app locking integration
- **Device Admin**: Enhanced security features
- **Material 3 Design**: Modern, intuitive interface
- **Dark/Light Theme**: Automatic theme switching support

## 📋 Requirements

- **Android 7.0+** (API level 24)
- **Root Access**: Not required
- **Device Admin**: Required for unlock protection features
- **Accessibility Service**: Required for app locking

## 🛠️ Development Setup

### Prerequisites
- **Android Studio** (latest version)
- **JDK 11+**
- **Android SDK** (API 24+)
- **Git**

### Building the App

```bash
# Clone the repository
git clone https://github.com/Toolbox-io/Toolbox-io.git
cd Toolbox-io/Android

# Build debug version
./gradlew assembleDebug

# Build release version
./gradlew assembleRelease
```

### Project Structure

```
Android/
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── java/io/toolbox/
│   │   │   ├── ui/         # UI components and activities
│   │   │   ├── api/        # API communication
│   │   │   ├── services/   # Background services
│   │   │   └── Settings.kt # App configuration
│   │   ├── res/           # Resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml
├── androidUtils/          # Utility library
├── utils/                 # Common utilities
└── build.gradle.kts      # Project configuration
```

## 🔧 Configuration

### Environment Variables
The app uses the following configuration:

```kotlin
// GitHub API for update checking
const val GITHUB_TOKEN = "your_github_token"  // Read-only token for rate limits
const val GITHUB_API_VERSION = "2022-11-28"

// API endpoints
const val BASE_URL = "https://toolbox-io.ru/api/auth"
```

### Permissions Required

```xml
<!-- Core permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Security features -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.DEVICE_ADMIN" />
```

## 🚀 Usage

### Initial Setup
1. **Install the app** from releases or build from source
2. **Grant permissions** for accessibility and device admin
3. **Set up app locking** by selecting apps to protect
4. **Configure unlock protection** with alarm and photo settings
5. **Enable quick settings tiles** for easy access

### App Locking
- Select apps to protect from the app list
- Set a password for unlocking protected apps
- Choose between popup or fullscreen unlock methods
- Protected apps will show fake crash messages to intruders

### Unlock Protection
- Enable alarm on failed unlock attempts
- Configure photo capture of intruders
- Set sensitivity and trigger conditions
- Works with any lock screen method

### Touch Protection
- Activate "Don't Touch My Phone" mode
- Triggers same actions as unlock protection
- Perfect for preventing unauthorized access

## 🔒 Security Considerations

### Data Protection
- **Encrypted Storage**: All passwords stored with encryption
- **Secure Communication**: HTTPS for all API calls
- **No Data Collection**: App doesn't collect personal information
- **Local Processing**: Security features work offline

### Privacy
- **No Tracking**: No analytics or user tracking
- **Local Data**: All data stays on your device
- **Open Source**: Code is publicly auditable
- **Transparent**: Clear about what permissions are used for

## 🐛 Troubleshooting

### Common Issues

**App Locking Not Working:**
- Ensure accessibility service is enabled
- Check if the app is in battery optimization whitelist
- Verify device admin permissions are granted

**Unlock Protection Not Triggering:**
- Enable device admin permissions
- Check if the feature is properly configured
- Ensure the app has necessary system permissions

**Quick Settings Tiles Not Appearing:**
- Add tiles manually from system settings
- Restart the device after installation
- Check if the app is properly installed

### Debug Mode
Enable debug logging for troubleshooting:

```kotlin
// In Settings.kt
var debugMode = true  // Enable detailed logging
```

## 🤝 Contributing

### Development Guidelines
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add KDoc comments for public functions
- Test on multiple Android versions
- Ensure accessibility compliance

### Code Style
- Use 4-space indentation
- Prefer immutable data structures
- Use proper error handling
- Follow Material Design guidelines

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

## 🙏 Acknowledgments

- **Material Design** for UI components
- **Jetpack Compose** for modern UI development
- **Ktor** for network communication
- **AndroidX** for core functionality
- **All contributors** and users

## 📞 Support

- **GitHub Issues**: [Report bugs and request features](https://github.com/Toolbox-io/app/issues)
- **Email**: support@toolbox-io.ru
- **Website**: https://toolbox-io.ru
- **Documentation**: [GitHub Wiki](https://github.com/Toolbox-io/site/wiki)

---

**Made with ❤️ for Android security**
