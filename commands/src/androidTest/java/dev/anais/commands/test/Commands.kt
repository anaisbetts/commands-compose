package dev.anais.commands.test

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.anais.commands.rememberCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComposeRunnerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theMostBasicOfBasicTests() {
        runBlocking {
            val flow = MutableSharedFlow<Boolean>()

            // Create the Jetpack Compose view
            val myButton = @Composable {
                Log.i("CommandRunner", "Composing our test!")

                val onClickCommand = rememberCommand("") {
                    flow.take(1).collect()
                    return@rememberCommand true
                }

                Log.i("CommandRunner", "running! ${onClickCommand.isRunning}")

                TextButton(onClick = onClickCommand::tryRun, modifier = Modifier.testTag("button")) {
                    if (onClickCommand.isRunning) {
                        Text("Loading...")
                    } else if (onClickCommand.result?.isFailure == true) {
                        Text("Didn't work: ${onClickCommand.result!!.exceptionOrNull()!!.message}")
                    } else if (onClickCommand.result == null) {
                        Text("Click me")
                    } else {
                        Text("We did it!")
                    }
                }
            }

            // Set the view as the content of the Activity
            composeTestRule.setContent {
                myButton()
            }

            // Find the Button view and perform a click action
            composeTestRule.onNodeWithTag("button").performClick()

            composeTestRule.awaitIdle()

            // We haven't signaled the flow, so we should be pending
            composeTestRule.onNodeWithText("Loading...").assertExists()

            flow.emit(true)

            // Now we signaled, so we find the result
            composeTestRule.onNodeWithText("We did it!").assertExists()
        }
    }

    @Test
    fun invokingTheTryRunMethodMultipleTimesIsIgnored() {
        var callCount = 0

        runBlocking {
            val flow = MutableSharedFlow<Boolean>()

            // Create the Jetpack Compose view
            val myButton = @Composable {
                Log.i("CommandRunner", "Composing our test!")

                val onClickCommand = rememberCommand("") {
                    callCount++
                    flow.take(1).collect()
                    return@rememberCommand true
                }

                Log.i("CommandRunner", "running! ${onClickCommand.isRunning}")

                TextButton(onClick = onClickCommand::tryRun, modifier = Modifier.testTag("button")) {
                    if (onClickCommand.isRunning) {
                        Text("Loading...")
                    } else if (onClickCommand.result?.isFailure == true) {
                        Text("Didn't work: ${onClickCommand.result!!.exceptionOrNull()!!.message}")
                    } else if (onClickCommand.result == null) {
                        Text("Click me")
                    } else {
                        Text("We did it!")
                    }
                }
            }

            // Set the view as the content of the Activity
            composeTestRule.setContent {
                myButton()
            }

            assertEquals(0, callCount)

            // Find the Button view and perform a click action
            composeTestRule.onNodeWithTag("button").performClick()
            composeTestRule.awaitIdle()

            // We haven't signaled the flow, so we should be pending
            composeTestRule.onNodeWithText("Loading...").assertExists()
            assertEquals(1, callCount)

            // Click it again, but it should be ignored!
            composeTestRule.onNodeWithTag("button").performClick()
            composeTestRule.awaitIdle()

            assertEquals(1, callCount)

            // Finally complete our result
            flow.emit(true)
            composeTestRule.awaitIdle()

            assertEquals(1, callCount)

            // One more time, we click the button, but because we don't have a
            // pending action, it works
            composeTestRule.onNodeWithTag("button").performClick()
            composeTestRule.awaitIdle()

            assertEquals(2, callCount)
        }
    }

    @Test
    fun throwingShouldBeCaptured() {
        runBlocking {
            val flow = MutableSharedFlow<Boolean>()

            // Create the Jetpack Compose view
            val errMsg = "Aieeeee!"
            val myButton = @Composable {
                Log.i("CommandRunner", "Composing our test!")

                val onClickCommand = rememberCommand("") {
                    flow.take(1).collect()
                    error(errMsg)
                }

                Log.i("CommandRunner", "running! ${onClickCommand.isRunning}")

                TextButton(onClick = onClickCommand::tryRun, modifier = Modifier.testTag("button")) {
                    if (onClickCommand.isRunning) {
                        Text("Loading...")
                    } else if (onClickCommand.result?.isFailure == true) {
                        Text("Didn't work: ${onClickCommand.result!!.exceptionOrNull()!!.message}")
                    } else if (onClickCommand.result == null) {
                        Text("Click me")
                    } else {
                        Text("We did it!")
                    }
                }
            }

            // Set the view as the content of the Activity
            composeTestRule.setContent {
                myButton()
            }

            // Find the Button view and perform a click action
            composeTestRule.onNodeWithTag("button").performClick()

            composeTestRule.awaitIdle()

            // We haven't signaled the flow, so we should be pending
            composeTestRule.onNodeWithText("Loading...").assertExists()

            flow.emit(true)

            // Now we signaled, so we find the failed result
            composeTestRule.onNodeWithText("We did it!").assertDoesNotExist()
            composeTestRule.onNodeWithText("Didn't work: $errMsg").assertExists()
        }
    }
}
