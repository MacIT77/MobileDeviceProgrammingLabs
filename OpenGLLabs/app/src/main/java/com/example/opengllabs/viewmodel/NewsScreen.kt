package com.example.opengllabs.viewmodel

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    val likesState by remember(displayedNews) {
        derivedStateOf {
            displayedNews.associate { news ->
                news.id to viewModel.getLikes(news.id)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Add GLSurfaceView as background
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                    setZOrderOnTop(false)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)

                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)

                    setRenderer(MyGLRenderer(context))
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Row(modifier = Modifier.weight(1f)) {
                NewsQuadrant(
                    news = displayedNews.getOrNull(0),
                    likes = likesState[displayedNews.getOrNull(0)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(0)?.id ?: 0) },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x80000000))
                )
                NewsQuadrant(
                    news = displayedNews.getOrNull(1),
                    likes = likesState[displayedNews.getOrNull(1)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(1)?.id ?: 0) },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x80000000))
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                NewsQuadrant(
                    news = displayedNews.getOrNull(2),
                    likes = likesState[displayedNews.getOrNull(2)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(2)?.id ?: 0) },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x80000000))
                )
                NewsQuadrant(
                    news = displayedNews.getOrNull(3),
                    likes = likesState[displayedNews.getOrNull(3)?.id] ?: 0,
                    onLikeClick = { viewModel.incrementLikes(displayedNews.getOrNull(3)?.id ?: 0) },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x80000000))
                )
            }
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
            .background(Color(0xFF161B22).copy(alpha = 0.55f))
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = news.content,
                    fontSize = 16.sp,
                    color = Color(0xFF8B949E),
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)
                .background(Color(0xFF21262D).copy(alpha = 0.4f))
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
