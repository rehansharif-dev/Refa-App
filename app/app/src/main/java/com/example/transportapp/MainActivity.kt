package com.example.transportapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.transportapp.data.session.SessionManager
import com.example.transportapp.presentation.navigation.SetupNavGraph
import com.example.transportapp.ui.theme.RefaTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Determine start route from session state
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val startRoute = when {
            firebaseUser != null && sessionManager.isSessionValid() -> {
                if (sessionManager.isDriverMode()) "driver_dashboard" else "home"
            }
            else -> {
                // Session expired or not logged in — force logout to clear any stale auth tokens
                if (firebaseUser != null && !sessionManager.isSessionValid()) {
                    FirebaseAuth.getInstance().signOut()
                    sessionManager.clearSession()
                }
                "login"
            }
        }

        setContent {
            RefaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SetupNavGraph(
                        navController = navController,
                        startRoute = startRoute,
                        sessionManager = sessionManager
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh session timer on every foreground return
        if (FirebaseAuth.getInstance().currentUser != null) {
            sessionManager.refreshActivity()
        }
    }
}
