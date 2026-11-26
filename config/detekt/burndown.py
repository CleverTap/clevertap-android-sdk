#!/usr/bin/env python3
"""Generate config/detekt/BASELINE_BURNDOWN.md from the per-module detekt baselines.

Usage (from the repo root): python3 config/detekt/burndown.py .
Baseline IDs look like `RuleName:FileName.kt$signature`.
"""
import glob
import os
import re
import sys
from collections import Counter, defaultdict

root = sys.argv[1]
MODULES = ["clevertap-core", "clevertap-geofence", "clevertap-hms", "clevertap-pushtemplates"]

# rule -> (category, how to fix)
GUIDE = {
    "UnsafeCallOnNullableType": ("Null safety", "Single read + `?:`/`?.let`/early return; `requireNotNull(x) { \"why\" }` where a crash is intended."),
    "MapGetWithNotNullAssertionOperator": ("Null safety", "`map.getValue(k)` or explicit null handling."),
    "UnnecessaryNotNullOperator": ("Null safety", "Drop the `!!` on a non-null value."),
    "UnnecessarySafeCall": ("Null safety", "Drop `?.` on a non-null value."),
    "CanBeNonNullable": ("Null safety", "Make the declaration non-null (often `lateinit` or a constructor `val`)."),
    "NullableToStringCall": ("Null safety", "Handle null explicitly instead of printing \"null\"."),
    "UseRequireNotNull": ("Idioms", "`requireNotNull(x)` instead of `require(x != null)`."),
    "UseCheckNotNull": ("Idioms", "`checkNotNull(x)` instead of `check(x != null)`."),
    "UseCheckOrError": ("Idioms", "`check(cond)`/`error(msg)` instead of `throw IllegalStateException`."),
    "UseRequire": ("Idioms", "`require(cond)` instead of `throw IllegalArgumentException`."),
    "UseLet": ("Idioms", "`x?.let { }` instead of if/else with a null branch."),
    "UnnecessaryLet": ("Idioms", "Remove a `let` that adds nothing."),
    "UseIsNullOrEmpty": ("Idioms", "`isNullOrEmpty()`."),
    "UseOrEmpty": ("Idioms", "`orEmpty()`."),
    "VarCouldBeVal": ("Idioms", "`val` for never-reassigned locals/properties."),
    "BracesOnIfStatements": ("Style", "Braces on every if/else (repo convention)."),
    "MagicNumber": ("Style", "Named constants."),
    "MaxLineLength": ("Style", "Wrap lines > 120."),
    "ReturnCount": ("Style", "Guard clauses / `when` expressions; max 2 non-guard returns."),
    "ForbiddenMethodCall": ("Style", "Log through `Logger`, no `Thread.sleep`/`println`."),
    "FunctionNaming": ("Naming", "Kotlin camelCase function names (not for public API / reflection names)."),
    "VariableNaming": ("Naming", "camelCase, no `m`/`_` prefixes (not for keys/reflection names)."),
    "FunctionParameterNaming": ("Naming", "camelCase parameters."),
    "PackageNaming": ("Naming", "Lower-case package segments (e.g. `product_config`, `featureFlags`) - public API packages, needs a deprecation plan."),
    "ClassNaming": ("Naming", "PascalCase classes."),
    "PrintStackTrace": ("Exceptions", "Log through `Logger`."),
    "ImplicitDefaultLocale": ("Null safety", "Pass `Locale.ROOT`/`Locale.US` for keys and identifiers (Turkish dotless-i); keep the default locale only for user-facing text."),
    "UnsafeCast": ("Null safety", "The cast can never succeed - fix the type."),
    "ForbiddenVoid": ("Idioms", "`Unit` instead of `Void` outside Java generic type arguments."),
    "MayBeConst": ("Idioms", "`const val` so Java gets a static final field."),
    "EmptyCatchBlock": ("Exceptions", "Name a deliberately ignored exception `ignored`/`expected`/`_`, or log it via Logger."),
    "TooGenericExceptionThrown": ("Exceptions", "Throw a specific exception type."),
    "ThrowingExceptionsWithoutMessageOrCause": ("Exceptions", "Add a message/cause."),
    "LongMethod": ("Complexity", "Extract private functions (threshold 30 lines)."),
    "LargeClass": ("Complexity", "Split responsibilities (threshold 300 lines)."),
    "LongParameterList": ("Complexity", "Parameter objects / default args."),
    "NestedBlockDepth": ("Complexity", "Guard clauses, extract functions (threshold 3)."),
    "CyclomaticComplexMethod": ("Complexity", "Split / `when` tables (threshold 10)."),
    "CognitiveComplexMethod": ("Complexity", "Split / flatten."),
    "TooManyFunctions": ("Complexity", "Split the type/file (threshold 15)."),
    "ComplexCondition": ("Complexity", "Name the sub-conditions."),
    "UndocumentedPublicClass": ("Public docs", "KDoc on public types (start with bucket-A API; @RestrictTo types can stay brief)."),
    "UndocumentedPublicFunction": ("Public docs", "KDoc on public functions."),
    "UndocumentedPublicProperty": ("Public docs", "KDoc on public properties."),
    "ForbiddenPublicDataClass": ("Library safety", "Public data classes break binary compatibility when a field is added; use a regular class with explicit equals/hashCode."),
    "LibraryCodeMustSpecifyReturnType": ("Library safety", "Explicit return types on public API (or enable explicitApi())."),
    "DoubleMutabilityForCollection": ("Library safety", "`val` + mutable collection, or `var` + read-only collection."),
}
ORDER = ["Null safety", "Idioms", "Exceptions", "Library safety", "Naming", "Style", "Complexity", "Public docs", "Other"]

per_rule = Counter()
per_rule_module = defaultdict(Counter)
per_rule_files = defaultdict(Counter)
per_module = Counter()
files_seen = {}
for m in MODULES:
    paths = sorted(glob.glob(os.path.join(root, m, "detekt-baseline*.xml")))
    files_seen[m] = [os.path.basename(p) for p in paths]
    for p in paths:
        for mm in re.finditer(r"<ID>([^<]+)</ID>", open(p, encoding="utf-8").read()):
            ident = mm.group(1)
            rule, _, rest = ident.partition(":")
            fname = rest.split("$", 1)[0] or "?"
            per_rule[rule] += 1
            per_rule_module[rule][m.replace("clevertap-", "")] += 1
            per_rule_files[rule][fname] += 1
            per_module[m] += 1

total = sum(per_rule.values())
out = []
out.append("# detekt baseline burn-down (SDK-6378)\n")
out.append("Generated from the per-module baselines (`<module>/detekt-baseline-release.xml`) on the post-migration code "
           "(feat/kotlin-migration + SDK-6375 idiomatic pass). Everything below is **ignored by the build today**; "
           "new code must not add to it. Regenerate after fixing: `./gradlew :<module>:detektBaselineRelease`, then "
           "`python3 config/detekt/burndown.py .` to refresh this file.\n")
out.append(f"**Total baselined findings: {total}**  \n" + " · ".join(f"{m.replace('clevertap-', '')}: {per_module[m]}" for m in MODULES) + "\n")
out.append("Baseline files: " + "; ".join(f"`{m}/{f}`" for m in MODULES for f in files_seen[m]) + "\n")

by_cat = defaultdict(list)
for rule, n in per_rule.items():
    cat = GUIDE.get(rule, ("Other", ""))[0]
    by_cat[cat].append((n, rule))

out.append("## Summary by category\n")
out.append("| Category | Findings | Rules |\n|---|---|---|")
for cat in ORDER:
    if cat in by_cat:
        rows = sorted(by_cat[cat], reverse=True)
        out.append(f"| {cat} | {sum(n for n, _ in rows)} | " + ", ".join(f"{r} ({n})" for n, r in rows) + " |")
out.append("")

out.append("## By rule (with how to fix and where)\n")
for cat in ORDER:
    if cat not in by_cat:
        continue
    out.append(f"### {cat}\n")
    for n, rule in sorted(by_cat[cat], reverse=True):
        how = GUIDE.get(rule, ("", "See https://detekt.dev/docs/rules"))[1]
        mods = ", ".join(f"{k} {v}" for k, v in per_rule_module[rule].most_common())
        top = ", ".join(f"`{f}` ({c})" for f, c in per_rule_files[rule].most_common(6))
        out.append(f"- **{rule}: {n}** ({mods}). {how}  \n  Top files: {top}")
    out.append("")

out.append("## Suggested order for the burn-down pass\n")
out.append("1. **Null safety + idioms** (mechanical, highest value, follows the SDK-6375 patterns).\n"
           "2. **Exceptions**: catching broadly and swallowing is SDK policy (those rules are off); only fix printStackTrace, raw Exception throws and unnamed empty catches.\n"
           "3. **Library safety**: public data classes and explicit return types (or turn on `explicitApi()`).\n"
           "4. **Naming/style** in internal code (not public API, keys, reflection-referenced names).\n"
           "5. **Complexity**: opportunistically when touching a method; don't refactor for the metric alone.\n"
           "6. **Public docs**: KDoc on bucket-A API first (see docs/KotlinMigrationPublicSurface.md).\n")
dest = os.path.join(root, "config", "detekt", "BASELINE_BURNDOWN.md")
open(dest, "w", encoding="utf-8").write("\n".join(out) + "\n")
print(f"wrote {dest}: {total} findings, {len(per_rule)} rules")
for cat in ORDER:
    if cat in by_cat:
        print(f"  {cat}: {sum(n for n, _ in by_cat[cat])}")
