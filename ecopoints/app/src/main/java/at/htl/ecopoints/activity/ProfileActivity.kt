package at.htl.ecopoints.activity

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import at.htl.ecopoints.R
import at.htl.ecopoints.ui.theme.EcoPointsTheme
import at.htl.ecopoints.databinding.ActivityProfileBinding
import at.htl.ecopoints.navigation.BottomNavBar

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val (currentScreen, setCurrentScreen) = remember { mutableStateOf("Profile") }
            EcoPointsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Profile()



                    Box {
                        BottomNavBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { newScreen -> setCurrentScreen(newScreen) },
                            context = this@ProfileActivity
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Profile(){
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(
                    color =
                    Color(android.graphics.Color.parseColor("#ececec"))
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConstraintLayout {
                val (topImg, profile) = createRefs()
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    null,
                    Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
