package ru.morozovit.ultimatesecurity.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import ru.morozovit.android.Button
import ru.morozovit.android.License
import ru.morozovit.android.Website
import ru.morozovit.android.invoke
import ru.morozovit.android.openUrl
import ru.morozovit.ultimatesecurity.BuildConfig
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.AppIcon
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@Composable
@PhonePreview
fun AboutScreen() {
    val context = LocalContext()

    WindowInsetsHandler {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(Modifier.padding(top = 20.dp)) {
                AppIcon(modifier = Modifier.size(150.dp))
            }
            Text(
                text = stringResource(R.string.app_name),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.app_desc_l),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 10.dp),
                textAlign = TextAlign.Center
            )

            val osl = stringResource(R.string.osl)

            Column(Modifier.width(IntrinsicSize.Max)) {
                Button(
                    onClick = {
                        OssLicensesMenuActivity.setActivityTitle(osl)
                        context.startActivity(
                            Intent(context, OssLicensesMenuActivity::class.java)
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.License,
                            contentDescription = osl
                        )
                    },
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    Text(osl)
                }

                Button(
                    onClick = {
                        context.openUrl("toolbox-io.ru")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Website,
                            contentDescription = stringResource(R.string.website)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.website))
                }

                Button(
                    onClick = {
                        context.openUrl(
                            "https://github.com/denis0001-dev/Toolbox-io-Website/issues/new" +
                                    "?assignees=denis0001-dev&labels=app%2C+bug" +
                                    "&projects=&template=application-bug-report.md&title="
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = stringResource(R.string.report_error)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.report_error))
                }
            }
        }
    }
}