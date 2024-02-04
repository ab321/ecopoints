package at.htl.ecopoints

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.lazy.items
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.htl.ecopoints.activity.TripActivity
import at.htl.ecopoints.db.DBHelper
import at.htl.ecopoints.model.Trip
import at.htl.ecopoints.navigation.BottomNavBar
import at.htl.ecopoints.service.TankerkoenigApiClient
import at.htl.ecopoints.ui.theme.EcoPointsTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    private val latLngList = mutableStateListOf<Pair<Color, Pair<LatLng, Double>>>()

    //sample data for latLngList for a polyline (different latlngs with different colors)
    init {
        latLngList.add(Pair(Color.Red, Pair(LatLng(49.0, 14.285830), 0.0)))
        latLngList.add(Pair(Color.Blue, Pair(LatLng(49.1, 14.285830), 0.0)))
        latLngList.add(Pair(Color.Green, Pair(LatLng(49.2, 14.285830), 0.0)))
        latLngList.add(Pair(Color.Yellow, Pair(LatLng(49.3, 14.285830), 0.0)))
        latLngList.add(Pair(Color.Cyan, Pair(LatLng(49.4, 14.285830), 0.0)))
    }


    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            EcoPointsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    ShowPrices()

                    ShowPhoto()
                    ShowText()

                    LastTrips()

                    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("Home") }
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ){

                        BottomNavBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { newScreen -> setCurrentScreen(newScreen) },
                            context = this@MainActivity
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ShowPhoto() {

        val painter = painterResource(id = R.drawable.app_icon)

        Box(
            modifier = Modifier
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .scale(2.0f)
                    .padding(top = 30.dp)
            )
        }
    }

    @Composable
    fun ShowPrices() {
        var dieselPrice = 0.0;
        var e5Price = 0.0;

        val tankerkoenigApiClient = TankerkoenigApiClient()
        try {
            thread {
            val gasData = tankerkoenigApiClient.getApiData()
                dieselPrice = gasData.diesel
                e5Price = gasData.e5
            }

            while(dieselPrice == 0.0 && e5Price == 0.0) {
                Thread.sleep(0.1.toLong())
            }

        }catch (e: Exception) {
            Log.e("Tankpreis Error", "Error: ${e.message}")
        }

        Text(
            text = "Diesel\n" + dieselPrice.toString() + "€",
            fontSize = 25.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(80.dp, 150.dp, 0.dp,0.dp),

            )

        Text(
            text = "Benzin\n" + e5Price.toString() + "€",
            fontSize = 25.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(260.dp, 150.dp, 0.dp,0.dp),

            )
    }

    @Composable
    fun ShowText(){
        val gradientColors = listOf(Gray, Green, DarkGray)

        Text(
            text = "Last Rides:",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(10.dp, 240.dp, 0.dp,0.dp),

            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = gradientColors
                )
            )
        )
    }

    @Composable
    fun LastTrips() {
        var showDialog by remember { mutableStateOf(false) }
        var selectedTripDate by remember { mutableStateOf<Date?>(null) }
        val gradientColors = listOf(
            Color(0xFF9bd99e),
            Color(0xFF05900a),
            Color(0xFF9bd99e)
        )

        readTripDataFromCsvAndAddToDB("tripData.csv")
        val trips = getTripDataFromDB()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(0.dp, 260.dp, 0.dp, 0.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(trips) { trip ->
                    Button(
                        onClick = {
                            showDialog = true
                            selectedTripDate = trip.date
                        },
                        modifier = Modifier
                            .padding(8.dp, 4.dp)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = gradientColors
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Transparent,
                            contentColor = Black
                        )
                    ) {
                        val formattedDate = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(trip.date)
                        Column(
                            modifier = Modifier
                                .padding(0.dp)
                                .fillMaxWidth()
                        ) {
                            Row() {
                                Text(formattedDate + " Uhr")
                            }
                            Row {
                                Text(trip.rewardedEcoPoints.toString() + " EP")
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = trip.distance.toString() + " km")
                            }
                        }
                    }
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 10.dp, 20.dp, 250.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        // Navigieren zur "View All" Activity
                    },
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = gradientColors
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .weight(1f)
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Transparent,
                        contentColor = Black
                    )
                ) {
                    Text("View All")
                }

                Spacer(modifier = Modifier.width(30.dp))

                Button(
                    onClick = {
                        startActivity(Intent(this@MainActivity, TripActivity::class.java)
                        )                    },
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = gradientColors
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .weight(1f)
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Transparent,
                        contentColor = Black
                    )
                ) {
                    Text("New Trip")
                }
            }


            if (showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        selectedTripDate = null
                    },
                    title = {
                        Text(
                            "Trip: " + (selectedTripDate?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                            } ?: "Unknown Date")
                        )
                    },
                    text = {

                        Column {
                            val selectedTrip = trips.find { it.date == selectedTripDate }
                            if (selectedTrip != null) {
                                Text("Distance: ${selectedTrip.distance} km")
                                Text("Average Speed: ${selectedTrip.avgSpeed} km/h")
                                Text("Average Engine Rotation: ${selectedTrip.avgEngineRotation} rpm")
                                Text("Eco Points: ${selectedTrip.rewardedEcoPoints}")
                            } else {
                                Text("Trip details not available.")
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            ShowMap(
                                cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(LatLng(48.306940, 14.285830), 10f)
                                })
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDialog = false
                            selectedTripDate = null
                        }) {
                            Text("OK")
                        }
                    },
                )
            }
        }
    }

    @Composable
    fun ShowMap(cameraPositionState: CameraPositionState){
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            cameraPositionState = cameraPositionState,
        ) {
            DrawPolyLine();
        }
    }

    @Composable
    fun DrawPolyLine() {
        for (i in 0 until latLngList.size - 1) {
            Polyline(
                points = listOf(
                    latLngList[i].second.first,
                    latLngList[i + 1].second.first
                ),
                color = latLngList[i].first,
                width = 10f
            )
        }
    }

    private fun readTripDataFromCsvAndAddToDB(fileName: String) {
        val dbHelper = DBHelper(this, null)

        dbHelper.onUpgrade(dbHelper.writableDatabase, 1, 2)

        try {
            val inputStream: InputStream = assets.open(fileName)
            val reader = CSVReaderBuilder(InputStreamReader(inputStream))
                .withCSVParser(CSVParserBuilder().withSeparator(';').build())
                .build()

            val header = reader.readNext()

            var line = reader.readNext()
            while (line != null) {
                val id = UUID.fromString(line[0])
                val distance = line[1].toDouble()
                val avgSpeed = line[2].toDouble()
                val avgEngineRotation = line[3].toDouble()
                val date = Date(line[4].toLong())
                val rewardedEcoPoints = line[5].toDouble()

                val trip = Trip(id, distance, avgSpeed, avgEngineRotation, date, rewardedEcoPoints)
                dbHelper.addTrip(trip)

                line = reader.readNext()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            dbHelper.close()
        }
    }

    private fun getTripDataFromDB(): List<Trip> {
        val dbHelper = DBHelper(this, null)
        val trips = dbHelper.getAllTrips()
        dbHelper.close()
        return trips
    }
}
