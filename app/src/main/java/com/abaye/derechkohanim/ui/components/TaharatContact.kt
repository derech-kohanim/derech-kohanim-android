package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * How to reach the Taharat HaKohanim association, the source of the zone data. The one place
 * these details live; the onboarding data screen and the settings contact dialog read them here.
 */
object TaharatContact {
    const val PHONE_DISPLAY = "08-313-2020"
    const val PHONE_DIAL = "083132020"
    const val EMAIL = "a083132020@gmail.com"

    /** Wraps a number or address in left-to-right isolates so it reads correctly in Hebrew. */
    fun ltr(text: String): String = "⁦$text⁩"

    fun dial(context: Context) {
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$PHONE_DIAL"))) }
    }

    fun email(context: Context) {
        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL"))) }
    }
}

/**
 * The association's contact, pinned under the settings menu: a royal-soft card with a phone
 * chip, the title and why to call, and two pills, the phone (dial) and the email (compose).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaharatContactCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = DkTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DkTheme.radius.lg),
        color = colors.primarySoft,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.primary, CircleShape),
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = colors.primaryContrast, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.taharat_contact_title),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.text,
                    )
                    Text(
                        text = stringResource(R.string.taharat_contact_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
                verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            ) {
                DkButton(
                    text = TaharatContact.ltr(TaharatContact.PHONE_DISPLAY),
                    onClick = { TaharatContact.dial(context) },
                    leadingIcon = Icons.Outlined.Phone,
                )
                DkButton(
                    text = TaharatContact.ltr(TaharatContact.EMAIL),
                    onClick = { TaharatContact.email(context) },
                    variant = DkButtonVariant.Secondary,
                    leadingIcon = Icons.Outlined.Email,
                )
            }
        }
    }
}
