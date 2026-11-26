# Notification Journal

A personal journaling app for capturing quick entries throughout the day, organizing them by tag, and reviewing and finalizing each day.

## Language

**Tag**:
A user-defined classification assigned to a Journal Entry to group related entries within a day.
_Avoid_: Category, label

**Journal Entry**:
A single timestamped note captured by the user, belonging to at most one Tag.

**Day Group**:
The collection of active Journal Entries for a single calendar date, organized by Tag.

**Reconcile**:
The act of finalizing a Day Group's entries once reviewed, removing that day from the active unreconciled queue.

**Reconcile Check**:
A user-configured expectation evaluated against a Day Group when reconciling, raising an overridable warning if not met.
_Avoid_: Reconcile rule, category check, validation
