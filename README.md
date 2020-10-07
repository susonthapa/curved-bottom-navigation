# curved-bottom-navigation
A simple curved bottom navigation for Android with AnimatedVectorDrawable and Jetpack Navigation support.

## Demo
![](/resources/cbn_demo.gif)

## Setup
Update your **project** level `build.gradle` file and add the maven repoistory like this.
```groovy
allprojects {
    repositories {
        google()
        jcenter()
        maven {
            url  "https://dl.bintray.com/susonthapa/curved-bottom-navigation"
        }
    }
}
```

Update your **module** level `build.gradle` file and add the following dependency. Please check the project releases for latest versions.
```groovy
dependencies {
  implementation 'np.com.susanthapa.curved_bottom_navigation:curved_bottom_navigation:0.6.2'
}
```

**Important!** This project uses AndroidX so make sure have AndroidX enabled by adding these line tog `gradle.properties`
```properties
android.useAndroidX=true
android.enableJetifier=true
```

## Usage

### Setup in XML
Add `CurvedBottomNavigationView` in your layout xml file.
```xml
<np.com.susanthapa.curved_bottom_navigation.CurvedBottomNavigationView
    android:id="@+id/nav_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### Setup in Code
In your `onCreate` of Activity create a list of `CbnMenuItem` that you want to appear in the `CurvedBottomNavigationView`. Then pass the list to the `setMenuItems()` function that also takes activeIndex(which is 0 by default) from which you can control which position item should be active.
```kotlin
val menuItems = arrayOf(
    CbnMenuItem(
        R.drawable.ic_notification, // the icon
        R.drawable.avd_notification, // the AVD that will be shown in FAB
        R.id.navigation_notifications // optional if you use Jetpack Navigation
    ),
    CbnMenuItem(
        R.drawable.ic_dashboard,
        R.drawable.avd_dashboard,
        R.id.navigation_dashboard
    ),
    CbnMenuItem(
        R.drawable.ic_home,
        R.drawable.avd_home,
        R.id.navigation_home
    ),
    CbnMenuItem(
        R.drawable.ic_profile,
        R.drawable.avd_profile,
        R.id.navigation_profile
    ),
    CbnMenuItem(
        R.drawable.ic_settings,
        R.drawable.avd_settings,
        R.id.navigation_settings
    )
)        
binding.navView.setMenuItems(menuItems, 2)
```
### Handling Navigation with Listener
To listen whenver the item is clicked you can pass a lambda to `setOnMenuItemClickListener`.
```kotlin
binding.navView.setOnMenuItemClickListener { cbnMenuItem, index -> 
    // handle your own navigation or other stuffs here
}
```

### Handling Navigaiton with Jetpack Navigation
If you are like me and :heart: Jetpack then there is a method called `setupWithNavController()` that accepts `NavController` and will handle the navigaiton for you. Just don't forget to pass the `id` of the destination when you are creating `CbnMenuItem`. 
```kotlin
binding.navView.setupWithNavController(navController)
```

### Manually setting the active item
If you need to manually set the active item you can call the `onMenuItemClick()` function and pass in the index that you would like to be selected.
```kotlin
binding.navView.onMenuItemClick(2)
```


### XML Attribues
Attribute | Description | Default Value
--------- | ----------- | -------------
app:cbn_selectedColor | Tint for the icon in selected state | `#000000`
app:cbn_unSelectedColor | Tint for the icon in unselected state | `#8F8F8F`
app:cbn_animDuration | Duration in millisecond for the curve animation | `300L`
app:cbn_fabElevation | Elevation for the Floating Action Button | `4dp`
app:cbn_elevation | Elevaton for the Curved Bottom Navigation View | `6dp`
app:cbn_fabBg | Background color of the Floating Action Button | `#FFFFFF`
app:cbn_bg | Background color of the Curved Bottom Navigation | `#FFFFFF`

### Note
The height of the `CurvedBottomNavigationView` is fixed to `56dp` and the size of the `FloatingActionButton` is also fixed to `56dp` for now.
Also the `AnimatedVectorDrawable` animation duration is taken as it is defined in the xml file.

Here is the link to my blog post that explains this library in some detail. 
