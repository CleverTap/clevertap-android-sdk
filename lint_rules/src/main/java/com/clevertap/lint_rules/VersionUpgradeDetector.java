package com.clevertap.lint_rules;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.GradleScanner;
import com.android.tools.lint.detector.api.GradleContext;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VersionUpgradeDetector extends Detector implements GradleScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            VersionUpgradeDetector.class,
            Scope.GRADLE_SCOPE);

    public static final Issue VERSION_UPGRADE_SUPPORT = Issue.create(
            "GradleDepsError", //$NON-NLS-1$
            "Gradle Version Upgrade Support Issues",
            "Gradle is highly flexible, and there are things you can do in Gradle files which " +
                    "can make it hard or impossible for IDEs to properly handle the project. This lint " +
                    "check looks for constructs that potentially break IDE support.",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION);

    @Override
    public void beforeCheckFile(@NotNull final Context context) {
        super.beforeCheckFile(context);
        System.out.println("file=" + context.file.getAbsolutePath());
    }

    @Override
    public void beforeCheckRootProject(@NotNull final Context context) {
        super.beforeCheckRootProject(context);
        for (File file : context.getMainProject().getDir().listFiles()) {
            if (file.getPath().contains("dependencies")) {
                GradleContext gradleContext = new GradleContext(context.getClient().getGradleVisitor(),
                        context.getDriver(), context.getProject(), context.getMainProject(), file);

                List<GradleScanner> scanners = new ArrayList<>();
                scanners.add(this);
                context.getClient().getGradleVisitor().visitBuildScript(gradleContext,
                        scanners);
                System.out.println("found");
            }
        }
    }

    @Override
    public void checkDslPropertyAssignment(@NotNull final GradleContext context, @NotNull final String property,
            @NotNull final String value, @NotNull final String parent, @Nullable final String parentParent,
            @NotNull final Object propertyCookie, @NotNull final Object valueCookie,
            @NotNull final Object statementCookie) {
        super.checkDslPropertyAssignment(context, property, value, parent, parentParent, propertyCookie, valueCookie,
                statementCookie);

        System.out.println("parent=" + parent);
    }

    @Override
    public void run(@NotNull final Context context) {
        super.run(context);
        System.out.println("RUN");
    }

    @Override
    public void visitBuildScript(@NotNull final Context context) {
        super.visitBuildScript(context);
    }

}