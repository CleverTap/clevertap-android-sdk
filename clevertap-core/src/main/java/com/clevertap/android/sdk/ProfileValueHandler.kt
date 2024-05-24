package com.clevertap.android.sdk

import com.clevertap.android.sdk.ProfileValueHandler.NumberValueType.DOUBLE_NUMBER
import com.clevertap.android.sdk.ProfileValueHandler.NumberValueType.FLOAT_NUMBER
import com.clevertap.android.sdk.ProfileValueHandler.NumberValueType.INT_NUMBER
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import org.json.JSONArray

class ProfileValueHandler(
    private val validator: Validator,
    private val validationResultStack: ValidationResultStack
) {

    internal enum class NumberValueType {
        INT_NUMBER,
        FLOAT_NUMBER,
        DOUBLE_NUMBER
    }

    private var numberValueType: NumberValueType? = null
    fun handleIncrementDecrementValues(value: Number, command: String, existingValue: Number?): Number? {
        var updatedValue: Number? = null

        /*When existing value is NOT present in local data store,
         we check the give value number type and do the necessary operation*/if (existingValue == null) {
            when (getNumberValueType(value)) {
                DOUBLE_NUMBER -> if (command == Constants.COMMAND_INCREMENT) {
                    updatedValue = value.toDouble()
                } else if (command == Constants.COMMAND_DECREMENT) {
                    updatedValue = -value.toDouble()
                }

                FLOAT_NUMBER -> if (command == Constants.COMMAND_INCREMENT) {
                    updatedValue = value.toFloat()
                } else if (command == Constants.COMMAND_DECREMENT) {
                    updatedValue = -value.toFloat()
                }

                else -> if (command == Constants.COMMAND_INCREMENT) {
                    updatedValue = value.toInt()
                } else if (command == Constants.COMMAND_DECREMENT) {
                    updatedValue = -value.toInt()
                }
            }
            return updatedValue
        }
        when (getNumberValueType(existingValue)) {
            DOUBLE_NUMBER -> if (command == Constants.COMMAND_INCREMENT) {
                updatedValue = existingValue.toDouble() + value.toDouble()
            } else if (command == Constants.COMMAND_DECREMENT) {
                updatedValue = existingValue.toDouble() - value.toDouble()
            }

            FLOAT_NUMBER -> if (command == Constants.COMMAND_INCREMENT) {
                updatedValue = existingValue.toFloat() + value.toFloat()
            } else if (command == Constants.COMMAND_DECREMENT) {
                updatedValue = existingValue.toFloat() - value.toFloat()
            }

            else -> if (command == Constants.COMMAND_INCREMENT) {
                updatedValue = existingValue.toInt() + value.toInt()
            } else if (command == Constants.COMMAND_DECREMENT) {
                updatedValue = existingValue.toInt() - value.toInt()
            }
        }
        return updatedValue
    }

    /*
        Based on the number value type returns the associated enum
        (INT_NUMBER,DOUBLE_NUMBER,FLOAT_NUMBER)
    */
    private fun getNumberValueType(value: Number): NumberValueType? {
        if (value == value.toInt()) {
            numberValueType = INT_NUMBER
        } else if (value == value.toDouble()) {
            numberValueType = DOUBLE_NUMBER
        } else if (value == value.toFloat()) {
            numberValueType = FLOAT_NUMBER
        }
        return numberValueType
    }

    fun computeMultiValues(key: String, values: JSONArray?, command: String, existingValues: Any?): JSONArray? {
        val currentValues = _constructExistingMultiValue(key, command, existingValues)
        val newValues = _cleanMultiValues(key, CTJsonConverter.toList(values!!) as ArrayList<String>)
        if (currentValues == null || newValues == null) {
            return null
        }
        val mergeOperation =
            if (command == Constants.COMMAND_REMOVE)
                Validator.REMOVE_VALUES_OPERATION
            else
                Validator.ADD_VALUES_OPERATION

        // merge currentValues and newValues
        val vr = validator.mergeMultiValuePropertyForKey(currentValues, newValues, mergeOperation, key)

        // Check for an error
        if (vr.errorCode != 0) {
            validationResultStack.pushValidationResult(vr)
        }
        val localValues = vr.getObject() as JSONArray
        return if (localValues.length() <= 0) {
            null
        } else localValues
    }

    private fun _constructExistingMultiValue(key: String, command: String, existing: Any?): JSONArray? {
        val remove = command == Constants.COMMAND_REMOVE
        val add = command == Constants.COMMAND_ADD

        // only relevant for add's and remove's; a set overrides the existing value, so return a new array
        if (!remove && !add) {
            return JSONArray()
        }

        // if there is no existing value
        if (existing == null) {
            // if its a remove then return null to abort operation
            // no point in running remove against a nonexistent value
            return if (remove) {
                null
            } else JSONArray()

            // otherwise return an empty array
        }

        // value exists
        // the value should only ever be a JSONArray or scalar (String really)
        // if its already a JSONArray return that
        if (existing is JSONArray) {
            return existing
        }

        // handle a scalar value as the existing value
        /*
            if its an add, our rule is to promote the scalar value to multi value and include the cleaned stringified
            scalar value as the first element of the resulting array

            NOTE: the existing scalar value is currently limited to 120 bytes; when adding it to a multi value
            it is subject to the current 40 byte limit

            if its a remove, our rule is to delete the key from the local copy
            if the cleaned stringified existing value is equal to any of the cleaned values passed to the remove method

            if its an add, return an empty array as the default,
            in the event the existing scalar value fails stringifying/cleaning

            returning null will signal that a remove operation should be aborted,
            as there is no valid promoted multi value to remove against
         */
        val _default = if (add) JSONArray() else null
        val stringified = stringifyAndCleanScalarProfilePropValue(existing)
        return if (stringified != null) JSONArray().put(stringified) else _default
    }

    private fun _cleanMultiValues(key: String, values: ArrayList<String>?): JSONArray? {
        return try {
            if (values == null) {
                return null
            }
            val cleanedValues = JSONArray()
            var vr: ValidationResult

            // loop through and clean the new values
            for (value in values) {

                // validate value
                vr = validator.cleanMultiValuePropertyValue(value)

                // Check for an error
                if (vr.errorCode != 0) {
                    validationResultStack.pushValidationResult(vr)
                }

                // reset the value
                val _value = vr.getObject()
                val cleanedValue = if (_value != null) vr.getObject().toString() else null

                // if value is empty return
                if (value.isEmpty()) {
                    generateEmptyMultiValueError(key)
                    return null
                }
                // add to the newValues to be merged
                cleanedValues.put(cleanedValue)
            }
            cleanedValues
        } catch (t: Throwable) {
            Logger.v("Error cleaning multi values for key $key", t)
            generateEmptyMultiValueError(key)
            null
        }
    }

    private fun generateEmptyMultiValueError(key: String?) {
        val error = ValidationResultFactory.create(512, Constants.INVALID_MULTI_VALUE, key)
        validationResultStack.pushValidationResult(error)
        Logger.v(error.errorDesc)
    }

    private fun stringifyAndCleanScalarProfilePropValue(value: Any): String? {
        var cleanedValue = CTJsonConverter.toJsonString(value)
        if (cleanedValue != null) {
            val vr = validator.cleanMultiValuePropertyValue(cleanedValue)

            // Check for an error
            if (vr.errorCode != 0) {
                validationResultStack.pushValidationResult(vr)
            }
            val _value = vr.getObject()
            cleanedValue = if (_value != null) vr.getObject().toString() else null
        }
        return cleanedValue
    }
}
