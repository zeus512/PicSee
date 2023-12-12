package com.darkbyte.groupit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
                            Text(
                                text = "Recent Searches",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray)
                                    .padding(14.dp)
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                                .background(Color(0xFFF8F7F3))
                        ) {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 14.dp),
                            ) {
                                items(list) { face ->
                                    face.bitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
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
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    items(gridList) { face ->
                                        Column(Modifier.padding(12.dp)) {
                                            ShowUserOriginalPhoto(face, context)
                                            Text(
                                                modifier = Modifier.background(Color.Gray),
                                                color = Color.White,
                                                text = "Name: ${face.title} \nFacesFound: ${face.facesFoundAlong} \ncloseness to Image: ${face.distance}"
                                            )
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

    @Composable
    private fun ShowUserOriginalPhoto(
        face: UserFace,
        context: Context
    ) {
        face.originalUri?.let {
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(data = it)
                    .build()
            )
            Image(
                modifier = Modifier
                    .size(240.dp)
                    .border(1.dp, Color.Gray),
                painter = painter,
                contentDescription = null
            )
        } ?: face.bitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(240.dp)
                    .border(1.dp, Color.Gray),
            )
        }
    }
}
