package com.clevertap.android.sdk.profile
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.profile.merge.ProfileOperation

/**
 * Maps profile command strings to their corresponding merge operations.
 * This provides a centralized mapping between API commands (like "$add", "$incr")
 * and the internal ProfileStateTraverser operations.
 *
 * @property commandString The command string used in the API (e.g., "$add", "$incr")
 * @property operation The corresponding ProfileOperation
 */
internal enum class ProfileCommand(
    val commandString: String,
    val operation: ProfileOperation
) {
    SET(Constants.COMMAND_SET, ProfileOperation.UPDATE),
    ADD(Constants.COMMAND_ADD, ProfileOperation.ARRAY_ADD),
    REMOVE(Constants.COMMAND_REMOVE, ProfileOperation.ARRAY_REMOVE),
    DELETE(Constants.COMMAND_DELETE, ProfileOperation.DELETE),
    INCREMENT(Constants.COMMAND_INCREMENT, ProfileOperation.INCREMENT),
    DECREMENT(Constants.COMMAND_DECREMENT, ProfileOperation.DECREMENT);
}