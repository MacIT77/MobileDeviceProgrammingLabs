package com.example.opengllabs.viewmodel

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.opengllabs.data.News
import com.example.opengllabs.render.MyGLRenderer

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val displayedNews by viewModel.displayedNews.collectAsState()
    var planetInfo by remember { mutableStateOf("Земля\nРасстояние: 5.5 а.е.\nРазмер: 0.35") }
    var showInfo by remember { mutableStateOf(false) }

    val likesState by remember(displayedNews) {
        derivedStateOf {
            displayedNews.associate { news ->
                news.id to viewModel.getLikes(news.id)
            }
        }
    }

    var rendererRef by remember { mutableStateOf<MyGLRenderer?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                    holder.setFormat(PixelFormat.RGBA_8888)

                    val renderer = MyGLRenderer(context)
                    renderer.onPlanetSelected = { info ->
                        planetInfo = info
                    }
                    rendererRef = renderer
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(modifier = Modifier.weight(0.75f)) {
                NewsQuadrant(
                    news = displayedNews.getOrNull(0),
                    likes = likesState[displayedNews.getOrNull(0)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(0)?.id ?: 0) },
                    modifier = Modifier.weight(1f)
                )
                NewsQuadrant(
                    news = displayedNews.getOrNull(1),
                    likes = likesState[displayedNews.getOrNull(1)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(1)?.id ?: 0) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.weight(0.75f)) {
                NewsQuadrant(
                    news = displayedNews.getOrNull(2),
                    likes = likesState[displayedNews.getOrNull(2)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(2)?.id ?: 0) },
                    modifier = Modifier.weight(1f)
                )
                NewsQuadrant(
                    news = displayedNews.getOrNull(3),
                    likes = likesState[displayedNews.getOrNull(3)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(3)?.id ?: 0) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .background(Color(0xCC000000))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { rendererRef?.selectPreviousPlanet() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF30363D), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Предыдущая",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { showInfo = true },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF238636), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Информация",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { rendererRef?.selectNextPlanet() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF30363D), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующая",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = {
                    Text(
                        "Информация о планете",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        planetInfo,
                        color = Color(0xFF8B949E),
                        fontSize = 16.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfo = false }) {
                        Text("Закрыть", color = Color(0xFF58A6FF))
                    }
                },
                containerColor = Color(0xFF161B22),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun NewsQuadrant(
    news: News?,
    likes: Int,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (news == null) return

    Column(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, Color(0xFF30363D).copy(alpha = 0.6f))
            .background(Color(0xFF161B22).copy(alpha = 0.7f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = news.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = news.content,
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E),
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)
                .background(Color(0xFF21262D).copy(alpha = 0.6f))
                .clickable { onLikeClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Лайк",
                tint = Color(0xFFFF4B4B),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$likes",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}