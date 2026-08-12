package com.amchiyatri.rider.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the dialer pre-filled with [phoneNumber]; the user still has to tap call. This is
 * ACTION_DIAL (not ACTION_CALL), so it needs no CALL_PHONE runtime permission.
 */
fun Context.dialNumber(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    startActivity(intent)
}

/** An SMS intent to [phoneNumber], for the "message driver" action - opens the user's SMS app. */
fun smsIntent(phoneNumber: String): Intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
