package com.silverlingo.app.chat

/** Local ID/target allow-list; server URLs and labels never authorize navigation. */
object ChatActions {
    private val routes = mapOf(
        "open_kiosk_course" to Triple("learning.kiosk", "learning", "키오스크 배우기 열기"),
        "open_guide_home" to Triple("guides.home", "guide", "병원 접수 도우미 열기")
    )

    fun destination(action: ChatAction): String? = routes[action.id]?.takeIf {
        action.type == "navigate" && action.target == it.first
    }?.second

    fun allowed(reply: ChatReply): List<ChatAction> = reply.actions.filter { action ->
        destination(action) != null && reply.sources.any { source ->
            source.status in setOf("implemented", "prototype") && when (action.id) {
                "open_kiosk_course" -> source.id == "app:learning_menu" || source.id == "course:kiosk" || source.id.startsWith("course:kiosk:")
                "open_guide_home" -> source.id == "app:guide_menu" || source.id == "guide:ddocdoc" || source.id.startsWith("guide:ddocdoc:")
                else -> false
            }
        }
    }.distinctBy { destination(it) }.take(2).map { it.copy(label = routes.getValue(it.id).third) }
}
