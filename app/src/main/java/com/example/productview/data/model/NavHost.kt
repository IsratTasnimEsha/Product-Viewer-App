package com.example.productview.data.model

sealed class Screen(val route: String) {
    object ProductList : Screen("product_list")

    object ProductDetail : Screen("product_detail/{thumbnail}/{title}/{category}/{brand}/{description}/{price}") {
        fun createRoute(
            thumbnail: String,
            title: String,
            category: String,
            brand: String,
            description: String,
            price: String
        ): String {
            val cleanThumbnail = thumbnail.removePunctuation()
            return "product_detail/${cleanThumbnail}/${title}/${category}/${brand}/${description}/${price}"
        }
    }
}

// Helper to remove punctuation from thumbnail
fun String.removePunctuation(): String =
    this.replace("/", "*").replace(":", "$")
    // this.replace(Regex("\\p{Punct}"), "")