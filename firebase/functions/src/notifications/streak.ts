import { addDays, isoWeekKey, isoWeekday } from "./time";

export interface StreakStatus {
  currentDays: number;
  todayActive: boolean;
  freezeUsedThisWeek: boolean;
}

/**
 * Server copy of `Streaks.compute` (core/domain/.../engagement/Streaks.kt), so "streak at risk" means the
 * same as the streak the app shows:
 * - active day: +1
 * - planned rest day without activity: +1 once a streak has started
 * - missed training day: uses the ISO week's single freeze if unused, otherwise the streak resets
 * - today never breaks the streak.
 * [trainingDays] empty means every day needs activity (no program yet).
 */
export function computeStreak(
  today: string,
  activeDates: Iterable<string>,
  trainingDays: Iterable<number>,
  lookbackDays: number,
): StreakStatus {
  const active = new Set(activeDates);
  const training = new Set(trainingDays);
  const frozenWeeks = new Set<string>();
  let run = 0;
  for (let offset = lookbackDays - 1; offset >= 0; offset--) {
    const day = addDays(today, -offset);
    const required = training.size === 0 || training.has(isoWeekday(day));
    if (active.has(day)) {
      run++;
    } else if (!required) {
      if (run > 0) run++;
    } else if (day === today || run === 0) {
      // Nothing yet.
    } else {
      const week = isoWeekKey(day);
      if (frozenWeeks.has(week)) {
        run = 0;
      } else {
        frozenWeeks.add(week);
      }
    }
  }
  return {
    currentDays: run,
    todayActive: active.has(today),
    freezeUsedThisWeek: frozenWeeks.has(isoWeekKey(today)),
  };
}
