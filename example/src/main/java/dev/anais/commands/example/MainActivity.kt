package dev.anais.commands.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CircularProgressIndicator
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
import dev.anais.commands.example.ui.theme.ExampleAppTheme
import dev.anais.commands.rememberCommand
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Composable
fun PokemonExamplePage() {
    var page by remember { mutableIntStateOf(0) }

    val loadPokemonByPage = rememberCommand(key = page) {
        val client = makeRetrofitClient()
        client.getPokemonList(page * 20, 20)
    }

    LaunchedEffect("") {
        loadPokemonByPage.tryRun()
    }

    when {
        loadPokemonByPage.isRunning -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Thinking about Pokemon!")
                    CircularProgressIndicator()
                }
            }
        }
        loadPokemonByPage.result?.isFailure == true -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("It Just Didn't")
                    Text("🙁")
                }
            }
        }
        else -> {
            loadPokemonByPage.result?.getOrNull()?.let {
                val items = it.results

                LazyColumn {
                    items(items.size, key = { i -> items[i].name }) { i ->
                        Text(items[i].name)
                    }
                }
            }
        }
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
