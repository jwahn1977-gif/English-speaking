package com.jwahn.englishspeaking.data.llm

import com.jwahn.englishspeaking.data.db.ScenarioEntity

/** 프롬프트 템플릿 모음. 응답은 모두 JSON 객체로 받는다. */
object Prompts {

    private const val PATTERN_SPEC =
        """{"pattern": "reusable sentence frame using ~ for the slot, e.g. I'm looking for ~", "meaning_ko": "Korean meaning", "example_en": "example sentence", "example_ko": "Korean translation"}"""

    private const val WORD_SPEC =
        """{"word": "base form", "ipa": "IPA without slashes", "meaning_ko": "Korean meaning", "example_en": "short example", "example_ko": "Korean translation"}"""

    fun tutorSystem(s: ScenarioEntity, levelPrompt: String, nickname: String): String = """
You are ${s.persona}. You are role-playing with an English learner whose native language is Korean.
Scenario: ${s.title}. ${s.goal}
Learner level: $levelPrompt. Match your vocabulary and sentence length to this level.
${if (nickname.isNotBlank()) "The learner's name is $nickname." else ""}
Learner missions (0-indexed):
${s.missions.lines().filter { it.isNotBlank() }.mapIndexed { i, m -> "$i. $m" }.joinToString("\n")}

Rules:
- Stay in character. Keep each reply short (1-3 sentences) and usually end with a question so the learner keeps talking.
- If the learner writes in Korean or mixes Korean, understand it, reply in English, and put the natural English for what they meant in "correction.better".
- Only correct real mistakes or clearly unnatural phrasing. Be encouraging.
- Respond ONLY with one JSON object, no markdown:
{
 "reply": "your in-character English reply",
 "reply_ko": "Korean translation of reply",
 "correction": null or {"original": "learner's last sentence", "better": "natural corrected English", "why_ko": "short Korean explanation"},
 "patterns": [0-2 useful reusable patterns from this turn: $PATTERN_SPEC],
 "words": [0-2 useful words from this turn the learner may not know: $WORD_SPEC],
 "missions_done": [indices of missions the learner has accomplished so far in the whole conversation]
}
""".trimIndent()

    const val TUTOR_OPENING =
        "(The learner just joined. Start the conversation in character with a short greeting and an opening question. JSON only.)"

    fun hint(korean: String, context: String) = """
A Korean learner is in this conversation:
$context

They want to say (in Korean or broken English): "$korean"
Give 3 natural English ways to say it, from simple to more natural.
JSON only: {"options": [{"en": "...", "note_ko": "short Korean nuance note"}]}
""".trimIndent()

    fun report(transcript: String, missions: String) = """
Review this English practice conversation by a Korean learner (lines starting with "Learner:").
$transcript

Missions:
$missions

Write feedback in Korean for the learner. JSON only:
{
 "summary_ko": "2-3 sentence overall feedback in Korean",
 "good_ko": ["what they did well (Korean)", "..."],
 "improvements": [{"original": "learner sentence", "better": "natural English", "why_ko": "Korean explanation"}],
 "patterns": [3-5 most useful patterns for this situation: $PATTERN_SPEC],
 "words": [up to 5 key words: $WORD_SPEC],
 "score": 0-100 integer for communication success
}
""".trimIndent()

    fun wordSet(topic: String, count: Int, level: String) = """
Create an English vocabulary set for a Korean learner.
Topic: $topic
Level: $level
Number of words: $count (practical, commonly used, no duplicates)
JSON only:
{"title_ko": "short Korean chapter title", "description_ko": "one-line Korean description",
 "words": [$WORD_SPEC]}
""".trimIndent()

    fun patternSet(topic: String, count: Int, level: String) = """
Create practical English speaking patterns (sentence frames) for a Korean learner.
Situation/topic: $topic
Level: $level
Number of patterns: $count
Each pattern must be a reusable frame with ~ marking the slot (e.g. "Could you help me ~?").
JSON only:
{"patterns": [{"pattern": "...", "meaning_ko": "...", "category": "short Korean category",
  "examples": [{"en": "...", "ko": "..."}, {"en": "...", "ko": "..."}, {"en": "...", "ko": "..."}]}]}
""".trimIndent()

    fun moreExamples(pattern: String, meaning: String, existing: List<String>) = """
Pattern: "$pattern" ($meaning)
Existing examples: ${existing.joinToString(" / ")}
Write 5 NEW natural example sentences using this pattern in different everyday situations.
JSON only: {"examples": [{"en": "...", "ko": "Korean translation"}]}
""".trimIndent()

    fun checkPatternSentence(pattern: String, sentence: String) = """
The learner practiced the pattern "$pattern" and said: "$sentence"
Check grammar and naturalness. JSON only:
{"ok": true/false, "better": "natural corrected sentence (same if ok)", "comment_ko": "short Korean feedback"}
""".trimIndent()

    fun newsLesson(article: String, mode: String, level: String) = if (mode == "EASY") """
Rewrite this news article as a listening lesson for a Korean English learner.
Learner level: $level
Rewrite it in clear, easy English (graded reader style) in 8-15 sentences, keeping the key facts.
Article:
<article>
$article
</article>
JSON only:
{"title": "easy English headline", "title_ko": "Korean headline", "summary_ko": "2-sentence Korean summary",
 "sentences": [{"en": "...", "ko": "Korean translation"}],
 "words": [5-8 key words: $WORD_SPEC],
 "patterns": [2-3 useful patterns from the text: $PATTERN_SPEC],
 "questions": [{"q": "comprehension question in English", "a": "short answer"}]}
""".trimIndent() else """
Turn this news article into a listening lesson for a Korean English learner.
Keep the ORIGINAL sentences exactly (fix only broken spacing), up to 20 sentences, and translate each into Korean.
Article:
<article>
$article
</article>
JSON only:
{"title": "original headline", "title_ko": "Korean headline", "summary_ko": "2-sentence Korean summary",
 "sentences": [{"en": "...", "ko": "Korean translation"}],
 "words": [5-8 harder key words for level $level: $WORD_SPEC],
 "patterns": [2-3 useful patterns from the text: $PATTERN_SPEC],
 "questions": [{"q": "comprehension question in English", "a": "short answer"}]}
""".trimIndent()

    fun customScenario(description: String) = """
A Korean English learner wants to practice this situation: "$description"
Design a role-play. JSON only:
{"title": "short Korean title", "emoji": "one emoji",
 "persona": "who the AI plays, in English, e.g. 'a friendly landlord named Tom'",
 "goal": "scenario goal in English",
 "missions": ["3 short Korean missions for the learner"]}
""".trimIndent()
}
