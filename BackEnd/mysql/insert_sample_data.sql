-- =============================================
-- 插入示例数据
-- =============================================

SET NAMES utf8mb4;

-- 插入用户数据
INSERT INTO `USERS` (`ID`, `USERNAME`, `CONTACT_INFO`, `PASSWORD`, `ROLE`, `CREATED_AT`) VALUES
(1, 'admin', 'admin@example.com', '$2a$10$S2.pYcCOB5uQjy4geXShTO8/iurJqDyWZ7YQugEkzINYoK6bK7lli', 'ADMIN', '2023-01-01 00:00:00'),
(2, 'expert', 'expert1@example.com', '$2a$10$S2.pYcCOB5uQjy4geXShTO8/iurJqDyWZ7YQugEkzINYoK6bK7lli', 'EXPERT', '2023-01-02 00:00:00'),
(3, 'user', 'user1@example.com', '$2a$10$S2.pYcCOB5uQjy4geXShTO8/iurJqDyWZ7YQugEkzINYoK6bK7lli', 'CROWDSOURCE_USER', '2023-01-03 00:00:00');

-- 插入标签数据
INSERT INTO `TAGS` (`ID`, `TAG_NAME`, `TAG_TYPE`, `DESCRIPTION`, `CREATED_BY_USER_ID`) VALUES
(1, '内科', '科室', '内科相关知识', 1),
(2, '外科', '科室', '外科相关知识', 1),
(3, '心脏病', '疾病', '与心脏相关的疾病', 1),
(4, '高血压', '疾病', '血压持续升高的疾病', 1),
(5, '糖尿病', '疾病', '以血糖升高为特征的代谢疾病', 1),
(6, '诊断', '主题', '疾病的诊断相关知识', 1),
(7, '治疗', '主题', '疾病的治疗相关知识', 1),
(8, '预防', '主题', '疾病的预防相关知识', 1),
(9, '呼吸系统疾病', '疾病分类', '与呼吸系统相关的疾病', 1),
(10, '消化系统疾病', '疾病分类', '与消化系统相关的疾病', 1),
(11, '神经系统疾病', '疾病分类', '与神经系统相关的疾病', 1),
(12, '传染病', '疾病分类', '具有传染性的疾病', 1),
(13, '急救知识', '主题分类', '关于急救处理的知识', 1),
(14, '生活方式', '主题分类', '与健康生活方式相关', 1),
(15, '儿童健康', '人群分类', '与儿童健康相关的问题', 1),
(16, '老年健康', '人群分类', '与老年健康相关的问题', 1),
(17, '维生素与矿物质', '营养学', '关于维生素和矿物质的知识', 1),
(18, '心理健康', '健康领域', '与心理健康相关的问题', 1),
(19, '药物使用', '药学', '关于药物正确使用的知识', 1),
(20, '公共卫生', '健康领域', '涉及公共卫生的问题', 1);

-- 插入原始问题数据 (与标准问题一一对应，简化处理)
-- 这里直接指定原始问题的 ID，使其与 STANDARD_QUESTIONS 的 ID 对应。
INSERT INTO `RAW_QUESTIONS` (`ID`, `SOURCE_URL`, `SOURCE_SITE`, `TITLE`, `CONTENT`, `CRAWL_TIME`, `TAGS`, `OTHER_METADATA`) VALUES
(1, 'http://example.com/forum/flu/1', '健康社区', '感冒了头疼咳嗽怎么办？吃什么药好？', '最近天气变化大，不小心感冒了，头特别疼，还咳得很厉害，请问有什么办法可以缓解症状？或者推荐一些非处方药？', NOW() - INTERVAL 1 DAY, JSON_ARRAY('感冒', '头痛', '咳嗽', '用药'), JSON_OBJECT('original_id', 1001)),
(2, 'http://example.com/forum/digestion/2', '医疗问答', '胃胀消化不良，吃什么能帮助消化？', '最近总是感觉胃胀胀的，吃完东西不消化，请问有没有什么食物或者方法可以帮助缓解胃胀和促进消化？', NOW() - INTERVAL 2 DAY, JSON_ARRAY('消化不良', '胃胀', '饮食'), JSON_OBJECT('original_id', 1002)),
(3, 'http://example.com/forum/sleep/3', '养生论坛', '长期失眠怎么办？有哪些助眠方法？', '我已经连续好几个晚上睡不着了，躺在床上翻来覆去，感觉很痛苦。想问问有没有什么比较有效的助眠方法，或者需要去看医生吗？', NOW() - INTERVAL 3 DAY, JSON_ARRAY('失眠', '睡眠障碍', '助眠'), JSON_OBJECT('original_id', 1003)),
(4, 'http://example.com/forum/skin/4', '皮肤健康', '脸上长痘痘，怎么祛痘不留痕？', '青春期开始脸上就一直长痘，现在工作了还是长。请问有什么好的祛痘方法，最重要的是不要留下痘印或者疤痕。', NOW() - INTERVAL 4 DAY, JSON_ARRAY('痘痘', '痤疮', '皮肤护理'), JSON_OBJECT('original_id', 1004)),
(5, 'http://example.com/forum/exercise/5', '健身区', '每天跑步半小时，对身体有什么好处？', '我最近开始坚持每天慢跑30分钟，想知道长期坚持跑步对身体健康有什么具体的好处？需要注意些什么？', NOW() - INTERVAL 5 DAY, JSON_ARRAY('跑步', '健身', '运动'), JSON_OBJECT('original_id', 1005)),
(6, 'http://example.com/forum/diet/6', '营养咨询', '减肥期间怎么安排饮食？需要戒掉碳水化合物吗？', '正在减肥，想知道怎么制定一个健康的饮食计划？是不是完全不能吃米饭、面条这些碳水？', NOW() - INTERVAL 6 DAY, JSON_ARRAY('减肥', '饮食', '营养'), JSON_OBJECT('original_id', 1006)),
(7, 'http://example.com/forum/allergy/7', '过敏防治', '春天花粉过敏严重，如何缓解鼻塞流涕？', '一到春天花粉季节就喷嚏不断，鼻塞流鼻涕眼睛痒，特别难受。请问除了吃抗过敏药，还有没有其他方法可以缓解这些症状？', NOW() - INTERVAL 7 DAY, JSON_ARRAY('花粉过敏', '过敏性鼻炎', '鼻塞'), JSON_OBJECT('original_id', 1007)),
(8, 'http://example.com/forum/baby/8', '育儿经验', '宝宝夜里哭闹不安，是不是饿了？', '我家宝宝刚满月，最近夜里总是突然哭醒，喂奶也不一定能哄好，有时候换尿布也没用。不知道是什么原因导致的？是不是饿了？', NOW() - INTERVAL 8 DAY, JSON_ARRAY('婴儿', '哭闹', '睡眠'), JSON_OBJECT('original_id', 1008)),
(9, 'http://example.com/forum/eyes/9', '眼部健康', '长时间看电脑眼睛干涩，怎么缓解？', '因为工作原因需要长时间盯着电脑屏幕，现在总感觉眼睛很干涩，有时候还会模糊。请问有什么方法可以缓解眼部疲劳和干涩？', NOW() - INTERVAL 9 DAY, JSON_ARRAY('眼干', '眼疲劳', '视力健康'), JSON_OBJECT('original_id', 1009)),
(10, 'http://example.com/forum/joints/10', '骨科问答', '膝盖爬楼梯疼，是什么原因？', '最近上下楼梯的时候感觉膝盖有点疼，平时走路还好。这种情况是什么原因引起的？需要去医院检查吗？', NOW() - INTERVAL 10 DAY, JSON_ARRAY('膝盖痛', '关节炎', '骨骼健康'), JSON_OBJECT('original_id', 1010)),
(11, 'http://example.com/forum/headache/11', '神经内科', '经常偏头痛，怎么预防和治疗？', '我经常单侧头部剧烈疼痛，医生说是偏头痛。请问除了吃止痛药，还有没有其他方法可以预防偏头痛发作？发作时怎么处理？', NOW() - INTERVAL 11 DAY, JSON_ARRAY('偏头痛', '头痛', '神经系统'), JSON_OBJECT('original_id', 1011)),
(12, 'http://example.com/forum/oral/12', '口腔健康', '牙龈出血刷牙疼，是什么问题？', '最近刷牙的时候发现牙龈容易出血，有时候还会感觉疼痛。这是牙周病吗？应该怎么护理牙齿？', NOW() - INTERVAL 12 DAY, JSON_ARRAY('牙龈出血', '口腔护理', '牙周病'), JSON_OBJECT('original_id', 1012)),
(13, 'http://example.com/forum/bloodpressure/13', '心血管科', '高血压病人饮食要注意什么？', '家里老人有高血压，想了解一下高血压病人在日常饮食上需要特别注意哪些方面？有哪些食物是应该避免的？', NOW() - INTERVAL 13 DAY, JSON_ARRAY('高血压', '心血管', '饮食'), JSON_OBJECT('original_id', 1013)),
(14, 'http://example.com/forum/diabetes/14', '内分泌科', '糖尿病患者如何控制血糖？', '我刚被诊断出糖尿病，医生让我控制饮食和运动。请问糖尿病患者具体应该怎么做才能有效控制血糖？', NOW() - INTERVAL 14 DAY, JSON_ARRAY('糖尿病', '血糖控制', '内分泌'), JSON_OBJECT('original_id', 1014)),
(15, 'http://example.com/forum/period/15', '妇科', '月经不调，量少是什么原因？', '我的月经周期不稳定，有时候提前有时候推后，而且量比较少。这是什么原因引起的？需要看医生吗？', NOW() - INTERVAL 15 DAY, JSON_ARRAY('月经不调', '妇科', '内分泌'), JSON_OBJECT('original_id', 1015)),
(16, 'http://example.com/forum/childhealth/16', '儿科', '儿童发烧，物理降温怎么做？', '我家小孩发烧了，医生让先物理降温。请问具体应该怎么进行物理降温，有哪些方法比较有效？', NOW() - INTERVAL 16 DAY, JSON_ARRAY('儿童发烧', '物理降温', '儿科'), JSON_OBJECT('original_id', 1016)),
(17, 'http://example.com/forum/vitamins/17', '营养与健康', '维生素C有什么作用？怎么补充？', '听人说维生素C对身体很好，想知道它具体有什么作用？平时应该怎么通过食物或者补充剂来获取足够的维生素C？', NOW() - INTERVAL 17 DAY, JSON_ARRAY('维生素C', '营养', '保健'), JSON_OBJECT('original_id', 1017)),
(18, 'http://example.com/forum/backpain/18', '康复医学', '久坐腰痛，怎么锻炼缓解？', '因为工作性质需要长时间坐着，最近感觉腰部很酸痛。请问有什么简单的锻炼方法可以缓解久坐引起的腰痛？', NOW() - INTERVAL 18 DAY, JSON_ARRAY('腰痛', '久坐', '康复'), JSON_OBJECT('original_id', 1018)),
(19, 'http://example.com/forum/exerciseinjury/19', '运动医学', '跑步扭到脚踝，怎么处理？', '跑步时不小心扭到脚踝了，现在有点肿胀疼痛。请问应该立即怎么处理？需要冰敷还是热敷？', NOW() - INTERVAL 19 DAY, JSON_ARRAY('运动损伤', '扭伤', '急救'), JSON_OBJECT('original_id', 1019)),
(20, 'http://example.com/forum/mentalhealth/20', '心理健康', '感觉压力大情绪低落，如何调整？', '最近工作压力很大，总感觉心情很低落，对什么都提不起兴趣。请问有什么方法可以帮助我调整情绪，缓解压力？', NOW() - INTERVAL 20 DAY, JSON_ARRAY('压力', '情绪', '心理健康'), JSON_OBJECT('original_id', 1020)),
(21, 'http://example.com/forum/dietarysupplements/21', '保健品', '鱼油有什么功效？适合什么人吃？', '听说鱼油对心血管很好，想知道鱼油到底有哪些功效？是不是所有人都适合吃鱼油？', NOW() - INTERVAL 21 DAY, JSON_ARRAY('鱼油', '保健品', '心血管'), JSON_OBJECT('original_id', 1021)),
(22, 'http://example.com/forum/immunization/22', '疫苗接种', '成年人需要打流感疫苗吗？', '每年秋天都会听到大家说打流感疫苗，请问成年人有必要打流感疫苗吗？对预防感冒有效吗？', NOW() - INTERVAL 22 DAY, JSON_ARRAY('流感疫苗', '疫苗', '预防'), JSON_OBJECT('original_id', 1022)),
(23, 'http://example.com/forum/pregnancy/23', '孕期健康', '怀孕初期应该注意什么？', '刚怀孕，想知道怀孕初期（前三个月）在饮食、运动和生活习惯上需要特别注意些什么？', NOW() - INTERVAL 23 DAY, JSON_ARRAY('怀孕', '孕期护理', '妇产科'), JSON_OBJECT('original_id', 1023)),
(24, 'http://example.com/forum/skincare/24', '美容护肤', '防晒霜怎么选？SPF值越高越好吗？', '夏天快到了，想买防晒霜。请问怎么选择适合自己的防晒霜？是不是SPF值越高防晒效果越好？', NOW() - INTERVAL 24 DAY, JSON_ARRAY('防晒', '护肤', '美容'), JSON_OBJECT('original_id', 1024)),
(25, 'http://example.com/forum/smoking/25', '戒烟', '戒烟很难坚持怎么办？', '我尝试戒烟好几次了，但总是坚持不下来。请问有什么好的方法或者建议可以帮助我成功戒烟？', NOW() - INTERVAL 25 DAY, JSON_ARRAY('戒烟', '尼古丁依赖', '健康习惯'), JSON_OBJECT('original_id', 1025)),
(26, 'http://example.com/forum/alcohol/26', '饮酒健康', '少量饮酒对身体有害吗？', '都说喝酒不好，但是少量饮酒呢？比如每天喝一小杯红酒，对身体健康有没有影响？', NOW() - INTERVAL 26 DAY, JSON_ARRAY('饮酒', '健康习惯', '心血管'), JSON_OBJECT('original_id', 1026)),
(27, 'http://example.com/forum/cholesterol/27', '高血脂', '体检查出高胆固醇，怎么降下来？', '体检报告显示总胆固醇偏高，医生建议调整饮食。请问高胆固醇患者在饮食和生活上应该怎么做才能把胆固醇降下来？', NOW() - INTERVAL 27 DAY, JSON_ARRAY('高胆固醇', '血脂异常', '饮食'), JSON_OBJECT('original_id', 1027)),
(28, 'http://example.com/forum/anemia/28', '贫血', '女性容易贫血，怎么补血？', '我是女生，听说女生比较容易贫血。请问怎么判断自己是不是贫血？如果贫血了应该怎么补血，吃什么好？', NOW() - INTERVAL 28 DAY, JSON_ARRAY('贫血', '女性健康', '营养'), JSON_OBJECT('original_id', 1028)),
(29, 'http://example.com/forum/yoga/29', '运动健身', '练瑜伽对身体有什么好处？', '最近想尝试练瑜伽，想知道练瑜伽除了塑形，对身体健康还有哪些好处？适合哪些人群？', NOW() - INTERVAL 29 DAY, JSON_ARRAY('瑜伽', '健身', '柔韧性'), JSON_OBJECT('original_id', 1029)),
(30, 'http://example.com/forum/meditation/30', '心理健康', '如何进行冥想？对缓解焦虑有效吗？', '听说冥想可以帮助放松和缓解焦虑，请问新手应该怎么开始进行冥想？冥想真的对缓解焦虑有效吗？', NOW() - INTERVAL 30 DAY, JSON_ARRAY('冥想', '放松', '焦虑'), JSON_OBJECT('original_id', 1030)),
(31, 'http://example.com/forum/firstaid/31', '急救知识', '家里小孩误食了不明物体，怎么处理？', '我家小孩玩耍时不小心把一个小东西放进嘴里咽下去了，我现在很着急。请问遇到这种情况应该怎么办？需要去医院吗？', NOW() - INTERVAL 31 DAY, JSON_ARRAY('急救', '误食', '儿童安全'), JSON_OBJECT('original_id', 1031)),
(32, 'http://example.com/forum/vaccination/32', '疫苗', '新冠疫苗需要每年打吗？', '我之前打过新冠疫苗，现在还需要每年打吗？如果需要，打哪种疫苗比较好？', NOW() - INTERVAL 32 DAY, JSON_ARRAY('新冠疫苗', '疫苗接种', '传染病'), JSON_OBJECT('original_id', 1032)),
(33, 'http://example.com/forum/pethealth/33', '宠物健康', '狗狗拉肚子怎么办？', '我家狗狗今天突然拉肚子，精神不太好。请问狗狗拉肚子是什么原因？应该怎么照顾它？需要带它去看医生吗？', NOW() - INTERVAL 33 DAY, JSON_ARRAY('宠物健康', '狗狗', '腹泻'), JSON_OBJECT('original_id', 1033)),
(34, 'http://example.com/forum/travelhealth/34', '旅行健康', '出国旅行需要打什么疫苗？', '准备出国旅行，目的地是一些热带地区。请问去这些地方旅行之前需要提前打什么疫苗来预防疾病？', NOW() - INTERVAL 34 DAY, JSON_ARRAY('旅行健康', '疫苗', '传染病预防'), JSON_OBJECT('original_id', 1034)),
(35, 'http://example.com/forum/dentalcare/35', '牙齿健康', '洗牙对牙齿有伤害吗？多久洗一次比较好？', '想去洗牙，但是听说洗牙会损伤牙齿。请问洗牙真的对牙齿有伤害吗？一般建议多久洗一次牙比较好？', NOW() - INTERVAL 35 DAY, JSON_ARRAY('洗牙', '口腔护理', '牙齿保健'), JSON_OBJECT('original_id', 1035)),
(36, 'http://example.com/forum/foothealth/36', '足部健康', '脚气总是复发怎么办？', '我的脚气反反复复，好了又长。请问脚气为什么容易复发？有没有什么根治的方法或者特别有效的药？', NOW() - INTERVAL 36 DAY, JSON_ARRAY('脚气', '真菌感染', '皮肤病'), JSON_OBJECT('original_id', 1036)),
(37, 'http://example.com/forum/hairloss/37', '脱发', '年轻人脱发严重，怎么改善？', '我年纪轻轻就开始掉很多头发，感觉发际线越来越高了。请问年轻人脱发是什么原因？有没有什么方法可以改善脱发情况？', NOW() - INTERVAL 37 DAY, JSON_ARRAY('脱发', '头发健康', '皮肤科'), JSON_OBJECT('original_id', 1037)),
(38, 'http://example.com/forum/neckpain/38', '颈椎病', '长时间低头玩手机，颈椎不舒服怎么办？', '我经常低头玩手机或者看电脑，现在感觉颈椎很僵硬不舒服，有时候还会头晕。请问这是颈椎病吗？有什么方法可以缓解？', NOW() - INTERVAL 38 DAY, JSON_ARRAY('颈椎病', '颈椎痛', '不良习惯'), JSON_OBJECT('original_id', 1038)),
(39, 'http://example.com/forum/obesity/39', '肥胖', '判定肥胖的标准是什么？肥胖对健康有什么影响？', '想知道怎么科学地判断一个人是不是肥胖？肥胖除了影响外形，对身体健康具体有哪些危害？', NOW() - INTERVAL 39 DAY, JSON_ARRAY('肥胖', '体重管理', '健康风险'), JSON_OBJECT('original_id', 1039)),
(40, 'http://example.com/forum/hydration/40', '饮水健康', '每天要喝多少水才够？喝水有什么好处？', '经常听人说要多喝水，请问成年人每天到底需要喝多少水才算是足够的？喝水对身体有什么具体的好处？', NOW() - INTERVAL 40 DAY, JSON_ARRAY('饮水', '水分', '健康习惯'), JSON_OBJECT('original_id', 1040)),
(41, 'http://example.com/forum/heatstroke/41', '防暑降温', '中暑了应该立即怎么办？怎么避免中暑？', '天气太热担心会中暑，请问如果自己或家人中暑了，第一时间应该怎么处理？以及平时怎么做可以预防中暑？', NOW() - INTERVAL 41 DAY, JSON_ARRAY('中暑', '急救', '预防'), JSON_OBJECT('original_id', 1041));


-- 插入原始回答数据 (每个原始问题至少一个回答)
-- 为了简化，我们为每个原始问题插入一个示例回答，并与原始问题ID关联。
-- 这里同样依赖 RAW_QUESTIONS 的 ID 按顺序生成。
INSERT INTO `RAW_ANSWERS` (`RAW_QUESTION_ID`, `AUTHOR_INFO`, `CONTENT`, `PUBLISH_TIME`, `UPVOTES`, `IS_ACCEPTED`, `OTHER_METADATA`) VALUES
(1, '医生李', '高血压早期确实可能没有症状，但头晕是常见症状之一。建议您带爸爸去医院量一下血压。', NOW() - INTERVAL 1 DAY + INTERVAL 1 HOUR, 15, TRUE, JSON_OBJECT('answer_id', 2001)),
(2, '注册营养师小张', '糖尿病饮食核心是控制总热量和均衡营养。要少油少盐少糖，多吃蔬菜、粗粮，定时定量。', NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR, 22, TRUE, JSON_OBJECT('answer_id', 2002)),
(3, '心内科医生刘', '您描述的胸痛、气短、出汗是冠心病的典型症状，但还需要结合其他检查。请尽快去医院心内科就诊。', NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR, 30, TRUE, JSON_OBJECT('answer_id', 2003)),
(4, '化验师王', '正常人的空腹血糖值通常在3.9-6.1 mmol/L之间。如果高于7.0 mmol/L，需要警惕糖尿病。', NOW() - INTERVAL 4 DAY + INTERVAL 1 HOUR, 18, TRUE, JSON_OBJECT('answer_id', 2004)),
(5, '健康顾问', '成年人安静时的心率正常范围是每分钟60到100次。', NOW() - INTERVAL 5 DAY + INTERVAL 1 HOUR, 12, TRUE, JSON_OBJECT('answer_id', 2005)),
(6, '百科知识', '正常人体温的核心温度大约在36.1°C到37.2°C之间，通常腋窝温度低于口腔和直肠温度。', NOW() - INTERVAL 6 DAY + INTERVAL 1 HOUR, 9, TRUE, JSON_OBJECT('answer_id', 2006)),
(7, '社区医生', '健康成人理想血压应低于120/80 mmHg。', NOW() - INTERVAL 7 DAY + INTERVAL 1 HOUR, 25, TRUE, JSON_OBJECT('answer_id', 2007)),
(8, '健身教练', '根据世界卫生组织的标准，BMI大于或等于30kg/m²被定义为肥胖。', NOW() - INTERVAL 8 DAY + INTERVAL 1 HOUR, 11, TRUE, JSON_OBJECT('answer_id', 2008)),
(9, '高中生物老师', '人体最大的器官是皮肤。', NOW() - INTERVAL 9 DAY + INTERVAL 1 HOUR, 14, TRUE, JSON_OBJECT('answer_id', 2009)),
(10, '感冒诊所医生', '普通感冒主要症状是鼻塞、流鼻涕、咳嗽、喉咙痛。流感常伴有高烧、全身肌肉酸痛等更严重的症状。', NOW() - INTERVAL 10 DAY + INTERVAL 1 HOUR, 17, TRUE, JSON_OBJECT('answer_id', 2010)),
(11, '疾控中心专家', '预防流感最有效的方式是每年接种流感疫苗，同时保持勤洗手、开窗通风等良好卫生习惯。', NOW() - INTERVAL 11 DAY + INTERVAL 1 HOUR, 28, TRUE, JSON_OBJECT('answer_id', 2011)),
(12, '药剂师', '抗生素耐药性是指细菌等微生物对抗生素产生抵抗力，药物失效。主要是因为滥用和不规范使用抗生素。', NOW() - INTERVAL 12 DAY + INTERVAL 1 HOUR, 35, TRUE, JSON_OBJECT('answer_id', 2012)),
(13, '饮水研究员', '健康成年人每日推荐饮水量大约在1.5到2升，约合8杯水。', NOW() - INTERVAL 13 DAY + INTERVAL 1 HOUR, 20, TRUE, JSON_OBJECT('answer_id', 2013)),
(14, '营养师', '缺乏维生素A可能导致夜盲症。多吃胡萝卜、动物肝脏等富含维生素A的食物可以预防。', NOW() - INTERVAL 14 DAY + INTERVAL 1 HOUR, 19, TRUE, JSON_OBJECT('answer_id', 2014)),
(15, '急诊科医生', '立即拨打120，让患者半卧位休息，如有医生指导可含服硝酸甘油。医院会尽快开通堵塞血管并进行后续治疗和康复。', NOW() - INTERVAL 15 DAY + INTERVAL 1 HOUR, 40, TRUE, JSON_OBJECT('answer_id', 2015)),
(16, '呼吸科医生', '长期吸烟是COPD最重要的危险因素，此外空气污染、职业暴露、遗传等也相关。', NOW() - INTERVAL 16 DAY + INTERVAL 1 HOUR, 21, TRUE, JSON_OBJECT('answer_id', 2016)),
(17, '性病防治专家', '艾滋病主要通过性接触、血液及血制品传播、母婴传播。日常接触如一起吃饭不会传播。', NOW() - INTERVAL 17 DAY + INTERVAL 1 HOUR, 26, TRUE, JSON_OBJECT('answer_id', 2017)),
(18, '骨科医生', '骨质疏松最常见的骨折部位是脊椎体（胸腰椎）、髋部（股骨近端）、腕部（桡骨远端）。', NOW() - INTERVAL 18 DAY + INTERVAL 1 HOUR, 23, TRUE, JSON_OBJECT('answer_id', 2018)),
(19, '内分泌科医生', '通过健康饮食（控制糖分和总热量）、规律运动、控制体重、戒烟限酒、规律作息等可以有效预防2型糖尿病。', NOW() - INTERVAL 19 DAY + INTERVAL 1 HOUR, 33, TRUE, JSON_OBJECT('answer_id', 2019)),
(20, '疼痛科专家', '"癌症三阶梯止痛法"是由世界卫生组织（WHO）于1986年提出的。', NOW() - INTERVAL 20 DAY + INTERVAL 1 HOUR, 16, TRUE, JSON_OBJECT('answer_id', 2020)),
(21, '儿科医生', '温水擦浴、冰袋敷额头、减少衣物是常用物理降温方法。饮用大量冰水不推荐，可能刺激胃肠道。', NOW() - INTERVAL 21 DAY + INTERVAL 1 HOUR, 24, TRUE, JSON_OBJECT('answer_id', 2021)),
(22, '儿科过敏专家', '儿童常见的过敏原包括花粉、尘螨、霉菌、宠物皮屑、牛奶、鸡蛋、花生、大豆等。', NOW() - INTERVAL 22 DAY + INTERVAL 1 HOUR, 29, TRUE, JSON_OBJECT('answer_id', 2022)),
(23, '心理咨询师', '抑郁症核心症状是持续情绪低落和兴趣丧失。初步识别注意观察情绪、行为、睡眠、食欲等变化，如持续两周以上需就医。', NOW() - INTERVAL 23 DAY + INTERVAL 1 HOUR, 31, TRUE, JSON_OBJECT('answer_id', 2023)),
(24, '血液科医生', '人体内负责凝血功能的重要血细胞是血小板。', NOW() - INTERVAL 24 DAY + INTERVAL 1 HOUR, 13, TRUE, JSON_OBJECT('answer_id', 2024)),
(25, '妇产科医生', '用于早期筛查宫颈癌的常用检查方法是HPV检测和液基薄层细胞学检查（TCT）。', NOW() - INTERVAL 25 DAY + INTERVAL 1 HOUR, 27, TRUE, JSON_OBJECT('answer_id', 2025)),
(26, '神经内科医生', '阿尔茨海默病早期主要表现为近记忆力减退，中期出现全面认知功能障碍和行为问题，晚期生活不能自理。', NOW() - INTERVAL 26 DAY + INTERVAL 1 HOUR, 36, TRUE, JSON_OBJECT('answer_id', 2026)),
(27, '消化内科医生', '幽门螺杆菌感染与慢性胃炎、消化性溃疡、胃癌、胃MALT淋巴瘤等密切相关。', NOW() - INTERVAL 27 DAY + INTERVAL 1 HOUR, 34, TRUE, JSON_OBJECT('answer_id', 2027)),
(28, '眼科医生', '白内障最主要的治疗方法是手术摘除浑浊的晶状体并植入人工晶体，药物无法治愈白内障。', NOW() - INTERVAL 28 DAY + INTERVAL 1 HOUR, 22, TRUE, JSON_OBJECT('answer_id', 2028)),
(29, '急诊神经科医生', '对于急性缺血性脑卒中，发病后4.5小时内是静脉溶栓治疗的最佳时间窗（需符合适应症）。', NOW() - INTERVAL 29 DAY + INTERVAL 1 HOUR, 38, TRUE, JSON_OBJECT('answer_id', 2029)),
(30, '免疫学专家', '疫苗是减毒或灭活的病原体制剂，接种后刺激免疫系统产生抗体和记忆细胞，再次遇到病原体时能快速清除。', NOW() - INTERVAL 30 DAY + INTERVAL 1 HOUR, 32, TRUE, JSON_OBJECT('answer_id', 2030)),
(31, '传染病医生', '狂犬病病毒主要通过被携带病毒的动物（主要是狗）咬伤或抓伤，病毒随唾液进入人体传播。', NOW() - INTERVAL 31 DAY + INTERVAL 1 HOUR, 25, TRUE, JSON_OBJECT('answer_id', 2031)),
(32, '风湿免疫科医生', '痛风患者应严格限制高嘌呤食物，如动物内脏、海鲜（沙丁鱼、贝类）、肉汤、啤酒等。', NOW() - INTERVAL 32 DAY + INTERVAL 1 HOUR, 29, TRUE, JSON_OBJECT('answer_id', 2032)),
(33, '肾内科医生', '人体内维持水电解质平衡最重要的离子包括钠离子(Na+)、钾离子(K+)、氯离子(Cl-)等。', NOW() - INTERVAL 33 DAY + INTERVAL 1 HOUR, 18, TRUE, JSON_OBJECT('answer_id', 2033)),
(34, '公共卫生专家', '食物中毒是摄入有毒有害食物引起的疾病。预防包括注意食品卫生、生熟分开、彻底加热、安全储存等。', NOW() - INTERVAL 34 DAY + INTERVAL 1 HOUR, 30, TRUE, JSON_OBJECT('answer_id', 2034)),
(35, '血液科护士', '缺铁性贫血最常见的症状是疲劳、乏力，以及面色苍白、指甲脆裂等。', NOW() - INTERVAL 35 DAY + INTERVAL 1 HOUR, 15, TRUE, JSON_OBJECT('answer_id', 2035)),
(36, '执业药师', '"OTC"是Over-The-Counter的缩写，指非处方药，可以直接在药店购买，不需要医生处方。处方药必须凭医生处方购买和使用。', NOW() - INTERVAL 36 DAY + INTERVAL 1 HOUR, 20, TRUE, JSON_OBJECT('answer_id', 2036)),
(37, '健康教育员', '健康的习惯包括规律作息、均衡饮食、适度运动、戒烟限酒、保持良好心态、定期体检等。', NOW() - INTERVAL 37 DAY + INTERVAL 1 HOUR, 37, TRUE, JSON_OBJECT('answer_id', 2037)),
(38, '免疫学研究员', '免疫力是身体抵抗疾病的能力。提高免疫力靠均衡营养、充足睡眠、适度运动、管理压力、戒烟限酒、接种疫苗等。', NOW() - INTERVAL 38 DAY + INTERVAL 1 HOUR, 39, TRUE, JSON_OBJECT('answer_id', 2038)),
(39, '新生儿科护士', '新生儿病理性黄疸通常在出生后24小时内出现，或持续时间长、黄疸程度深，需要及时就医进行干预。', NOW() - INTERVAL 39 DAY + INTERVAL 1 HOUR, 22, TRUE, JSON_OBJECT('answer_id', 2039)),
(40, '结核病防治医生', '肺结核最主要的传播途径是呼吸道传播，特别是通过病人咳嗽、打喷嚏产生的飞沫传播。', NOW() - INTERVAL 40 DAY + INTERVAL 1 HOUR, 28, TRUE, JSON_OBJECT('answer_id', 2040)),
(41, '急救志愿者', '中暑了应迅速移到阴凉处，用湿毛巾擦拭降温，补充淡盐水。预防要避免高温暴晒，及时补水，穿着透气衣物。', NOW() - INTERVAL 41 DAY + INTERVAL 1 HOUR, 35, TRUE, JSON_OBJECT('answer_id', 2041));

-- 首先插入变更日志主表数据
INSERT INTO `CHANGE_LOG` (`ID`, `CHANGE_TIME`, `CHANGED_BY_USER_ID`, `CHANGE_TYPE`, `COMMIT_MESSAGE`, `ASSOCIATED_STANDARD_QUESTION_ID`) VALUES
(1, '2024-01-15 09:00:00', 1, 'BULK_CREATE', '批量导入医疗健康领域标准问题数据集，包含各种类型的医学问题', NULL),
(2, '2024-01-15 09:15:00', 1, 'CREATE', '创建高血压症状相关主观题', 1),
(3, '2024-01-15 09:16:00', 1, 'CREATE', '创建糖尿病饮食指导主观题', 2),
(4, '2024-01-15 09:17:00', 1, 'CREATE', '创建冠心病症状识别多选题', 3),
(5, '2024-01-15 09:18:00', 1, 'CREATE', '创建空腹血糖正常值单选题', 4),
(6, '2024-01-15 09:19:00', 1, 'CREATE', '创建成人心率范围事实题', 5),
(7, '2024-01-15 09:20:00', 1, 'CREATE', '创建人体正常体温事实题', 6),
(8, '2024-01-15 09:21:00', 1, 'CREATE', '创建血压理想范围事实题', 7),
(9, '2024-01-15 09:22:00', 1, 'CREATE', '创建BMI肥胖标准事实题', 8),
(10, '2024-01-15 09:23:00', 1, 'CREATE', '创建人体最大器官事实题', 9),
(11, '2024-01-15 09:24:00', 1, 'CREATE', '创建感冒症状识别单选题', 10),
(12, '2024-01-15 09:25:00', 1, 'CREATE', '创建流感预防措施多选题', 11),
(13, '2024-01-15 09:26:00', 1, 'CREATE', '创建抗生素耐药性解释主观题', 12),
(14, '2024-01-15 09:27:00', 1, 'CREATE', '创建每日饮水量事实题', 13),
(15, '2024-01-15 09:28:00', 1, 'CREATE', '创建维生素缺乏症单选题', 14),
(16, '2024-01-15 09:29:00', 1, 'CREATE', '创建心肌梗死急救主观题', 15),
(17, '2024-01-15 09:30:00', 1, 'CREATE', '创建COPD高危因素多选题', 16),
(18, '2024-01-15 09:31:00', 1, 'CREATE', '创建艾滋病传播途径事实题', 17),
(19, '2024-01-15 09:32:00', 1, 'CREATE', '创建骨质疏松骨折部位单选题', 18),
(20, '2024-01-15 09:33:00', 1, 'CREATE', '创建2型糖尿病预防主观题', 19),
(21, '2024-01-15 09:34:00', 1, 'CREATE', '创建癌症止痛法来源事实题', 20),
(22, '2024-01-15 09:35:00', 1, 'CREATE', '创建物理降温方法单选题', 21),
(23, '2024-01-15 09:36:00', 1, 'CREATE', '创建儿童过敏原多选题', 22),
(24, '2024-01-15 09:37:00', 1, 'CREATE', '创建抑郁症症状识别主观题', 23),
(25, '2024-01-15 09:38:00', 1, 'CREATE', '创建凝血血细胞事实题', 24),
(26, '2024-01-15 09:39:00', 1, 'CREATE', '创建宫颈癌筛查方法单选题', 25),
(27, '2024-01-15 09:40:00', 1, 'CREATE', '创建阿尔茨海默病临床表现主观题', 26),
(28, '2024-01-15 09:41:00', 1, 'CREATE', '创建幽门螺杆菌相关疾病多选题', 27),
(29, '2024-01-15 09:42:00', 1, 'CREATE', '创建白内障治疗方法事实题', 28),
(30, '2024-01-15 09:43:00', 1, 'CREATE', '创建脑卒中溶栓时间窗单选题', 29),
(31, '2024-01-15 09:44:00', 1, 'CREATE', '创建疫苗作用机制主观题', 30),
(32, '2024-01-15 09:45:00', 1, 'CREATE', '创建狂犬病传播方式事实题', 31),
(33, '2024-01-15 09:46:00', 1, 'CREATE', '创建痛风饮食限制单选题', 32),
(34, '2024-01-15 09:47:00', 1, 'CREATE', '创建水电解质平衡离子多选题', 33),
(35, '2024-01-15 09:48:00', 1, 'CREATE', '创建食物中毒预防主观题', 34),
(36, '2024-01-15 09:49:00', 1, 'CREATE', '创建缺铁性贫血症状事实题', 35),
(37, '2024-01-15 09:50:00', 1, 'CREATE', '创建OTC药品定义单选题', 36),
(38, '2024-01-15 09:51:00', 1, 'CREATE', '创建健康生活习惯多选题', 37),
(39, '2024-01-15 09:52:00', 1, 'CREATE', '创建免疫力提升方法主观题', 38),
(40, '2024-01-15 09:53:00', 1, 'CREATE', '创建新生儿黄疸分类事实题', 39),
(41, '2024-01-15 09:54:00', 1, 'CREATE', '创建肺结核传播途径单选题', 40),
(42, '2024-01-15 09:55:00', 1, 'CREATE', '创建中暑处理预防主观题', 41);

-- 然后插入变更日志详情数据
INSERT INTO CHANGE_LOG_DETAILS (CHANGE_LOG_ID, ENTITY_TYPE, ENTITY_ID, ATTRIBUTE_NAME, OLD_VALUE, NEW_VALUE) VALUES
-- 问题1的详情
(2, 'STANDARD_QUESTION', 1, 'QUESTION_TEXT', NULL, '"高血压的主要症状有哪些？"'),
(2, 'STANDARD_QUESTION', 1, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(2, 'STANDARD_QUESTION', 1, 'DIFFICULTY', NULL, '"MEDIUM"'),
(2, 'STANDARD_QUESTION', 1, 'ORIGINAL_RAW_QUESTION_ID', NULL, '1'),
(2, 'STANDARD_QUESTION', 1, 'CREATED_BY_USER_ID', NULL, '1'),

-- 问题2的详情
(3, 'STANDARD_QUESTION', 2, 'QUESTION_TEXT', NULL, '"糖尿病患者的日常饮食应该注意什么？"'),
(3, 'STANDARD_QUESTION', 2, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(3, 'STANDARD_QUESTION', 2, 'DIFFICULTY', NULL, '"MEDIUM"'),
(3, 'STANDARD_QUESTION', 2, 'ORIGINAL_RAW_QUESTION_ID', NULL, '2'),
(3, 'STANDARD_QUESTION', 2, 'CREATED_BY_USER_ID', NULL, '2'),

-- 问题3的详情
(4, 'STANDARD_QUESTION', 3, 'QUESTION_TEXT', NULL, '"以下哪些是冠心病的典型症状？\\nA. 胸痛\\nB. 气短\\nC. 出汗\\nD. 恶心"'),
(4, 'STANDARD_QUESTION', 3, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(4, 'STANDARD_QUESTION', 3, 'DIFFICULTY', NULL, '"MEDIUM"'),
(4, 'STANDARD_QUESTION', 3, 'ORIGINAL_RAW_QUESTION_ID', NULL, '3'),
(4, 'STANDARD_QUESTION', 3, 'CREATED_BY_USER_ID', NULL, '3'),

-- 问题4的详情
(5, 'STANDARD_QUESTION', 4, 'QUESTION_TEXT', NULL, '"正常人的空腹血糖值范围是多少？\\nA. 3.9-6.1 mmol/L\\nB. 7.0-8.0 mmol/L\\nC. 2.0-3.5 mmol/L\\nD. 8.0-10.0 mmol/L"'),
(5, 'STANDARD_QUESTION', 4, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(5, 'STANDARD_QUESTION', 4, 'DIFFICULTY', NULL, '"EASY"'),
(5, 'STANDARD_QUESTION', 4, 'ORIGINAL_RAW_QUESTION_ID', NULL, '4'),
(5, 'STANDARD_QUESTION', 4, 'CREATED_BY_USER_ID', NULL, '4'),

-- 问题5的详情
(6, 'STANDARD_QUESTION', 5, 'QUESTION_TEXT', NULL, '"成人正常心率范围是多少？"'),
(6, 'STANDARD_QUESTION', 5, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(6, 'STANDARD_QUESTION', 5, 'DIFFICULTY', NULL, '"EASY"'),
(6, 'STANDARD_QUESTION', 5, 'ORIGINAL_RAW_QUESTION_ID', NULL, '5'),
(6, 'STANDARD_QUESTION', 5, 'CREATED_BY_USER_ID', NULL, '5'),

-- 问题6的详情
(7, 'STANDARD_QUESTION', 6, 'QUESTION_TEXT', NULL, '"人体正常体温是多少摄氏度？"'),
(7, 'STANDARD_QUESTION', 6, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(7, 'STANDARD_QUESTION', 6, 'DIFFICULTY', NULL, '"EASY"'),
(7, 'STANDARD_QUESTION', 6, 'ORIGINAL_RAW_QUESTION_ID', NULL, '6'),
(7, 'STANDARD_QUESTION', 6, 'CREATED_BY_USER_ID', NULL, '6'),

-- 问题7的详情
(8, 'STANDARD_QUESTION', 7, 'QUESTION_TEXT', NULL, '"正常成人血压的理想范围是多少？"'),
(8, 'STANDARD_QUESTION', 7, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(8, 'STANDARD_QUESTION', 7, 'DIFFICULTY', NULL, '"EASY"'),
(8, 'STANDARD_QUESTION', 7, 'ORIGINAL_RAW_QUESTION_ID', NULL, '7'),
(8, 'STANDARD_QUESTION', 7, 'CREATED_BY_USER_ID', NULL, '7'),

-- 问题8的详情
(9, 'STANDARD_QUESTION', 8, 'QUESTION_TEXT', NULL, '"BMI指数超过多少被定义为肥胖？"'),
(9, 'STANDARD_QUESTION', 8, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(9, 'STANDARD_QUESTION', 8, 'DIFFICULTY', NULL, '"EASY"'),
(9, 'STANDARD_QUESTION', 8, 'ORIGINAL_RAW_QUESTION_ID', NULL, '8'),
(9, 'STANDARD_QUESTION', 8, 'CREATED_BY_USER_ID', NULL, '8'),

-- 问题9的详情
(10, 'STANDARD_QUESTION', 9, 'QUESTION_TEXT', NULL, '"人体中最大的器官是什么？"'),
(10, 'STANDARD_QUESTION', 9, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(10, 'STANDARD_QUESTION', 9, 'DIFFICULTY', NULL, '"EASY"'),
(10, 'STANDARD_QUESTION', 9, 'ORIGINAL_RAW_QUESTION_ID', NULL, '9'),
(10, 'STANDARD_QUESTION', 9, 'CREATED_BY_USER_ID', NULL, '9'),

-- 问题10的详情
(11, 'STANDARD_QUESTION', 10, 'QUESTION_TEXT', NULL, '"以下哪项不是常见的感冒症状？\\nA. 发烧\\nB. 咳嗽\\nC. 腹泻\\nD. 鼻塞"'),
(11, 'STANDARD_QUESTION', 10, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(11, 'STANDARD_QUESTION', 10, 'DIFFICULTY', NULL, '"EASY"'),
(11, 'STANDARD_QUESTION', 10, 'ORIGINAL_RAW_QUESTION_ID', NULL, '10'),
(11, 'STANDARD_QUESTION', 10, 'CREATED_BY_USER_ID', NULL, '10'),

-- 问题11的详情
(12, 'STANDARD_QUESTION', 11, 'QUESTION_TEXT', NULL, '"预防流感的有效措施包括哪些？\\nA. 每年接种流感疫苗\\nB. 保持良好的个人卫生习惯\\nC. 避免前往人群密集场所\\nD. 多喝水"'),
(12, 'STANDARD_QUESTION', 11, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(12, 'STANDARD_QUESTION', 11, 'DIFFICULTY', NULL, '"MEDIUM"'),
(12, 'STANDARD_QUESTION', 11, 'ORIGINAL_RAW_QUESTION_ID', NULL, '11'),
(12, 'STANDARD_QUESTION', 11, 'CREATED_BY_USER_ID', NULL, '11'),

-- 问题12的详情
(13, 'STANDARD_QUESTION', 12, 'QUESTION_TEXT', NULL, '"什么是抗生素耐药性？它是如何产生的？"'),
(13, 'STANDARD_QUESTION', 12, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(13, 'STANDARD_QUESTION', 12, 'DIFFICULTY', NULL, '"MEDIUM"'),
(13, 'STANDARD_QUESTION', 12, 'ORIGINAL_RAW_QUESTION_ID', NULL, '12'),
(13, 'STANDARD_QUESTION', 12, 'CREATED_BY_USER_ID', NULL, '12'),

-- 问题13的详情
(14, 'STANDARD_QUESTION', 13, 'QUESTION_TEXT', NULL, '"健康成年人每日推荐的饮水量大约是多少？"'),
(14, 'STANDARD_QUESTION', 13, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(14, 'STANDARD_QUESTION', 13, 'DIFFICULTY', NULL, '"EASY"'),
(14, 'STANDARD_QUESTION', 13, 'ORIGINAL_RAW_QUESTION_ID', NULL, '13'),
(14, 'STANDARD_QUESTION', 13, 'CREATED_BY_USER_ID', NULL, '13'),

-- 问题14的详情
(15, 'STANDARD_QUESTION', 14, 'QUESTION_TEXT', NULL, '"下列哪种维生素缺乏可能导致夜盲症？\\nA. 维生素A\\nB. 维生素C\\nC. 维生素D\\nD. 维生素B12"'),
(15, 'STANDARD_QUESTION', 14, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(15, 'STANDARD_QUESTION', 14, 'DIFFICULTY', NULL, '"MEDIUM"'),
(15, 'STANDARD_QUESTION', 14, 'ORIGINAL_RAW_QUESTION_ID', NULL, '14'),
(15, 'STANDARD_QUESTION', 14, 'CREATED_BY_USER_ID', NULL, '14'),

-- 问题15的详情
(16, 'STANDARD_QUESTION', 15, 'QUESTION_TEXT', NULL, '"请简述心肌梗死的急救措施和后续治疗原则。"'),
(16, 'STANDARD_QUESTION', 15, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(16, 'STANDARD_QUESTION', 15, 'DIFFICULTY', NULL, '"HARD"'),
(16, 'STANDARD_QUESTION', 15, 'ORIGINAL_RAW_QUESTION_ID', NULL, '15'),
(16, 'STANDARD_QUESTION', 15, 'CREATED_BY_USER_ID', NULL, '15'),

-- 问题16的详情
(17, 'STANDARD_QUESTION', 16, 'QUESTION_TEXT', NULL, '"以下哪些属于慢性阻塞性肺疾病（COPD）的高危因素？\\nA. 长期吸烟\\nB. 空气污染\\nC. 遗传因素\\nD. 过敏性鼻炎"'),
(17, 'STANDARD_QUESTION', 16, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(17, 'STANDARD_QUESTION', 16, 'DIFFICULTY', NULL, '"MEDIUM"'),
(17, 'STANDARD_QUESTION', 16, 'ORIGINAL_RAW_QUESTION_ID', NULL, '16'),
(17, 'STANDARD_QUESTION', 16, 'CREATED_BY_USER_ID', NULL, '16'),

-- 问题17的详情
(18, 'STANDARD_QUESTION', 17, 'QUESTION_TEXT', NULL, '"艾滋病的主要传播途径是什么？"'),
(18, 'STANDARD_QUESTION', 17, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(18, 'STANDARD_QUESTION', 17, 'DIFFICULTY', NULL, '"EASY"'),
(18, 'STANDARD_QUESTION', 17, 'ORIGINAL_RAW_QUESTION_ID', NULL, '17'),
(18, 'STANDARD_QUESTION', 17, 'CREATED_BY_USER_ID', NULL, '17'),

-- 问题18的详情
(19, 'STANDARD_QUESTION', 18, 'QUESTION_TEXT', NULL, '"骨质疏松症最常发生骨折的部位是哪里？\\nA. 颅骨\\nB. 肋骨\\nC. 脊椎体\\nD. 指骨"'),
(19, 'STANDARD_QUESTION', 18, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(19, 'STANDARD_QUESTION', 18, 'DIFFICULTY', NULL, '"MEDIUM"'),
(19, 'STANDARD_QUESTION', 18, 'ORIGINAL_RAW_QUESTION_ID', NULL, '18'),
(19, 'STANDARD_QUESTION', 18, 'CREATED_BY_USER_ID', NULL, '18'),

-- 问题19的详情
(20, 'STANDARD_QUESTION', 19, 'QUESTION_TEXT', NULL, '"如何通过改善生活方式来预防2型糖尿病？"'),
(20, 'STANDARD_QUESTION', 19, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(20, 'STANDARD_QUESTION', 19, 'DIFFICULTY', NULL, '"MEDIUM"'),
(20, 'STANDARD_QUESTION', 19, 'ORIGINAL_RAW_QUESTION_ID', NULL, '19'),
(20, 'STANDARD_QUESTION', 19, 'CREATED_BY_USER_ID', NULL, '19'),

-- 问题20的详情
(21, 'STANDARD_QUESTION', 20, 'QUESTION_TEXT', NULL, '"癌症三阶梯止痛法是由哪个组织提出的？"'),
(21, 'STANDARD_QUESTION', 20, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(21, 'STANDARD_QUESTION', 20, 'DIFFICULTY', NULL, '"MEDIUM"'),
(21, 'STANDARD_QUESTION', 20, 'ORIGINAL_RAW_QUESTION_ID', NULL, '20'),
(21, 'STANDARD_QUESTION', 20, 'CREATED_BY_USER_ID', NULL, '20'),

-- 问题21的详情
(22, 'STANDARD_QUESTION', 21, 'QUESTION_TEXT', NULL, '"发热时，物理降温的常用方法不包括？\\nA. 温水擦浴\\nB. 冰袋敷额头\\nC. 饮用大量冰水\\nD. 减少衣物"'),
(22, 'STANDARD_QUESTION', 21, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(22, 'STANDARD_QUESTION', 21, 'DIFFICULTY', NULL, '"EASY"'),
(22, 'STANDARD_QUESTION', 21, 'ORIGINAL_RAW_QUESTION_ID', NULL, '21'),
(22, 'STANDARD_QUESTION', 21, 'CREATED_BY_USER_ID', NULL, '21'),

-- 问题22的详情
(23, 'STANDARD_QUESTION', 22, 'QUESTION_TEXT', NULL, '"儿童常见的过敏原有哪些？\\nA. 花粉\\nB. 尘螨\\nC. 牛奶\\nD. 宠物皮屑"'),
(23, 'STANDARD_QUESTION', 22, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(23, 'STANDARD_QUESTION', 22, 'DIFFICULTY', NULL, '"MEDIUM"'),
(23, 'STANDARD_QUESTION', 22, 'ORIGINAL_RAW_QUESTION_ID', NULL, '22'),
(23, 'STANDARD_QUESTION', 22, 'CREATED_BY_USER_ID', NULL, '22'),

-- 问题23的详情
(24, 'STANDARD_QUESTION', 23, 'QUESTION_TEXT', NULL, '"什么是抑郁症的核心症状？如何进行初步识别？"'),
(24, 'STANDARD_QUESTION', 23, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(24, 'STANDARD_QUESTION', 23, 'DIFFICULTY', NULL, '"MEDIUM"'),
(24, 'STANDARD_QUESTION', 23, 'ORIGINAL_RAW_QUESTION_ID', NULL, '23'),
(24, 'STANDARD_QUESTION', 23, 'CREATED_BY_USER_ID', NULL, '23'),

-- 问题24的详情
(25, 'STANDARD_QUESTION', 24, 'QUESTION_TEXT', NULL, '"人体内负责凝血功能的重要血细胞是什么？"'),
(25, 'STANDARD_QUESTION', 24, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(25, 'STANDARD_QUESTION', 24, 'DIFFICULTY', NULL, '"EASY"'),
(25, 'STANDARD_QUESTION', 24, 'ORIGINAL_RAW_QUESTION_ID', NULL, '24'),
(25, 'STANDARD_QUESTION', 24, 'CREATED_BY_USER_ID', NULL, '24'),

-- 问题25的详情
(26, 'STANDARD_QUESTION', 25, 'QUESTION_TEXT', NULL, '"用于早期筛查宫颈癌的常用检查方法是？\\nA. B超检查\\nB. HPV检测和TCT检查\\nC. 阴道镜检查\\nD. 肿瘤标志物检测"'),
(26, 'STANDARD_QUESTION', 25, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(26, 'STANDARD_QUESTION', 25, 'DIFFICULTY', NULL, '"MEDIUM"'),
(26, 'STANDARD_QUESTION', 25, 'ORIGINAL_RAW_QUESTION_ID', NULL, '25'),
(26, 'STANDARD_QUESTION', 25, 'CREATED_BY_USER_ID', NULL, '25'),

-- 问题26的详情
(27, 'STANDARD_QUESTION', 26, 'QUESTION_TEXT', NULL, '"简述阿尔茨海默病的主要临床表现和分期。"'),
(27, 'STANDARD_QUESTION', 26, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(27, 'STANDARD_QUESTION', 26, 'DIFFICULTY', NULL, '"HARD"'),
(27, 'STANDARD_QUESTION', 26, 'ORIGINAL_RAW_QUESTION_ID', NULL, '26'),
(27, 'STANDARD_QUESTION', 26, 'CREATED_BY_USER_ID', NULL, '26'),

-- 问题27的详情
(28, 'STANDARD_QUESTION', 27, 'QUESTION_TEXT', NULL, '"幽门螺杆菌感染与下列哪些疾病密切相关？\\nA. 慢性胃炎\\nB. 消化性溃疡\\nC. 胃癌\\nD. 胃食管反流病"'),
(28, 'STANDARD_QUESTION', 27, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(28, 'STANDARD_QUESTION', 27, 'DIFFICULTY', NULL, '"MEDIUM"'),
(28, 'STANDARD_QUESTION', 27, 'ORIGINAL_RAW_QUESTION_ID', NULL, '27'),
(28, 'STANDARD_QUESTION', 27, 'CREATED_BY_USER_ID', NULL, '27'),

-- 问题28的详情
(29, 'STANDARD_QUESTION', 28, 'QUESTION_TEXT', NULL, '"白内障最主要的治疗方法是什么？"'),
(29, 'STANDARD_QUESTION', 28, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(29, 'STANDARD_QUESTION', 28, 'DIFFICULTY', NULL, '"EASY"'),
(29, 'STANDARD_QUESTION', 28, 'ORIGINAL_RAW_QUESTION_ID', NULL, '28'),
(29, 'STANDARD_QUESTION', 28, 'CREATED_BY_USER_ID', NULL, '28'),

-- 问题29的详情
(30, 'STANDARD_QUESTION', 29, 'QUESTION_TEXT', NULL, '"对于急性缺血性脑卒中，发病后多少小时内进行静脉溶栓治疗效果最佳（时间窗）？\\nA. 1小时内\\nB. 3小时内\\nC. 4.5小时内\\nD. 6小时内"'),
(30, 'STANDARD_QUESTION', 29, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(30, 'STANDARD_QUESTION', 29, 'DIFFICULTY', NULL, '"HARD"'),
(30, 'STANDARD_QUESTION', 29, 'ORIGINAL_RAW_QUESTION_ID', NULL, '29'),
(30, 'STANDARD_QUESTION', 29, 'CREATED_BY_USER_ID', NULL, '29'),

-- 问题30的详情
(31, 'STANDARD_QUESTION', 30, 'QUESTION_TEXT', NULL, '"什么是疫苗？它在预防传染病中的作用机制是什么？"'),
(31, 'STANDARD_QUESTION', 30, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(31, 'STANDARD_QUESTION', 30, 'DIFFICULTY', NULL, '"MEDIUM"'),
(31, 'STANDARD_QUESTION', 30, 'ORIGINAL_RAW_QUESTION_ID', NULL, '30'),
(31, 'STANDARD_QUESTION', 30, 'CREATED_BY_USER_ID', NULL, '30'),

-- 问题31的详情
(32, 'STANDARD_QUESTION', 31, 'QUESTION_TEXT', NULL, '"狂犬病病毒主要通过哪种方式传播给人类？"'),
(32, 'STANDARD_QUESTION', 31, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(32, 'STANDARD_QUESTION', 31, 'DIFFICULTY', NULL, '"EASY"'),
(32, 'STANDARD_QUESTION', 31, 'ORIGINAL_RAW_QUESTION_ID', NULL, '31'),
(32, 'STANDARD_QUESTION', 31, 'CREATED_BY_USER_ID', NULL, '31'),

-- 问题32的详情
(33, 'STANDARD_QUESTION', 32, 'QUESTION_TEXT', NULL, '"痛风患者饮食中应严格限制哪类食物的摄入？\\nA. 高嘌呤食物\\nB. 高蛋白食物\\nC. 高碳水化合物食物\\nD. 高维生素食物"'),
(33, 'STANDARD_QUESTION', 32, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(33, 'STANDARD_QUESTION', 32, 'DIFFICULTY', NULL, '"MEDIUM"'),
(33, 'STANDARD_QUESTION', 32, 'ORIGINAL_RAW_QUESTION_ID', NULL, '32'),
(33, 'STANDARD_QUESTION', 32, 'CREATED_BY_USER_ID', NULL, '32'),

-- 问题33的详情
(34, 'STANDARD_QUESTION', 33, 'QUESTION_TEXT', NULL, '"维持人体水电解质平衡至关重要的离子有哪些？\\nA. 钠离子 (Na+)\\nB. 钾离子 (K+)\\nC. 氯离子 (Cl-)\\nD. 钙离子 (Ca2+)"'),
(34, 'STANDARD_QUESTION', 33, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(34, 'STANDARD_QUESTION', 33, 'DIFFICULTY', NULL, '"MEDIUM"'),
(34, 'STANDARD_QUESTION', 33, 'ORIGINAL_RAW_QUESTION_ID', NULL, '33'),
(34, 'STANDARD_QUESTION', 33, 'CREATED_BY_USER_ID', NULL, '33'),

-- 问题34的详情
(35, 'STANDARD_QUESTION', 34, 'QUESTION_TEXT', NULL, '"什么是食物中毒？常见的预防措施有哪些？"'),
(35, 'STANDARD_QUESTION', 34, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(35, 'STANDARD_QUESTION', 34, 'DIFFICULTY', NULL, '"MEDIUM"'),
(35, 'STANDARD_QUESTION', 34, 'ORIGINAL_RAW_QUESTION_ID', NULL, '34'),
(35, 'STANDARD_QUESTION', 34, 'CREATED_BY_USER_ID', NULL, '34'),

-- 问题35的详情
(36, 'STANDARD_QUESTION', 35, 'QUESTION_TEXT', NULL, '"缺铁性贫血最常见的症状是什么？"'),
(36, 'STANDARD_QUESTION', 35, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(36, 'STANDARD_QUESTION', 35, 'DIFFICULTY', NULL, '"EASY"'),
(36, 'STANDARD_QUESTION', 35, 'ORIGINAL_RAW_QUESTION_ID', NULL, '35'),
(36, 'STANDARD_QUESTION', 35, 'CREATED_BY_USER_ID', NULL, '35'),

-- 问题36的详情
(37, 'STANDARD_QUESTION', 36, 'QUESTION_TEXT', NULL, '"OTC药品是指什么类型的药品？\\nA. 处方药\\nB. 非处方药\\nC. 精神药品\\nD. 特殊管理药品"'),
(37, 'STANDARD_QUESTION', 36, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(37, 'STANDARD_QUESTION', 36, 'DIFFICULTY', NULL, '"EASY"'),
(37, 'STANDARD_QUESTION', 36, 'ORIGINAL_RAW_QUESTION_ID', NULL, '36'),
(37, 'STANDARD_QUESTION', 36, 'CREATED_BY_USER_ID', NULL, '36'),

-- 问题37的详情
(38, 'STANDARD_QUESTION', 37, 'QUESTION_TEXT', NULL, '"以下哪些是健康的生活习惯？\\nA. 规律作息，保证充足睡眠\\nB. 均衡饮食，多吃蔬菜水果\\nC. 坚持适度体育锻炼\\nD. 长期熬夜，吸烟饮酒"'),
(38, 'STANDARD_QUESTION', 37, 'QUESTION_TYPE', NULL, '"MULTIPLE_CHOICE"'),
(38, 'STANDARD_QUESTION', 37, 'DIFFICULTY', NULL, '"MEDIUM"'),
(38, 'STANDARD_QUESTION', 37, 'ORIGINAL_RAW_QUESTION_ID', NULL, '37'),
(38, 'STANDARD_QUESTION', 37, 'CREATED_BY_USER_ID', NULL, '37'),

-- 问题38的详情
(39, 'STANDARD_QUESTION', 38, 'QUESTION_TEXT', NULL, '"请解释什么是免疫力？如何科学地提高个人免疫力？"'),
(39, 'STANDARD_QUESTION', 38, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(39, 'STANDARD_QUESTION', 38, 'DIFFICULTY', NULL, '"HARD"'),
(39, 'STANDARD_QUESTION', 38, 'ORIGINAL_RAW_QUESTION_ID', NULL, '38'),
(39, 'STANDARD_QUESTION', 38, 'CREATED_BY_USER_ID', NULL, '38'),

-- 问题39的详情
(40, 'STANDARD_QUESTION', 39, 'QUESTION_TEXT', NULL, '"新生儿黄疸分为生理性和病理性，哪种需要及时就医干预？"'),
(40, 'STANDARD_QUESTION', 39, 'QUESTION_TYPE', NULL, '"SIMPLE_FACT"'),
(40, 'STANDARD_QUESTION', 39, 'DIFFICULTY', NULL, '"MEDIUM"'),
(40, 'STANDARD_QUESTION', 39, 'ORIGINAL_RAW_QUESTION_ID', NULL, '39'),
(40, 'STANDARD_QUESTION', 39, 'CREATED_BY_USER_ID', NULL, '39'),

-- 问题40的详情
(41, 'STANDARD_QUESTION', 40, 'QUESTION_TEXT', NULL, '"肺结核最主要的传播途径是？\\nA. 消化道传播\\nB. 血液传播\\nC. 呼吸道传播（飞沫传播）\\nD. 接触传播"'),
(41, 'STANDARD_QUESTION', 40, 'QUESTION_TYPE', NULL, '"SINGLE_CHOICE"'),
(41, 'STANDARD_QUESTION', 40, 'DIFFICULTY', NULL, '"MEDIUM"'),
(41, 'STANDARD_QUESTION', 40, 'ORIGINAL_RAW_QUESTION_ID', NULL, '40'),
(41, 'STANDARD_QUESTION', 40, 'CREATED_BY_USER_ID', NULL, '40'),

-- 问题41的详情
(42, 'STANDARD_QUESTION', 41, 'QUESTION_TEXT', NULL, '"中暑了应该怎么办？如何预防中暑？"'),
(42, 'STANDARD_QUESTION', 41, 'QUESTION_TYPE', NULL, '"SUBJECTIVE"'),
(42, 'STANDARD_QUESTION', 41, 'DIFFICULTY', NULL, '"MEDIUM"'),
(42, 'STANDARD_QUESTION', 41, 'ORIGINAL_RAW_QUESTION_ID', NULL, '41'),
(42, 'STANDARD_QUESTION', 41, 'CREATED_BY_USER_ID', NULL, '1');



-- 现在修改原来的标准问题插入语句，加上CREATED_CHANGE_LOG_ID字段
INSERT INTO `STANDARD_QUESTIONS` (`ID`, `ORIGINAL_RAW_QUESTION_ID`, `QUESTION_TEXT`, `QUESTION_TYPE`, `DIFFICULTY`, `CREATED_BY_USER_ID`, `CREATED_CHANGE_LOG_ID`) VALUES
(1, 1, '高血压的主要症状有哪些？', 'SUBJECTIVE', 'MEDIUM', 1, 2),
(2, 2, '糖尿病患者的日常饮食应该注意什么？', 'SUBJECTIVE', 'MEDIUM', 1, 3),
(3, 3, '以下哪些是冠心病的典型症状？\nA. 胸痛\nB. 气短\nC. 出汗\nD. 恶心', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 4),
(4, 4, '正常人的空腹血糖值范围是多少？\nA. 3.9-6.1 mmol/L\nB. 7.0-8.0 mmol/L\nC. 2.0-3.5 mmol/L\nD. 8.0-10.0 mmol/L', 'SINGLE_CHOICE', 'EASY', 1, 5),
(5, 5, '成人正常心率范围是多少？', 'SIMPLE_FACT', 'EASY', 1, 6),
(6, 6, '人体正常体温是多少摄氏度？', 'SIMPLE_FACT', 'EASY', 1, 7),
(7, 7, '正常成人血压的理想范围是多少？', 'SIMPLE_FACT', 'EASY', 1, 8),
(8, 8, 'BMI指数超过多少被定义为肥胖？', 'SIMPLE_FACT', 'EASY', 1, 9),
(9, 9, '人体中最大的器官是什么？', 'SIMPLE_FACT', 'EASY', 1, 10),
(10, 10, '以下哪项不是常见的感冒症状？\nA. 发烧\nB. 咳嗽\nC. 腹泻\nD. 鼻塞', 'SINGLE_CHOICE', 'EASY', 1, 11),
(11, 11, '预防流感的有效措施包括哪些？\nA. 每年接种流感疫苗\nB. 保持良好的个人卫生习惯\nC. 避免前往人群密集场所\nD. 多喝水', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 12),
(12, 12, '什么是抗生素耐药性？它是如何产生的？', 'SUBJECTIVE', 'MEDIUM', 1, 13),
(13, 13, '健康成年人每日推荐的饮水量大约是多少？', 'SIMPLE_FACT', 'EASY', 1, 14),
(14, 14, '下列哪种维生素缺乏可能导致夜盲症？\nA. 维生素A\nB. 维生素C\nC. 维生素D\nD. 维生素B12', 'SINGLE_CHOICE', 'MEDIUM', 1, 15),
(15, 15, '请简述心肌梗死的急救措施和后续治疗原则。', 'SUBJECTIVE', 'HARD', 1, 16),
(16, 16, '以下哪些属于慢性阻塞性肺疾病（COPD）的高危因素？\nA. 长期吸烟\nB. 空气污染\nC. 遗传因素\nD. 过敏性鼻炎', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 17),
(17, 17, '艾滋病的主要传播途径是什么？', 'SIMPLE_FACT', 'EASY', 1, 18),
(18, 18, '骨质疏松症最常发生骨折的部位是哪里？\nA. 颅骨\nB. 肋骨\nC. 脊椎体\nD. 指骨', 'SINGLE_CHOICE', 'MEDIUM', 1, 19),
(19, 19, '如何通过改善生活方式来预防2型糖尿病？', 'SUBJECTIVE', 'MEDIUM', 1, 20),
(20, 20, '"癌症三阶梯止痛法"是由哪个组织提出的？', 'SIMPLE_FACT', 'MEDIUM', 1, 21),
(21, 21, '发热时，物理降温的常用方法不包括？\nA. 温水擦浴\nB. 冰袋敷额头\nC. 饮用大量冰水\nD. 减少衣物', 'SINGLE_CHOICE', 'EASY', 1, 22),
(22, 22, '儿童常见的过敏原有哪些？\nA. 花粉\nB. 尘螨\nC. 牛奶\nD. 宠物皮屑', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 23),
(23, 23, '什么是抑郁症的核心症状？如何进行初步识别？', 'SUBJECTIVE', 'MEDIUM', 1, 24),
(24, 24, '人体内负责凝血功能的重要血细胞是什么？', 'SIMPLE_FACT', 'EASY', 1, 25),
(25, 25, '用于早期筛查宫颈癌的常用检查方法是？\nA. B超检查\nB. HPV检测和TCT检查\nC. 阴道镜检查\nD. 肿瘤标志物检测', 'SINGLE_CHOICE', 'MEDIUM', 1, 26),
(26, 26, '简述阿尔茨海默病的主要临床表现和分期。', 'SUBJECTIVE', 'HARD', 1, 27),
(27, 27, '幽门螺杆菌感染与下列哪些疾病密切相关？\nA. 慢性胃炎\nB. 消化性溃疡\nC. 胃癌\nD. 胃食管反流病', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 28),
(28, 28, '白内障最主要的治疗方法是什么？', 'SIMPLE_FACT', 'EASY', 1, 29),
(29, 29, '对于急性缺血性脑卒中，发病后多少小时内进行静脉溶栓治疗效果最佳（时间窗）？\nA. 1小时内\nB. 3小时内\nC. 4.5小时内\nD. 6小时内', 'SINGLE_CHOICE', 'HARD', 1, 30),
(30, 30, '什么是疫苗？它在预防传染病中的作用机制是什么？', 'SUBJECTIVE', 'MEDIUM', 1, 31),
(31, 31, '狂犬病病毒主要通过哪种方式传播给人类？', 'SIMPLE_FACT', 'EASY', 1, 32),
(32, 32, '痛风患者饮食中应严格限制哪类食物的摄入？\nA. 高嘌呤食物\nB. 高蛋白食物\nC. 高碳水化合物食物\nD. 高维生素食物', 'SINGLE_CHOICE', 'MEDIUM', 1, 33),
(33, 33, '维持人体水电解质平衡至关重要的离子有哪些？\nA. 钠离子 (Na+)\nB. 钾离子 (K+)\nC. 氯离子 (Cl-)\nD. 钙离子 (Ca2+)', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 34),
(34, 34, '什么是食物中毒？常见的预防措施有哪些？', 'SUBJECTIVE', 'MEDIUM', 1, 35),
(35, 35, '缺铁性贫血最常见的症状是什么？', 'SIMPLE_FACT', 'EASY', 1, 36),
(36, 36, '"OTC"药品是指什么类型的药品？\nA. 处方药\nB. 非处方药\nC. 精神药品\nD. 特殊管理药品', 'SINGLE_CHOICE', 'EASY', 1, 37),
(37, 37, '以下哪些是健康的生活习惯？\nA. 规律作息，保证充足睡眠\nB. 均衡饮食，多吃蔬菜水果\nC. 坚持适度体育锻炼\nD. 长期熬夜，吸烟饮酒', 'MULTIPLE_CHOICE', 'MEDIUM', 1, 38),
(38, 38, '请解释什么是"免疫力"？如何科学地提高个人免疫力？', 'SUBJECTIVE', 'HARD', 1, 39),
(39, 39, '新生儿黄疸分为生理性和病理性，哪种需要及时就医干预？', 'SIMPLE_FACT', 'MEDIUM', 1, 40),
(40, 40, '肺结核最主要的传播途径是？\nA. 消化道传播\nB. 血液传播\nC. 呼吸道传播（飞沫传播）\nD. 接触传播', 'SINGLE_CHOICE', 'MEDIUM', 1, 41),
(41, 41, '中暑了应该怎么办？如何预防中暑？', 'SUBJECTIVE', 'MEDIUM', 1, 42);





-- 插入标准客观题答案
INSERT INTO `STANDARD_OBJECTIVE_ANSWERS` (`STANDARD_QUESTION_ID`, `OPTIONS`, `CORRECT_IDS`, `DETERMINED_BY_USER_ID`) VALUES
(3, '[{"id":"A","text":"胸痛"},{"id":"B","text":"气短"},{"id":"C","text":"出汗"},{"id":"D","text":"恶心"}]', '["A","B","C"]', 2),
(4, '[{"id":"A","text":"3.9-6.1 mmol/L"},{"id":"B","text":"7.0-8.0 mmol/L"},{"id":"C","text":"2.0-3.5 mmol/L"},{"id":"D","text":"8.0-10.0 mmol/L"}]', '["A"]', 2),
(10, '[{"id":"A","text":"发烧"},{"id":"B","text":"咳嗽"},{"id":"C","text":"腹泻"},{"id":"D","text":"鼻塞"}]', '["C"]', 2),
(11, '[{"id":"A","text":"每年接种流感疫苗"},{"id":"B","text":"保持良好的个人卫生习惯"},{"id":"C","text":"避免前往人群密集场所"},{"id":"D","text":"多喝水"}]', '["A","B","C"]', 2),
(14, '[{"id":"A","text":"维生素A"},{"id":"B","text":"维生素C"},{"id":"C","text":"维生素D"},{"id":"D","text":"维生素B12"}]', '["A"]', 2),
(16, '[{"id":"A","text":"长期吸烟"},{"id":"B","text":"空气污染"},{"id":"C","text":"遗传因素"},{"id":"D","text":"过敏性鼻炎"}]', '["A","B","C"]', 2),
(18, '[{"id":"A","text":"颅骨"},{"id":"B","text":"肋骨"},{"id":"C","text":"脊椎体"},{"id":"D","text":"指骨"}]', '["C"]', 2),
(21, '[{"id":"A","text":"温水擦浴"},{"id":"B","text":"冰袋敷额头"},{"id":"C","text":"饮用大量冰水"},{"id":"D","text":"减少衣物"}]', '["C"]', 2),
(22, '[{"id":"A","text":"花粉"},{"id":"B","text":"尘螨"},{"id":"C","text":"牛奶"},{"id":"D","text":"宠物皮屑"}]', '["A","B","C","D"]', 2),
(25, '[{"id":"A","text":"B超检查"},{"id":"B","text":"HPV检测和TCT检查"},{"id":"C","text":"阴道镜检查"},{"id":"D","text":"肿瘤标志物检测"}]', '["B"]', 2),
(27, '[{"id":"A","text":"慢性胃炎"},{"id":"B","text":"消化性溃疡"},{"id":"C","text":"胃癌"},{"id":"D","text":"胃食管反流病"}]', '["A","B","C"]', 2),
(29, '[{"id":"A","text":"1小时内"},{"id":"B","text":"3小时内"},{"id":"C","text":"4.5小时内"},{"id":"D","text":"6小时内"}]', '["C"]', 2),
(32, '[{"id":"A","text":"高嘌呤食物"},{"id":"B","text":"高蛋白食物"},{"id":"C","text":"高碳水化合物食物"},{"id":"D","text":"高维生素食物"}]', '["A"]', 2),
(33, '[{"id":"A","text":"钠离子 (Na+)"},{"id":"B","text":"钾离子 (K+)"},{"id":"C","text":"氯离子 (Cl-)"},{"id":"D","text":"钙离子 (Ca2+)"}]', '["A","B","C","D"]', 2),
(36, '[{"id":"A","text":"处方药"},{"id":"B","text":"非处方药"},{"id":"C","text":"精神药品"},{"id":"D","text":"特殊管理药品"}]', '["B"]', 2),
(37, '[{"id":"A","text":"规律作息，保证充足睡眠"},{"id":"B","text":"均衡饮食，多吃蔬菜水果"},{"id":"C","text":"坚持适度体育锻炼"},{"id":"D","text":"长期熬夜，吸烟饮酒"}]', '["A","B","C"]', 2),
(40, '[{"id":"A","text":"消化道传播"},{"id":"B","text":"血液传播"},{"id":"C","text":"呼吸道传播（飞沫传播）"},{"id":"D","text":"接触传播"}]', '["C"]', 2);

-- 插入标准主观题答案
INSERT INTO `STANDARD_SUBJECTIVE_ANSWERS` (`STANDARD_QUESTION_ID`, `ANSWER_TEXT`, `SCORING_GUIDANCE`, `DETERMINED_BY_USER_ID`) VALUES
(1, '高血压的主要症状包括：\n1. 头痛，特别是后脑部\n2. 头晕和眩晕\n3. 耳鸣\n4. 心悸\n5. 疲劳\n6. 视物模糊\n7. 失眠\n\n需要注意的是，早期高血压可能没有明显症状，因此定期测量血压很重要。', '评分要点：\n1. 症状的完整性（3分）\n2. 症状的准确性（4分）\n3. 补充说明的合理性（3分）', 2),
(2, '糖尿病患者的日常饮食注意事项：\n1. 控制总热量摄入\n2. 定时定量进餐\n3. 主食以复杂碳水化合物为主\n4. 增加膳食纤维的摄入\n5. 限制单糖和双糖的摄入\n6. 适量摄入优质蛋白\n7. 限制饱和脂肪酸的摄入\n8. 补充适量维生素和矿物质', '评分要点：\n1. 饮食原则的完整性（4分）\n2. 具体建议的实用性（3分）\n3. 说明的合理性（3分）', 2),
(12, '抗生素耐药性是指细菌、病毒、真菌和寄生虫等微生物对抗微生物药物产生抵抗力，导致药物治疗效果降低或失效的现象。\n产生的主要原因包括：\n1. 抗生素的滥用：不遵医嘱随意使用或过量使用抗生素。\n2. 抗生素的不当使用：如未完成整个疗程，或用于病毒性感染等不适宜情况。\n3. 农业和畜牧业中的抗生素使用：为促进动物生长或预防疾病而广泛使用抗生素，导致耐药菌通过食物链传播给人类。\n4. 医院内感染控制不足：导致耐药菌在医疗机构内传播。\n5. 新型抗生素研发缓慢：跟不上耐药性发展的速度。', '评分要点：\n1. 对抗生素耐药性的准确定义（4分）\n2. 产生原因的阐述（至少3个主要原因，每个原因2分，共6分）\n3. 逻辑清晰，表达准确（2分）', 2),
(15, '心肌梗死的急救措施（院前急救）：\n1. 立即呼叫急救中心（如120）。\n2. 让患者保持安静休息，最好是半卧位，避免用力活动。\n3. 如有条件，可给予硝酸甘油舌下含服（需排除禁忌症，如低血压）。\n4. 如有阿司匹林，且患者无禁忌症，可嚼服300mg。\n5. 如患者出现心脏骤停，立即进行心肺复苏（CPR）。\n\n后续治疗原则（院内治疗）：\n1. 尽快开通堵塞的冠状动脉：包括急诊经皮冠状动脉介入治疗（PCI）或溶栓治疗。\n2. 缓解症状：如止痛、吸氧等。\n3. 防止并发症：如心律失常、心力衰竭、心脏破裂等。\n4. 二级预防：包括抗血小板治疗、他汀类药物调脂、β受体阻滞剂、ACEI/ARB类药物等，并进行生活方式干预（戒烟、合理饮食、适度运动、控制体重、管理情绪）。\n5. 康复治疗：进行心脏康复计划。', '评分要点：\n1. 急救措施的及时性和正确性（5分，至少包含呼救、休息、硝酸甘油/阿司匹林（如适用）、CPR（如适用））\n2. 后续治疗原则的全面性（5分，至少包含开通血管、缓解症状、防治并发症、二级预防）\n3. 专业术语准确，逻辑清晰（2分）', 2),
(19, '预防2型糖尿病的生活方式干预主要包括：\n1. 健康饮食：保持膳食平衡，多吃蔬菜水果和全谷物，限制高糖、高脂肪、高盐食物的摄入，控制总热量。\n2. 规律运动：每周至少进行150分钟中等强度的有氧运动，如快走、慢跑、游泳等，并结合每周2-3次的抗阻运动。\n3. 控制体重：保持健康的体重指数（BMI），超重或肥胖者应积极减重。\n4. 戒烟限酒：吸烟和过量饮酒都是2型糖尿病的危险因素。\n5. 保证充足睡眠：长期睡眠不足会影响胰岛素敏感性。\n6. 管理压力：长期精神压力过大也可能增加患病风险。\n7. 定期体检：对于高危人群，应定期监测血糖，及早发现问题并干预。', '评分要点：\n1. 至少列出4项主要的生活方式干预措施（每项1.5分，共6分）\n2. 每项措施有简要合理的解释（每项0.5分，共2分）\n3. 建议具有科学性和可操作性（2分）', 2),
(23, '抑郁症的核心症状主要包括：\n1. 心境低落：持续的情绪悲伤、空虚感，对日常活动失去兴趣或乐趣（快感缺乏）。\n2. 精力减退：感到持续疲乏，即使没有进行体力劳动也觉得非常累。\n\n初步识别可以通过以下方面观察：\n- 情绪方面：是否长时间表现出悲伤、哭泣、易怒、焦虑，或者对以前喜欢的事物不再感兴趣。\n- 思维方面：是否出现思维迟缓、注意力不集中、记忆力下降、自我评价过低、感到无助无望，甚至出现自杀念头或行为。\n- 行为方面：是否活动减少、不愿与人交往、工作学习效率下降、睡眠障碍（失眠或嗜睡）、食欲改变（食欲减退或增加）。\n- 躯体方面：是否出现不明原因的头痛、背痛、消化不良等躯体不适。\n如果上述症状持续两周以上，并且对日常生活造成了明显影响，应警惕抑郁症的可能，建议及时就医寻求专业帮助。', '评分要点：\n1. 准确指出至少两个核心症状并解释（4分）\n2. 初步识别方法至少包含情绪、思维、行为三个方面，并有具体表现描述（4分）\n3. 提及持续时间和影响程度，并建议就医（2分）', 2),
(26, '阿尔茨海默病（AD）是一种起病隐匿的进行性发展的神经系统退行性疾病。临床上以记忆障碍、失语、失用、失认、视空间技能损害、执行功能障碍以及人格和行为改变等全面性痴呆表现为特征。\n\n主要临床表现：\n1. 记忆障碍：尤其是近期记忆减退，是AD早期最核心的症状。\n2. 语言功能障碍：早期表现为找词困难，逐渐发展为命名困难、失语等。\n3. 视空间功能障碍：不认路、不能准确判断物体位置和距离等。\n4. 执行功能障碍：计划、组织、抽象思维能力下降。\n5. 精神行为异常：如淡漠、焦虑、抑郁、幻觉、妄想、攻击行为、睡眠障碍等。\n\n临床分期（简要）：\n1. 轻度（早期）：主要表现为近记忆力减退，对工作和日常生活造成轻微影响。患者可能意识到自己的问题。\n2. 中度（中期）：记忆力减退加重，远期记忆也受损。出现明显的语言、视空间、执行功能障碍。生活自理能力下降，需要部分帮助。精神行为异常开始突出。\n3. 重度（晚期）：认知功能严重受损，完全丧失生活自理能力，需要全面护理。出现运动功能障碍，如行走困难、大小便失禁。常因并发症（如感染）死亡。', '评分要点：\n1. 主要临床表现描述准确全面（至少4项，4分）\n2. 临床分期清晰，各期核心特征描述准确（轻、中、重三期，每期2分，共6分）\n3. 定义准确，逻辑连贯（2分）', 2),
(30, '疫苗是将病原微生物（如细菌、病毒等）及其代谢产物，经过人工减毒、灭活或利用基因工程等方法制成的，用于预防传染性疾病的自动免疫制剂。\n\n作用机制：\n疫苗接种到人体后，会刺激机体的免疫系统产生针对特定病原体的特异性免疫应答。这个过程主要包括：\n1. 识别与提呈：疫苗中的抗原被免疫细胞（如巨噬细胞、树突状细胞）识别、吞噬和处理，然后将抗原信息提呈给T淋巴细胞。\n2. 激活免疫细胞：T淋巴细胞被激活后，会分化为辅助T细胞（Th细胞）和细胞毒性T细胞（CTL细胞）。Th细胞可以帮助B淋巴细胞活化。\n3. 产生抗体：活化的B淋巴细胞会分化为浆细胞，浆细胞产生大量的特异性抗体。这些抗体可以中和病原体或标记病原体以便其他免疫细胞清除。\n4. 形成免疫记忆：部分活化的T细胞和B细胞会分化为记忆细胞。当机体再次接触到相同的病原体时，记忆细胞会被迅速激活，产生更快、更强的免疫应答，从而有效清除病原体，防止发病或减轻疾病的严重程度。', '评分要点：\n1. 对疫苗的定义准确（3分）\n2. 作用机制阐述清晰，包含抗原识别、免疫细胞激活、抗体产生、免疫记忆形成等关键步骤（5分）\n3. 语言表达科学、流畅（2分）', 2),
(34, '食物中毒是指摄入了含有有毒有害物质（包括生物性、化学性）的食物或者把有毒有害物质当作食物摄入后出现的非传染性（不包括细菌性痢疾和伤寒、副伤寒）的急性、亚急性疾病。\n\n常见的预防措施：\n1. 选择安全食材：购买新鲜、卫生的食物，不购买来源不明或已变质的食物。\n2. 彻底加热食物：特别是肉、禽、蛋和海产品，确保中心温度达到70℃以上。\n3. 生熟分开：处理生食和熟食的用具（刀、砧板等）及储存容器要分开，避免交叉污染。\n4. 尽快食用熟食：熟食在室温下存放不宜超过2小时，未吃完的食物应及时冷藏。\n5. 安全储存食物：冰箱内食物不宜存放过久，冷藏温度应在5℃以下，冷冻温度在-18℃以下。解冻食物不宜在室温下进行。\n6. 保持清洁：注意个人卫生，饭前便后要洗手；保持厨房环境和餐具清洁。\n7. 安全用水：使用安全、清洁的水源加工食物和饮用。', '评分要点：\n1. 食物中毒定义准确（3分）\n2. 预防措施至少列出4条主要且合理的措施（每条1.5分，共6分）\n3. 表达清晰，具有可操作性（1分）', 2),
(38, '免疫力是机体抵抗外来侵袭（如细菌、病毒等病原体）以及处理自身衰老、损伤、死亡、变性的细胞的能力，是人体的一种生理保护反应。\n它包括非特异性免疫（先天免疫）和特异性免疫（获得性免疫）两个方面。\n\n科学提高个人免疫力的方法：\n1. 均衡营养：保证蛋白质、维生素（如A、C、D、E、B族）、矿物质（如锌、铁、硒）等多种营养素的充足摄入。多吃新鲜蔬果、全谷物、优质蛋白（鱼、禽、蛋、奶、豆类）。\n2. 规律作息：保证充足的睡眠时间（成人一般7-8小时），避免熬夜。良好的睡眠有助于免疫细胞的修复和再生。\n3. 适度运动：规律的体育锻炼能增强免疫细胞的活性和数量，提高机体抗病能力。建议每周进行至少150分钟中等强度有氧运动。\n4. 保持良好心态：长期的精神压力和负面情绪会抑制免疫系统功能。学会调适情绪，保持乐观积极的心态。\n5. 戒烟限酒：吸烟和过量饮酒都会直接损害免疫系统。\n6. 及时接种疫苗：疫苗是增强对特定传染病免疫力的有效手段。\n7. 注意个人卫生：勤洗手，保持环境清洁，减少病原体接触机会。\n\n需要强调的是，不存在能"神奇"快速提高免疫力的保健品或单一食物，免疫力的维持和提高是一个系统性的、长期的健康生活方式的结果。', '评分要点：\n1. 对免疫力的解释准确、全面（3分）\n2. 提高免疫力的方法科学、合理，至少列出4项主要措施并有简要说明（每项1.5分，共6分）\n3. 强调免疫力提升的系统性和长期性，避免误导（1分）', 2),
(41, '中暑了的紧急处理方法：\n1. 脱离高温环境：迅速将患者移至阴凉通风处，解开衣物，松开衣领。\n2. 降温：用湿毛巾擦拭全身，或在额头、腋下、腹股沟等大血管处放置冰袋或冷毛巾。可用风扇或空调帮助降温。避免直接用冰块长时间敷在皮肤上。\n3. 补充水分和电解质：如果患者清醒，可少量多次饮用淡盐水、运动饮料或凉开水。不要饮用含咖啡因或酒精的饮料。\n4. 观察生命体征：注意患者的意识、呼吸、脉搏。如果出现意识模糊、抽搐、高热不退等严重症状，应立即送医或呼叫急救。\n\n预防中暑的措施：\n1. 避免长时间在高温高湿环境下活动或工作，尤其是在中午时段。\n2. 外出时做好防晒，戴帽子、太阳镜，穿透气性好的浅色衣物。\n3. 及时补充水分，不要等到口渴才喝水。可适量饮用含盐分的饮料。\n4. 保证充足睡眠，增强身体对高温的耐受力。\n5. 注意饮食清淡，多吃蔬果。\n6. 老年人、儿童、孕产妇及有慢性基础疾病者是中暑高危人群，需特别注意防护。', '评分要点：\n1. 中暑紧急处理方法描述准确、关键步骤完整（至少3项，4分）\n2. 预防中暑措施合理、全面（至少3项，4分）\n3. 建议实用，语言清晰（2分）', 2);

-- 插入标准简单题答案
INSERT INTO `STANDARD_SIMPLE_ANSWERS` (`STANDARD_QUESTION_ID`, `ANSWER_TEXT`, `ALTERNATIVE_ANSWERS`, `DETERMINED_BY_USER_ID`) VALUES
(5, '60-100次/分', '["60-100 bpm", "成人静息心率60-100次/分"]', 2),
(6, '36.1-37.2°C', '["36.1-37.2摄氏度", "约37°C"]', 2),
(7, '收缩压<120mmHg且舒张压<80mmHg', '["收缩压低于120mmHg，舒张压低于80mmHg", "120/80 mmHg以下"]', 2),
(8, '30', '["30 kg/m²", "BMI≥30"]', 2),
(9, '皮肤', '["皮肤器官"]', 2),
-- 更多标准简单题答案 (扩充)
(13, '约1.5-2升', '["1500-2000毫升", "8杯水左右"]', 2),
(17, '性传播、血液传播、母婴传播', '["性接触、血液及血制品、母婴垂直传播"]', 2),
(20, '世界卫生组织 (WHO)', '["WHO"]', 2),
(24, '血小板', '["Thrombocyte"]', 2),
(28, '手术治疗', '["白内障手术"]', 2),
(31, '被携带病毒的动物（主要是狗）咬伤或抓伤', '["动物咬伤"]', 2),
(35, '疲劳乏力、面色苍白', '["乏力", "面色苍白"]', 2),
(39, '病理性黄疸', '["病理性"]', 2);

-- 插入评测标准
INSERT INTO EVALUATION_CRITERIA (NAME, DESCRIPTION, DATA_TYPE, SCORE_RANGE, APPLICABLE_QUESTION_TYPES, CREATED_BY_USER_ID) VALUES
('专业性', '答案在医学专业方面的准确性和规范性', 'SCORE', '0-10', '["SUBJECTIVE", "MULTIPLE_CHOICE", "SINGLE_CHOICE"]', 1),
('完整性', '答案是否完整覆盖了问题的各个方面', 'SCORE', '0-10', '["SUBJECTIVE", "MULTIPLE_CHOICE"]', 1),
('逻辑性', '答案的逻辑结构是否清晰', 'SCORE', '0-10', '["SUBJECTIVE"]', 1),
('实用性', '答案是否具有实际应用价值', 'SCORE', '0-10', '["SUBJECTIVE", "SIMPLE_FACT"]', 1);

-- 插入问题标签关联
INSERT INTO `STANDARD_QUESTION_TAGS` (`STANDARD_QUESTION_ID`, `TAG_ID`, `CREATED_BY_USER_ID`) VALUES
(1, 4, 1), -- 高血压问题关联"高血压"标签
(1, 6, 1), -- 高血压问题关联"诊断"标签
(2, 5, 1), -- 糖尿病问题关联"糖尿病"标签
(2, 7, 1), -- 糖尿病问题关联"治疗"标签
(3, 3, 1), -- 冠心病问题关联"心脏病"标签
(3, 6, 1), -- 冠心病问题关联"诊断"标签
(4, 5, 1), -- 血糖问题关联"糖尿病"标签
(4, 6, 1), -- 血糖问题关联"诊断"标签
(10, 9, 1), (10, 12, 1), -- 问题10 (感冒症状): 呼吸系统疾病, 传染病
(11, 9, 1), (11, 12, 1), (11, 20, 1), -- 问题11 (流感预防): 呼吸系统疾病, 传染病, 公共卫生
(12, 19, 1), (12, 20, 1), -- 问题12 (抗生素耐药性): 药物使用, 公共卫生
(13, 14, 1), -- 问题13 (饮水量): 生活方式
(14, 17, 1), -- 问题14 (夜盲症与维生素A): 维生素与矿物质
(15, 3, 1), (15, 13, 1), -- 问题15 (心梗急救): 心脏病, 急救知识
(16, 9, 1), -- 问题16 (COPD高危因素): 呼吸系统疾病
(17, 12, 1), -- 问题17 (艾滋病传播): 传染病
(18, 16, 1), -- 问题18 (骨质疏松骨折部位): 老年健康
(19, 5, 1), (19, 14, 1), -- 问题19 (2型糖尿病预防): 糖尿病, 生活方式
(20, 19, 1), -- 问题20 (癌症止痛法): 药物使用
(21, 13, 1), (21, 15, 1), -- 问题21 (物理降温): 急救知识, 儿童健康
(22, 15, 1), -- 问题22 (儿童过敏原): 儿童健康
(23, 18, 1), -- 问题23 (抑郁症): 心理健康
(24, 1, 1), -- 问题24 (凝血细胞): 内科
(25, 20, 1), -- 问题25 (宫颈癌筛查): 公共卫生
(26, 11, 1), (26, 16, 1), -- 问题26 (阿尔茨海默病): 神经系统疾病, 老年健康
(27, 10, 1), -- 问题27 (幽门螺杆菌): 消化系统疾病
(28, 16, 1), -- 问题28 (白内障治疗): 老年健康
(29, 11, 1), (29, 13, 1), -- 问题29 (脑卒中溶栓时间窗): 神经系统疾病, 急救知识
(30, 12, 1), (30, 20, 1), -- 问题30 (疫苗机制): 传染病, 公共卫生
(31, 12, 1), -- 问题31 (狂犬病传播): 传染病
(32, 1, 1), (32, 14, 1), -- 问题32 (痛风饮食): 内科, 生活方式
(33, 1, 1), -- 问题33 (水电解质平衡): 内科
(34, 10, 1), (34, 13, 1), -- 问题34 (食物中毒): 消化系统疾病, 急救知识
(35, 1, 1), -- 问题35 (缺铁性贫血): 内科
(36, 19, 1), -- 问题36 (OTC药品): 药物使用
(37, 14, 1), -- 问题37 (健康生活习惯): 生活方式
(38, 14, 1), (38, 20, 1), -- 问题38 (免疫力): 生活方式, 公共卫生
(39, 15, 1), -- 问题39 (新生儿黄疸): 儿童健康
(40, 9, 1), (40, 12, 1), -- 问题40 (肺结核传播): 呼吸系统疾病, 传染病
(41, 13, 1); -- 问题41 (中暑处理): 急救知识

-- =============================================
-- 插入示例Prompt数据
-- =============================================

-- 插入回答场景的标签提示词
INSERT INTO `ANSWER_TAG_PROMPTS` (`TAG_ID`, `NAME`, `PROMPT_TEMPLATE`, `DESCRIPTION`, `PROMPT_PRIORITY`, `CREATED_BY_USER_ID`, `IS_ACTIVE`) VALUES
(1, '内科基础知识prompt', '作为一名内科医生，你需要：\n1. 使用准确的医学术语\n2. 解释复杂的内科疾病机制\n3. 强调疾病的系统性表现\n4. 注重药物治疗的详细说明', '内科回答的基础指导prompt', 10, 1, TRUE),
(2, '外科基础知识prompt', '作为一名外科医生，你需要：\n1. 详细描述手术适应症和禁忌症\n2. 解释手术方式的选择依据\n3. 说明手术风险和并发症\n4. 强调围手术期管理要点\n5. 注重手术后康复指导', '外科回答的基础指导prompt', 10, 1, TRUE),
(3, '心脏病专业prompt', '在回答心脏病相关问题时：\n1. 详细说明心血管系统的病理生理变化\n2. 强调症状与体征的关联性\n3. 说明心电图等检查的重要性\n4. 突出用药注意事项和禁忌症', '心脏病问题的专业指导prompt', 20, 1, TRUE),
(4, '高血压专业prompt', '回答高血压相关问题时：\n1. 强调血压值的正常范围和异常标准\n2. 详细说明生活方式的影响\n3. 解释各类降压药物的作用机制\n4. 说明并发症的预防措施', '高血压问题的专业指导prompt', 20, 1, TRUE),
(5, '糖尿病专业prompt', '回答糖尿病相关问题时：\n1. 明确血糖控制目标范围\n2. 强调血糖监测的重要性\n3. 详细说明饮食和运动管理\n4. 解释降糖药物选择原则\n5. 说明常见并发症的预防和处理', '糖尿病问题的专业指导prompt', 20, 1, TRUE),
(6, '诊断类prompt', '对于诊断类问题：\n1. 系统性列举症状和体征\n2. 说明必要的检查项目\n3. 解释鉴别诊断要点\n4. 强调诊断的金标准', '诊断类问题的通用prompt', 30, 1, TRUE),
(7, '治疗类prompt', '对于治疗类问题：\n1. 按照循证医学证据等级排序治疗方案\n2. 详细说明用药方案和注意事项\n3. 解释非药物治疗的重要性\n4. 说明治疗效果评估方法', '治疗类问题的通用prompt', 30, 1, TRUE),
(8, '预防类prompt', '对于预防类问题：\n1. 区分一级预防和二级预防措施\n2. 强调生活方式干预的具体方法\n3. 说明预防保健的关键时间点\n4. 解释风险因素的控制方法\n5. 提供可操作的预防建议', '预防类问题的通用prompt', 30, 1, TRUE),
-- 更多回答场景的标签提示词 (扩充)
(9, '呼吸系统疾病通用回答prompt', '当回答呼吸系统疾病相关问题时，请确保：\n1. 准确描述相关解剖结构和生理功能。\n2. 详细解释疾病的病因、发病机制。\n3. 清晰列出典型临床表现和体征。\n4. 说明常用的诊断方法和鉴别诊断。\n5. 概述主要的治疗原则和常用药物。\n6. 强调预防措施和患者教育要点。', '呼吸系统疾病问题的通用回答指导', 25, 1, TRUE),
(10, '消化系统疾病通用回答prompt', '针对消化系统疾病问题，回答应包含：\n1. 消化系统相关解剖和生理概述。\n2. 疾病的常见病因及发病机制。\n3. 主要症状（如腹痛、恶心、呕吐、腹泻、便秘等）的特点。\n4. 辅助检查手段（如内镜、影像学检查）。\n5. 治疗方案（包括药物治疗、生活方式调整、必要时手术治疗）。\n6. 饮食指导和预后情况。', '消化系统疾病问题的通用回答指导', 25, 1, TRUE),
(11, '神经系统疾病通用回答prompt', '回答神经系统疾病问题时，需注意：\n1. 神经系统的复杂解剖和功能定位。\n2. 疾病的病理生理改变。\n3. 神经系统症状（如头痛、眩晕、抽搐、意识障碍、运动感觉障碍）的详细描述。\n4. 神经系统检查和影像学（CT/MRI）等诊断方法。\n5. 治疗策略（急性期处理、恢复期治疗、康复治疗）。\n6. 患者的护理和功能恢复重点。', '神经系统疾病问题的通用回答指导', 25, 1, TRUE),
(12, '传染病通用回答prompt', '关于传染病问题，回答应强调：\n1. 病原体类型（细菌、病毒、真菌、寄生虫）。\n2. 传播途径（呼吸道、消化道、接触、血液、虫媒等）。\n3. 潜伏期和传染期。\n4. 临床表现及并发症。\n5. 实验室诊断方法。\n6. 治疗原则（抗感染治疗、对症支持治疗）。\n7. 预防和控制措施（管理传染源、切断传播途径、保护易感人群、疫苗接种）。', '传染病问题的通用回答指导', 22, 1, TRUE),
(13, '急救知识问答prompt', '提供急救知识时，请确保：\n1. 步骤清晰、简洁、易于理解和操作。\n2. 强调现场安全评估。\n3. 明确指出何时以及如何寻求专业医疗帮助（如拨打120）。\n4. 针对不同情况（如外伤、烧烫伤、中毒、心搏骤停等）给出核心处理要点。\n5. 提醒常见错误操作和注意事项。', '急救知识类问题的回答指导', 15, 1, TRUE),
(14, '健康生活方式指导prompt', '提供健康生活方式建议时，应覆盖：\n1. 合理膳食：食物多样性、营养均衡、三餐规律。\n2. 适量运动：运动类型、频率、强度的建议。\n3. 充足睡眠：睡眠时间、睡眠质量的重要性。\n4. 戒烟限酒：说明其危害和戒断方法。\n5. 心理平衡：压力管理、情绪调节技巧。\n6. 定期体检：强调早期发现、早期干预。', '健康生活方式相关问题的回答指导', 35, 1, TRUE),
(15, '儿童健康问题回答prompt', '回答儿童健康问题时，请特别注意：\n1. 不同年龄段儿童的生理特点和常见疾病。\n2. 生长发育评估指标。\n3. 儿童用药的特殊性（剂量计算、剂型选择）。\n4. 儿童常见意外伤害的预防和处理。\n5. 儿童免疫接种的重要性。\n6. 家长在儿童保健中的角色和注意事项。', '儿童健康相关问题的回答指导', 28, 1, TRUE),
(16, '老年健康问题回答prompt', '针对老年健康问题，回答应关注：\n1. 老年人的生理衰老特征和常见慢性病。\n2. 老年人用药原则（多重用药、药物相互作用）。\n3. 跌倒预防和骨骼健康。\n4. 认知功能维护和老年痴呆预防。\n5. 营养支持和合理膳食。\n6. 心理健康和社会支持的重要性。', '老年健康相关问题的回答指导', 28, 1, TRUE),
(17, '维生素与矿物质知识prompt', '解答维生素与矿物质相关问题时，应包含：\n1. 该营养素的主要生理功能。\n2. 推荐摄入量及主要食物来源。\n3. 缺乏时的临床表现和危害。\n4. 过量摄入的潜在风险。\n5. 特殊人群（如孕妇、儿童、老人）的需求特点。', '维生素与矿物质知识的回答指导', 30, 1, TRUE),
(18, '心理健康问题回答prompt', '回答心理健康问题，请注意：\n1. 尊重和共情，避免评判性语言。\n2. 常见心理问题（如焦虑、抑郁、失眠）的识别。\n3. 简单的自我调适方法。\n4. 强调寻求专业帮助（心理咨询、精神科医生）的重要性。\n5. 介绍可用的社会支持资源。\n6. 破除对心理疾病的污名化。', '心理健康相关问题的回答指导', 20, 1, TRUE),
(19, '安全用药指导prompt', '提供药物使用指导时，务必强调：\n1. 遵医嘱或药品说明书用药的重要性。\n2. 药物的正确用法、用量、疗程。\n3. 常见不良反应和注意事项。\n4. 药物相互作用风险。\n5. 特殊人群（孕妇、哺乳期、儿童、老人、肝肾功能不全者）用药须知。\n6. 药品储存条件。', '安全合理用药的回答指导', 18, 1, TRUE),
(20, '公共卫生问题回答prompt', '回答公共卫生相关问题时，应侧重于：\n1. 问题的群体性、社会性特征。\n2. 疾病预防和健康促进策略（如健康教育、筛查、环境改善）。\n3. 传染病防控体系和应急响应。\n4. 卫生政策法规和健康公平性。\n5. 个体在公共卫生事件中的责任和行为准则。', '公共卫生领域问题的回答指导', 26, 1, TRUE);

-- 插入回答场景的题型提示词
INSERT INTO `ANSWER_QUESTION_TYPE_PROMPTS` (`NAME`, `QUESTION_TYPE`, `PROMPT_TEMPLATE`, `RESPONSE_FORMAT_INSTRUCTION`, `RESPONSE_EXAMPLE`, `CREATED_BY_USER_ID`) VALUES
('单选题回答prompt', 'SINGLE_CHOICE', '请仔细分析每个选项，选择最准确的一个答案。只需要给出选项，务必别给出分析或者回答格式以外的字', '回答格式：\n[选项字母]\n', 'A\n', 1),
('多选题回答prompt', 'MULTIPLE_CHOICE', '请仔细分析所有选项，选择所有正确的答案。只需要给出选项，务必别给出分析或者回答格式以外的字', '回答格式：\n[选项字母]\n', 'A,B,C\n', 1),
('简单事实题回答prompt', 'SIMPLE_FACT', '请提供简洁、准确的事实回答。只需要给出答案，务必别给出分析或者回答格式以外的字', '回答格式：\n答案：[核心事实]\n', '正常成人心率为60-100次/分\n', 1),
('主观题回答prompt', 'SUBJECTIVE', '请提供详细、系统的回答。\n需要：\n1. 结构化组织内容\n2. 使用专业准确的医学术语\n3. 提供具体的例子或解释\n4. 注意回答的完整性和逻辑性', '回答格式：\n[主要观点]\n1. [要点1]\n2. [要点2]\n...\n补充说明：[其他重要信息]', '高血压的主要症状：\n1. 头痛（特别是后枕部）\n2. 头晕\n3. 视物模糊\n补充说明：部分患者可能无明显症状', 1);

-- 插入评测场景的标签提示词
INSERT INTO `EVALUATION_TAG_PROMPTS` (`TAG_ID`, `NAME`, `PROMPT_TEMPLATE`, `PROMPT_PRIORITY`, `CREATED_BY_USER_ID`, `IS_ACTIVE`) VALUES
(1, '内科评测prompt', '评估内科问题回答时，请注意：\n1. 医学术语使用的准确性（0-10分）\n2. 病理生理机制解释的深度（0-10分）\n3. 治疗方案的规范性（0-10分）\n4. 整体专业水平（0-10分）', 10, 1, TRUE),
(3, '心脏病评测prompt', '评估心脏病相关回答时，重点关注：\n1. 心血管病理生理的解释准确性（0-10分）\n2. 症状体征描述的完整性（0-10分）\n3. 检查方法推荐的合理性（0-10分）\n4. 治疗方案的循证医学支持（0-10分）', 20, 1, TRUE),
(6, '诊断评测prompt', '评估诊断相关回答时，请考虑：\n1. 诊断思路的清晰度（0-10分）\n2. 鉴别诊断的完整性（0-10分）\n3. 检查建议的必要性（0-10分）\n4. 诊断依据的循证等级（0-10分）', 30, 1, TRUE),
-- 更多评测场景的标签提示词 (扩充)
(9, '呼吸系统疾病评测prompt', '评估呼吸系统疾病回答，请关注：\n1. 解剖生理知识的准确性（0-10分）\n2. 病因病机阐述的深度（0-10分）\n3. 临床表现描述的全面性（0-10分）\n4. 诊疗措施的规范性和先进性（0-10分）\n5. 预防宣教的实用性（0-10分）', 25, 1, TRUE),
(10, '消化系统疾病评测prompt', '评估消化系统疾病回答，请关注：\n1. 症状描述与疾病关联的准确性（0-10分）\n2. 辅助检查选择的合理性（0-10分）\n3. 治疗方案（含饮食指导）的完整性和科学性（0-10分）\n4. 专业术语使用的规范性（0-10分）', 25, 1, TRUE),
(11, '神经系统疾病评测prompt', '评估神经系统疾病回答，请关注：\n1. 神经功能定位分析的准确性（0-10分）\n2. 疾病特征描述的细致程度（0-10分）\n3. 诊疗及康复建议的全面性（0-10分）\n4. 对患者及家属指导的人文关怀（0-10分）', 25, 1, TRUE),
(12, '传染病评测prompt', '评估传染病回答，请关注：\n1. 病原学和流行病学特点阐述的准确性（0-10分）\n2. "三个环节"控制措施描述的完整性（0-10分）\n3. 治疗和预防方案的科学依据（0-10分）\n4. 公共卫生意义的强调（0-10分）', 22, 1, TRUE),
(13, '急救知识评测prompt', '评估急救知识回答，请关注：\n1. 急救措施的正确性和及时性（0-15分）\n2. 操作步骤的清晰度和可操作性（0-15分）\n3. 强调呼救和注意事项的完整性（0-10分）\n4. 避免常见错误操作的提示（0-10分）', 15, 1, TRUE),
(14, '健康生活方式评测prompt', '评估健康生活方式建议，请关注：\n1. 建议的科学性和循证依据（0-15分）\n2. 建议的全面性（覆盖饮食、运动、睡眠、心理等）（0-15分）\n3. 建议的可行性和个体化程度（0-10分）\n4. 表达的鼓励性和积极性（0-10分）', 35, 1, TRUE),
(15, '儿童健康评测prompt', '评估儿童健康回答，请关注：\n1. 是否充分考虑儿童年龄特点和生长发育规律（0-15分）\n2. 用药、护理等建议的准确性和安全性（0-15分）\n3. 对家长指导的实用性和易懂性（0-10分）\n4. 预防保健意识的强调（0-10分）', 28, 1, TRUE),
(16, '老年健康评测prompt', '评估老年健康回答，请关注：\n1. 对老年生理病理特点的把握（0-15分）\n2. 慢性病管理和多重用药指导的合理性（0-15分）\n3. 功能维护（认知、运动）和生活质量关注的全面性（0-10分）\n4. 人文关怀和心理支持的体现（0-10分）', 28, 1, TRUE),
(17, '维生素与矿物质评测prompt', '评估维生素与矿物质回答，请关注：\n1. 生理功能和缺乏/过量表现描述的准确性（0-15分）\n2. 食物来源和推荐摄入量信息的正确性（0-15分）\n3. 对特殊人群需求的考虑（0-10分）\n4. 信息来源的科学性（0-10分）', 30, 1, TRUE),
(18, '心理健康评测prompt', '评估心理健康回答，请关注：\n1. 共情和非评判态度的体现（0-15分）\n2. 常见问题识别和自我调适建议的合理性（0-15分）\n3. 强调专业求助重要性的恰当性（0-10分）\n4. 信息的积极导向和建设性（0-10分）', 20, 1, TRUE),
(19, '安全用药评测prompt', '评估药物使用指导，请关注：\n1. 用法用量、疗程等核心信息准确无误（0-15分）\n2. 不良反应、禁忌症、相互作用提示的全面性（0-15分）\n3. 对特殊人群用药指导的审慎性（0-10分）\n4. 强调遵医嘱和阅读说明书的重要性（0-10分）', 18, 1, TRUE),
(20, '公共卫生评测prompt', '评估公共卫生回答，请关注：\n1. 对问题社会性和群体性理解的深度（0-10分）\n2. 预防控制策略的科学性和系统性（0-10分）\n3. 对个体责任和行为引导的有效性（0-10分）\n4. 卫生政策和伦理考量的体现（0-10分）\n5. 信息的权威性和时效性（0-10分）', 26, 1, TRUE);

-- 插入评测场景的主观题提示词
INSERT INTO `EVALUATION_SUBJECTIVE_PROMPTS` (`NAME`, `PROMPT_TEMPLATE`, `SCORING_INSTRUCTION`, `OUTPUT_FORMAT_INSTRUCTION`, `CREATED_BY_USER_ID`) VALUES
('标准主观题评测prompt', '请对以下回答进行全面评估：\n1. 专业性（医学术语、概念准确性）\n2. 完整性（要点覆盖程度）\n3. 逻辑性（条理性、结构性）\n4. 实用性（临床应用价值）', '评分标准：\n专业性（0-25分）：\n- 术语准确：0-10分\n- 概念清晰：0-15分\n\n完整性（0-25分）：\n- 核心要点：0-15分\n- 补充信息：0-10分\n\n逻辑性（0-25分）：\n- 结构完整：0-10分\n- 条理清晰：0-15分\n\n实用性（0-25分）：\n- 临床相关：0-15分\n- 可操作性：0-10分', '请按以下格式输出评分：\n{\n  "专业性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "完整性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "逻辑性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "实用性": {\n    "分数": X,\n    "评语": "..."\n  },\n  "总分": X,\n  "总评": "..."\n}', 1);

-- 插入prompt组装配置
INSERT INTO `ANSWER_PROMPT_ASSEMBLY_CONFIGS` (`NAME`, `DESCRIPTION`, `BASE_SYSTEM_PROMPT`, `TAG_PROMPTS_SECTION_HEADER`, `QUESTION_TYPE_SECTION_HEADER`, `FINAL_INSTRUCTION`, `CREATED_BY_USER_ID`) VALUES
('标准医学回答配置', '用于医学问题回答的标准prompt组装配置', '你是一个专业的医学AI助手，请基于循证医学和最新指南提供准确、专业的回答。', '## 专业知识要求', '## 回答要求', '请严格按照上述要求进行回答，确保专业性和准确性。', 1);

INSERT INTO `EVALUATION_PROMPT_ASSEMBLY_CONFIGS` (`NAME`, `DESCRIPTION`, `BASE_SYSTEM_PROMPT`, `TAG_PROMPTS_SECTION_HEADER`, `SUBJECTIVE_SECTION_HEADER`, `FINAL_INSTRUCTION`, `CREATED_BY_USER_ID`) VALUES
('标准医学评测配置', '用于医学问题评测的标准prompt组     装配置', '你是一个专业的医学评测专家，请基于专业知识对回答进行客观、公正的评估。', '## 专业评测标准', '## 评分要求', '请严格按照评分标准进行评估，给出详细的评分依据和建议。', 1);

-- 插入数据集版本和映射关系
INSERT INTO `DATASET_VERSIONS` (`VERSION_NUMBER`, `NAME`, `DESCRIPTION`, `CREATED_BY_USER_ID`) VALUES
('v1.0.0', '医学问答基础数据集', '包含基础医学问题的初始数据集', 1);

-- 插入数据集-问题映射关系
INSERT INTO `DATASET_QUESTION_MAPPING` (`DATASET_VERSION_ID`, `STANDARD_QUESTION_ID`, `ORDER_IN_DATASET`, `CREATED_BY_USER_ID`) VALUES
(1, 1, 1, 1),
(1, 2, 2, 1),
(1, 3, 3, 1),
(1, 4, 4, 1),
(1, 5, 5, 1),
(1, 6, 6, 1),
(1, 7, 7, 1),
(1, 8, 8, 1),
(1, 9, 9, 1);
