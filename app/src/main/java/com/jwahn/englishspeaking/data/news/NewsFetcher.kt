package com.jwahn.englishspeaking.data.news

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

data class NewsItem(
    val feed: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
)

class NewsException(message: String) : Exception(message)

/** RSS/Atom 피드와 기사 본문을 가져온다. */
class NewsFetcher(private val http: OkHttpClient) {

    private val ua = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private fun get(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", ua).build()
        try {
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw NewsException("HTTP ${r.code}")
                return r.body?.string().orEmpty()
            }
        } catch (e: java.io.IOException) {
            throw NewsException("네트워크 오류: ${e.message}")
        } catch (e: IllegalArgumentException) {
            throw NewsException("주소가 올바르지 않아요: $url")
        }
    }

    suspend fun fetchFeed(name: String, url: String): List<NewsItem> = withContext(Dispatchers.IO) {
        parseFeed(name, get(url))
    }

    private fun parseFeed(feedName: String, xml: String): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        val items = mutableListOf<NewsItem>()
        var inItem = false
        var title = ""; var link = ""; var desc = ""; var date = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase()
            when (event) {
                XmlPullParser.START_TAG -> when {
                    tag == "item" || tag == "entry" -> {
                        inItem = true; title = ""; link = ""; desc = ""; date = ""
                    }
                    inItem && tag == "title" -> title = readText(parser)
                    inItem && tag == "link" -> {
                        val href = parser.getAttributeValue(null, "href")
                        if (href != null) { if (link.isEmpty()) link = href } else link = readText(parser)
                    }
                    inItem && (tag == "description" || tag == "summary" || tag == "content") ->
                        if (desc.isEmpty()) desc = readText(parser)
                    inItem && (tag == "pubdate" || tag == "updated" || tag == "published" || tag == "dc:date") ->
                        if (date.isEmpty()) date = readText(parser)
                }
                XmlPullParser.END_TAG -> if (tag == "item" || tag == "entry") {
                    inItem = false
                    if (title.isNotBlank() && link.isNotBlank()) {
                        items += NewsItem(
                            feedName, clean(title), link.trim(),
                            clean(desc).take(300), date.trim().take(25),
                        )
                    }
                }
            }
            event = runCatching { parser.next() }.getOrElse { XmlPullParser.END_DOCUMENT }
        }
        return items
    }

    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(p.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }

    private fun clean(html: String) = Jsoup.parse(html).text().trim()

    /** 기사 페이지에서 본문 문단만 추출 */
    suspend fun fetchArticleText(url: String): String = withContext(Dispatchers.IO) {
        val doc = Jsoup.parse(get(url), url)
        doc.select("script, style, nav, header, footer, aside, figure, form, noscript, [role=navigation]").remove()
        val candidates = listOf("article p", "main p", "[itemprop=articleBody] p", "p")
        var paragraphs = emptyList<String>()
        for (sel in candidates) {
            paragraphs = doc.select(sel).map { it.text().trim() }
                .filter { it.length > 40 && !it.startsWith("©") }
            if (paragraphs.size >= 3) break
        }
        val text = paragraphs.distinct().joinToString("\n")
        if (text.length < 200) throw NewsException("기사 본문을 가져오지 못했어요. (유료 기사이거나 형식이 달라요)\n'직접 붙여넣기'를 이용해 주세요.")
        text
    }
}
