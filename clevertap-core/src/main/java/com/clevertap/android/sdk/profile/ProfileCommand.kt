package com.clevertap.android.sdk.profile
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.profile.merge.MergeOperation

/**
 * Maps profile command strings to their corresponding merge operations.
 * This provides a centralized mapping between API commands (like "$add", "$incr")
 * and the internal ProfileStateMerger operations.
 *
 * @property commandString The command string used in the API (e.g., "$add", "$incr")
 * @property operation The corresponding MergeOperation
 */
internal enum class ProfileCommand(
    val commandString: String,
    val operation: MergeOperation
) {
    SET(Constants.COMMAND_SET, MergeOperation.UPDATE),
    ADD(Constants.COMMAND_ADD, MergeOperation.ARRAY_ADD),
    REMOVE(Constants.COMMAND_REMOVE, MergeOperation.ARRAY_REMOVE),
    DELETE(Constants.COMMAND_DELETE, MergeOperation.DELETE),
    INCREMENT(Constants.COMMAND_INCREMENT, MergeOperation.INCREMENT),
    DECREMENT(Constants.COMMAND_DECREMENT, MergeOperation.DECREMENT);
}