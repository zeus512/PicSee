package com.darkbyte.groupit

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.darkbyte.groupit.facedetection.Utils.initiateDetection
import com.darkbyte.groupit.facedetection.Utils.initiateTFLite
import com.darkbyte.groupit.ui.theme.GroupItTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroupItTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(key1 = Unit) {
                        initiateTFLite(context, assets)
                    }
                    var photoUri: Uri? by remember { mutableStateOf(null) }
                    val launcher =
                        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
                            photoUri = it
                            if (it != null) {
                                scope.launch(Dispatchers.IO) {
                                    initiateDetection(context, it)
                                }
                            }
                        }
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (photoUri != null) {
                            //Use Coil to display the selected image
                            val painter = rememberAsyncImagePainter(
                                ImageRequest
                                    .Builder(context)
                                    .data(data = photoUri)
                                    .build()
                            )

                            Image(
                                modifier = Modifier.fillMaxSize(),
                                painter = painter,
                                contentDescription = null
                            )
                        }
                    }
                    Box(contentAlignment = Alignment.BottomCenter) {

                        Button(
                            modifier = Modifier.padding(24.dp),
                            onClick = {
                                launcher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Text("Select Photo")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GroupItTheme {
        Greeting("Android")
    }
}