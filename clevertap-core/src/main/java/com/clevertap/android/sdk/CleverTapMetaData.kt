package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/*
Marker interface for future abstraction purposes.
Example - If any other module uses MetaData, then they can extend this class and common
properties can be abstracted.
 */
// Public (restricted) only because the wrapper-used CoreMetaData extends it: Kotlin does not
// allow a public class to extend a less visible one.
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public abstract class CleverTapMetaData
