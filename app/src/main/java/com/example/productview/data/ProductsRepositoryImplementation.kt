package com.example.productview.data

import com.example.productview.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class ProductsRepositoryImplementation(
    private val api: Api
): ProductsRepository  {
    override suspend fun getProductsList(): Flow<Result<List<Product>>> {
        return flow {
            val productsFromApi = try {
                api.getProductsList()
            } catch (e: IOException) {
                e.printStackTrace()
                emit(Result.Error(message = "Error loading products"))
                return@flow
            } catch (e: HttpException) {
                e.printStackTrace()
                emit(Result.Error(message = "Error loading products"))
                return@flow
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Result.Error(message = "Error loading products"))
                return@flow
            }

            emit(Result.Success(productsFromApi.products))
        }
    }
}