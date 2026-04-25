package com.example.smartmeal.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {
    /**
     * Открывает системное меню "Поделиться" для рецепта.
     * Удалена браузерная ссылка, оставлена только прямая ссылка в приложение.
     */
    fun shareRecipe(context: Context, recipeTitle: String, recipeId: Int) {
        val shareText = """
            Посмотри, какой крутой рецепт я нашел в SmartMeal! 😋
            
            🍴 $recipeTitle
            
            📲 Открыть в приложении:
            smartmeal://recipe/$recipeId
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Рецепт из SmartMeal")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        context.startActivity(Intent.createChooser(intent, "Поделиться рецептом"))
    }

    /**
     * Поделиться рецептом вместе с изображением (карточкой).
     */
    fun shareRecipeWithImage(context: Context, recipeTitle: String, recipeId: Int, bitmap: Bitmap) {
        val shareText = """
            Посмотри, какой крутой рецепт я нашел в SmartMeal! 😋
            🍴 $recipeTitle
            📲 smartmeal://recipe/$recipeId
        """.trimIndent()

        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/recipe_card.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(context.cacheDir, "shared_images")
            val newFile = File(imagePath, "recipe_card.png")
            val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "image/png"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться рецептом"))
            }
        } catch (e: Exception) {
            // В случае ошибки с фото - отправляем просто текст
            shareRecipe(context, recipeTitle, recipeId)
        }
    }
}
