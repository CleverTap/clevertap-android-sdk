package com.clevertap.lint_rules_detekt.rules

import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity.CodeSmell
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class DependencyUpdateCheckRule : Rule() {

    override val issue = Issue(
        javaClass.simpleName,
        CodeSmell,
        """
                This rule checks with a central repository to see if there are newer \
                versions available for the dependencies used by this project.""",
        Debt.TWENTY_MINS
    )

    private var amount: Int = 0

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        if (amount > THRESHOLD) {
            report(
                io.gitlab.arturbosch.detekt.api.CodeSmell(
                    issue,
                    Entity.atPackageOrFirstDecl(file),
                    message = "The file ${file.name} has $amount function declarations. " +
                            "Threshold is specified with $THRESHOLD."
                )
            )
        }
        amount = 0
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        amount++
    }
}

const val THRESHOLD = 10