package com.mobile.bible.kjv.ui.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.bible.kjv.data.KjvDataPopulator
import com.mobile.bible.kjv.data.KjvDatabase
import com.mobile.bible.kjv.data.entity.BookEntity
import com.mobile.bible.kjv.data.entity.VerseEntity
import com.mobile.bible.kjv.data.repository.KjvRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KjvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KjvRepository(application)

    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    private val _verses = MutableStateFlow<List<VerseEntity>>(emptyList())
    val verses: StateFlow<List<VerseEntity>> = _verses.asStateFlow()

    private var versesJob: Job? = null

    init {
        viewModelScope.launch {
            ensureDatabasePopulated()
            loadBooks()
        }
    }

    private suspend fun ensureDatabasePopulated() {
        if (!repository.isDatabasePopulated()) {
            Log.d("KjvViewModel", "Database is empty, populating...")
            val database = KjvDatabase.getInstance(getApplication())
            KjvDataPopulator.populateDatabase(getApplication(), database)
        }
    }

    private suspend fun loadBooks() {
        repository.getAllBooks().collect { bookList ->
            Log.d("KjvViewModel", "Loaded ${bookList.size} books")
            if (bookList.isNotEmpty()) {
                Log.d("KjvViewModel", "First: ${bookList.first().name}, Last: ${bookList.last().name}")
            }
            _books.value = bookList
        }
    }

    fun loadVerses(bookId: Int, chapter: Int) {
        versesJob?.cancel()
        versesJob = viewModelScope.launch {
            repository.getVersesByChapter(bookId, chapter).collect { verseList ->
                Log.d("KjvViewModel", "Loaded ${verseList.size} verses for book $bookId chapter $chapter")
                _verses.value = verseList
            }
        }
    }
}