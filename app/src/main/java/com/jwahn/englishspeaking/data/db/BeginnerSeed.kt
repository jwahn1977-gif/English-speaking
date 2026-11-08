package com.jwahn.englishspeaking.data.db

/**
 * 왕초보 과정 (자체 제작). AI 없이 동작한다.
 * 형식: 단어|발음기호|뜻|예문|예문 해석|이모지
 */
object BeginnerSeed {

    data class Chapter(val title: String, val emoji: String, val desc: String, val rows: String)

    val chapters = listOf(
        Chapter("알파벳 소리", "🔤", "글자마다 나는 기본 소리 (파닉스)", """
            A a|æ|'애' 소리 — apple 사과|A is for apple.|A는 apple의 A|🍎
            B b|b|'브' 소리 — ball 공|B is for ball.|B는 ball의 B|⚽
            C c|k|'크' 소리 — cat 고양이|C is for cat.|C는 cat의 C|🐱
            D d|d|'드' 소리 — dog 개|D is for dog.|D는 dog의 D|🐶
            E e|e|'에' 소리 — egg 달걀|E is for egg.|E는 egg의 E|🥚
            F f|f|'프' (윗니로 아랫입술) — fish 물고기|F is for fish.|F는 fish의 F|🐟
            G g|ɡ|'그' 소리 — girl 여자아이|G is for girl.|G는 girl의 G|👧
            H h|h|'흐' 소리 — hat 모자|H is for hat.|H는 hat의 H|🎩
            I i|ɪ|'이' (짧게) — insect 곤충|I is for insect.|I는 insect의 I|🐞
            J j|dʒ|'즈' 소리 — juice 주스|J is for juice.|J는 juice의 J|🧃
            K k|k|'크' 소리 — king 왕|K is for king.|K는 king의 K|🤴
            L l|l|'을' (혀끝을 윗니 뒤에) — lion 사자|L is for lion.|L은 lion의 L|🦁
            M m|m|'음' 소리 — moon 달|M is for moon.|M은 moon의 M|🌙
            N n|n|'은' 소리 — nose 코|N is for nose.|N은 nose의 N|👃
            O o|ɑ|'아' (입을 크게) — octopus 문어|O is for octopus.|O는 octopus의 O|🐙
            P p|p|'프' (바람을 터뜨리며) — pig 돼지|P is for pig.|P는 pig의 P|🐷
            Q q|kw|'크우' 소리 — queen 여왕|Q is for queen.|Q는 queen의 Q|👸
            R r|r|'르' (혀를 말아서) — rabbit 토끼|R is for rabbit.|R은 rabbit의 R|🐰
            S s|s|'스' 소리 — sun 해|S is for sun.|S는 sun의 S|☀️
            T t|t|'트' 소리 — tiger 호랑이|T is for tiger.|T는 tiger의 T|🐯
            U u|ʌ|'어' 소리 — umbrella 우산|U is for umbrella.|U는 umbrella의 U|☂️
            V v|v|'브' (윗니로 아랫입술, 울리게) — van 승합차|V is for van.|V는 van의 V|🚐
            W w|w|'우' (입술을 모아서) — water 물|W is for water.|W는 water의 W|💧
            X x|ks|'크스' 소리 — box 상자|X is in box.|box에 X가 있어요|📦
            Y y|j|'이' (다음 소리와 이어서) — yellow 노란색|Y is for yellow.|Y는 yellow의 Y|💛
            Z z|z|'즈' (울리게) — zebra 얼룩말|Z is for zebra.|Z는 zebra의 Z|🦓
        """),
        Chapter("인사와 기본 말", "👋", "처음 만났을 때, 매일 쓰는 짧은 말", """
            hello|həˈloʊ|안녕하세요|Hello, I'm Minji.|안녕하세요, 저는 민지예요.|👋
            hi|haɪ|안녕|Hi, Tom!|안녕, 톰!|🙋
            bye|baɪ|잘 가, 안녕|Bye, see you tomorrow.|안녕, 내일 봐.|👋
            yes|jes|네|Yes, please.|네, 주세요.|✅
            no|noʊ|아니요|No, thank you.|아니요, 괜찮아요.|❌
            please|pliːz|부디, ~해 주세요|Water, please.|물 주세요.|🙏
            thank you|ˈθæŋk juː|고맙습니다|Thank you so much.|정말 고맙습니다.|😊
            sorry|ˈsɑːri|미안해요|I'm sorry.|미안해요.|😔
            okay|oʊˈkeɪ|좋아요, 알겠어요|Okay, let's go.|좋아요, 가요.|👌
            name|neɪm|이름|My name is Jisoo.|제 이름은 지수예요.|📛
            good|ɡʊd|좋은|Good morning!|좋은 아침!|👍
            morning|ˈmɔːrnɪŋ|아침|Good morning, Mom.|엄마, 좋은 아침이에요.|🌅
            night|naɪt|밤|Good night!|잘 자요!|🌙
            friend|frend|친구|She is my friend.|그녀는 내 친구예요.|🤝
            welcome|ˈwelkəm|환영합니다|Welcome to my home.|우리 집에 온 걸 환영해요.|🏠
            excuse me|ɪkˈskjuːz miː|실례합니다|Excuse me, where is the bus stop?|실례합니다, 버스 정류장이 어디예요?|🙋
            nice|naɪs|좋은, 반가운|Nice to meet you.|만나서 반가워요.|😀
            help|help|돕다, 도움|Can you help me?|도와주실 수 있어요?|🆘
            again|əˈɡen|다시|Say it again, please.|다시 말해 주세요.|🔁
            slowly|ˈsloʊli|천천히|Please speak slowly.|천천히 말해 주세요.|🐢
        """),
        Chapter("숫자", "🔢", "1부터 100까지, 나이와 개수 말하기", """
            one|wʌn|하나, 1|I have one sister.|나는 언니가 한 명 있어요.|1️⃣
            two|tuː|둘, 2|Two coffees, please.|커피 두 잔 주세요.|2️⃣
            three|θriː|셋, 3|I have three cats.|고양이가 세 마리 있어요.|3️⃣
            four|fɔːr|넷, 4|We are a family of four.|우리는 네 식구예요.|4️⃣
            five|faɪv|다섯, 5|It's five o'clock.|5시예요.|5️⃣
            six|sɪks|여섯, 6|My son is six.|아들은 여섯 살이에요.|6️⃣
            seven|ˈsevən|일곱, 7|There are seven days in a week.|일주일은 7일이에요.|7️⃣
            eight|eɪt|여덟, 8|I get up at eight.|나는 8시에 일어나요.|8️⃣
            nine|naɪn|아홉, 9|Nine apples.|사과 아홉 개.|9️⃣
            ten|ten|열, 10|Ten minutes, please.|10분만요.|🔟
            eleven|ɪˈlevən|열하나, 11|She is eleven.|그녀는 열한 살이에요.|🕚
            twelve|twelv|열둘, 12|Twelve eggs.|달걀 열두 개.|🥚
            thirteen|ˌθɜːrˈtiːn|열셋, 13|He is thirteen years old.|그는 열세 살이에요.|1️⃣
            fifteen|ˌfɪfˈtiːn|열다섯, 15|Fifteen dollars.|15달러요.|💵
            twenty|ˈtwenti|스물, 20|Twenty people came.|스무 명이 왔어요.|👥
            thirty|ˈθɜːrti|서른, 30|I'm thirty.|저는 서른 살이에요.|🎂
            forty|ˈfɔːrti|마흔, 40|Forty minutes.|40분.|⏱️
            fifty|ˈfɪfti|쉰, 50|Fifty percent off.|50퍼센트 할인.|🏷️
            hundred|ˈhʌndrəd|백, 100|One hundred won.|100원.|💯
            first|fɜːrst|첫 번째|This is my first time.|이번이 처음이에요.|🥇
            how many|haʊ ˈmeni|몇 개|How many apples?|사과 몇 개요?|❓
            how old|haʊ oʊld|몇 살|How old are you?|몇 살이에요?|🎂
        """),
        Chapter("색깔과 모양", "🎨", "색과 크기, 모양 말하기", """
            red|red|빨간색|I like red.|빨간색을 좋아해요.|🔴
            blue|bluː|파란색|The sky is blue.|하늘이 파래요.|🔵
            yellow|ˈjeloʊ|노란색|A yellow banana.|노란 바나나.|🟡
            green|ɡriːn|초록색|Green tea, please.|녹차 주세요.|🟢
            black|blæk|검은색|My bag is black.|내 가방은 검은색이에요.|⚫
            white|waɪt|흰색|White snow.|하얀 눈.|⚪
            pink|pɪŋk|분홍색|A pink dress.|분홍색 원피스.|🩷
            orange|ˈɔːrɪndʒ|주황색, 오렌지|An orange shirt.|주황색 셔츠.|🟠
            purple|ˈpɜːrpəl|보라색|Purple grapes.|보라색 포도.|🟣
            brown|braʊn|갈색|A brown dog.|갈색 개.|🟤
            gray|ɡreɪ|회색|A gray cat.|회색 고양이.|🩶
            color|ˈkʌlər|색깔|What color is it?|무슨 색이에요?|🎨
            big|bɪɡ|큰|A big house.|큰 집.|🐘
            small|smɔːl|작은|A small cup.|작은 컵.|🐭
            long|lɔːŋ|긴|Long hair.|긴 머리.|📏
            short|ʃɔːrt|짧은, 키가 작은|A short story.|짧은 이야기.|✂️
            circle|ˈsɜːrkəl|동그라미|Draw a circle.|동그라미를 그려요.|⭕
            square|skwer|네모|A square box.|네모난 상자.|⬛
            triangle|ˈtraɪæŋɡəl|세모|A triangle sign.|세모 표지판.|🔺
            star|stɑːr|별|I see a star.|별이 보여요.|⭐
        """),
        Chapter("가족과 사람", "👨‍👩‍👧", "가족 소개하기", """
            family|ˈfæməli|가족|I love my family.|나는 가족을 사랑해요.|👨‍👩‍👧
            mom|mɑːm|엄마|My mom is a teacher.|엄마는 선생님이에요.|👩
            dad|dæd|아빠|My dad cooks well.|아빠는 요리를 잘해요.|👨
            mother|ˈmʌðər|어머니|His mother is kind.|그의 어머니는 친절해요.|👩
            father|ˈfɑːðər|아버지|My father works in Seoul.|아버지는 서울에서 일해요.|👨
            brother|ˈbrʌðər|형제(형, 오빠, 남동생)|I have a little brother.|남동생이 있어요.|👦
            sister|ˈsɪstər|자매(언니, 누나, 여동생)|My sister is tall.|언니는 키가 커요.|👧
            baby|ˈbeɪbi|아기|The baby is sleeping.|아기가 자고 있어요.|👶
            son|sʌn|아들|My son likes soccer.|아들은 축구를 좋아해요.|👦
            daughter|ˈdɔːtər|딸|My daughter is ten.|딸은 열 살이에요.|👧
            grandma|ˈɡrænmɑː|할머니|Grandma makes kimchi.|할머니가 김치를 담가요.|👵
            grandpa|ˈɡrænpɑː|할아버지|Grandpa reads the news.|할아버지는 뉴스를 읽어요.|👴
            husband|ˈhʌzbənd|남편|My husband is at work.|남편은 회사에 있어요.|🤵
            wife|waɪf|아내|This is my wife.|이쪽은 제 아내예요.|👰
            man|mæn|남자|Who is that man?|저 남자는 누구예요?|👨
            woman|ˈwʊmən|여자|The woman is a doctor.|그 여자는 의사예요.|👩
            boy|bɔɪ|남자아이|The boy is running.|남자아이가 뛰고 있어요.|👦
            girl|ɡɜːrl|여자아이|The girl is singing.|여자아이가 노래하고 있어요.|👧
            teacher|ˈtiːtʃər|선생님|My teacher is funny.|우리 선생님은 재미있어요.|🧑‍🏫
            people|ˈpiːpəl|사람들|Many people are here.|사람이 많아요.|👥
        """),
        Chapter("몸", "🧍", "몸의 부분과 아픈 곳 말하기", """
            head|hed|머리|My head hurts.|머리가 아파요.|🗣️
            hair|her|머리카락|She has long hair.|그녀는 머리가 길어요.|💇
            face|feɪs|얼굴|Wash your face.|세수해.|🙂
            eye|aɪ|눈|Close your eyes.|눈을 감아요.|👁️
            ear|ɪr|귀|My ear hurts.|귀가 아파요.|👂
            nose|noʊz|코|Touch your nose.|코를 만져 봐.|👃
            mouth|maʊθ|입|Open your mouth.|입을 벌려요.|👄
            tooth|tuːθ|이, 치아|Brush your teeth.|이를 닦아.|🦷
            hand|hænd|손|Wash your hands.|손을 씻어요.|✋
            finger|ˈfɪŋɡər|손가락|I cut my finger.|손가락을 베었어요.|☝️
            arm|ɑːrm|팔|My arm is tired.|팔이 피곤해요.|💪
            leg|leɡ|다리|My legs are long.|내 다리는 길어요.|🦵
            foot|fʊt|발|My foot is cold.|발이 차가워요.|🦶
            back|bæk|등, 허리|My back hurts.|허리가 아파요.|🔙
            stomach|ˈstʌmək|배|My stomach hurts.|배가 아파요.|🤰
            heart|hɑːrt|심장, 마음|My heart is beating fast.|심장이 빨리 뛰어요.|❤️
            body|ˈbɑːdi|몸|Exercise is good for your body.|운동은 몸에 좋아요.|🧍
            sick|sɪk|아픈|I feel sick.|몸이 안 좋아요.|🤒
            hurt|hɜːrt|아프다|Where does it hurt?|어디가 아파요?|🤕
            tired|ˈtaɪərd|피곤한|I'm tired.|피곤해요.|😪
        """),
        Chapter("음식과 음료", "🍚", "먹고 마시는 것, 주문하기", """
            food|fuːd|음식|I love Korean food.|한국 음식을 좋아해요.|🍱
            rice|raɪs|밥, 쌀|Rice and soup, please.|밥이랑 국 주세요.|🍚
            bread|bred|빵|I eat bread for breakfast.|아침으로 빵을 먹어요.|🍞
            egg|eɡ|달걀|Two eggs, please.|달걀 두 개 주세요.|🥚
            milk|mɪlk|우유|A glass of milk.|우유 한 잔.|🥛
            water|ˈwɔːtər|물|Can I have some water?|물 좀 주실래요?|💧
            coffee|ˈkɔːfi|커피|I drink coffee every day.|매일 커피를 마셔요.|☕
            tea|tiː|차|Do you want some tea?|차 드실래요?|🍵
            juice|dʒuːs|주스|Orange juice, please.|오렌지 주스 주세요.|🧃
            apple|ˈæpəl|사과|An apple a day.|하루에 사과 하나.|🍎
            banana|bəˈnænə|바나나|Monkeys like bananas.|원숭이는 바나나를 좋아해요.|🍌
            chicken|ˈtʃɪkɪn|닭, 닭고기|Fried chicken is my favorite.|프라이드치킨을 제일 좋아해요.|🍗
            meat|miːt|고기|I don't eat meat.|저는 고기를 안 먹어요.|🥩
            fish|fɪʃ|생선, 물고기|Grilled fish, please.|생선구이 주세요.|🐟
            noodles|ˈnuːdəlz|국수, 면|I want noodles.|국수 먹고 싶어요.|🍜
            soup|suːp|수프, 국|The soup is hot.|국이 뜨거워요.|🍲
            salad|ˈsæləd|샐러드|A green salad.|그린 샐러드.|🥗
            cake|keɪk|케이크|Birthday cake!|생일 케이크!|🎂
            ice cream|ˈaɪs kriːm|아이스크림|Chocolate ice cream, please.|초콜릿 아이스크림 주세요.|🍦
            breakfast|ˈbrekfəst|아침 식사|Did you eat breakfast?|아침 먹었어요?|🍳
            lunch|lʌntʃ|점심|Let's have lunch.|점심 먹어요.|🥪
            dinner|ˈdɪnər|저녁 식사|Dinner is ready.|저녁 준비됐어요.|🍽️
            hungry|ˈhʌŋɡri|배고픈|I'm hungry.|배고파요.|😋
            thirsty|ˈθɜːrsti|목마른|I'm thirsty.|목말라요.|🥤
            delicious|dɪˈlɪʃəs|맛있는|It's delicious!|맛있어요!|😋
        """),
        Chapter("동물", "🐶", "아이들이 좋아하는 동물", """
            dog|dɔːɡ|개|I have a dog.|개가 한 마리 있어요.|🐶
            cat|kæt|고양이|The cat is sleeping.|고양이가 자고 있어요.|🐱
            bird|bɜːrd|새|A bird can fly.|새는 날 수 있어요.|🐦
            fish|fɪʃ|물고기|Fish can swim.|물고기는 헤엄칠 수 있어요.|🐠
            rabbit|ˈræbɪt|토끼|The rabbit is white.|토끼가 하얘요.|🐰
            horse|hɔːrs|말|I can ride a horse.|나는 말을 탈 수 있어요.|🐴
            cow|kaʊ|소|Cows give us milk.|소는 우리에게 우유를 줘요.|🐮
            pig|pɪɡ|돼지|The pig is pink.|돼지는 분홍색이에요.|🐷
            duck|dʌk|오리|Ducks say quack.|오리는 꽥꽥 해요.|🦆
            chicken|ˈtʃɪkɪn|닭|The chicken lays eggs.|닭이 알을 낳아요.|🐔
            lion|ˈlaɪən|사자|The lion is strong.|사자는 힘이 세요.|🦁
            tiger|ˈtaɪɡər|호랑이|Tigers have stripes.|호랑이는 줄무늬가 있어요.|🐯
            bear|ber|곰|A big brown bear.|커다란 갈색 곰.|🐻
            monkey|ˈmʌŋki|원숭이|The monkey is funny.|원숭이가 웃겨요.|🐵
            elephant|ˈeləfənt|코끼리|Elephants are big.|코끼리는 커요.|🐘
            giraffe|dʒəˈræf|기린|A giraffe has a long neck.|기린은 목이 길어요.|🦒
            zebra|ˈziːbrə|얼룩말|The zebra is black and white.|얼룩말은 검고 하얘요.|🦓
            snake|sneɪk|뱀|I'm afraid of snakes.|나는 뱀이 무서워요.|🐍
            frog|frɔːɡ|개구리|The frog can jump.|개구리는 뛸 수 있어요.|🐸
            butterfly|ˈbʌtərflaɪ|나비|A pretty butterfly.|예쁜 나비.|🦋
            dolphin|ˈdɑːlfɪn|돌고래|Dolphins are smart.|돌고래는 똑똑해요.|🐬
            penguin|ˈpeŋɡwɪn|펭귄|Penguins can't fly.|펭귄은 날 수 없어요.|🐧
            animal|ˈænɪməl|동물|What's your favorite animal?|제일 좋아하는 동물이 뭐예요?|🐾
        """),
        Chapter("집과 물건", "🏠", "집 안의 물건과 장소", """
            house|haʊs|집(건물)|This is my house.|여기가 우리 집이에요.|🏠
            home|hoʊm|집, 가정|I'm home!|다녀왔습니다!|🏡
            room|ruːm|방|My room is small.|내 방은 작아요.|🚪
            door|dɔːr|문|Close the door, please.|문 좀 닫아 주세요.|🚪
            window|ˈwɪndoʊ|창문|Open the window.|창문을 열어요.|🪟
            bed|bed|침대|Time for bed.|잘 시간이야.|🛏️
            table|ˈteɪbəl|탁자, 식탁|The keys are on the table.|열쇠는 식탁 위에 있어요.|🪑
            chair|tʃer|의자|Sit on the chair.|의자에 앉아요.|🪑
            kitchen|ˈkɪtʃɪn|부엌|Mom is in the kitchen.|엄마는 부엌에 있어요.|🍳
            bathroom|ˈbæθruːm|화장실, 욕실|Where is the bathroom?|화장실이 어디예요?|🚻
            phone|foʊn|전화기, 휴대폰|Where is my phone?|내 휴대폰 어디 있지?|📱
            book|bʊk|책|I'm reading a book.|책을 읽고 있어요.|📖
            bag|bæɡ|가방|My bag is heavy.|가방이 무거워요.|🎒
            key|kiː|열쇠|I lost my key.|열쇠를 잃어버렸어요.|🔑
            cup|kʌp|컵|A cup of tea.|차 한 잔.|☕
            clock|klɑːk|시계(벽시계)|The clock is fast.|시계가 빨라요.|🕰️
            TV|ˌtiːˈviː|텔레비전|Turn off the TV.|텔레비전 꺼.|📺
            computer|kəmˈpjuːtər|컴퓨터|I work on a computer.|컴퓨터로 일해요.|💻
            shoes|ʃuːz|신발|Take off your shoes.|신발 벗어요.|👟
            clothes|kloʊðz|옷|I need new clothes.|새 옷이 필요해요.|👕
            umbrella|ʌmˈbrelə|우산|Take an umbrella.|우산 챙겨요.|☂️
            money|ˈmʌni|돈|I don't have money.|돈이 없어요.|💰
            car|kɑːr|자동차|My car is blue.|내 차는 파란색이에요.|🚗
        """),
        Chapter("기본 동사 50", "🏃", "매일 쓰는 움직임 말", """
            be|biː|~이다, 있다|I am happy.|나는 행복해요.|🟰
            have|hæv|가지고 있다, 먹다|I have a car.|차가 있어요.|🤲
            do|duː|하다|I do my homework.|숙제를 해요.|✅
            go|ɡoʊ|가다|I go to school.|학교에 가요.|🚶
            come|kʌm|오다|Come here, please.|이리 오세요.|👉
            eat|iːt|먹다|Let's eat.|먹자.|🍽️
            drink|drɪŋk|마시다|Drink some water.|물 좀 마셔.|🥤
            sleep|sliːp|자다|I sleep at eleven.|11시에 자요.|😴
            wake up|weɪk ʌp|잠에서 깨다|I wake up at seven.|7시에 일어나요.|⏰
            see|siː|보다, 보이다|I see a bird.|새가 보여요.|👀
            look|lʊk|보다(쳐다보다)|Look at me.|나를 봐.|👁️
            watch|wɑːtʃ|지켜보다, 시청하다|I watch TV.|텔레비전을 봐요.|📺
            hear|hɪr|들리다|Can you hear me?|제 말 들려요?|👂
            listen|ˈlɪsən|듣다(귀 기울이다)|Listen to me.|내 말 들어 봐.|🎧
            say|seɪ|말하다(말을 하다)|Say hello.|인사해.|💬
            speak|spiːk|말하다(언어를)|I speak a little English.|영어를 조금 해요.|🗣️
            talk|tɔːk|이야기하다|Let's talk.|이야기하자.|💬
            tell|tel|말해 주다|Tell me your name.|이름을 말해 줘.|📢
            ask|æsk|묻다|Can I ask a question?|질문해도 돼요?|❓
            answer|ˈænsər|대답하다|Answer the phone.|전화 받아.|📞
            read|riːd|읽다|I read every night.|매일 밤 책을 읽어요.|📖
            write|raɪt|쓰다|Write your name here.|여기에 이름을 쓰세요.|✍️
            learn|lɜːrn|배우다|I'm learning English.|영어를 배우고 있어요.|🎓
            study|ˈstʌdi|공부하다|I study every day.|매일 공부해요.|📚
            know|noʊ|알다|I don't know.|모르겠어요.|💡
            think|θɪŋk|생각하다|I think so.|그런 것 같아요.|🤔
            want|wɑːnt|원하다|I want some water.|물을 원해요.|🙋
            like|laɪk|좋아하다|I like music.|음악을 좋아해요.|👍
            love|lʌv|사랑하다, 아주 좋아하다|I love you.|사랑해.|❤️
            need|niːd|필요하다|I need help.|도움이 필요해요.|🆘
            make|meɪk|만들다|Let's make a cake.|케이크를 만들자.|🧁
            give|ɡɪv|주다|Give me the ball.|공 줘.|🎁
            take|teɪk|가져가다, 타다|Take the bus.|버스를 타요.|🚌
            buy|baɪ|사다|I want to buy this.|이거 사고 싶어요.|🛍️
            pay|peɪ|지불하다|Can I pay by card?|카드로 내도 돼요?|💳
            open|ˈoʊpən|열다|Open the door.|문 열어.|🔓
            close|kloʊz|닫다|Close your book.|책을 덮어.|🔒
            sit|sɪt|앉다|Sit down, please.|앉으세요.|🪑
            stand|stænd|서다|Stand up.|일어서.|🧍
            walk|wɔːk|걷다|I walk to work.|걸어서 출근해요.|🚶
            run|rʌn|달리다|Don't run!|뛰지 마!|🏃
            play|pleɪ|놀다, (경기·악기를) 하다|Let's play soccer.|축구 하자.|⚽
            work|wɜːrk|일하다|I work at a bank.|은행에서 일해요.|💼
            wash|wɑːʃ|씻다|Wash your hands.|손 씻어.|🧼
            cook|kʊk|요리하다|I cook dinner.|저녁을 요리해요.|👩‍🍳
            call|kɔːl|전화하다, 부르다|Call me later.|나중에 전화해.|📞
            wait|weɪt|기다리다|Wait a minute.|잠깐만 기다려.|⏳
            stop|stɑːp|멈추다|Stop, please.|멈춰 주세요.|🛑
            help|help|돕다|Help me, please.|도와주세요.|🤝
            understand|ˌʌndərˈstænd|이해하다|I don't understand.|이해가 안 돼요.|🧐
        """),
        Chapter("기분과 상태", "😊", "느낌과 상태를 말하는 말", """
            happy|ˈhæpi|행복한, 기쁜|I'm happy today.|오늘 기분 좋아요.|😊
            sad|sæd|슬픈|Why are you sad?|왜 슬퍼요?|😢
            angry|ˈæŋɡri|화난|Don't be angry.|화내지 마.|😠
            scared|skerd|무서운, 겁먹은|I'm scared of dogs.|개가 무서워요.|😨
            excited|ɪkˈsaɪtɪd|신나는|I'm so excited!|너무 신나요!|🤩
            bored|bɔːrd|지루한|I'm bored.|심심해요.|🥱
            busy|ˈbɪzi|바쁜|I'm busy now.|지금 바빠요.|🏃
            sleepy|ˈsliːpi|졸린|I'm sleepy.|졸려요.|😴
            fine|faɪn|괜찮은|I'm fine, thanks.|괜찮아요, 고마워요.|👌
            hot|hɑːt|더운, 뜨거운|It's hot today.|오늘 더워요.|🥵
            cold|koʊld|추운, 차가운|I'm cold.|추워요.|🥶
            new|nuː|새로운|I have a new phone.|새 휴대폰이 있어요.|🆕
            old|oʊld|오래된, 나이 든|This is an old house.|이건 오래된 집이에요.|🏚️
            easy|ˈiːzi|쉬운|English is easy!|영어는 쉬워요!|🙂
            difficult|ˈdɪfɪkəlt|어려운|This is difficult.|이건 어려워요.|😵
            fast|fæst|빠른|You are fast!|너 빠르다!|⚡
            slow|sloʊ|느린|The bus is slow.|버스가 느려요.|🐢
            beautiful|ˈbjuːtɪfəl|아름다운|What a beautiful day!|정말 아름다운 날이에요!|🌸
            cute|kjuːt|귀여운|The baby is so cute.|아기가 너무 귀여워요.|🥰
            kind|kaɪnd|친절한|She is very kind.|그녀는 아주 친절해요.|🤗
            funny|ˈfʌni|웃기는|That's funny!|웃기다!|😂
            ready|ˈredi|준비된|Are you ready?|준비됐어요?|✅
            sure|ʃʊr|확실한, 물론|Sure, no problem.|물론이죠, 문제없어요.|👍
        """),
        Chapter("시간과 날씨", "⏰", "요일, 때, 날씨 말하기", """
            today|təˈdeɪ|오늘|Today is Monday.|오늘은 월요일이에요.|📅
            tomorrow|təˈmɑːroʊ|내일|See you tomorrow.|내일 봐요.|➡️
            yesterday|ˈjestərdeɪ|어제|I was busy yesterday.|어제 바빴어요.|⬅️
            now|naʊ|지금|I'm busy now.|지금 바빠요.|⏱️
            later|ˈleɪtər|나중에|See you later.|나중에 봐요.|🔜
            time|taɪm|시간|What time is it?|몇 시예요?|⏰
            day|deɪ|날, 낮|Have a nice day.|좋은 하루 보내세요.|☀️
            week|wiːk|주|See you next week.|다음 주에 봐요.|🗓️
            month|mʌnθ|달, 월|This month is busy.|이번 달은 바빠요.|📆
            year|jɪr|해, 년|Happy New Year!|새해 복 많이 받으세요!|🎆
            Monday|ˈmʌndeɪ|월요일|I work on Monday.|월요일에 일해요.|1️⃣
            Friday|ˈfraɪdeɪ|금요일|Thank God it's Friday!|드디어 금요일이다!|🎉
            weekend|ˈwiːkend|주말|What do you do on the weekend?|주말에 뭐 해요?|🛋️
            weather|ˈweðər|날씨|How's the weather?|날씨 어때요?|🌤️
            sunny|ˈsʌni|화창한|It's sunny today.|오늘 화창해요.|☀️
            rain|reɪn|비, 비가 오다|It's raining.|비가 와요.|🌧️
            snow|snoʊ|눈, 눈이 오다|I love snow.|나는 눈을 좋아해요.|❄️
            windy|ˈwɪndi|바람 부는|It's windy outside.|밖에 바람이 불어요.|🌬️
            cloudy|ˈklaʊdi|흐린|It's cloudy.|날이 흐려요.|☁️
            spring|sprɪŋ|봄|Flowers bloom in spring.|봄에 꽃이 펴요.|🌷
            summer|ˈsʌmər|여름|Summer is hot.|여름은 더워요.|🏖️
            fall|fɔːl|가을|I like fall.|나는 가을이 좋아요.|🍂
            winter|ˈwɪntər|겨울|Winter is cold.|겨울은 추워요.|☃️
        """),
        Chapter("장소와 길", "🗺️", "어디에 있는지, 어디로 가는지", """
            school|skuːl|학교|My son goes to school.|아들은 학교에 다녀요.|🏫
            hospital|ˈhɑːspɪtəl|병원|Where is the hospital?|병원이 어디예요?|🏥
            store|stɔːr|가게|The store is open.|가게가 열었어요.|🏪
            restaurant|ˈrestərɑːnt|식당|Let's go to a restaurant.|식당에 가요.|🍽️
            park|pɑːrk|공원|Let's walk in the park.|공원에서 걷자.|🌳
            station|ˈsteɪʃən|역|Where is the station?|역이 어디예요?|🚉
            airport|ˈerpɔːrt|공항|Take me to the airport.|공항으로 가 주세요.|✈️
            hotel|hoʊˈtel|호텔|I'm staying at a hotel.|호텔에 묵고 있어요.|🏨
            bank|bæŋk|은행|The bank is closed.|은행이 닫았어요.|🏦
            bus|bʌs|버스|I take the bus.|버스를 타요.|🚌
            subway|ˈsʌbweɪ|지하철|The subway is fast.|지하철이 빨라요.|🚇
            taxi|ˈtæksi|택시|Let's take a taxi.|택시 타자.|🚕
            here|hɪr|여기|Come here.|이리 와.|📍
            there|ðer|거기, 저기|It's over there.|저기 있어요.|👉
            left|left|왼쪽|Turn left.|왼쪽으로 도세요.|⬅️
            right|raɪt|오른쪽, 맞은|Turn right.|오른쪽으로 도세요.|➡️
            straight|streɪt|곧장|Go straight.|곧장 가세요.|⬆️
            near|nɪr|가까운|Is it near here?|여기서 가까워요?|📏
            far|fɑːr|먼|It's not far.|멀지 않아요.|🛣️
            where|wer|어디|Where are you?|어디예요?|❓
        """),
        Chapter("생활 표현 50", "💬", "통째로 외워 바로 쓰는 문장", """
            How are you?|haʊ ɑːr juː|잘 지내요?|How are you? — I'm good, thanks.|잘 지내요? — 잘 지내요, 고마워요.|🙂
            I'm good, thanks.|aɪm ɡʊd θæŋks|잘 지내요, 고마워요|I'm good, thanks. And you?|잘 지내요, 고마워요. 당신은요?|👍
            Nice to meet you.|naɪs tə miːt juː|만나서 반가워요|Hi, I'm Jin. Nice to meet you.|안녕하세요, 진이에요. 만나서 반가워요.|🤝
            What's your name?|wʌts jər neɪm|이름이 뭐예요?|What's your name? — I'm Sora.|이름이 뭐예요? — 소라예요.|📛
            Where are you from?|wer ɑːr juː frʌm|어디에서 왔어요?|Where are you from? — I'm from Korea.|어디에서 왔어요? — 한국에서 왔어요.|🌏
            I'm from Korea.|aɪm frʌm kəˈriːə|한국에서 왔어요|I'm from Korea. I live in Busan.|한국에서 왔어요. 부산에 살아요.|🇰🇷
            Thank you very much.|θæŋk juː ˈveri mʌtʃ|정말 고맙습니다|Thank you very much for your help.|도와줘서 정말 고마워요.|🙏
            You're welcome.|jʊr ˈwelkəm|천만에요|Thank you! — You're welcome.|고마워요! — 천만에요.|😊
            I'm sorry.|aɪm ˈsɑːri|미안해요|I'm sorry, I'm late.|늦어서 미안해요.|😔
            That's okay.|ðæts oʊˈkeɪ|괜찮아요|I'm sorry. — That's okay.|미안해요. — 괜찮아요.|👌
            Excuse me.|ɪkˈskjuːz miː|실례합니다|Excuse me, can I sit here?|실례합니다, 여기 앉아도 될까요?|🙋
            Can you help me?|kæn juː help miː|도와줄 수 있어요?|Excuse me, can you help me?|실례지만 도와줄 수 있어요?|🆘
            I don't understand.|aɪ doʊnt ˌʌndərˈstænd|이해가 안 돼요|Sorry, I don't understand.|죄송해요, 이해가 안 돼요.|🧐
            Please speak slowly.|pliːz spiːk ˈsloʊli|천천히 말해 주세요|Please speak slowly. My English is not good.|천천히 말해 주세요. 영어를 잘 못해요.|🐢
            Can you say that again?|kæn juː seɪ ðæt əˈɡen|다시 말해 줄래요?|Sorry, can you say that again?|미안해요, 다시 말해 줄래요?|🔁
            How do you say this in English?|haʊ duː juː seɪ ðɪs ɪn ˈɪŋɡlɪʃ|이거 영어로 뭐라고 해요?|How do you say this in English?|이거 영어로 뭐라고 해요?|❓
            I speak a little English.|aɪ spiːk ə ˈlɪtəl ˈɪŋɡlɪʃ|영어를 조금 해요|I speak a little English.|영어를 조금 해요.|🤏
            How much is this?|haʊ mʌtʃ ɪz ðɪs|이거 얼마예요?|How much is this bag?|이 가방 얼마예요?|💰
            I'll take it.|aɪl teɪk ɪt|이걸로 할게요|I like it. I'll take it.|마음에 들어요. 이걸로 할게요.|🛍️
            Can I pay by card?|kæn aɪ peɪ baɪ kɑːrd|카드로 계산해도 돼요?|Can I pay by card?|카드로 계산해도 돼요?|💳
            Where is the bathroom?|wer ɪz ðə ˈbæθruːm|화장실이 어디예요?|Excuse me, where is the bathroom?|실례지만 화장실이 어디예요?|🚻
            I'd like this, please.|aɪd laɪk ðɪs pliːz|이거 주세요|I'd like this, please.|이거 주세요.|👉
            Water, please.|ˈwɔːtər pliːz|물 주세요|Water, please.|물 주세요.|💧
            The check, please.|ðə tʃek pliːz|계산서 주세요|Excuse me. The check, please.|저기요, 계산서 주세요.|🧾
            It's delicious.|ɪts dɪˈlɪʃəs|맛있어요|Wow, it's delicious!|와, 맛있어요!|😋
            I'm hungry.|aɪm ˈhʌŋɡri|배고파요|I'm hungry. Let's eat.|배고파요. 먹어요.|🍽️
            What time is it?|wʌt taɪm ɪz ɪt|몇 시예요?|What time is it now?|지금 몇 시예요?|⏰
            See you later.|siː juː ˈleɪtər|나중에 봐요|Bye! See you later.|안녕! 나중에 봐요.|👋
            Have a nice day.|hæv ə naɪs deɪ|좋은 하루 보내세요|Thanks! Have a nice day.|고마워요! 좋은 하루 보내세요.|☀️
            Good job!|ɡʊd dʒɑːb|잘했어요!|You did it. Good job!|해냈구나. 잘했어!|👏
            Let's go.|lets ɡoʊ|가자|Are you ready? Let's go.|준비됐어? 가자.|🚶
            Wait a minute.|weɪt ə ˈmɪnɪt|잠깐만요|Wait a minute, please.|잠깐만요.|⏳
            I don't know.|aɪ doʊnt noʊ|모르겠어요|Sorry, I don't know.|미안해요, 모르겠어요.|🤷
            I think so.|aɪ θɪŋk soʊ|그런 것 같아요|Is it right? — I think so.|맞아요? — 그런 것 같아요.|🤔
            Me too.|miː tuː|나도요|I like pizza. — Me too!|나 피자 좋아해. — 나도!|🙌
            Of course.|əv kɔːrs|물론이죠|Can you help me? — Of course.|도와줄래요? — 물론이죠.|👍
            No problem.|noʊ ˈprɑːbləm|문제없어요|Thanks! — No problem.|고마워요! — 문제없어요.|👌
            Take care.|teɪk ker|잘 지내요, 몸 조심해요|Bye, take care.|안녕, 잘 지내.|💙
            I love it.|aɪ lʌv ɪt|정말 좋아요|This gift? I love it!|이 선물이요? 정말 좋아요!|😍
            Are you okay?|ɑːr juː oʊˈkeɪ|괜찮아요?|You look tired. Are you okay?|피곤해 보여요. 괜찮아요?|🤗
            I feel sick.|aɪ fiːl sɪk|몸이 안 좋아요|I feel sick. I want to go home.|몸이 안 좋아요. 집에 가고 싶어요.|🤒
            Call 119, please.|kɔːl wʌn wʌn naɪn pliːz|119에 전화해 주세요|Help! Call 119, please.|도와주세요! 119에 전화해 주세요.|🚑
            I'm lost.|aɪm lɔːst|길을 잃었어요|Excuse me, I'm lost.|실례지만 길을 잃었어요.|🧭
            How can I get to the station?|haʊ kæn aɪ ɡet tə ðə ˈsteɪʃən|역에 어떻게 가요?|How can I get to the station from here?|여기서 역에 어떻게 가요?|🚉
            Is it far from here?|ɪz ɪt fɑːr frʌm hɪr|여기서 멀어요?|Is the museum far from here?|박물관이 여기서 멀어요?|🛣️
            Can I have the menu?|kæn aɪ hæv ðə ˈmenjuː|메뉴판 주실래요?|Hi, can I have the menu?|안녕하세요, 메뉴판 주실래요?|📋
            I have a reservation.|aɪ hæv ə ˌrezərˈveɪʃən|예약했어요|I have a reservation for two.|두 명 예약했어요.|📅
            What do you do?|wʌt duː juː duː|무슨 일 하세요?|What do you do? — I'm a nurse.|무슨 일 하세요? — 간호사예요.|💼
            I like it here.|aɪ laɪk ɪt hɪr|여기 좋아요|This town is nice. I like it here.|이 동네 좋네요. 여기 좋아요.|🏙️
            Happy birthday!|ˈhæpi ˈbɜːrθdeɪ|생일 축하해요!|Happy birthday, Mom!|엄마, 생일 축하해요!|🎂
        """),
    )

    fun all(): List<Pair<ChapterEntity, List<WordEntity>>> = chapters.map { c ->
        ChapterEntity(title = c.title, description = c.desc, source = "beginner", level = 0, emoji = c.emoji) to
            c.rows.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
                val p = line.split("|")
                WordEntity(
                    chapterId = 0, text = p[0], ipa = p[1], meaningKo = p[2],
                    exampleEn = p.getOrElse(3) { "" }, exampleKo = p.getOrElse(4) { "" },
                    emoji = p.getOrElse(5) { "" },
                )
            }
    }
}
