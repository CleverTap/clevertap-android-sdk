package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo

/**
 * Discriminator for the two inbox message origins.
 *
 * - [V1] — messages delivered passively via the `/a1` response under the
 *   `inbox_notifs` key. Device-local; never synced to the V2 server.
 * - [V2] — messages delivered actively via `POST /inbox/v2/getMessages`
 *   under the `inbox_notifs_v2` key. Backed by FStore (cross-device).
 *
 * Lives only on the internal [CTMessageDAO] and the `inboxMessages.source`
 * column. Never written into any JSON exposed to the public
 * [CTInboxMessage] model.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class InboxMessageSource { V1, V2 }
