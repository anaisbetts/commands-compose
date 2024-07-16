package dev.anais.commands.example

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PokeApi {
    @GET("pokemon")
    suspend fun getPokemonList(@Query("offset") offset: Int, @Query("limit") limit: Int): PokemonListResult
}

@JsonClass(generateAdapter = true)
data class PokemonListResult (
    val count: Long,
    val next: String?,
    val previous: String?,
    val results: List<PokemonListItem>
)

@JsonClass(generateAdapter = true)
data class PokemonListItem (
    val name: String,
    val url: String
)

fun makeRetrofitClient(): PokeApi = lazy {
    val moshi = Moshi.Builder().build()

    Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PokeApi::class.java)
}.value
