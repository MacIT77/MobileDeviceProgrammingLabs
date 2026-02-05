package com.example.opengllabs.viewmodel

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.opengllabs.data.News

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Row(modifier = Modifier.weight(1f)) {
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
        Row(modifier = Modifier.weight(1f)) {
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
            .border(2.dp, Color(0xFF30363D))
            .background(Color(0xFF161B22))
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
                .background(Color(0xFF21262D))
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
