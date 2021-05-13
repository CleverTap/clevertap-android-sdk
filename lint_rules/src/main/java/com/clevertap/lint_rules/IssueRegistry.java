package com.clevertap.lint_rules;

import static com.android.tools.lint.detector.api.ApiKt.CURRENT_API;

import com.android.tools.lint.detector.api.Issue;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class IssueRegistry extends com.android.tools.lint.client.api.IssueRegistry {

    @Override
    public int getApi() {
        return CURRENT_API;
    }

    @NotNull
    @Override
    public List<Issue> getIssues() {
        ArrayList<Issue> issueArrayList = new ArrayList<>();
        issueArrayList.add(VersionUpgradeDetector.VERSION_UPGRADE_SUPPORT);

        return issueArrayList;
    }

}
