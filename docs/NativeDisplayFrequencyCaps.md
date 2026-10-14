# Frequency Capping — In-App & Native Display

Frequency capping lets you control **how often** a user sees a campaign — so a single In-App message or Native Display unit does not show too many times in a session, a day, or over the user's lifetime. You configure the limits on the CleverTap dashboard; the SDK and backend enforce them together.

This guide explains frequency capping from an integration point of view: what you can configure, what the SDK does for you, and the one thing your app must do for Native Display caps to work. A high-level technical overview is at the [end](#how-it-works-high-level).

# Table of contents

- [What you can cap](#what-you-can-cap)
- [In-App frequency capping](#in-app-frequency-capping)
- [Native Display frequency capping](#native-display-frequency-capping)
  - [Managing units across batches](#managing-units-across-batches)
  - [Reporting impressions (required)](#reporting-impressions-required)
  - [Reporting clicks](#reporting-clicks)
- [Excluding a campaign from caps](#excluding-a-campaign-from-caps)
- [When counters reset](#when-counters-reset)
- [How it works (high level)](#how-it-works-high-level)

# What you can cap

All caps are set **on the dashboard** when you build the campaign — there is nothing to configure in code. The same cap types apply to both In-App and Native Display:

| Cap | Meaning |
|---|---|
| **Per session** | Show at most *N* times in a single app session. |
| **Per day** | Show at most *N* times in a calendar day. |
| **Total (lifetime)** | Show at most *N* times ever, for this user. |
| **Advanced frequency limits** | More granular rules, for example "no more than 2 times in 7 days". |
| **Occurrence limits** | Tie display to how often a triggering event happens (for example, show on every 3rd occurrence). |
| **Global caps** | Account-wide limits across campaigns of the same channel (with a per-campaign option to opt out — see [below](#excluding-a-campaign-from-caps)). |

There are also sensible account-level defaults (for example, one per session and up to ten per day) that apply when a campaign does not set its own limits.

# In-App frequency capping

For In-App notifications, **frequency capping is fully automatic** — you do not write any code for it. The SDK renders In-Apps itself, so it counts every display and enforces the configured caps before showing the next one. Set the limits on the campaign and you are done.

> If you suspend or discard In-Apps at runtime (for example with `suspendInAppNotifications()`), those are display controls, not caps — they are independent of the frequency limits above.

# Native Display frequency capping

Native Display (Display Units) is different in one important way: **your app renders the unit, not the SDK.** The SDK hands you the display units through a listener, and your UI draws them wherever you want. Because the SDK never draws the unit itself, it cannot know a unit was actually shown unless **you tell it**.

So for Native Display there is one required step: **report an impression when a unit becomes visible.** Everything else (deciding which units are eligible, applying the caps) is handled by the SDK and backend.

The typical flow:

```java
// 1. Receive the units the SDK has deemed eligible to show.
clevertapInstance.setDisplayUnitListener(units -> {
    for (CleverTapDisplayUnit unit : units) {
        renderInYourUi(unit);            // your app draws it
    }
});

// (You can also pull them on demand.)
ArrayList<CleverTapDisplayUnit> units = clevertapInstance.getAllDisplayUnits();
CleverTapDisplayUnit unit = clevertapInstance.getDisplayUnitForId(unitID);
```

## Managing units across batches

By default the SDK keeps only the **latest batch** of display units. Each response **replaces** the cache, so `onDisplayUnitsLoaded(...)` is invoked with the units from the most recent response only, and `getAllDisplayUnits()` / `getDisplayUnitForId(...)` return that same current set — not an accumulation of everything received during the session.

This is the right behaviour for most apps, where a screen just shows whichever units are currently active. But if you need to **manage all units together** — for example keep units from several responses on screen at once, or merge newly delivered units into a list you have already built — install your own cache with `setDisplayUnitCache(DisplayUnitCache)` and accumulate instead of replacing:

```java
clevertapInstance.setDisplayUnitCache(new DisplayUnitCache() {
    @Override
    public void updateDisplayUnits(List<CleverTapDisplayUnit> units) {
        // Merge/accumulate the new batch into your own collection,
        // de-duplicating by unitID as you prefer, instead of replacing.
    }

    @Override
    public ArrayList<CleverTapDisplayUnit> getAllDisplayUnits() {
        // Return your combined set of units.
    }

    @Override
    public CleverTapDisplayUnit getDisplayUnitForID(String unitID) {
        // Look up a unit in your combined set.
    }

    @Override
    public void reset() {
        // Clear your collection.
    }
});
```

Once a custom cache is installed, `getAllDisplayUnits()` and `getDisplayUnitForId(...)` route through it, so the rest of your integration — including impression and click reporting — is unchanged. Pass `null` to revert to the default replace-on-each-response cache.

## Reporting impressions (required)

When a unit actually appears on screen, report it. This is what advances the frequency counters — **without it, caps cannot work and a capped campaign may keep being served.**

```java
clevertapInstance.pushDisplayUnitViewedEventForID(unit.getUnitID());
```

Call this once per genuine impression (for example when the unit scrolls into view), not on every layout pass.

## Reporting clicks

Report taps so clicks are attributed. This does not affect capping but completes the analytics:

```java
// Whole-unit click
clevertapInstance.pushDisplayUnitClickedEventForID(unit.getUnitID());

// Element-level click (for example a specific button or carousel slide)
clevertapInstance.pushDisplayUnitElementClickedEventForID(unit.getUnitID(), additionalProperties);
```

# Excluding a campaign from caps

A campaign can be marked to **ignore global/account caps** when you need it to always reach the user (for example a critical transactional message). This is a per-campaign setting on the dashboard. Note that such a campaign is still *counted* — it simply is not *held back* by the shared caps.

# When counters reset

- **Session caps** reset at the start of each new app session.
- **Daily caps** reset at the first midnight (local time). This matches iOS — a "1 per day" campaign becomes eligible again at the next midnight.
- **Total (lifetime) caps** never reset for a given user.

# How it works (high level)

> This section is background only — none of it requires action on your side.

- **Two enforcement sides.** Some caps are enforced by the **backend** (per-target daily/lifetime limits and the account daily ceiling), and some by the **SDK** at the moment a unit would be shown (session caps and advanced frequency/occurrence rules). This split avoids double-counting while keeping decisions fast.
- **Eligibility vs delivery.** The SDK continuously evaluates the event stream against each campaign's advanced rules and reports which campaigns are eligible; the backend then sends only eligible content. For Native Display, the SDK additionally gates *which* units it hands to your listener based on the session caps.
- **Impression accounting.** In-App impressions are recorded by the SDK automatically; Native Display impressions are recorded from your `pushDisplayUnitViewedEventForID` call. The SDK is the source of truth for the counters it reports to the backend, which is why reporting Native Display impressions accurately matters.
- **Separate state per channel.** Native Display keeps its own counters, impression windows, and metadata, entirely separate from In-App — the two never share state.
- **Compatibility.** Native Display frequency caps are delivered to SDK versions that support them; older SDKs continue to receive display units exactly as before, uncapped by this layer.
