package com.example.opengllabs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.opengllabs.data.News
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class NewsViewModel : ViewModel() {

    private val allNews = listOf(
        News(1, "Новость 1", "Мессенджер MAX удалён"),
        News(2, "Новость 2", "Открытие нового музея холодильников на Заельцовской"),
        News(3, "Новость 3", "Мы вступили и что-то наступило"),
        News(4, "Новость 4", "Новая версия RED STAR OS представлена"),
        News(5, "Новость 5", "Рекордный урожай дурианов в этом году"),
        News(6, "Новость 6", "Международная конференция по помидорам"),
        News(7, "Новость 7", "Открыта метро 'Спортивная'"),
        News(8, "Новость 8", "Чемпионат мира по удерживанию хорька в штанах стартует"),
        News(9, "Новость 9", "Инновации в области производства расчёсок"),
        News(10, "Новость 10", "Принято решение отменить производство плюшевых кирпичей")
    )

    private val _displayedNews = MutableStateFlow(getInitialNews())
    val displayedNews: StateFlow<List<News>> = _displayedNews.asStateFlow()

    private val newsLikes = mutableMapOf<Int, Int>()

    init {
        startNewsRotation()
    }

    private fun getInitialNews(): List<News> = allNews.shuffled().take(4)

    private fun startNewsRotation() {
        kotlinx.coroutines.MainScope().launch {
            while (true) {
                delay(5000)
                rotateRandomNews()
            }
        }
    }

    private fun rotateRandomNews() {
        val currentNews = _displayedNews.value.toMutableList()
        val randomIndex = Random.nextInt(4)

        val availableNews = allNews.filter { news ->
            currentNews.none { it.id == news.id }
        }

        if (availableNews.isNotEmpty()) {
            currentNews[randomIndex] = availableNews.random()
            _displayedNews.value = currentNews
        }
    }

    fun incrementLikes(newsId: Int) {
        val currentLikes = newsLikes[newsId] ?: 0
        newsLikes[newsId] = currentLikes + 1
        _displayedNews.value = _displayedNews.value.toList()
    }

    fun getLikes(newsId: Int): Int = newsLikes[newsId] ?: 0
}