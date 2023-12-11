package com.darkbyte.groupit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darkbyte.groupit.facedetection.Utils
import com.darkbyte.groupit.tflite.UserFace
import com.darkbyte.groupit.ui.theme.GroupItTheme

class UsersUI : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroupItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val list = remember { mutableStateListOf<UserFace>() }
                    var userFace by remember { mutableStateOf<UserFace?>(null) }
                    val gridList = remember { mutableStateListOf<UserFace>() }
                    val gridListState = rememberLazyGridState()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(key1 = userFace) {
                        Utils.fetchDetector()
                            ?.fetchAllPhotosFromSingleFace(userFace?.title.orEmpty())?.let {
                                gridList.clear()
                                gridList.addAll(it)
                            }
                    }
                    LaunchedEffect(key1 = Unit) {
                        Utils.fetchDetector()?.fetchRegisteredList()?.let {
                            list.clear()
                            list.addAll(it)
                        }
                    }
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize(),
                        topBar = {
                            TopAppBar(title = {
                                Text(text = "Recent Searches")
                            })
                        }
                    ) {
                        Column(modifier = Modifier.padding(it)) {
                            LazyRow {
                                items(list) { face ->
                                    face.bitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color.Gray, CircleShape)
                                                .clickable {
                                                    userFace = face
                                                }
                                        )
                                    }
                                }
                            }
                            if (gridList.size > 0) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    state = gridListState,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    items(gridList) { face ->
                                        Column {
                                            face.bitmap?.let { bitmap ->
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.FillBounds,
                                                    modifier = Modifier
                                                        .size(400.dp)

                                                        .border(2.dp, Color.Gray)
                                                )
                                            }
                                            Text(text = "${face.title} - ${face.facesFoundAlong} - ${face.distance}")
                                        }
                                    }

                                }


                            }
                        }
                    }
                }
            }
        }
    }
}
