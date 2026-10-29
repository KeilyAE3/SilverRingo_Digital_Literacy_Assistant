package kr.silverringo.learning.phone

const val PRACTICE_PHONE = "01063442138"
fun volumeEvidence(previous: Int, current: Int): Pair<Boolean, Boolean> = Pair(current > previous, current < previous)
fun settingsPracticePassed(raised: Boolean, lowered: Boolean, brightnessChanged: Boolean, fontChoiceConfirmed: Boolean): Boolean = raised && lowered && brightnessChanged && fontChoiceConfirmed
fun externalPracticePassed(appOpened: Boolean, firstConfirmed: Boolean, secondConfirmed: Boolean): Boolean = appOpened && firstConfirmed && secondConfirmed
