# commands-compose

[![](https://jitpack.io/v/anaisbetts/commands-compose.svg)](https://jitpack.io/#anaisbetts/commands-compose)


Commands are a new primitive for writing Jetpack Compose components that invoke asynchronous methods easily. If you've ever tried to write a bunch of inProgress remembers to manage the state of running code in the background, this is much better and easier!

Commands also automatically ensure that only one invocation of the method is running concurrently, and makes it really easy to write pending and error states.

## How to Get It

This library uses JitPack, Maven Central is too cursed:

```gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

then in your app's build.gradle:

```gradle
dependencies {
  implementation 'com.github.anaisbetts:commands-compose:VERSION'
}
```

## Show me an example!

```kotlin
@Composable
fun PokemonLookupPage() {
  var pokemonToFind by remember { mutableStateOf("") }

  val search = rememberCommand(key = pokemonToFind) {
    if (pokemonToFind.length < 3) {
      return@rememberCommand emptyList()
    }

    // This is a suspend function!
    fetchPokemonByName(pokemonToFind)
  }

  Column {
    TextField(
      value = pokemonToFind,
      onValueChange = { pokemonToFind = it }
    )

    // Here, we can easily set up our UI based on the async state of
    // our Command
    when {
      search.isRunning -> Text("Searching..."),

      search.result.isFailure -> Text("It didn't work!"),

      default -> {
        LazyColumn {
          items(search.result?.getOrNull() ?: emptyList()) { pokemon ->
            Text("${pokemon.Name} - ${pokemon.Information}")
          }
        }
      }
    }

    Button(
      onClick = search::tryRun,
      enabled = !search.isRunning
    ) {
      Text("Search for Pokemon")
    }
  }

}
```