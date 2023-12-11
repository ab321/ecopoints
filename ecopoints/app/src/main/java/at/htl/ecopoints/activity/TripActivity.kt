package at.htl.ecopoints.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import at.htl.ecopoints.interfaces.OnLocationChangedListener
import at.htl.ecopoints.service.BluetoothDeviceListService
import at.htl.ecopoints.service.BluetoothService
import at.htl.ecopoints.service.Obd2Service
import at.htl.ecopoints.service.TestLocationService
import at.htl.ecopoints.ui.theme.EcoPointsTheme
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand
import com.github.pires.obd.enums.ObdProtocols
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Error
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TripActivity : ComponentActivity(), OnLocationChangedListener {
    private val TAG = "TripActivity"
    private var selectedDevice: BluetoothDevice? = null
    private val bluetoothDeviceService = BluetoothDeviceListService()
    private val bluetoothService: BluetoothService = BluetoothService()
    private val testLocationService: TestLocationService by lazy {
        TestLocationService(this)
    }
    private var longitude = 14.285830
    private var latitude = 48.306940
    private var latLngHasChanged = mutableStateOf(false)
    private var tripActive = false
    private var timer = java.util.Timer()

    private val latLngList = mutableStateListOf<Pair<Color, Pair<LatLng, Double>>>()


    @SuppressLint("MissingPermission", "UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showDialog: Boolean by remember { mutableStateOf(false) }
            var deviceNameText by remember { mutableStateOf("Not Selected") }
            var isConnecting by remember { mutableStateOf(false) }
            var connection by remember { mutableStateOf("Not Connected") }
            val currentLocation = LatLng(latitude, longitude)
            var cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(currentLocation, 10f)
            }
            var mapProperties by remember {
                mutableStateOf(MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = true))
            }
            val activity = LocalContext.current as Activity

            testLocationService.setOnLocationChangedListener(this)

            EcoPointsTheme {
                activity.requestedOrientation =
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(backgroundColor = MaterialTheme.colorScheme.background, topBar = {
                        TopAppBar(backgroundColor = MaterialTheme.colorScheme.background, title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                MapTypeControls(onMapTypeClick = {
                                    Log.d("GoogleMap", "Selected map type $it")
                                    mapProperties = mapProperties.copy(mapType = it)
                                })
                            }
                        })
                    }) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                        ) {
                            DrawPolyline()
                        }
                    }

                    if (isConnecting) {
                        Connect(connecting = isConnecting,
                            selectedDevice,
                            onDismiss = { isConnecting = false },
                            onConnect = { it ->
                                connection = it
                            })
                    }


                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        //TestReadCustomComm()
                        Spacer(modifier = Modifier.height(16.dp)) // Space between text and buttons

                        StartStopButton()
                        ConnectionInfo(deviceNameText, connection)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { isConnecting = true },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .weight(1f)
                            ) {
                                Text(text = "Connect")
                            }
                            Button(
                                onClick = { showDialog = true },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .width(150.dp)
                            ) {
                                Text(text = "Select Device")
                            }
                        }

                        if (ActivityCompat.checkSelfPermission(
                                this@TripActivity, Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@TripActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1
                            )
                        } else {
                            BluetoothDeviceSelectionDialog(pairedDevices = bluetoothDeviceService.getAllDevices(),
                                showDialog = showDialog,
                                onDismiss = { showDialog = false },
                                onDeviceSelected = { device ->
                                    selectedDevice = device
                                    showDialog = false
                                    deviceNameText = device.name
                                })
                        }
                    }
                }
            }
        }
    }

    private fun onStartBtnClick() {
        tripActive = true
        Log.d("TripActivity", "Trip started")
    }

    private fun onStopBtnClick() {
        tripActive = false
        // save the trip to the database
        //TODO: save the trip to the database

        Log.d("TripActivity", "Trip stopped")
    }

    @Composable
    private fun TestReadCustomComm() {
        if (selectedDevice != null) {

            var rpm by remember { mutableStateOf("0") }
            var speed by remember { mutableStateOf("0") }
            var coolantTemp by remember { mutableStateOf("0") }
            var buttonClicked by remember { mutableStateOf(false) }

            val service = Obd2Service(selectedDevice!!.address)
            val coroutineScope = rememberCoroutineScope()

            Button(onClick = { buttonClicked = true }) {
                Text("Read with Custom Comm")
            }

            var i = AtomicInteger(0)

            LaunchedEffect(buttonClicked) {
                if (buttonClicked) {
                    coroutineScope.launch(Dispatchers.IO) {
                        service.initOBD()
                        if (service.socket != null && service.socket?.isConnected!!) {
                            try {

                                var socket = service.getSocketTest()
                                EchoOffCommand().run(
                                    socket.getInputStream(), socket.getOutputStream()
                                );
                                LineFeedOffCommand().run(
                                    socket.getInputStream(), socket.getOutputStream()
                                );
                                TimeoutCommand(125).run(
                                    socket.getInputStream(), socket.getOutputStream()
                                );
                                SelectProtocolCommand(ObdProtocols.AUTO).run(
                                    socket.getInputStream(), socket.getOutputStream()
                                );

                                timer.schedule(object : java.util.TimerTask() {
                                    override fun run() {
//                            if (i.get() == 1) {
//                                rpm = service.getRPM()
//                            }
//                            if (i.get() == 2) {
//                                speed = service.getSpeed()
//                            }
//                            if (i.get() == 3) {
//                                coolantTemp = service.getCoolantTemp()
//                                i.set(0v)
//                            }
//                            i.incrementAndGet()
                                        try {
//                                            var socket = service.getSocketTest()
//                                            var spd = SpeedCommand();
//                                            spd.run(socket.inputStream, socket.outputStream);
//
//                                            var r = RPMCommand();
//                                            r.run(socket.inputStream, socket.outputStream);
//                                            var res = r.result;
//                                            Log.d("Test", res)
//                                            res.replace("410C", "").toInt()
//                                            Log.d("Test", res)
//                                            val combinedDecimal = res.toInt(16)
//                                            Log.d("Test", combinedDecimal.toString())
//                                            rpm = (combinedDecimal / 4).toString();
//                                            Log.d("Test", rpm)


                                            var t = EngineCoolantTemperatureCommand();
                                            t.run(socket.inputStream, socket.outputStream);
                                            coolantTemp = t.result;

                                        } catch (err: Error) {
                                            Log.e("Obd2Service", err.toString())
                                        }
                                    }
                                }, 500, 1000)
                                buttonClicked = false
                            } catch (_: Error) {
                            }
                        }
                    }
                }
            }

            Text(text = "Current-Speed $speed")
            Text(text = "Current-Rpm $rpm")
            Text(text = "Coolant-Temp $coolantTemp")
        }
    }

    @SuppressLint("MissingPermission", "CoroutineCreationDuringComposition")
    @Composable
    private fun Connect(
        connecting: Boolean,
        device: BluetoothDevice?,
        onDismiss: () -> Unit,
        onConnect: (String) -> Unit
    ) {
        if (device == null) {
            NoDeviceSelectedAlert(onDismiss = onDismiss)
        } else {
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            var inputStream: InputStream? by remember { mutableStateOf(null) }
            var outputStream: OutputStream? by remember { mutableStateOf(null) }
            var bluetoothSocket: BluetoothSocket? by remember { mutableStateOf(null) }
            var isConnected by remember {
                mutableStateOf(false);
            }

            LaunchedEffect(Unit) {
                val job = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        onConnect("Connecting ...")
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                        bluetoothSocket?.connect()
                        inputStream = bluetoothSocket?.inputStream
                        outputStream = bluetoothSocket?.outputStream
                        onConnect("Connected")
                        Log.d(TAG, inputStream.toString())
                        Log.d(TAG, outputStream.toString())
                        isConnected = true;
                    } catch (e: IOException) {
                        Log.e(TAG, e.toString())
                        onConnect("Failed to connect")
                    }
                }
            }

            if (isConnected) {
                Read(
                    bluetoothSocket = bluetoothSocket,
                    inputStream = inputStream,
                    outputStream = outputStream
                )
            }
        }
    }

    @Composable
    fun Read(
        bluetoothSocket: BluetoothSocket?, inputStream: InputStream?, outputStream: OutputStream?
    ) {
        var rpm by remember { mutableStateOf("0") }
        var speed by remember { mutableStateOf("0") }
        var coolantTemp by remember { mutableStateOf("0") }
        val coroutineScope = rememberCoroutineScope()
        if (bluetoothSocket != null && inputStream != null && outputStream != null) {
            EchoOffCommand().run(inputStream, outputStream)
            LineFeedOffCommand().run(inputStream, outputStream)
            TimeoutCommand(125).run(inputStream, outputStream)
            SelectProtocolCommand(ObdProtocols.AUTO).run(inputStream, outputStream)

            val timer = Timer()
            val task = object : TimerTask() {
                val obdConnection = ObdDeviceConnection(inputStream, outputStream)
                suspend fun getRpm(): String {
                    try {
                        val res = obdConnection.run(RPMCommand()).value
                        Log.d(TAG, "RPM: $res")
                        return res

                    } catch (_: Exception) {

                    }
                    return "00";
                }

                suspend fun getSpeed(): String {
                    try {
                        val res = obdConnection.run(SpeedCommand()).value
                        Log.d(TAG, "Speed: $res")
                        return res

                    } catch (_: Exception) {

                    }
                    return "00";
                }

                override fun run() {
                    try {
                        coroutineScope.launch(Dispatchers.IO) {
                            rpm = getRpm()
                           // speed = getSpeed()
                        }
//                        val speedCommand = SpeedCommand();
//                        val rpmCommand = RPMCommand();
//                        val coolantTemperatureCommand = EngineCoolantTemperatureCommand()
//
////                        speedCommand.run(inputStream, outputStream)
////                        val speedResponse = speedCommand.result.replace("410D", "")
////                        speed = speedResponse.toInt(16).toString()
//
//                        rpmCommand.run(inputStream, outputStream)
//                        val rpmResponse = rpmCommand.result.replace("410C", "")


//                        coolantTemperatureCommand.run(inputStream, outputStream)
//                        val coolantResponse = rpmCommand.result.replace("410D", "")
//                        coolantTemp = (coolantResponse.toInt(16) - 40).toString()


//                        val spd = SpeedCommand();
//                        spd.run(
//                            inputStream, outputStream
//                        );
//                        val speedtes = spd.result;
//                        Log.d(TAG, "Speed: $speedtes")
//                        speed = speedtes;
//
//                        var r = RPMCommand();
//                        r.run(
//                            inputStream, outputStream
//                        );
//                        var rpmres = r.result;
//                        Log.d(TAG, "RPM: $rpmres")
//                        rpm = rpmres;
//
//
//                        var t = EngineCoolantTemperatureCommand();
//                        t.run(
//                            inputStream, outputStream
//                        );
//                        coolantTemp = t.result;
//                        Log.d(TAG, "Cooland: $coolantTemp")
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }
            }

            timer.schedule(task, 1000, 1000)
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                // Handle the uncaught exception here
                Log.e("UncaughtException", "Unhandled exception: $throwable")
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Current-Speed $speed")
            Text(text = "Current-Rpm $rpm")
            Text(text = "Coolant-Temp $coolantTemp")
        }
    }


    @Composable
    fun StartStopButton() {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { onStartBtnClick() }, modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text(text = "Start")
            }

            Button(
                onClick = { onStopBtnClick() }, modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text(text = "Stop")
            }
        }
    }

    @Composable
    fun ConnectionInfo(deviceName: String, connection: String) {
        var connectionStateColor: Color = Color.Red
        if (connection == "Connected") connectionStateColor = Color.Green
        else if (connection == "Connecting ...") connectionStateColor = Color.Yellow


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Connection:",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp,
                )

                Text(
                    text = "Device:",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp,
                )
            }

            Column(
                modifier = Modifier.weight(0.4f)
            ) {
                Text(
                    text = connection,
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp,
                    color = connectionStateColor
                )

                Text(
                    text = deviceName,
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp,
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun BluetoothDeviceSelectionDialog(
        pairedDevices: List<BluetoothDevice>,
        showDialog: Boolean,
        onDismiss: () -> Unit,
        onDeviceSelected: (BluetoothDevice) -> Unit
    ) {
        if (showDialog) {
            AlertDialog(onDismissRequest = onDismiss,
                title = { Text("Select a Bluetooth Device") },
                text = {
                    LazyColumn {
                        items(
                            pairedDevices.filter { it -> it.name.lowercase().contains("obd") }
                        ) { device ->
                            TextButton(
                                onClick = { onDeviceSelected(device) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(device.name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                })
        }
    }

//    @SuppressLint("CoroutineCreationDuringComposition")
//    @Composable
//    fun ConnectToDevice(
//        connecting: Boolean,
//        device: BluetoothDevice?,
//        onDismiss: () -> Unit,
//        onConnect: (Boolean) -> Unit
//    ) {
//        if (device == null) {
//            NoDeviceSelectedAlert(onDismiss = onDismiss)
//        } else if (connecting) {
//
//            CoroutineScope(Dispatchers.Main).launch {
//                bluetoothService.connectDevice(device)
//                onConnect(bluetoothService.connected())
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color(0x88000000)),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(50.dp)
//                )
//            }
//        }
//    }

    @Composable
    fun NoDeviceSelectedAlert(onDismiss: () -> Unit) {
        AlertDialog(onDismissRequest = { onDismiss() },
            title = { Text(text = "No Device Selected") },
            text = { Text(text = "Please select a device to connect.") },
            confirmButton = {
                Button(onClick = { onDismiss() }) {
                    Text("OK")
                }
            })
    }

//region Map

    @Composable
    private fun DrawPolyline() {
        if (!latLngHasChanged.value) {
            for (i in 0 until latLngList.size - 1) {
                Polyline(
                    points = listOf(latLngList[i].second.first, latLngList[i + 1].second.first),
                    color = latLngList[i].first,
                    width = 10f
                )
            }
        }
        latLngHasChanged.value = false
    }

    @Composable
    private fun MapTypeControls(
        onMapTypeClick: (MapType) -> Unit
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(state = ScrollState(0)),
            horizontalArrangement = Arrangement.Center
        ) {
            MapType.values().forEach {
                MapTypeButton(type = it) { onMapTypeClick(it) }
            }
        }
    }

    @Composable
    private fun MapTypeButton(type: MapType, onClick: () -> Unit) =
        MapButton(text = type.toString(), onClick = onClick)

    @Composable
    private fun MapButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.padding(4.dp), onClick = onClick
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }

    private fun addItemToList(newItem: LatLng) {
        val fuelCons = generateRandomFuelCons()
        val color = when {
            fuelCons <= 6.0 -> Color.Green
            fuelCons > 6.0 && fuelCons <= 12 -> Color.Yellow
            fuelCons > 12 && fuelCons <= 20 -> Color.Red
            else -> Color.Black
        }
        latLngList.add(Pair(color, Pair(newItem, fuelCons)))
        latLngHasChanged.value = true

        Log.d("FuelTracking", "Fuel consumption: $fuelCons")
    }

    //for testing purposes, remove if database is set up
    private fun generateRandomFuelCons(): Double {
        return (3..21).random().toDouble()
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(
                dLon / 2
            ) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c * 1000
    }

    private fun isLocationChanged(
        newLat: Double, newLon: Double, oldLat: Double, oldLon: Double, threshold: Double
    ): Boolean {
        val distance = haversine(newLat, newLon, oldLat, oldLon)
        return distance > threshold
    }

    override fun onLocationChanged(lat: String, lon: String) {
        val newLatitude = lat.toDouble()
        val newLongitude = lon.toDouble()

        if (isLocationChanged(newLatitude, newLongitude, latitude, longitude, 1.0)) {
            latitude = newLatitude
            longitude = newLongitude

            Log.d("MapTracking", "Location changed to $latitude, $longitude")
            addItemToList(LatLng(latitude, longitude))
        }
    }

//endregion
}