package np.com.susanthapa.curvedbottomnavigation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem
import np.com.susanthapa.curvedbottomnavigation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.BLACK)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activeIndex = savedInstanceState?.getInt("activeIndex") ?: 2

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_profile,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        val menuItems = arrayOf(
            CbnMenuItem(
                R.drawable.ic_notification,
                R.drawable.avd_notification,
                R.id.navigation_notifications,
                "Notifications"
            ),
            CbnMenuItem(
                R.drawable.ic_dashboard,
                R.drawable.avd_dashboard,
                R.id.navigation_dashboard,
                "Dashboard"
            ),
            CbnMenuItem(
                R.drawable.ic_home,
                R.drawable.avd_home,
                R.id.navigation_home,
                "Home"
            ),
            CbnMenuItem(
                R.drawable.ic_profile,
                R.drawable.avd_profile,
                R.id.navigation_profile,
                "Profile"
            ),
            CbnMenuItem(
                R.drawable.ic_settings,
                R.drawable.avd_settings,
                R.id.navigation_settings,
                "Settings"
            )
        )

        binding.navView.setMenuItems(menuItems, activeIndex)
        binding.navView.setupWithNavController(navController)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("activeIndex", binding.navView.getSelectedIndex())
        super.onSaveInstanceState(outState)
    }
}