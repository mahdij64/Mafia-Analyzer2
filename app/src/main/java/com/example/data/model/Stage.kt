package com.example.data.model

data class GameStage(
    val index: Int,
    val title: String,
    val description: String
) {
    companion object {
        val DEFAULT_STAGES = listOf(
            GameStage(0, "معارفه", "مرحله معارفه اولیه"),
            GameStage(1, "روز ۱", "رأی‌گیری و چالش‌های روز اول"),
            GameStage(2, "روز ۲", "بررسی تارگت‌های روز دوم"),
            GameStage(3, "روز ۳", "روز حساس و تقابل‌ها"),
            GameStage(4, "روز ۴", "فینال و بررسی نهایی")
        )

        fun getStage(index: Int): GameStage {
            return if (index < DEFAULT_STAGES.size) {
                DEFAULT_STAGES[index]
            } else {
                GameStage(index, "روز $index", "مرحله روز $index")
            }
        }
    }
}
