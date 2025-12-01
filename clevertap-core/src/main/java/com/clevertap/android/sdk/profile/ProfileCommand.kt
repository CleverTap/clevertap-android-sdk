package com.clevertap.android.sdk.profile
import com.clevertap.android.sdk.Constants

/**
 * Maps profile command strings to their corresponding merge operations.
 * This provides a centralized mapping between API commands (like "$add", "$incr")
 * and the internal ProfileStateMerger operations.
 *
 * @property commandString The command string used in the API (e.g., "$add", "$incr")
 * @property operation The corresponding ProfileStateMerger.MergeOperation
 */
internal enum class ProfileCommand(
    val commandString: String,
    val operation: ProfileStateMerger.MergeOperation
) {
    SET(Constants.COMMAND_SET, ProfileStateMerger.MergeOperation.UPDATE),
    ADD(Constants.COMMAND_ADD, ProfileStateMerger.MergeOperation.ARRAY_ADD),
    REMOVE(Constants.COMMAND_REMOVE, ProfileStateMerger.MergeOperation.ARRAY_REMOVE),
    DELETE(Constants.COMMAND_DELETE, ProfileStateMerger.MergeOperation.DELETE),
    INCREMENT(Constants.COMMAND_INCREMENT, ProfileStateMerger.MergeOperation.INCREMENT),
    DECREMENT(Constants.COMMAND_DECREMENT, ProfileStateMerger.MergeOperation.DECREMENT);

    companion object {
        /**
         * Converts a command string to its corresponding MergeOperation.
         *
         * @param command The command string (e.g., "$add", "$incr")
         * @return The corresponding MergeOperation, or null if not found
         */
        @JvmStatic
        fun fromCommand(command: String?): ProfileStateMerger.MergeOperation? {
            if (command == null) return null
            return values().firstOrNull { it.commandString == command }?.operation
        }
    }
}