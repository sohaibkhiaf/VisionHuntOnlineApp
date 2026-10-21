package com.example.visionhuntonline.data

import com.example.visionhuntonline.model.Rarity
import com.example.visionhuntonline.model.TaskDefinition

object CocoTasks {

    const val GLOBAL_CONFIDENCE_THRESHOLD = 0.85f
    const val DISPLAY_CONFIDENCE_THRESHOLD = 0.30f

    val COCO_LABELS = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "???", "stop sign", "parking meter", "bench", "bird",
        "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "???",
        "backpack", "umbrella", "???", "???", "handbag", "tie", "suitcase", "frisbee", "skis",
        "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
        "surfboard", "tennis racket", "bottle", "???", "wine glass", "cup", "fork", "knife",
        "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog",
        "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed", "???", "dining table",
        "???", "???", "toilet", "???", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "???", "book", "clock", "vase",
        "scissors", "teddy bear", "hair drier", "toothbrush"
    )

    private data class ObjectDef(
        val label: String,
        val displayNameAr: String,
        val rarity: Rarity
    )

    private val COCO_OBJECTS = listOf(
        ObjectDef("person", "شخص", Rarity.COMMON),
        ObjectDef("bicycle", "دراجة هوائية", Rarity.UNCOMMON),
        ObjectDef("car", "سيارة", Rarity.COMMON),
        ObjectDef("motorcycle", "دراجة نارية", Rarity.UNCOMMON),
        ObjectDef("airplane", "طائرة", Rarity.RARE),
        ObjectDef("bus", "حافلة", Rarity.UNCOMMON),
        ObjectDef("train", "قطار", Rarity.RARE),
        ObjectDef("truck", "شاحنة", Rarity.UNCOMMON),
        ObjectDef("boat", "قارب", Rarity.RARE),
        ObjectDef("traffic light", "إشارة مرور", Rarity.UNCOMMON),
        ObjectDef("fire hydrant", "صنبور إطفاء الحريق", Rarity.RARE),
        ObjectDef("stop sign", "علامة قف", Rarity.UNCOMMON),
        ObjectDef("parking meter", "عداد مواقف السيارات", Rarity.RARE),
        ObjectDef("bench", "مقعد عام", Rarity.COMMON),
        ObjectDef("bird", "طائر", Rarity.COMMON),
        ObjectDef("cat", "قط", Rarity.COMMON),
        ObjectDef("dog", "كلب", Rarity.COMMON),
        ObjectDef("horse", "حصان", Rarity.RARE),
        ObjectDef("sheep", "خروف", Rarity.UNCOMMON),
        ObjectDef("cow", "بقرة", Rarity.UNCOMMON),
        ObjectDef("elephant", "فيل", Rarity.RARE),
        ObjectDef("bear", "دب", Rarity.RARE),
        ObjectDef("zebra", "حمار وحشي", Rarity.RARE),
        ObjectDef("giraffe", "زرافة", Rarity.RARE),
        ObjectDef("backpack", "حقيبة ظهر", Rarity.COMMON),
        ObjectDef("umbrella", "مظلة", Rarity.COMMON),
        ObjectDef("handbag", "حقيبة يد", Rarity.COMMON),
        ObjectDef("tie", "ربطة عنق", Rarity.UNCOMMON),
        ObjectDef("suitcase", "حقيبة سفر", Rarity.UNCOMMON),
        ObjectDef("frisbee", "قرص فريسبي", Rarity.RARE),
        ObjectDef("skis", "زلاجات ثلجية", Rarity.RARE),
        ObjectDef("snowboard", "لوح تزلج ثلجي", Rarity.RARE),
        ObjectDef("sports ball", "كرة رياضية", Rarity.COMMON),
        ObjectDef("kite", "طائرة ورقية", Rarity.UNCOMMON),
        ObjectDef("baseball bat", "مضرب بيسبول", Rarity.RARE),
        ObjectDef("baseball glove", "قفاز بيسبول", Rarity.RARE),
        ObjectDef("skateboard", "لوح تزلج", Rarity.UNCOMMON),
        ObjectDef("surfboard", "لوح ركوب الأمواج", Rarity.RARE),
        ObjectDef("tennis racket", "مضرب تنس", Rarity.UNCOMMON),
        ObjectDef("bottle", "زجاجة", Rarity.COMMON),
        ObjectDef("wine glass", "كأس زجاجي", Rarity.UNCOMMON),
        ObjectDef("cup", "كوب", Rarity.COMMON),
        ObjectDef("fork", "شوكة", Rarity.COMMON),
        ObjectDef("knife", "سكين", Rarity.COMMON),
        ObjectDef("spoon", "ملعقة", Rarity.COMMON),
        ObjectDef("bowl", "وعاء", Rarity.COMMON),
        ObjectDef("banana", "موز", Rarity.COMMON),
        ObjectDef("apple", "تفاحة", Rarity.COMMON),
        ObjectDef("sandwich", "شطيرة", Rarity.COMMON),
        ObjectDef("orange", "برتقالة", Rarity.COMMON),
        ObjectDef("broccoli", "بروكلي", Rarity.UNCOMMON),
        ObjectDef("carrot", "جزر", Rarity.COMMON),
        ObjectDef("hot dog", "هوت دوغ", Rarity.UNCOMMON),
        ObjectDef("pizza", "بيتزا", Rarity.COMMON),
        ObjectDef("donut", "دونات", Rarity.UNCOMMON),
        ObjectDef("cake", "كعكة", Rarity.COMMON),
        ObjectDef("chair", "كرسي", Rarity.COMMON),
        ObjectDef("couch", "أريكة", Rarity.COMMON),
        ObjectDef("potted plant", "نبات في أصيص", Rarity.COMMON),
        ObjectDef("bed", "سرير", Rarity.COMMON),
        ObjectDef("dining table", "طاولة طعام", Rarity.COMMON),
        ObjectDef("toilet", "مرحاض", Rarity.UNCOMMON),
        ObjectDef("tv", "تلفاز", Rarity.COMMON),
        ObjectDef("laptop", "حاسوب محمول", Rarity.UNCOMMON),
        ObjectDef("mouse", "فأرة حاسوب", Rarity.COMMON),
        ObjectDef("remote", "جهاز تحكم عن بعد", Rarity.COMMON),
        ObjectDef("keyboard", "لوحة مفاتيح", Rarity.COMMON),
        ObjectDef("cell phone", "هاتف محمول", Rarity.COMMON),
        ObjectDef("microwave", "ميكروويف", Rarity.UNCOMMON),
        ObjectDef("oven", "فرن", Rarity.UNCOMMON),
        ObjectDef("toaster", "محمصة خبز", Rarity.UNCOMMON),
        ObjectDef("sink", "حوض", Rarity.COMMON),
        ObjectDef("refrigerator", "ثلاجة", Rarity.COMMON),
        ObjectDef("book", "كتاب", Rarity.COMMON),
        ObjectDef("clock", "ساعة", Rarity.COMMON),
        ObjectDef("vase", "مزهرية", Rarity.UNCOMMON),
        ObjectDef("scissors", "مقص", Rarity.COMMON),
        ObjectDef("teddy bear", "دب محشو", Rarity.COMMON),
        ObjectDef("hair drier", "مجفف شعر", Rarity.UNCOMMON),
        ObjectDef("toothbrush", "فرشاة أسنان", Rarity.COMMON)
    )

    val GAME_TASKS: List<TaskDefinition> = COCO_OBJECTS.map { def ->
        TaskDefinition(
            id = def.label.replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            label = def.label,
            displayNameAr = def.displayNameAr,
            displayNameEn = def.label.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            },
            rarity = def.rarity,
            confidenceThreshold = GLOBAL_CONFIDENCE_THRESHOLD
        )
    }

    val TASK_BY_LABEL: Map<String, TaskDefinition> = GAME_TASKS.associateBy {
        it.label.trim().lowercase()
    }
}
