package np.com.susanthapa.curvedbottomnavigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import np.com.susanthapa.curved_bottom_navigation.MenuItem
import np.com.susanthapa.curvedbottomnavigation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            MenuItem(R.drawable.ic_home, R.drawable.avd_home, R.id.navigation_home),
            MenuItem(
                R.drawable.ic_dashboard,
                R.drawable.avd_dashboard,
                R.id.navigation_dashboard
            ),
            MenuItem(
                R.drawable.ic_notification,
                R.drawable.avd_notification,
                R.id.navigation_notifications
            ),
            MenuItem(
                R.drawable.ic_profile,
                R.drawable.avd_profile,
                R.id.navigation_profile
            ),
            MenuItem(
                R.drawable.ic_settings,
                R.drawable.avd_settings,
                R.id.navigation_settings
            )
        )
        binding.navView.setMenuItems(menuItems)
        binding.navView.setupWithNavController(navController)
    }
}