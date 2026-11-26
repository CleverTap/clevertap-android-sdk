package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.FunctionContext
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext

/**
 * Base interface for template presenters. See [TemplatePresenter] and [FunctionPresenter].
 */
public fun interface CustomTemplatePresenter<C : CustomTemplateContext> {

    /**
     * Called when a [CustomTemplate] should be presented or a function should be executed. For visual templates
     * (code templates or functions with [CustomTemplate.isVisual]` = true`): Implementing classes should use the provided
     * [CustomTemplateContext] methods [setPresented][CustomTemplateContext.setPresented] and
     * [setDismissed][TemplateContext.setDismissed] to notify the SDK of the state of the template invocation. Only
     * one visual template or other InApp message can be displayed at a time by the SDK and no new messages can be
     * shown until the current one is dismissed.
     */
    public fun onPresent(context: C)
}

/**
 * A handler of code templates [CustomTemplate] invocations. Its methods are called when the corresponding InApp
 * message should be presented to the user or closed.
 */
public interface TemplatePresenter : CustomTemplatePresenter<TemplateContext> {

    public fun onClose(context: TemplateContext)
}

/**
 * A handler of function [CustomTemplate] invocations, called when the function should be executed.
 * See [onPresent].
 */
public fun interface FunctionPresenter : CustomTemplatePresenter<FunctionContext>
