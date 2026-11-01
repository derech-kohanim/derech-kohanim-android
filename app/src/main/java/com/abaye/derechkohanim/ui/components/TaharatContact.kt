package com.abaye.derechkohanim.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
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

/** The contact card: why to call them, then the phone and email rows, and a close button. */
@Composable
fun TaharatContactDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DkTheme.colors.surface,
        shape = RoundedCornerShape(DkTheme.radius.lg),
        title = {
            Text(
                text = stringResource(R.string.taharat_contact_title),
                style = MaterialTheme.typography.titleLarge,
                color = DkTheme.colors.text,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3)) {
                Text(
                    text = stringResource(R.string.taharat_contact_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DkTheme.colors.textBody,
                )
                DkListBox {
                    DkListRow(
                        title = stringResource(R.string.taharat_contact_phone),
                        supporting = TaharatContact.ltr(TaharatContact.PHONE_DISPLAY),
                        icon = Icons.Outlined.Phone,
                        onClick = { TaharatContact.dial(context) },
                    )
                    DkListDivider()
                    DkListRow(
                        title = stringResource(R.string.taharat_contact_email),
                        supporting = TaharatContact.ltr(TaharatContact.EMAIL),
                        icon = Icons.Outlined.Email,
                        onClick = { TaharatContact.email(context) },
                    )
                }
            }
        },
        confirmButton = {
            DkButton(text = stringResource(R.string.action_close), onClick = onDismiss, variant = DkButtonVariant.Secondary)
        },
    )
}
