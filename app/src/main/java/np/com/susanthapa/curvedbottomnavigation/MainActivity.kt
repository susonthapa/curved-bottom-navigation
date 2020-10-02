package np.com.susanthapa.curvedbottomnavigation

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
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
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        val menuItems = arrayOf(
            MenuItem(R.drawable.ic_home_black_24dp),
            MenuItem(R.drawable.ic_dashboard_black_24dp),
            MenuItem(R.drawable.ic_notifications_black_24dp),
            MenuItem(R.drawable.ic_baseline_access_alarm_24),
            MenuItem(R.drawable.ic_baseline_settings_24)
        )
        binding.navView.setMenuItems(menuItems)
//        binding.navView.bottomNavigation.setupWithNavController(navController)
    }
}