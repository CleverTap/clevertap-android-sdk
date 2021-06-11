package com.clevertap.lint_rules_detekt

import com.clevertap.lint_rules_detekt.rules.DependencyUpdateCheckRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class DependencyUpdateRulesProvider : RuleSetProvider {

    override val ruleSetId: String = "dependencyupdatecheck"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            DependencyUpdateCheckRule()
        )
    )
}