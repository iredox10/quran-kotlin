package com.nur.quran.data.planner

/** Pure progress changes, applied to the latest plan while the writer lock is held. */
object PlannerReadingProgress {
    private fun normalizeCompletion(plan: ReadingPlan) = plan.copy(
        completedDays = plan.assignments.filter { PlannerEngine.getAssignmentProgress(plan, it).isComplete }
            .map { it.dayNumber }.sorted()
    )

    fun markPageRead(current: ReadingPlan, dayNumber: Int, pageNumber: Int, today: String = PlannerEngine.formatPlannerDate()): ReadingPlan {
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return current
        if (pageNumber !in assignment.pageStart..assignment.pageEnd) return current
        val currentReadPages = current.assignmentReadPages[dayNumber] ?: emptyList()
        if (currentReadPages.contains(pageNumber)) return normalizeCompletion(current)

        val newReadPages = currentReadPages + pageNumber
        val updatedReadPagesMap = current.assignmentReadPages.toMutableMap()
        updatedReadPagesMap[dayNumber] = newReadPages

        val existingCompleted = PlannerEngine.getAssignmentProgress(current, assignment).completedRangeValues
        val nextCompletedSet = existingCompleted.toMutableSet()
        assignment.items.forEach { item ->
            // Web parity (useAppStore:670-671): `pageStart || 1`, `pageEnd || pStart`.
            val pStart = item.pageStart.takeIf { it != 0 } ?: 1
            val pEnd = item.pageEnd.takeIf { it != 0 } ?: pStart
            if ((pStart..pEnd).all { newReadPages.contains(it) }) {
                nextCompletedSet.add(item.rangeValue)
            }
        }
        val nextCompleted = nextCompletedSet.filter { v -> assignment.items.any { it.rangeValue == v } }

        val completedItemsMap = current.assignmentCompletedItems.toMutableMap()
        completedItemsMap[dayNumber] = nextCompleted
        val updatedProgressMap = current.assignmentProgress.toMutableMap()
        updatedProgressMap[dayNumber] = nextCompleted.size

        val updatedCompletedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (nextCompleted.size >= assignment.items.size && assignment.items.isNotEmpty()) {
            if (!updatedCompletedAtMap.containsKey(dayNumber)) {
                updatedCompletedAtMap[dayNumber] = today
            }
        }


        val updatedPlan = current.copy(
            assignmentReadPages = updatedReadPagesMap,
            readingActivityDates = (current.readingActivityDates + today).distinct(),
            assignmentCompletedAt = updatedCompletedAtMap,
            assignmentCompletedItems = completedItemsMap,
            assignmentProgress = updatedProgressMap,
            lastReadPage = pageNumber
        )

        return normalizeCompletion(updatedPlan)
    }

    fun setItemCount(current: ReadingPlan, dayNumber: Int, completedCount: Int, today: String = PlannerEngine.formatPlannerDate()): ReadingPlan {
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return current
        val total = assignment.items.size
        val safe = completedCount.coerceIn(0, total)
        val completedItems = assignment.items.take(safe).map { it.rangeValue }

        val completedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (safe >= total && total > 0) {
            if (!completedAtMap.containsKey(dayNumber)) {
                completedAtMap[dayNumber] = today
            }
        } else {
            completedAtMap.remove(dayNumber)
        }

        val completedItemsMap = current.assignmentCompletedItems.toMutableMap()
        completedItemsMap[dayNumber] = completedItems
        val progressMap = current.assignmentProgress.toMutableMap()
        progressMap[dayNumber] = safe


        val rollingBack = safe < PlannerEngine.getAssignmentProgress(current, assignment).completedCount
        val readPages = if (rollingBack) {
            // Completed items already imply their pages. Retaining a shared boundary page
            // would immediately complete a neighbouring item the user just unchecked.
            current.assignmentReadPages + (dayNumber to emptyList())
        } else current.assignmentReadPages
        return normalizeCompletion(current.copy(
                assignmentReadPages = readPages,
                readingActivityDates = if (safe > PlannerEngine.getAssignmentProgress(current, assignment).completedCount)
                    (current.readingActivityDates + today).distinct() else current.readingActivityDates,
                // A deliberate undo uses the existing revisioned replacement contract.
                assignmentsUpdatedAt = if (rollingBack) maxOf(System.currentTimeMillis(), current.assignmentsUpdatedAt + 1) else current.assignmentsUpdatedAt,
                assignmentProgress = progressMap,
                assignmentCompletedItems = completedItemsMap,
                assignmentCompletedAt = completedAtMap,
            ))
    }

    /** Prayer actions change that slot alone, preserving other prayers and the resume page. */
    fun setPrayerSlotCompleted(current: ReadingPlan, dayNumber: Int, slotStart: Int, slotEnd: Int, complete: Boolean): ReadingPlan {
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return current
        if (slotStart < 0 || slotEnd > assignment.items.size || slotEnd <= slotStart) return current
        val selected = assignment.items.subList(slotStart, slotEnd)
        if (complete) return selected.fold(current) { plan, item -> completeItem(plan, dayNumber, item.rangeValue) }
        val selectedIds = selected.map { it.rangeValue }.toSet()
        val remaining = PlannerEngine.getAssignmentProgress(current, assignment).completedRangeValues.filterNot { it in selectedIds }
        val pagesToClear = selected.flatMap { (it.pageStart..it.pageEnd).toList() }.toSet()
        val readPages = current.assignmentReadPages[dayNumber].orEmpty().filterNot { it in pagesToClear }
        return normalizeCompletion(current.copy(
            assignmentProgress = current.assignmentProgress + (dayNumber to remaining.size),
            assignmentCompletedItems = current.assignmentCompletedItems + (dayNumber to remaining),
            assignmentReadPages = current.assignmentReadPages + (dayNumber to readPages),
            assignmentCompletedAt = current.assignmentCompletedAt - dayNumber,
            assignmentsUpdatedAt = maxOf(System.currentTimeMillis(), current.assignmentsUpdatedAt + 1)
        ))
    }

    fun completeItem(current: ReadingPlan, dayNumber: Int, rangeValue: String, today: String = PlannerEngine.formatPlannerDate()): ReadingPlan {
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return current
        if (assignment.items.none { it.rangeValue == rangeValue }) return current
        val existing = PlannerEngine.getAssignmentProgress(current, assignment).completedRangeValues
        if (existing.contains(rangeValue)) return normalizeCompletion(current)
        val next = (existing + rangeValue).filter { v -> assignment.items.any { it.rangeValue == v } }

        val completedItemsMap = current.assignmentCompletedItems.toMutableMap()
        completedItemsMap[dayNumber] = next
        val progressMap = current.assignmentProgress.toMutableMap()
        progressMap[dayNumber] = next.size
        val completedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (next.size >= assignment.items.size) {
            if (!completedAtMap.containsKey(dayNumber)) {
                completedAtMap[dayNumber] = today
            }
        }

        return normalizeCompletion(current.copy(
                readingActivityDates = (current.readingActivityDates + today).distinct(),
                assignmentProgress = progressMap,
                assignmentCompletedItems = completedItemsMap,
                assignmentCompletedAt = completedAtMap,
            ))
    }
}
