package com.mobile.bible.kjv.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mobile.bible.kjv.ui.adapter.VerseAdapter
import com.mobile.bible.kjv.ui.adapter.VerseListItem
import com.mobile.bible.kjv.ui.dialog.AdjustTextSizeBottomDialog
import com.mobile.bible.kjv.ui.dialog.ChooseChapterBottomDialog
import com.mobile.bible.kjv.ui.activity.OpenKjvVideoActivity
import com.mobile.bible.kjv.ui.vm.KjvViewModel
import com.mobile.bible.kjv.constant.PrefKeys
import com.mobile.bible.kjv.R
import com.mobile.bible.kjv.databinding.FragmentBibleBinding
import com.remax.base.ext.KvBoolDelegate
import com.remax.base.ext.KvIntDelegate
import com.remax.base.ext.KvStringDelegate
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager
import java.util.Locale
import kotlin.math.abs

class KjvFragment : Fragment() {
    private var _binding: FragmentBibleBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: KjvViewModel
    private lateinit var verseAdapter: VerseAdapter
    
    private val uiHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isPlaying = false
    private var pendingPlayAfterInit = false
    private var currentVerseIndex = 0
    private var currentVerses: List<VerseListItem.Verse> = emptyList()
    private var chapterDialogFrom: String = "Current_Chapter"

    companion object {
        var lastBookId by KvIntDelegate("last_book_id", 1)
        var lastBookName by KvStringDelegate("last_book_name", "Genesis")
        var lastChapter by KvIntDelegate("last_chapter", 1)
        var hasPlayedOpenBibleVideo by KvBoolDelegate(PrefKeys.HAS_PLAYED_OPEN_BIBLE_VIDEO, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBibleBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[KjvViewModel::class.java]
        fitSystemUI()
        initPageUI()
        bindFlow()
        restoreLastReading()
        initTts()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        maybePlayOpenBibleVideoOnce()
    }

    private fun maybePlayOpenBibleVideoOnce() {
        if (hasPlayedOpenBibleVideo) return
        hasPlayedOpenBibleVideo = true
        startActivity(Intent(requireContext(), OpenKjvVideoActivity::class.java))
        requireActivity().overridePendingTransition(0, 0)
    }

    private fun fitSystemUI() {
        val lp = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
        val baseTopMargin = lp.topMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = baseTopMargin + topInset
            v.layoutParams = params
            insets
        }
        ViewCompat.requestApplyInsets(binding.toolbar)
    }

    private fun initPageUI() {
        binding.rcyVerse.layoutManager = LinearLayoutManager(requireContext())
        // Prevent default item change fade when highlight moves to verse 1 after chapter switch.
        (binding.rcyVerse.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        verseAdapter = VerseAdapter()
        verseAdapter.setVerseTextSize(AdjustTextSizeBottomDialog.savedTextSize)
        binding.rcyVerse.adapter = verseAdapter
        verseAdapter.submit(
            listOf(
                VerseListItem.Title(
                    "The Creation",
                    listOf(
                        VerseListItem.Verse(1, "In the beginning God created the heavens and the earth,"),
                        VerseListItem.Verse(2, "And the earth was a formless and desolate emptiness, and darkness was over the surface of the deep, and the Spirit of God was hovering over the surface of the waters."),
                        VerseListItem.Verse(3, "Then God said, \"Let there be light\"; and there was light."),
                        VerseListItem.Verse(4, "God saw that the light was good; and God separated the light from the darkness."),
                        VerseListItem.Verse(5, "God called the light \"day,\" and the darkness He called \"night.\" And there was evening and there was morning, one day.")
                    )
                )
            )
        )

        binding.btnAdjustTextSize.setOnClickListener {
            AdjustTextSizeBottomDialog.show(requireActivity())
        }

        binding.btnSelectBook.setOnClickListener {
            openChooseChapterDialog("Bible_Filte")
        }

        binding.tagA.setOnClickListener {
            openChooseChapterDialog("Current_Chapter")
        }

        parentFragmentManager.setFragmentResultListener("choose_chapter", this) { _, result ->
            val bookId = result.getInt("book_id")
            val bookName = result.getString("book_name") ?: return@setFragmentResultListener
            val chapter = result.getInt("chapter")
            ReportDataManager.reportData(
                "Change_Chapter_Click",
                mapOf(
                    "Book_Name" to bookName,
                    "Chapter" to chapter,
                    "From" to chapterDialogFrom
                )
            )
            android.util.Log.d("KjvFragment", "Selected: bookId=$bookId, bookName=$bookName, chapter=$chapter")
            lastBookId = bookId
            lastBookName = bookName
            lastChapter = chapter
            binding.tagA.text = "$bookName $chapter"
            viewModel.loadVerses(bookId, chapter)
        }

        parentFragmentManager.setFragmentResultListener("adjust_text_size", this) { _, result ->
            val textSizeSp = result.getInt("text_size_sp", 16)
            verseAdapter.setVerseTextSize(textSizeSp)
        }
        
        setupPlayerControls()
    }

    private fun openChooseChapterDialog(from: String) {
        chapterDialogFrom = from
        val books = viewModel.books.value
        android.util.Log.d("KjvFragment", "Opening dialog with ${books.size} books")
        if (books.isNotEmpty()) {
            android.util.Log.d("KjvFragment", "First: ${books.first().name}, Last: ${books.last().name}")
        }
        ChooseChapterBottomDialog.show(requireActivity(), books, lastBookId, lastChapter)
    }
    
    private fun setupPlayerControls() {
        // Next chapter button
        binding.btnPlayerNext.setOnClickListener {
            goToNextChapter()
        }
        
        // Previous chapter button
        binding.btnPlayerPrevious.setOnClickListener {
            goToPreviousChapter()
        }
        
        // Play button - toggles TTS playback
        binding.btnPlayerPlay.setOnClickListener {
            togglePlayback()
        }
        
        // Extended controls (shown during auto-play)
        binding.btnPlayerNextEx.setOnClickListener {
            goToNextChapter()
        }
        
        binding.btnPlayerPreviousEx.setOnClickListener {
            goToPreviousChapter()
        }
        
        // Play button in extended controls - toggles pause/resume
        binding.btnPlayerPlayEx.setOnClickListener {
            togglePlayback()
        }
    }
    
    private fun goToNextChapter() {
        ReportDataManager.reportData("Next_Page_Click", emptyMap())
        viewLifecycleOwner.lifecycleScope.launch {
            val books = viewModel.books.value
            val currentBook = books.find { it.id == lastBookId } ?: return@launch
            
            if (lastChapter < currentBook.chapters) {
                // Move to next chapter in the same book
                lastChapter += 1
            } else {
                // Move to chapter 1 of the next book
                val currentIndex = books.indexOfFirst { it.id == lastBookId }
                if (currentIndex < books.size - 1) {
                    val nextBook = books[currentIndex + 1]
                    lastBookId = nextBook.id
                    lastBookName = nextBook.name
                    lastChapter = 1
                } else {
                    // Already at the last chapter of the last book (Revelation)
                    showToast(getString(R.string.already_at_last_chapter))
                    return@launch
                }
            }
            
            updateChapterDisplay()
        }
    }
    
    private fun goToPreviousChapter() {
        ReportDataManager.reportData("Previous_Page_Click", emptyMap())
        viewLifecycleOwner.lifecycleScope.launch {
            val books = viewModel.books.value
            
            if (lastChapter > 1) {
                // Move to previous chapter in the same book
                lastChapter -= 1
            } else {
                // Move to last chapter of the previous book
                val currentIndex = books.indexOfFirst { it.id == lastBookId }
                if (currentIndex > 0) {
                    val previousBook = books[currentIndex - 1]
                    lastBookId = previousBook.id
                    lastBookName = previousBook.name
                    lastChapter = previousBook.chapters
                } else {
                    // Already at the first chapter of the first book (Genesis 1)
                    showToast(getString(R.string.already_at_first_chapter))
                    return@launch
                }
            }
            
            updateChapterDisplay()
        }
    }
    
    private fun updateChapterDisplay() {
        stopPlayback(resetPosition = true, showToast = false)
        binding.tagA.text = "$lastBookName $lastChapter"
        viewModel.loadVerses(lastBookId, lastChapter)
        // Scroll to top when chapter changes
        binding.rcyVerse.scrollToPosition(0)
    }

    private fun initTts() {
        tts = TextToSpeech(requireContext().applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = tts?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
                isTtsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isTtsReady) {
                    showToast(getString(R.string.autoscroll_off))
                } else if (pendingPlayAfterInit) {
                    pendingPlayAfterInit = false
                    startPlayback()
                }
            } else {
                isTtsReady = false
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = utteranceId?.removePrefix("verse_")?.toIntOrNull() ?: return
                uiHandler.post {
                    if (_binding == null || !isPlaying) return@post
                    currentVerseIndex = index
                    verseAdapter.setCurrentReadingVerseIndex(currentVerseIndex)
                    scrollToCurrentVerse()
                    updatePlaybackProgress()
                }
            }

            override fun onDone(utteranceId: String?) {
                uiHandler.post {
                    if (!isPlaying) return@post
                    playNextVerse()
                }
            }

            override fun onError(utteranceId: String?) {
                uiHandler.post {
                    if (!isPlaying) return@post
                    playNextVerse()
                }
            }
        })
    }

    private fun togglePlayback() {
        if (isPlaying) {
            ReportDataManager.reportData("Pause_Button_Click", emptyMap())
            stopPlayback(resetPosition = false, showToast = true)
        } else {
            ReportDataManager.reportData("Play_Button_Click", emptyMap())
            startPlayback()
        }
    }

    private fun startPlayback() {
        if (currentVerses.isEmpty()) {
            showToast(getString(R.string.autoscroll_off))
            return
        }
        if (!isTtsReady) {
            pendingPlayAfterInit = true
            showToast(getString(R.string.autoscroll_off))
            return
        }
        if (currentVerseIndex >= currentVerses.size) {
            currentVerseIndex = resolveCurrentVerseIndexFromViewport()
        }

        isPlaying = true
        verseAdapter.setCurrentReadingVerseIndex(currentVerseIndex)
        binding.playerControlsEx.visibility = View.VISIBLE
        binding.playerControls.visibility = View.GONE
        binding.btnPlayerPlayEx.setImageResource(R.drawable.svg_pause_bible)
        showToast(getString(R.string.autoscroll_on))
        speakVerse(currentVerseIndex)
    }

    private fun stopPlayback(resetPosition: Boolean, showToast: Boolean) {
        pendingPlayAfterInit = false
        isPlaying = false
        tts?.stop()
        binding.playerControls.visibility = View.VISIBLE
        binding.playerControlsEx.visibility = View.GONE
        if (resetPosition) {
            currentVerseIndex = 0
            // Chapter switched: clear old chapter highlight, new chapter data will re-apply.
            verseAdapter.setCurrentReadingVerseIndex(-1)
        } else {
            // Keep current reading target highlighted even when paused/stopped.
            verseAdapter.setCurrentReadingVerseIndex(currentVerseIndex)
        }
        if (showToast) {
            showToast(getString(R.string.autoscroll_off))
        }
    }

    private fun speakVerse(index: Int) {
        val verse = currentVerses.getOrNull(index)
        if (verse == null) {
            stopPlayback(resetPosition = false, showToast = true)
            return
        }
        val utteranceId = "verse_$index"
        val text = verse.text.trim()
        if (text.isBlank()) {
            playNextVerse()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun playNextVerse() {
        if (!isPlaying) return
        val nextIndex = currentVerseIndex + 1
        if (nextIndex >= currentVerses.size) {
            stopPlayback(resetPosition = false, showToast = true)
            return
        }
        currentVerseIndex = nextIndex
        verseAdapter.setCurrentReadingVerseIndex(currentVerseIndex)
        speakVerse(currentVerseIndex)
    }

    private fun scrollToCurrentVerse() {
        // Adapter contains one title row at position 0, verses begin at position 1.
        val adapterPosition = (currentVerseIndex + 1).coerceAtLeast(0)
        val recyclerView = binding.rcyVerse
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        if (layoutManager == null) return

        val centerY = recyclerView.height / 2
        val targetView = layoutManager?.findViewByPosition(adapterPosition)
        if (targetView == null) {
            // Bring target row near center first, then do a gentle correction.
            layoutManager.scrollToPositionWithOffset(adapterPosition, centerY)
            recyclerView.post { centerVisibleTarget(adapterPosition) }
            return
        }
        val targetCenter = (targetView.top + targetView.bottom) / 2
        val dy = targetCenter - centerY
        smoothScrollIfNeeded(dy)
    }

    private fun centerVisibleTarget(adapterPosition: Int) {
        if (_binding == null) return
        val recyclerView = binding.rcyVerse
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val targetView = layoutManager.findViewByPosition(adapterPosition) ?: return
        val centerY = recyclerView.height / 2
        val targetCenter = (targetView.top + targetView.bottom) / 2
        val dy = targetCenter - centerY
        smoothScrollIfNeeded(dy)
    }

    private fun smoothScrollIfNeeded(dy: Int) {
        if (_binding == null) return
        if (abs(dy) <= 8) return
        val recyclerView = binding.rcyVerse
        val canScrollDirection = when {
            dy < 0 -> recyclerView.canScrollVertically(-1)
            dy > 0 -> recyclerView.canScrollVertically(1)
            else -> false
        }
        if (!canScrollDirection) return
        recyclerView.smoothScrollBy(0, dy)
    }

    private fun resolveCurrentVerseIndexFromViewport(): Int {
        val layoutManager = binding.rcyVerse.layoutManager as? LinearLayoutManager ?: return 0
        val position = layoutManager.findFirstVisibleItemPosition().coerceAtLeast(1)
        return (position - 1).coerceAtLeast(0).coerceAtMost((currentVerses.size - 1).coerceAtLeast(0))
    }

    private fun updatePlaybackProgress() {
        if (currentVerses.isEmpty()) {
            binding.progressbarEx.progress = 0
            return
        }
        val progress = (((currentVerseIndex + 1) * 100f) / currentVerses.size).toInt().coerceIn(0, 100)
        binding.progressbarEx.progress = progress
    }

    private fun showToast(message: String) {
        binding.bibleToast.text = message
        binding.bibleToast.visibility = View.VISIBLE
        uiHandler.postDelayed({
            if (_binding != null) {
                binding.bibleToast.visibility = View.GONE
            }
        }, 2000)
    }

    private fun bindFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verses.collect { verses ->
                if (verses.isNotEmpty()) {
                    val verseItems = verses.map { VerseListItem.Verse(it.verse, it.text) }
                    currentVerses = verseItems
                    currentVerseIndex = 0
                    verseAdapter.submit(listOf(VerseListItem.Title("", verseItems)))
                    verseAdapter.setCurrentReadingVerseIndex(currentVerseIndex)
                    updatePlaybackProgress()
                } else {
                    currentVerses = emptyList()
                    currentVerseIndex = 0
                    verseAdapter.submit(emptyList())
                    verseAdapter.setCurrentReadingVerseIndex(-1)
                    updatePlaybackProgress()
                }
            }
        }
    }

    private fun restoreLastReading() {
        binding.tagA.text = "$lastBookName $lastChapter"
        viewModel.loadVerses(lastBookId, lastChapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlayback(resetPosition = false, showToast = false)
        uiHandler.removeCallbacksAndMessages(null)
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

}
