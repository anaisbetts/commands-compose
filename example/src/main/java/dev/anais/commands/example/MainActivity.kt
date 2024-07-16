@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.anais.commands.example

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.anais.commands.example.ui.theme.ExampleAppTheme
import dev.anais.commands.rememberCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun PokemonExamplePage() {
    var page by remember { mutableIntStateOf(0) }

    //
    // Commands aren't just for button clicks! They can be used to manage any
    // async state that you want to recompose as its progress changes
    //
    // In our block, we can call any suspendable function
    //
    val loadPokemonByPage = rememberCommand(key = page) { fetchPokemonList(page) }

    //
    // Since loadPokemonByPage's key is tied to the page number, it will
    // update when the page changes. So, every time loadPokemonByPage
    // updates, we want to run it to load new data
    //
    LaunchedEffect(loadPokemonByPage) {
        loadPokemonByPage.tryRun()
    }

    //
    // This is a great pattern to handle all three states of a command!
    //
    when {
        // Pending
        loadPokemonByPage.isRunning -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Thinking about Pokemon!", style = MaterialTheme.typography.headlineLarge)
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        }

        // Failed
        loadPokemonByPage.hasFailed -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // We could fetch the Exception text or type out of
                    // loadPokemonByPage.result here to display more detailed
                    // information
                    Text("It Just Didn't")
                    Text("🙁")
                }
            }
        }
        // It worked
        loadPokemonByPage.hasValue -> {
            loadPokemonByPage.result?.getOrNull()?.let {
                Column(modifier = Modifier.fillMaxSize()) {
                    val pokemon = it.items

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(R.drawable.pokeapi), contentDescription = null, modifier = Modifier.padding(vertical = 16.dp).height(48.dp))
                    }

                    LazyColumn(modifier = Modifier.weight(1.0f)) {
                        items(pokemon.size) { i -> PokemonListTile(pokemon[i], modifier = Modifier.fillMaxWidth()) }
                    }

                    Row(modifier = Modifier.padding(16.dp)) {
                        // Here, we rely on loadPokemonByPage's key to update when the page changes
                        Button(onClick = { page-- }, enabled = it.hasPrev) {
                            Text("Previous")
                        }

                        Button(onClick = { page++ }, enabled = it.hasNext, modifier = Modifier.padding(start = 16.dp)) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonListTile(item: PokemonInfo, modifier: Modifier = Modifier) {
    //
    // Here's a more traditional use of rememberCommand, where we'll tie it to a
    // button press. This incidentally guarantees that we will only have one instance
    // in-flight, meaning that we can't be playing the same sound concurrently
    //
    val playSound = rememberCommand("") { playSound(item.cries.latest) }

    // After 5 seconds of displaying an error, reset so we can try again
    LaunchedEffect(playSound.result?.isFailure) {
        if (!playSound.hasFailed) return@LaunchedEffect

        delay(5*1000)

        //
        // This method resets the command to the same state as before we ran it
        //
        playSound.reset()
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        AsyncImage(
            model = item.sprites.frontDefault,
            contentDescription = null,
            modifier = Modifier
                .width(192.dp)
                .height(192.dp)
        )

        Column(modifier = Modifier.weight(1.0f)) {
            Text(item.name, style = MaterialTheme.typography.headlineLarge)
            Text("Pokemon #${item.id}", style = MaterialTheme.typography.bodyLarge)
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            //
            // Another example of the Command's "three states" pattern here!
            //
            when {
                playSound.hasFailed ->
                    Text("😤", style = MaterialTheme.typography.headlineLarge)
                else ->
                    Button(onClick = playSound::tryRun, enabled = !playSound.isRunning) {
                        Text("Cry!")
                    }
            }
        }
    }
}

data class PokemonList(
    val items: List<PokemonInfo>,
    val hasNext: Boolean,
    val hasPrev: Boolean,
)

// NB: Don't be Annoying to the PokeAPI maintainers!
val limitConcurrency = Dispatchers.IO.limitedParallelism(4)

suspend fun fetchPokemonList(page: Int): PokemonList {
    val client = makeRetrofitClient()
    val offset = page * 20
    val list = client.getPokemonList(offset, 20)

    // NB: To test the error handling in PokemonExamplePage, uncomment this line
    //throw Exception("no")

    val items = coroutineScope {
        list.results.map {
            async(limitConcurrency) { client.getPokemon(it.name) }
        }.awaitAll()
    }

    return PokemonList(items, list.next != null, list.previous != null)
}

suspend fun playSound(soundUrl: String) {
    val mp = MediaPlayer()

    try {
        suspendCancellableCoroutine { task ->
            // NB: To test the error handling code in PokemonListTile, uncomment these lines
            //task.resumeWithException(Exception("no"))
            //return@suspendCancellableCoroutine

            mp.apply {
                setDataSource(soundUrl)
                setOnPreparedListener { start() }
                setOnCompletionListener { task.resume(Unit) }

                prepareAsync()

                setOnErrorListener { _, what, _ ->
                    task.resumeWithException(Exception("Failed to play sound: $what"))
                    true
                }

                task.invokeOnCancellation { stop() }
            }
        }
    } finally {
        mp.release()
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExampleAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        PokemonExamplePage()
                    }
                }
            }
        }
    }
}
