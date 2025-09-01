package org.tensorflow.codelabs.objectdetection

data class Recipe(
    val name: String,
    val instructions: String,
    val recipeName: CharSequence? = name,
    val ingredients: String, // 재료를 문자열로 변경
    val image: String? = null,
    val id: String // 레시피 ID 추가
)