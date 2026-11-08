package com.jwahn.englishspeaking.data.db

/** 첫 실행 시 넣는 기본 데이터 (자체 제작 콘텐츠) */
object Seed {

    private data class SeedChapter(val title: String, val desc: String, val rows: String)

    private val chapters = listOf(
        SeedChapter(
            "일상생활", "하루 일과에서 자주 쓰는 단어",
            """
            breakfast|ˈbrekfəst|아침 식사|I usually skip breakfast on weekdays.|나는 평일에는 보통 아침을 거른다.
            commute|kəˈmjuːt|통근하다, 통근|My commute takes about an hour.|내 출퇴근 시간은 한 시간 정도 걸린다.
            laundry|ˈlɔːndri|빨래, 세탁물|I need to do the laundry tonight.|오늘 밤에 빨래를 해야 한다.
            grocery|ˈɡroʊsəri|식료품|Let's stop by the grocery store.|식료품점에 잠깐 들르자.
            appointment|əˈpɔɪntmənt|약속, 예약|I have a dentist appointment at three.|3시에 치과 예약이 있다.
            schedule|ˈskedʒuːl|일정, 일정을 잡다|My schedule is packed this week.|이번 주 내 일정은 꽉 찼다.
            neighbor|ˈneɪbər|이웃|Our new neighbor brought us some cookies.|새 이웃이 쿠키를 가져다주었다.
            errand|ˈerənd|볼일, 심부름|I have a few errands to run.|처리할 볼일이 몇 개 있다.
            chore|tʃɔːr|집안일|Washing dishes is my least favorite chore.|설거지는 내가 제일 싫어하는 집안일이다.
            nap|næp|낮잠|I took a short nap after lunch.|점심 먹고 잠깐 낮잠을 잤다.
            tidy|ˈtaɪdi|정돈된, 정리하다|Please keep your room tidy.|방을 깔끔하게 정리해 두렴.
            exhausted|ɪɡˈzɔːstɪd|기진맥진한|I was exhausted after work.|퇴근 후에 녹초가 됐다.
            """
        ),
        SeedChapter(
            "몸과 건강", "병원·약국·컨디션 표현",
            """
            headache|ˈhedeɪk|두통|I have a terrible headache.|머리가 너무 아프다.
            stomach|ˈstʌmək|배, 위|My stomach hurts a little.|배가 조금 아프다.
            throat|θroʊt|목구멍|I have a sore throat.|목이 따끔거린다.
            fever|ˈfiːvər|열|She has had a fever since last night.|그녀는 어젯밤부터 열이 났다.
            cough|kɔːf|기침, 기침하다|This medicine will help your cough.|이 약이 기침에 도움이 될 거예요.
            sore|sɔːr|아픈, 쑤시는|My legs are sore from hiking.|등산 때문에 다리가 쑤신다.
            dizzy|ˈdɪzi|어지러운|I feel dizzy when I stand up.|일어나면 어지럽다.
            prescription|prɪˈskrɪpʃən|처방전|You need a prescription for this.|이건 처방전이 필요해요.
            shoulder|ˈʃoʊldər|어깨|My shoulder is stiff.|어깨가 뻐근하다.
            ankle|ˈæŋkəl|발목|I twisted my ankle yesterday.|어제 발목을 삐었다.
            recover|rɪˈkʌvər|회복하다|It took a week to recover.|회복하는 데 일주일이 걸렸다.
            healthy|ˈhelθi|건강한|I'm trying to eat healthy food.|건강한 음식을 먹으려고 노력 중이다.
            """
        ),
        SeedChapter(
            "여행", "공항·호텔·관광",
            """
            passport|ˈpæspɔːrt|여권|Don't forget your passport.|여권 잊지 마.
            luggage|ˈlʌɡɪdʒ|짐, 수하물|How many pieces of luggage do you have?|짐이 몇 개인가요?
            reservation|ˌrezərˈveɪʃən|예약|I have a reservation under Kim.|김 이름으로 예약했어요.
            departure|dɪˈpɑːrtʃər|출발|The departure time has changed.|출발 시간이 바뀌었다.
            boarding pass|ˈbɔːrdɪŋ pæs|탑승권|May I see your boarding pass?|탑승권 좀 보여 주시겠어요?
            aisle|aɪl|통로|Can I get an aisle seat?|통로 쪽 좌석으로 주실 수 있나요?
            souvenir|ˌsuːvəˈnɪr|기념품|I bought a souvenir for my mom.|엄마 드릴 기념품을 샀다.
            currency|ˈkɜːrənsi|통화, 화폐|Where can I exchange currency?|어디서 환전할 수 있나요?
            itinerary|aɪˈtɪnəreri|여행 일정표|Let me check our itinerary.|우리 일정표 좀 확인해 볼게.
            delay|dɪˈleɪ|지연, 지연시키다|Our flight had a two-hour delay.|비행기가 두 시간 지연됐다.
            sightseeing|ˈsaɪtsiːɪŋ|관광|We went sightseeing downtown.|우리는 시내 관광을 했다.
            destination|ˌdestɪˈneɪʃən|목적지|What is your final destination?|최종 목적지가 어디인가요?
            """
        ),
        SeedChapter(
            "음식과 식당", "주문·맛·식사 표현",
            """
            recommend|ˌrekəˈmend|추천하다|What do you recommend?|뭘 추천하세요?
            spicy|ˈspaɪsi|매운|Is this dish very spicy?|이 요리 많이 매운가요?
            bill|bɪl|계산서|Could we get the bill, please?|계산서 좀 주시겠어요?
            leftover|ˈleftoʊvər|남은 음식|Can I take the leftovers home?|남은 음식 포장해 가도 될까요?
            ingredient|ɪnˈɡriːdiənt|재료|What ingredients are in this sauce?|이 소스에 무슨 재료가 들어가나요?
            appetizer|ˈæpɪtaɪzər|전채 요리|Let's share an appetizer.|에피타이저 하나 같이 먹자.
            dessert|dɪˈzɜːrt|디저트|I always have room for dessert.|디저트 들어갈 배는 항상 있다.
            vegetarian|ˌvedʒəˈteriən|채식주의자, 채식의|Do you have any vegetarian options?|채식 메뉴 있나요?
            allergic|əˈlɜːrdʒɪk|알레르기가 있는|I'm allergic to peanuts.|저는 땅콩 알레르기가 있어요.
            refill|ˈriːfɪl|리필|Can I get a refill?|리필 되나요?
            delicious|dɪˈlɪʃəs|맛있는|This soup is delicious.|이 수프 정말 맛있다.
            to go|tə ˈɡoʊ|포장(해 가는)|I'll have a latte to go.|라테 하나 포장해 주세요.
            """
        ),
        SeedChapter(
            "직장과 비즈니스", "회의·메일·업무 표현",
            """
            deadline|ˈdedlaɪn|마감 기한|The deadline is next Friday.|마감은 다음 주 금요일이다.
            colleague|ˈkɑːliːɡ|동료|I had lunch with a colleague.|동료와 점심을 먹었다.
            agenda|əˈdʒendə|안건, 의제|What's on the agenda today?|오늘 안건이 뭐죠?
            proposal|prəˈpoʊzəl|제안(서)|I'll send you the proposal by email.|제안서를 이메일로 보내 드릴게요.
            feedback|ˈfiːdbæk|피드백, 의견|Thanks for your helpful feedback.|유익한 피드백 감사합니다.
            promotion|prəˈmoʊʃən|승진|She got a promotion last month.|그녀는 지난달에 승진했다.
            budget|ˈbʌdʒɪt|예산|We are over budget.|예산을 초과했다.
            negotiate|nɪˈɡoʊʃieɪt|협상하다|We need to negotiate the price.|가격을 협상해야 한다.
            responsible|rɪˈspɑːnsəbəl|책임이 있는|Who is responsible for this project?|이 프로젝트 담당자가 누구죠?
            postpone|poʊstˈpoʊn|연기하다|Can we postpone the meeting?|회의를 미룰 수 있을까요?
            attach|əˈtætʃ|첨부하다|I've attached the file.|파일을 첨부했습니다.
            client|ˈklaɪənt|고객, 의뢰인|We're meeting a new client tomorrow.|내일 새 고객을 만난다.
            """
        ),
    )

    fun chapters(): List<Pair<ChapterEntity, List<WordEntity>>> = chapters.map { c ->
        ChapterEntity(title = c.title, description = c.desc, source = "builtin") to
            c.rows.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
                val p = line.split("|")
                WordEntity(
                    chapterId = 0, text = p[0], ipa = p[1], meaningKo = p[2],
                    exampleEn = p.getOrElse(3) { "" }, exampleKo = p.getOrElse(4) { "" },
                )
            }
    }

    data class SeedPattern(val template: String, val meaning: String, val category: String, val examples: List<Pair<String, String>>)

    val patterns = listOf(
        SeedPattern("I'm thinking of ~ing", "~할까 생각 중이야", "계획", listOf(
            "I'm thinking of learning to swim." to "수영을 배워 볼까 생각 중이야.",
            "I'm thinking of moving closer to work." to "회사 가까이로 이사할까 생각 중이야.",
        )),
        SeedPattern("Do you mind if I ~?", "~해도 될까요?", "부탁", listOf(
            "Do you mind if I open the window?" to "창문 열어도 될까요?",
            "Do you mind if I sit here?" to "여기 앉아도 될까요?",
        )),
        SeedPattern("I'm looking for ~", "~을 찾고 있어요", "쇼핑", listOf(
            "I'm looking for a gift for my sister." to "여동생 줄 선물을 찾고 있어요.",
            "I'm looking for the subway station." to "지하철역을 찾고 있어요.",
        )),
        SeedPattern("It's not that ~", "그렇게 ~하진 않아", "의견", listOf(
            "It's not that difficult." to "그렇게 어렵진 않아.",
            "It's not that expensive." to "그렇게 비싸진 않아.",
        )),
        SeedPattern("I used to ~", "예전에는 ~하곤 했어", "경험", listOf(
            "I used to play the piano." to "예전에 피아노를 치곤 했어.",
            "I used to live in Busan." to "예전에 부산에 살았어.",
        )),
        SeedPattern("Would you like me to ~?", "제가 ~해 드릴까요?", "제안", listOf(
            "Would you like me to call a taxi?" to "택시 불러 드릴까요?",
            "Would you like me to send it again?" to "다시 보내 드릴까요?",
        )),
        SeedPattern("I can't wait to ~", "빨리 ~하고 싶어", "감정", listOf(
            "I can't wait to see you." to "빨리 보고 싶어.",
            "I can't wait to try this restaurant." to "이 식당 빨리 가 보고 싶어.",
        )),
        SeedPattern("What do you think about ~?", "~에 대해 어떻게 생각해?", "의견", listOf(
            "What do you think about this plan?" to "이 계획 어떻게 생각해?",
            "What do you think about working from home?" to "재택근무에 대해 어떻게 생각해?",
        )),
    )

    val scenarios = listOf(
        // 롤플레이
        ScenarioEntity(category = "ROLEPLAY", title = "카페에서 주문하기", emoji = "☕",
            persona = "a friendly barista named Leo at a busy neighborhood coffee shop",
            goal = "The learner orders drinks and a snack, asks about options and pays.",
            missions = "음료 사이즈 정하기\n추천 메뉴 물어보기\n포장 또는 매장 식사 말하기"),
        ScenarioEntity(category = "ROLEPLAY", title = "옷 가게 쇼핑", emoji = "👕",
            persona = "a helpful sales clerk named Mia at a clothing store",
            goal = "The learner looks for clothes, asks about sizes and colors, tries them on, and pays.",
            missions = "원하는 옷 설명하기\n사이즈·색상 물어보기\n할인 여부 물어보기"),
        ScenarioEntity(category = "ROLEPLAY", title = "호텔 체크인", emoji = "🏨",
            persona = "a polite front desk receptionist named Daniel at a city hotel",
            goal = "The learner checks in, asks about breakfast, Wi-Fi and check-out time.",
            missions = "예약자 이름 말하기\n조식 시간 물어보기\n체크아웃 시간 확인하기"),
        ScenarioEntity(category = "ROLEPLAY", title = "길 묻기", emoji = "🗺️",
            persona = "a kind local named Grace walking in downtown",
            goal = "The learner asks for directions to a museum and how long it takes.",
            missions = "목적지 말하기\n교통수단 물어보기\n소요 시간 물어보기"),
        ScenarioEntity(category = "ROLEPLAY", title = "병원 진료", emoji = "🏥",
            persona = "a calm family doctor named Dr. Patel",
            goal = "The learner explains symptoms, answers questions and asks about medicine.",
            missions = "증상 설명하기\n언제부터 아팠는지 말하기\n약 복용법 물어보기"),
        ScenarioEntity(category = "ROLEPLAY", title = "공항 체크인", emoji = "✈️",
            persona = "an airline check-in agent named Sophie",
            goal = "The learner checks in for a flight, chooses a seat and checks luggage.",
            missions = "좌석 선택하기\n수하물 부치기\n탑승 게이트 확인하기"),
        // 일상 대화
        ScenarioEntity(category = "DAILY", title = "새 이웃과 인사", emoji = "🏠",
            persona = "a cheerful new neighbor named Emma who just moved in next door",
            goal = "Small talk: introduce yourselves, talk about the neighborhood and recommend places.",
            missions = "자기소개하기\n동네 장소 추천하기\n다음 만남 제안하기"),
        ScenarioEntity(category = "DAILY", title = "주말 계획 이야기", emoji = "📅",
            persona = "a close friend named Jake",
            goal = "Talk about weekend plans and decide to do something together.",
            missions = "주말 계획 말하기\n함께할 일 제안하기\n시간·장소 정하기"),
        ScenarioEntity(category = "DAILY", title = "취미 이야기", emoji = "🎨",
            persona = "a curious coworker named Olivia during a coffee break",
            goal = "Talk about hobbies, how you started them and why you like them.",
            missions = "취미 소개하기\n시작한 계기 말하기\n상대 취미 질문하기"),
        // 비즈니스
        ScenarioEntity(category = "BUSINESS", title = "영어 면접 – 경력 질문", emoji = "💼",
            persona = "a professional hiring manager named Ms. Carter interviewing the learner",
            goal = "A job interview: ask about background, experience, strengths and weaknesses one question at a time.",
            missions = "자기소개하기\n대표 성과 설명하기\n강점과 약점 말하기"),
        ScenarioEntity(category = "BUSINESS", title = "회의 진행하기", emoji = "📊",
            persona = "a team member named Ryan in a weekly project meeting led by the learner",
            goal = "The learner runs the meeting: opens, reviews progress, assigns action items and closes.",
            missions = "회의 시작 알리기\n진행 상황 묻기\n담당자와 기한 정하기"),
        ScenarioEntity(category = "BUSINESS", title = "1:1 면담", emoji = "🤝",
            persona = "a team member named Chris having a one-on-one meeting with the learner, who is the manager",
            goal = "The learner gives constructive feedback, listens to concerns and agrees on next steps.",
            missions = "잘한 점 칭찬하기\n개선점 구체적으로 말하기\n다음 목표 합의하기"),
        ScenarioEntity(category = "BUSINESS", title = "전화로 일정 조율", emoji = "📞",
            persona = "a client's assistant named Laura on the phone",
            goal = "Schedule or reschedule a meeting by phone and confirm details.",
            missions = "통화 목적 말하기\n가능한 시간 제안하기\n일정 다시 확인하기"),
        // OPIc
        ScenarioEntity(category = "OPIC", title = "OPIc 자기소개", emoji = "🙋",
            persona = "an OPIc-style interviewer named Ava",
            goal = "Ask the learner to introduce themselves, then ask follow-up questions like an OPIc test. Encourage 4–6 sentence answers.",
            missions = "1분 자기소개\n직업·일상 묘사\n구체적 예시 들기"),
        ScenarioEntity(category = "OPIC", title = "OPIc 집 묘사", emoji = "🏡",
            persona = "an OPIc-style interviewer named Ava",
            goal = "Ask the learner to describe their home, favorite room and a change they made. Encourage detailed descriptions.",
            missions = "집 구조 묘사하기\n좋아하는 공간 설명하기\n과거와 현재 비교하기"),
        ScenarioEntity(category = "OPIC", title = "OPIc 과거 경험", emoji = "🕰️",
            persona = "an OPIc-style interviewer named Ava",
            goal = "Ask about a memorable past experience (trip, problem, event) and dig into details with follow-up questions.",
            missions = "과거 시제로 이야기하기\n문제와 해결 설명하기\n느낀 점 말하기"),
        // 고급
        ScenarioEntity(category = "ADVANCED", title = "토론: 재택근무", emoji = "⚖️",
            persona = "a debate partner named Professor Lee who takes the opposite side of the learner",
            goal = "Debate whether remote work is better than office work. Challenge the learner's arguments politely.",
            missions = "입장 밝히기\n근거 두 가지 말하기\n반론에 대응하기"),
        ScenarioEntity(category = "ADVANCED", title = "기술과 사회", emoji = "🤖",
            persona = "a thoughtful podcast host named Sam",
            goal = "Discuss how AI and technology are changing daily life, work and education.",
            missions = "의견 말하기\n개인 경험 예시 들기\n미래 예측하기"),
        // 어린이 (가족용)
        ScenarioEntity(category = "KIDS", title = "좋아하는 동물", emoji = "🐶",
            persona = "a gentle, playful teacher named Miss Lily talking with a child",
            goal = "Talk with a child about favorite animals using very simple words and short sentences. Praise often.",
            missions = "좋아하는 동물 말하기\n동물 소리 흉내 말하기\n색깔·크기 말하기"),
        ScenarioEntity(category = "KIDS", title = "오늘 하루", emoji = "🎒",
            persona = "a gentle, playful teacher named Miss Lily talking with a child",
            goal = "Ask a child about their day at school, lunch and friends using very simple English. Praise often.",
            missions = "오늘 한 일 말하기\n점심 메뉴 말하기\n친구 이름 말하기"),
    )

    val feeds = listOf(
        NewsFeedEntity(name = "BBC World", url = "https://feeds.bbci.co.uk/news/world/rss.xml"),
        NewsFeedEntity(name = "BBC Technology", url = "https://feeds.bbci.co.uk/news/technology/rss.xml"),
        NewsFeedEntity(name = "NPR News", url = "https://feeds.npr.org/1001/rss.xml"),
        NewsFeedEntity(name = "The Guardian World", url = "https://www.theguardian.com/world/rss"),
    )
}
