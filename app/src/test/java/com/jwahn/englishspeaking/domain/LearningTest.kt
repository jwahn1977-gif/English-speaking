package com.jwahn.englishspeaking.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LearningTest {

    @Test fun srs_correct_moves_up_and_schedules() {
        val now = 1_000_000L
        val r1 = Srs.answer(0, true, now)
        assertEquals(1, r1.box)
        assertEquals(now + 1 * 86_400_000L, r1.nextDue)
        val r5 = Srs.answer(5, true, now)
        assertEquals(5, r5.box)
        assertEquals(now + 15 * 86_400_000L, r5.nextDue)
    }

    @Test fun srs_wrong_resets_to_box1() {
        val r = Srs.answer(4, false, 0L)
        assertEquals(1, r.box)
        assertEquals(86_400_000L, r.nextDue)
    }

    @Test fun speech_score_exact_and_partial() {
        assertEquals(100, SpeechScore.score("I'm looking for the station.", "i'm looking for the station"))
        val partial = SpeechScore.score("Could we get the bill please", "could we get bill")
        assertTrue(partial in 50..80)
        assertEquals(0, SpeechScore.score("", "anything"))
        assertTrue(SpeechScore.score("hello", "hello um yeah so like hello there") < 100)
    }

    @Test fun word_hits_marks_missing_words() {
        val hits = SpeechScore.wordHits("I love coffee", "I like coffee")
        assertEquals(listOf("I" to true, "love" to false, "coffee" to true), hits)
    }

    @Test fun json_text_handles_fences_and_prose() {
        assertNotNull(JsonText.parseObject("```json\n{\"reply\":\"Hi\"}\n```"))
        val o = JsonText.parseObject("Sure! Here you go: {\"reply\": \"Hello\", \"n\": 2} Hope it helps.")
        assertEquals("Hello", o!!.str("reply"))
        assertNull(JsonText.parseObject("no json here"))
    }

    @Test fun json_helpers() {
        val o = JsonText.parseObject("""{"missions_done":[0,2],"patterns":[{"pattern":"a ~"}],"correction":null}""")!!
        assertEquals(listOf(0, 2), o.ints("missions_done"))
        assertEquals(1, o.objs("patterns").size)
        assertNull(o.obj("correction"))
        assertEquals("", o.str("missing"))
    }

    @Test fun levels_by_used_words() {
        assertEquals(1, Levels.of(0).no)
        assertEquals(2, Levels.of(100).no)
        assertEquals(4, Levels.of(1199).no)
        assertEquals(6, Levels.of(99_999).no)
        assertEquals(1f, Levels.progress(99_999))
        assertTrue(Levels.contentWords("I am going to the park, okay?").containsAll(listOf("going", "park")))
        assertTrue("the" !in Levels.contentWords("the the the"))
    }

    @Test fun streak_counts_consecutive_days() {
        val today = LocalDate.of(2026, 11, 8)
        val days = setOf("2026-11-08", "2026-11-07", "2026-11-06", "2026-11-04")
        assertEquals(3, Streak.count(days, today))
        // 오늘 아직 안 했어도 어제까지 이어졌으면 유지
        assertEquals(2, Streak.count(setOf("2026-11-07", "2026-11-06"), today))
        assertEquals(0, Streak.count(setOf("2026-11-01"), today))
    }

    @Test fun sentence_split_protects_abbreviations() {
        val s = Sentences.split("Mr. Kim arrived in the U.S. on Monday. He said: \"It was great!\" Prices rose 2.5% today.")
        assertEquals(3, s.size)
        assertTrue(s[0].startsWith("Mr. Kim"))
    }
}
