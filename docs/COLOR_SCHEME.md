# Changing the color scheme
## Generating the color scheme
1. Go to 
   [Material Theme Builder](
   https://www.figma.com/community/plugin/1034969338659738588/material-theme-builder
   ) on Figma
2. Generate a color scheme based on the icon
3. Export the color scheme as **Jetpack Compose (`Theme.kt`)**.

## Using the color scheme
Unzip the theme and open the `ui/Color.kt` file.

### Applying to Jetpack Compose


#### Applying to light theme
1. Copy the block that starts with:
   
   ```kotlin
   val primaryLight = Color(/* ... */)
   ```
2. Paste it to the following part of [`Theme.kt`](../app/src/main/java/io/toolbox/ui/Theme.kt):
   
   ```kotlin
   val appLightColorScheme by lazy { 
       lightColorScheme(
           // paste here, remove any content
       )
   }
   ```
3. Choose the block and **find an replace**:
   - Find:
     
     ```regexp
     val (\w+)Light = Color\((.*)\)
     ```
   - Replace:

     ```text
     $1 = Color($2),
     ```
4. Remove the trailing comma at the end.

#### Applying to dark theme
1. Copy the block that starts with:

   ```kotlin
   val primaryDark = Color(/* ... */)
   ```
2. Paste it to the following part of [`Theme.kt`](../app/src/main/java/io/toolbox/ui/Theme.kt):

   ```kotlin
   val appDarkColorScheme by lazy { 
       darkColorScheme(
           // paste here, remove any content
       )
   }
   ```
3. Choose the block and **find an replace**:
    - Find:

      ```regexp
      val (\w+)Dark = Color\((.*)\)
      ```
    - Replace:

      ```text
      $1 = Color($2),
      ```
4. Remove the trailing comma at the end.

### Applying to Views

#### Applying to light theme
1. Copy the block that starts with:

   ```kotlin
   val primaryLight = Color(/* ... */)
   ```
2. Paste it to the following part of [`themes.xml`](../app/src/main/res/values/themes.xml):

   ```xml
   <style name="Theme.Toolbox_io.Light.NoDynamicColor" parent="Theme.Material3.Light">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>

        <!-- paste here, remove any content -->
    </style>
   ```
3. Choose the block and **find an replace**:
    - Find:

      ```regexp
      val (\w)(\w+)Light = Color\(0x(.*)\)
      ```
    - Replace:
   
      ```text
      <item name="color\U$1\E$2">#$3</item>
      ```

#### Applying to dark theme
1. Copy the block that starts with:

   ```kotlin
   val primaryDark = Color(/* ... */)
   ```
2. Paste it to the following part of [`themes.xml`](../app/src/main/res/values/themes.xml):

   ```xml
   <style name="Theme.Toolbox_io.Night.NoDynamicColor" parent="Theme.Material3.Dark">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>

        <!-- paste here, remove any content -->
    </style>
   ```
3. Choose the block and **find an replace**:
    - Find:

      ```regexp
      val (\w)(\w+)Dark = Color\(0x(.*)\)
      ```
    - Replace:

      ```text
      <item name="color\U$1\E$2">#$3</item>
      ```

### Applying to splash screen
Change the `windowSplashScreenBackground` attribute in each variant of `Theme.Toolbox_io.SplashScreen`
to the **surface color** of the theme, making sure to choose the right variant for light/dark theme.