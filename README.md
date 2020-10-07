# curved-bottom-navigation
A simple curved bottom navigation for Android with AnimatedVectorDrawable and Jetpack Navigation support.

## Demo
![](/resources/cbn_demo.gif)

## Usage
Update your **project** level `build.gradle` file and add the maven repoistory like this.
```groovy
allprojects {
    repositories {
        google()
        jcenter()
        maven {
            url 'http://www.idescout.com/maven/repo/'
            name 'IDEScout, Inc.'
        }

        maven {
            url  "https://dl.bintray.com/susonthapa/curved-bottom-navigation"
        }
    }
}
```

Update your **module** level `build.gradle` file and add the project dependency
```groovy
dependencies {
  implementation 'np.com.susanthapa.curved_bottom_navigation:curved_bottom_navigation:0.6.0'
}
```

**Important!** This project uses AndroidX so make sure have AndroidX enabled by adding these line tog `gradle.properties`
```properties
android.useAndroidX=true
android.enableJetifier=true
```

## Usage
Add `CurvedBottomNavigation` in your layout xml file.
```xml






