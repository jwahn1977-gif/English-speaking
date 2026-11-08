package com.jwahn.englishspeaking.news

import com.jwahn.englishspeaking.reminder.Reminder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class NewsPlayerTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val spoken = mutableListOf<String>()
    private val done = mutableListOf<Int>()
    private val playingChanges = mutableListOf<Boolean>()
    private var stops = 0

    private fun player() = NewsPlayer(
        scope = scope,
        speaker = Speaker { text, _ -> spoken += text; delay(1000) },
        stopSpeech = { stops++ },
        onSentenceDone = { _, i -> done += i },
    ).also { it.onPlayingChanged = { p -> playingChanges += p } }

    private val sentences = listOf("One.", "Two.", "Three.")

    @Test fun plays_all_sentences_then_stops() {
        val p = player()
        p.load(1, "t", sentences, 0, 1f)
        p.play()
        scope.advanceUntilIdle()
        assertEquals(sentences, spoken)
        assertEquals(listOf(0, 1, 2), done)
        assertFalse(p.state.value.playing)
        assertEquals(2, p.state.value.index)
        assertEquals(listOf(true, false), playingChanges)
    }

    @Test fun repeat_reads_each_sentence_n_times() {
        val p = player()
        p.load(1, "t", sentences.take(2), 0, 1f)
        p.setRepeat(2)
        p.play()
        scope.advanceUntilIdle()
        assertEquals(listOf("One.", "One.", "Two.", "Two."), spoken)
    }

    @Test fun pause_stops_and_resume_continues_from_same_sentence() {
        val p = player()
        p.load(1, "t", sentences, 0, 1f)
        p.play()
        scope.advanceTimeBy(500); scope.runCurrent()
        p.pause()
        assertFalse(p.state.value.playing)
        assertTrue(stops > 0)
        scope.advanceUntilIdle()
        assertEquals(listOf("One."), spoken)
        p.play()
        scope.advanceUntilIdle()
        assertEquals(listOf("One.", "One.", "Two.", "Three."), spoken)
    }

    @Test fun seek_jumps_and_autoplays() {
        val p = player()
        p.load(1, "t", sentences, 0, 1f)
        p.seek(2, autoplay = true)
        scope.advanceUntilIdle()
        assertEquals(listOf("Three."), spoken)
        p.seek(99, autoplay = false)
        assertEquals(2, p.state.value.index)
        p.prev()
        assertEquals(1, p.state.value.index)
    }

    @Test fun load_resumes_saved_position_and_close_resets() {
        val p = player()
        p.load(7, "t", sentences, 1, 1f)
        assertEquals(1, p.state.value.index)
        p.close()
        assertFalse(p.state.value.active)
    }

    @Test fun reminder_delay_and_message() {
        val now = LocalDateTime.of(2026, 11, 8, 19, 30)
        assertEquals(Duration.ofMinutes(30), Reminder.delayUntil(20, 0, now))
        assertEquals(Duration.ofHours(23).plusMinutes(30), Reminder.delayUntil(19, 0, now))
        assertTrue(Reminder.message(3, 2, false, 5)!!.contains("단어 3개, 패턴 2개"))
        assertTrue(Reminder.message(3, 0, false, 5)!!.contains("5일 연속"))
        assertTrue(Reminder.message(0, 0, false, 0)!!.contains("아직"))
        assertNull(Reminder.message(0, 0, true, 3))
    }
}
