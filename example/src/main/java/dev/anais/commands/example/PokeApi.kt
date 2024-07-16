package dev.anais.commands.example

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApi {
    @GET("pokemon")
    suspend fun getPokemonList(@Query("offset") offset: Int, @Query("limit") limit: Int): PokemonListResult

    @GET("pokemon/{name}")
    suspend fun getPokemon(@Path("name") name: String): PokemonInfo
}

fun makeRetrofitClient(): PokeApi = lazy {
    val moshi = Moshi.Builder().build()

    Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PokeApi::class.java)
}.value

@JsonClass(generateAdapter = true)
data class PokemonListResult (
    val count: Long,
    val next: String?,
    val previous: String?,
    val results: List<NamedLink>
)

@JsonClass(generateAdapter = true)
data class NamedLink (
    val name: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class PokemonInfo (
    @Json(name = "base_experience") val baseExperience: Long?,
    val cries: Cries,
    val forms: List<NamedLink>?,
    val height: Long?,
    val id: Long,
    @Json(name = "is_default") val isDefault: Boolean?,
    @Json(name = "location_area_encounters") val locationAreaEncounters: String?,
    val moves: List<Move>,
    val name: String,
    val order: Long?,
    val species: NamedLink?,
    val sprites: Sprites,
    val stats: List<Stat>,
    val types: List<Type>,
    val weight: Long?
)

@JsonClass(generateAdapter = true)
data class Cries (
    val latest: String,
    val legacy: String
)

@JsonClass(generateAdapter = true)
data class Move (
    val move: NamedLink,
    @Json(name = "version_group_details") val versionGroupDetails: List<VersionGroupDetail>?
)

@JsonClass(generateAdapter = true)
data class Sprites (
    @Json(name = "back_default") val backDefault: String?,
    @Json(name = "back_female") val backFemale: String?,
    @Json(name = "back_shiny") val backShiny: String?,
    @Json(name = "back_shiny_female") val backShinyFemale: String?,
    @Json(name = "front_default") val frontDefault: String?,
    @Json(name = "front_female") val frontFemale: String?,
    @Json(name = "front_shiny") val frontShiny: String?,
    @Json(name = "front_shiny_female") val frontShinyFemale: String?
)

@JsonClass(generateAdapter = true)
data class Stat (
    @Json(name = "base_stat") val baseStat: Long?,
    val effort: Long?,
    val stat: NamedLink
)

@JsonClass(generateAdapter = true)
data class Type (
    val slot: Long,
    val type: NamedLink
)

@JsonClass(generateAdapter = true)
data class VersionGroupDetail (
    @Json(name = "level_learned_at") val levelLearnedAt: Long,
    @Json(name = "move_learn_method") val moveLearnMethod: NamedLink,
    @Json(name = "version_group") val versionGroup: NamedLink
)
