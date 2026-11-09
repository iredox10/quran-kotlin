# Planner reminders

Reference lock: the existing full-screen Outside Reading and Planner History routes are the dominant references; Planner Settings supplies prayer selection and the cream/charcoal/gold palette. Use existing NurIcons, standard touch targets, labelled controls, native time pickers, and a single scrolling setup screen. No external design service is available in this session.

Reminders are device-local and opt-in. Configuration requires explicit custom local deadlines or confirmed coordinates, calculation method and time zone. Never derive alarms from the old fallback prayer times. Calculations use Adhan Java 1.2.1 (MIT, https://github.com/batoulapps/adhan-java).

Preparation uses measured in-app page pace plus five minutes; otherwise a ten-minute lead without an invented estimate. Focused/Strong deliver preparation, deadline, +10 and +25 minute alerts. Gentle delivers the deadline and one quiet follow-up. Missed phases collapse; alerts expire rather than replay all day. Snooze does not modify progress. Reading/logging suppress alerts. Every delivery re-reads the active planner and unfinished pages, including external logs. Android notification settings and Do Not Disturb remain in control.

Native AlarmManager schedules one next event with optional exact-alarm access. WorkManager repairs scheduling and supplies a delayed backup. Boot, clock, time-zone and package updates rebuild locally. Notification actions open a freshly validated planner position; no network or server is needed.
