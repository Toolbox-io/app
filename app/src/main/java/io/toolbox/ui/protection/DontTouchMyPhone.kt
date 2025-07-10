package io.toolbox.ui.protection

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import io.toolbox.R
import io.toolbox.services.DontTouchMyPhoneService
import io.toolbox.services.DontTouchMyPhoneService.Companion.working
import io.toolbox.ui.LocalHazeState
import io.toolbox.ui.protection.actions.ActionsActivity
import ru.morozovit.android.copy
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.WindowInsetsHandler

@Composable
fun DontTouchMyPhoneScreen(@Suppress("LocalVariableName") EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .hazeSource(LocalHazeState())
            ) {
                val context = LocalContext()

                Category(margin = CategoryDefaults.margin.copy(top = 16.dp)) {
                    ListItem(
                        headline = stringResource(R.string.actions),
                        supportingText = stringResource(R.string.actions_d),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    ActionsActivity::class.java
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null
                            )
                        }
                    )
                }

                Button(
                    onClick = {
                        if (working) {
                            DontTouchMyPhoneService.stop()
                        } else {
                            DontTouchMyPhoneService.start(context)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (working)
                            stringResource(R.string.stop)
                        else
                            stringResource(R.string.start)
                    )
                }
            }
        }
    }
}