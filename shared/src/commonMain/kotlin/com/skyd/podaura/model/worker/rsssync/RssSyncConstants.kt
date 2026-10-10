package com.skyd.podaura.model.worker.rsssync

import kotlin.time.Duration

/** Platform refresh budget; infinite when the native scheduler controls cancellation. */
expect val RSS_SYNC_TIMEOUT: Duration
