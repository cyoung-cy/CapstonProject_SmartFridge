package org.tensorflow.codelabs.objectdetection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tensorflow.codelabs.objectdetection.R

class RecipeAdapter(
    private val recipes: List<Recipe>, // 레시피 리스트
    private val clickListener: (Recipe, String) -> Unit // 클릭 리스너 수정
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    // ViewHolder 클래스 정의
    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeName: TextView = itemView.findViewById(R.id.recipeNameTextView)
        val ingredientsTextView: TextView = itemView.findViewById(R.id.ingredientsTextView)
        val recipeImageView: ImageView = itemView.findViewById(R.id.recipeImageView)

        // 레시피 바인딩 메서드
        fun bind(recipe: Recipe) {
            // 레시피 이름이 비어있을 경우 숨기기
            if (recipe.name.isNotEmpty()) {
                recipeName.text = recipe.name
                recipeName.visibility = View.VISIBLE
            } else {
                recipeName.visibility = View.GONE // 이름이 없을 경우 숨김
            }

            ingredientsTextView.text = recipe.ingredients.split(",").joinToString(", ")

            // Glide를 사용하여 이미지 로드
            Glide.with(itemView.context)
                .load(recipe.image)
                .into(recipeImageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)

        // 클릭 리스너 추가
        holder.itemView.setOnClickListener {
            clickListener(recipe, recipe.id)
        }
    }

    override fun getItemCount(): Int {
        return recipes.size
    }
}
