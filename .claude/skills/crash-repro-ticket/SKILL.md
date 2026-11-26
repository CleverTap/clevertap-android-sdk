---
name: crash-repro-ticket
description: Reproduce a reported crash or bug in the CleverTap Android SDK and write a clear ticket with the exact stack line, cause, fix and test. Use when given a crash log, a Play Console or customer report, or asked to investigate and file a bug.
---

# Reproduce a crash and write the ticket

## 1. Read the log

- Take the first frame that is in SDK code. Name the file and line.
- Note the Android version, device type and SDK version if the report has them.
- Check that the line exists in the SDK version the reporter used. The code may have changed since.

## 2. Reproduce

- Smallest repro first: a unit test, or an instrumentation test for race and database problems.
- Otherwise use the sample app and the `verify-on-device` skill.
- Confirm the repro fails for the reason in the log, not for a different one.

## 3. Find the cause

Say in one or two sentences what is null, missing or too large, and why. Check whether the same pattern
exists elsewhere in the code, and say how many places and which.

## 4. Propose the fix

- The smallest change that fixes it. Count the lines.
- Say what could change for customers. A fix that touches public API or visible behaviour needs a
  conversation first.
- If the real fix depends on a bigger planned change, say so and suggest a smaller guard.

## 5. Ticket

Write it so someone who has not seen the log can act on it:

- **Problem**: the exact exception and the first SDK line from the log.
- **Who is affected**: devices, Android versions, how often, if known.
- **Cause**: what the repro showed.
- **Fix**: the proposed change and its size.
- **Testing**: the test or device steps that prove the fix, and that the test fails without it.

Do not paste tokens, account ids or user data from the report.
