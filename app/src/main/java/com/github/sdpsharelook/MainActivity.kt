package com.github.sdpsharelook

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

const val EXTRA_MESSAGE = "com.github.sdpsharelook.NAME"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setDrawerListener()

    }

    private fun setDrawerListener() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        findViewById<ImageView>(R.id.menu_hamburger).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val navView = findViewById<NavigationView>(R.id.navView)
        val bottomView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val navController = Navigation.findNavController(this, R.id.navHostFragment)
        NavigationUI.setupWithNavController(navView, navController)
        NavigationUI.setupWithNavController(bottomView, navController)
    }
}
