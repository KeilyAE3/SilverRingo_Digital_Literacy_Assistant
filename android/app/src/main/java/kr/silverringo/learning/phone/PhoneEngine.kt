package kr.silverringo.learning.phone

/** Pure Kotlin simulation. No Android services, intents, calls or SMS APIs. */
data class PhoneState(
    val lesson: PhoneLesson, val stage: Int = 0, val guided: Boolean = true,
    val brightness: Int = 30, val volume: Int = 80, val number: String = "",
    val name: String = "", val query: String = "", val message: String = "",
    val feedback: String = "", val mistakes: Int = 0, val hints: Int = 0,
    val actions: Int = 0, val evidence: String = "simulation"
) { val complete get() = stage == lesson.guides.size }

sealed interface PhoneAction {
    data object Continue : PhoneAction
    data object Back : PhoneAction
    data object Hint : PhoneAction
    data class Brightness(val value: Int) : PhoneAction
    data class Volume(val value: Int) : PhoneAction
    data class Number(val value: String) : PhoneAction
    data class Name(val value: String) : PhoneAction
    data class Query(val value: String) : PhoneAction
    data class Message(val value: String) : PhoneAction
}

fun reducePhone(s: PhoneState, action: PhoneAction): PhoneState {
    if (s.complete) return s
    if (action == PhoneAction.Hint) return s.copy(hints = s.hints + 1, feedback = s.lesson.guides[s.stage])
    if (action == PhoneAction.Back) return s.copy(stage = (s.stage - 1).coerceAtLeast(0), feedback = "")
    val n = s.copy(feedback = "", actions = s.actions + 1)
    when (action) {
        is PhoneAction.Brightness -> return if (s.lesson == PhoneLesson.SETTINGS && s.stage == 1) n.copy(brightness = action.value.coerceIn(0,100)) else s
        is PhoneAction.Volume -> return if (s.lesson == PhoneLesson.SETTINGS && s.stage == 2) n.copy(volume = action.value.coerceIn(0,100)) else s
        is PhoneAction.Number -> return if ((s.lesson == PhoneLesson.CALLS && s.stage == 1) || (s.lesson == PhoneLesson.CONTACTS && s.stage == 2) || (s.lesson == PhoneLesson.MESSAGES && s.stage == 3)) n.copy(number = action.value.take(20)) else s
        is PhoneAction.Name -> return if (s.lesson == PhoneLesson.CONTACTS && s.stage == 2) n.copy(name = action.value.take(40)) else s
        is PhoneAction.Query -> return if (s.lesson == PhoneLesson.CONTACTS && s.stage == 3) n.copy(query = action.value.take(40)) else s
        is PhoneAction.Message -> return if (s.lesson == PhoneLesson.MESSAGES && s.stage == 3) n.copy(message = action.value.take(200)) else s
        else -> Unit
    }
    if (action != PhoneAction.Continue) return s
    val validNumber = s.number.replace("-", "").replace(" ", "") == PRACTICE_PHONE
    val error = when {
        s.lesson == PhoneLesson.SETTINGS && s.stage == 1 && s.brightness != 70 -> "밝기 목표는 70%예요. 다시 조절해 보세요."
        s.lesson == PhoneLesson.SETTINGS && s.stage == 2 && s.volume != 40 -> "소리 목표는 40%예요. 다시 조절해 보세요."
        s.lesson == PhoneLesson.CALLS && s.stage == 1 && !validNumber -> "연습 번호 010-6344-2138을 확인해 주세요."
        s.lesson == PhoneLesson.CONTACTS && s.stage == 2 && (s.name.trim() != "연습 친구" || !validNumber) -> "이름은 연습 친구, 번호는 010-6344-2138으로 저장해 주세요."
        s.lesson == PhoneLesson.CONTACTS && s.stage == 3 && s.query.trim() != "연습 친구" -> "저장한 이름 ‘연습 친구’로 검색해 보세요."
        s.lesson == PhoneLesson.MESSAGES && s.stage == 3 && (!validNumber || s.message.trim() != "안녕하세요") -> "받는 번호와 ‘안녕하세요’라는 내용을 확인해 주세요."
        else -> ""
    }
    return if (error.isEmpty()) n.copy(stage = s.stage + 1) else n.copy(feedback = error, mistakes = s.mistakes + 1)
}
