package com.example.productview

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.productview.data.ProductsRepositoryImplementation
import com.example.productview.data.model.Product
import com.example.productview.data.model.Screen
import com.example.productview.presentation.ProductsViewModel
import com.example.productview.ui.theme.ProductViewTheme
import kotlinx.coroutines.flow.collectLatest
import retrofit2.Retrofit
import androidx.compose.foundation.clickable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.productview.data.model.removePunctuation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<ProductsViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory {
            override fun <T: ViewModel> create(modelClass: Class<T>): T {
                return ProductsViewModel(ProductsRepositoryImplementation(RetrofitInstance.api))
                        as T
            }
        }
    })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProductViewTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Screen.ProductList.route) {
                    composable(Screen.ProductList.route) {
                        ProductListScreen(viewModel, navController)
                    }

                    composable(
                        route = Screen.ProductDetail.route,
                        arguments = listOf(
                            navArgument("thumbnail") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                            navArgument("category") { type = NavType.StringType },
                            navArgument("brand") { type = NavType.StringType },
                            navArgument("description") { type = NavType.StringType },
                            navArgument("price") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val thumbnail = backStackEntry.arguments?.getString("thumbnail") ?: ""
                        val title = backStackEntry.arguments?.getString("title") ?: ""
                        val category = backStackEntry.arguments?.getString("category") ?: ""
                        val brand = backStackEntry.arguments?.getString("brand") ?: ""
                        val description = backStackEntry.arguments?.getString("description") ?: ""
                        val price = backStackEntry.arguments?.getString("price") ?: ""

                        ProductDetailScreen(thumbnail, title, category, brand, description, price)
                    }
                }
            }
        }
    }
}

@Composable
fun Product(product: Product, onClick: () -> Unit) {
    val imageState = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current).data(product.thumbnail)
            .size(Size.ORIGINAL).build()
    ).state

    Column (
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .height(300.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick() } // ✅ add clickable here
    ) {
        if (imageState is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (imageState is AsyncImagePainter.State.Success) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(15.dp) // outer margin inside the colored card
                    .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp)) // white background box
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize(), // fill the white box
                    painter = imageState.painter,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "${product.title}",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Price: ${product.price}$",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

///



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(viewModel: ProductsViewModel, navController: NavHostController) {
    val productList = viewModel.products.collectAsState().value
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }  // Search input state

    LaunchedEffect(key1 = viewModel.showErrorToastChannel) {
        viewModel.showErrorToastChannel.collectLatest { show ->
            if (show) {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filter productList by title or brand safely handling nulls
    val filteredList = if (searchQuery.isEmpty()) {
        productList
    } else {
        productList.filter { product ->
            (product.title?.contains(searchQuery, ignoreCase = true) ?: false) ||
                    (product.category?.contains(searchQuery, ignoreCase = true) ?: false) ||
                    (product.brand?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    Column {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by title or category or brand") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (searchQuery.isNotEmpty()) {
                    Text("No results found")
                } else {
                    CircularProgressIndicator()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(16.dp)
            ) {
                items(filteredList) { product ->
                    Product(product = product, onClick = {
                        navController.navigate(
                            com.example.productview.data.model.Screen.ProductDetail.createRoute(
                                product.thumbnail,
                                product.title ?: "",
                                product.category ?: "",
                                product.brand ?: "",
                                product.description ?: "",
                                product.price.toString(),
                            )
                        )
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


@Composable
fun ProductDetailScreen(thumbnail: String, title: String, category: String, brand: String, description: String, price: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val thumbnail = thumbnail.replace("*", "/").replace("$", ":")
        Image(
            painter = rememberAsyncImagePainter(thumbnail),
            contentDescription = "Product Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("$title", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val capitalizedCategory = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        Text("Category: $capitalizedCategory", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Brand: $brand", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Price: $price$", fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Description:\n$description", fontSize = 14.sp)
    }
}