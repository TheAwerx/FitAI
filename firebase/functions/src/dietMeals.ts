import { Locale, asInt, asNumber, asRecord, asString, clamp, round1, splitCodes, truncate } from "./common";

// Detailed meals of generateDietPlan: schema, prompt rules and normalization. The app has the same shapes in
// core/domain/.../model/NutritionModels.kt (PlanMeal, PlanMealOption, PlanFood) and the same meal split and
// codes in StarterDiet.kt / FoodModels.kt (MealSlot, FoodUnits). When the meals can't be trusted the server
// returns `meals: []` and the app builds them from its food database instead.

export const MEAL_SLOTS = [
  "breakfast", "morning_snack", "lunch", "afternoon_snack", "dinner", "evening_snack",
] as const;
export type MealSlot = typeof MEAL_SLOTS[number];

export const FOOD_UNITS = [
  "g", "ml", "piece", "slice", "portion", "bowl", "glass", "tea_glass", "cup", "mug",
  "tbsp", "dsp", "tsp", "handful", "ladle", "can", "pack", "scoop",
] as const;

const DEFAULT_TIMES: Record<MealSlot, string> = {
  breakfast: "08:00",
  morning_snack: "10:30",
  lunch: "13:00",
  afternoon_snack: "16:30",
  dinner: "19:30",
  evening_snack: "21:30",
};

const MAX_MEALS = 6;
const OPTIONS_PER_MEAL = 3;
const MAX_FOODS = 8;
const MAX_FOOD_GRAMS = 2000;

export interface PlanFood {
  foodId: null;
  name: string;
  amount: number;
  unit: string;
  grams: number;
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
}

export interface PlanMealOption {
  title: string;
  note: string;
  foods: PlanFood[];
}

export interface PlanMeal {
  slot: MealSlot;
  time: string;
  options: PlanMealOption[];
  selected: number;
}

const planFoodSchema = {
  type: "OBJECT",
  properties: {
    name: { type: "STRING" },
    amount: { type: "NUMBER" },
    unit: { type: "STRING", enum: [...FOOD_UNITS] },
    grams: { type: "NUMBER" },
    calories: { type: "INTEGER" },
    proteinG: { type: "NUMBER" },
    carbsG: { type: "NUMBER" },
    fatG: { type: "NUMBER" },
  },
  required: ["name", "amount", "unit", "grams", "calories", "proteinG", "carbsG", "fatG"],
};

export const planMealSchema = {
  type: "OBJECT",
  properties: {
    slot: { type: "STRING", enum: [...MEAL_SLOTS] },
    time: { type: "STRING" },
    options: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          title: { type: "STRING" },
          note: { type: "STRING" },
          foods: { type: "ARRAY", items: planFoodSchema },
        },
        required: ["title", "note", "foods"],
      },
    },
  },
  required: ["slot", "time", "options"],
};

/** Same split as StarterDiet.slotShares: five meals for muscle gain or 2600+ kcal, otherwise four. */
export function mealShares(calories: number, goal: string): Array<[MealSlot, number]> {
  if (goal === "build_muscle" || calories >= 2600) {
    return [["breakfast", 0.25], ["morning_snack", 0.10], ["lunch", 0.30], ["afternoon_snack", 0.10], ["dinner", 0.25]];
  }
  return [["breakfast", 0.25], ["lunch", 0.35], ["afternoon_snack", 0.10], ["dinner", 0.30]];
}

const RESTRICTION_RULES: Record<string, string> = {
  vegetarian: "vegetarian: no meat, poultry, fish or seafood (eggs and dairy are fine)",
  vegan: "vegan: no animal products at all (no meat, fish, eggs, dairy, honey, whey)",
  pescatarian: "pescatarian: fish and seafood are fine, no meat or poultry",
  halal: "halal: no pork or pork products, no alcohol",
  kosher: "kosher: no pork or shellfish, and never meat or poultry together with dairy in the same meal option",
  "gluten-free": "gluten-free: no wheat, barley, rye, bulgur, couscous, regular oats, bread, pasta, pastry",
  "dairy-free": "dairy-free: no milk, cheese, yogurt, ayran, butter, cream or whey",
};

/** Prompt lines for the meal part of the diet plan. */
export function mealPromptRules(
  calories: number, proteinG: number, goal: string, dietCodes: string[], locale: Locale,
): string[] {
  const shares = mealShares(calories, goal);
  const split = shares.map(([slot, share]) =>
    `${slot} ~${Math.round(calories * share)} kcal / ~${Math.round(proteinG * share)} g protein`).join("; ");
  const restrictions = dietCodes.map((code) => RESTRICTION_RULES[code]).filter((rule): rule is string => !!rule);
  return [
    "Also plan one day of meals in `meals` that adds up to the targets you set.",
    `- Meals, in this order with these slot codes: ${split}. time is "HH:MM" (24 h).`,
    `- Every meal has exactly ${OPTIONS_PER_MEAL} options: options[0] is the main suggestion, options[1] and ` +
      "options[2] are alternatives with about the same calories (within 10%) and protein, using different foods.",
    "- Each option: title (max 5 words), note (one short preparation tip, max 15 words), and 2-6 foods.",
    "- Each food: name of a single food or dish, amount in a household unit, unit as one of these codes: " +
      "piece (adet), slice (dilim), portion (porsiyon), bowl (kase, about 200 g), glass (su bardağı, 200 ml), " +
      "tea_glass (çay bardağı, about 100 ml), cup (fincan), mug (kupa), tbsp (yemek kaşığı, about 15 g), " +
      "dsp (tatlı kaşığı, about 10 g), tsp (çay kaşığı, about 5 g), handful (avuç, about 30 g), ladle (kepçe), " +
      "can (kutu), pack (paket), scoop (ölçek), or g / ml when weighing is natural (meat, fish). " +
      "grams is the total weight of that amount; calories, proteinG, carbsG and fatG are for that amount.",
    "- Food calories must match its macros (protein 4, carbs 4, fat 9 kcal per gram) within 10%, and the main " +
      "options together must be within 5% of the daily calories and protein.",
    locale === "tr" ?
      "- Prefer everyday Turkish foods (e.g. yumurta, beyaz peynir, tam buğday ekmeği, menemen, mercimek çorbası, " +
        "kuru fasulye, bulgur pilavı, ızgara tavuk, köfte, zeytinyağlı sebze, yoğurt, ayran, meyve, kuruyemiş), " +
        "with realistic Turkish home portions." :
      "- Prefer simple, widely available foods with realistic home portions.",
    restrictions.length ?
      `- Diet rules, never break them in any option: ${restrictions.join("; ")}.` :
      "- No diet restrictions.",
    locale === "tr" ?
      "- Write meal titles, notes and food names in Turkish." :
      "- Write meal titles, notes and food names in English.",
  ];
}

// ---------- normalization ----------

function fold(text: string): string {
  return text.toLocaleLowerCase("tr-TR")
    .replace(/ı/g, "i").replace(/ş/g, "s").replace(/ğ/g, "g").replace(/ç/g, "c")
    .replace(/ö/g, "o").replace(/ü/g, "u").replace(/â/g, "a").replace(/î/g, "i").replace(/û/g, "u")
    .normalize("NFD").replace(/[̀-ͯ]/g, "");
}

function words(text: string): string[] {
  return fold(text).split(/[^a-z0-9]+/).filter((w) => w.length > 0);
}

// Short keywords match whole words only ("et" must not match "etkili", "ton" not "tonik"); longer ones
// also match suffixed forms ("tavuklu", "peynirli").
const GROUPS: Record<string, string[]> = {
  meat: ["et", "etli", "dana", "kuzu", "sigir", "kiyma", "kiymali", "sucuk", "sucuklu", "pastirma", "salam",
    "sosis", "doner", "kebap", "kebab", "kavurma", "ciger", "iskembe", "beef", "lamb", "veal", "steak", "burger",
    "meatball", "meatballs", "sausage", "salami", "mince", "kofte"],
  poultry: ["tavuk", "tavuklu", "hindi", "pilic", "chicken", "turkey"],
  fish: ["balik", "balikli", "ton", "somon", "levrek", "cipura", "hamsi", "uskumru", "palamut", "sardalya",
    "alabalik", "fish", "tuna", "salmon", "sardine", "sardines", "anchovy", "anchovies", "mackerel", "trout", "cod"],
  shellfish: ["karides", "midye", "kalamar", "ahtapot", "istakoz", "yengec", "shrimp", "prawn", "prawns", "mussel",
    "mussels", "squid", "octopus", "lobster", "crab", "oyster", "oysters"],
  pork: ["domuz", "jambon", "bacon", "ham", "pork", "prosciutto", "pepperoni", "chorizo"],
  alcohol: ["sarap", "bira", "raki", "wine", "beer", "vodka", "rum", "whisky"],
  dairy: ["sut", "sutlu", "sutlac", "peynir", "yogurt", "ayran", "kefir", "tereyag", "tereyagi", "kaymak", "labne",
    "krema", "lor", "cokelek", "kasar", "kasarli", "mozzarella", "cheddar", "parmesan", "milk", "cheese", "butter",
    "cream", "whey", "cacik", "haydari", "latte", "muhallebi", "dondurma", "yoghurt"],
  egg: ["yumurta", "yumurtali", "omlet", "menemen", "egg", "eggs", "omelette", "omelet"],
  honey: ["bal", "balli", "honey"],
  gluten: ["ekmek", "ekmegi", "simit", "bulgur", "makarna", "eriste", "sehriye", "un", "unlu", "yulaf", "arpa",
    "cavdar", "kuskus", "borek", "pogaca", "pide", "lahmacun", "manti", "lavas", "yufka", "gozleme", "kraker",
    "galeta", "biskuvi", "kek", "kurabiye", "baklava", "bread", "pasta", "wheat", "barley", "rye", "couscous", "oats",
    "oatmeal", "cracker", "crackers", "noodle", "noodles", "tortilla", "granola", "muesli", "seitan", "bagel",
    "croissant", "pizza", "spaghetti"],
};

// Names that contain a keyword but are fine: meatless kofte, plant milks and yogurts, gluten-free bread.
// Keyed by keyword so "etli nohut" still counts as meat while "mercimek köftesi" does not.
const PLANT_WORDS = ["bitkisel", "vegan", "soya", "soy", "badem", "almond", "hindistan", "coconut", "plant"];
const EXEMPT_BY_GROUP: Record<string, { keywords: string[] | "all"; when: string[] }> = {
  meat: { keywords: ["kofte"], when: ["mercimek", "etsiz", "nohut", "sebze", "patates", "lentil", "meatless", "vegan"] },
  dairy: { keywords: "all", when: PLANT_WORDS },
  gluten: { keywords: "all", when: ["glutensiz", "gluten"] },
};

const BANNED: Record<string, string[]> = {
  vegetarian: ["meat", "poultry", "fish", "shellfish", "pork"],
  vegan: ["meat", "poultry", "fish", "shellfish", "pork", "dairy", "egg", "honey"],
  pescatarian: ["meat", "poultry", "pork"],
  halal: ["pork", "alcohol"],
  kosher: ["pork", "shellfish"],
  "gluten-free": ["gluten"],
  "dairy-free": ["dairy"],
};

function inGroup(name: string, group: string): boolean {
  const ws = words(name);
  const exempt = EXEMPT_BY_GROUP[group];
  const exempted = exempt != null && exempt.when.some((e) => ws.some((w) => w.startsWith(e)));
  return GROUPS[group].some((keyword) => {
    if (exempted && (exempt.keywords === "all" || exempt.keywords.includes(keyword))) return false;
    return ws.some((w) => w === keyword || (keyword.length >= 5 && w.startsWith(keyword)));
  });
}

/** True when a food name of the option breaks one of [dietCodes] (best effort, by name). */
export function violatesDiet(option: PlanMealOption, dietCodes: string[]): boolean {
  const groups = new Set(dietCodes.flatMap((code) => BANNED[code] ?? []));
  for (const food of option.foods) {
    for (const group of groups) {
      if (inGroup(food.name, group)) return true;
    }
  }
  if (dietCodes.includes("kosher")) {
    const meat = option.foods.some((f) => inGroup(f.name, "meat") || inGroup(f.name, "poultry"));
    const dairy = option.foods.some((f) => inGroup(f.name, "dairy"));
    if (meat && dairy) return true;
  }
  return false;
}

function normalizeTime(value: unknown, slot: MealSlot): string {
  const match = typeof value === "string" ? value.trim().match(/^(\d{1,2})[:.](\d{2})$/) : null;
  if (!match) return DEFAULT_TIMES[slot];
  const h = Number(match[1]);
  const m = Number(match[2]);
  if (h > 23 || m > 59) return DEFAULT_TIMES[slot];
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}

function normalizeFood(item: unknown): PlanFood | null {
  const f = asRecord(item);
  const name = truncate(asString(f.name, "").trim(), 120);
  if (!name) return null;
  const unit = (FOOD_UNITS as readonly string[]).includes(f.unit as string) ? f.unit as string : "g";
  let grams = asNumber(f.grams, NaN);
  let amount = asNumber(f.amount, NaN);
  if (!(grams > 0)) grams = unit === "g" || unit === "ml" ? amount : NaN;
  if (!(grams > 0)) return null;
  grams = clamp(grams, 1, MAX_FOOD_GRAMS);
  if (!(amount > 0)) amount = unit === "g" || unit === "ml" ? grams : 1;
  const proteinG = Math.max(0, asNumber(f.proteinG, 0));
  const carbsG = Math.max(0, asNumber(f.carbsG, 0));
  const fatG = Math.max(0, asNumber(f.fatG, 0));
  const fromMacros = proteinG * 4 + carbsG * 4 + fatG * 9;
  const given = Math.max(0, asInt(f.calories, Math.round(fromMacros)));
  // Keep the model's calories when they agree with its macros; otherwise trust the macros.
  const calories = Math.abs(given - fromMacros) <= Math.max(15, fromMacros * 0.15) ? given : Math.round(fromMacros);
  return { foodId: null, name, amount: round2(amount), unit, grams: round1(grams), calories, proteinG: round1(proteinG), carbsG: round1(carbsG), fatG: round1(fatG) };
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}

function optionKcal(option: PlanMealOption): number {
  return option.foods.reduce((sum, f) => sum + f.calories, 0);
}

function optionProtein(option: PlanMealOption): number {
  return option.foods.reduce((sum, f) => sum + f.proteinG, 0);
}

/** Scales every amount of the option; household amounts move in quarters, weights in whole grams. */
function scaleOption(option: PlanMealOption, factor: number): PlanMealOption {
  return {
    ...option,
    foods: option.foods.map((f) => {
      const weighed = f.unit === "g" || f.unit === "ml";
      const amount = weighed ? Math.max(1, Math.round(f.amount * factor)) : Math.max(0.25, Math.round(f.amount * factor * 4) / 4);
      const real = amount / f.amount;
      return {
        ...f,
        amount,
        grams: round1(clamp(f.grams * real, 1, MAX_FOOD_GRAMS)),
        calories: Math.round(f.calories * real),
        proteinG: round1(f.proteinG * real),
        carbsG: round1(f.carbsG * real),
        fatG: round1(f.fatG * real),
      };
    }),
  };
}

/**
 * Cleans the model's meals: known slots once each and in order, valid times, up to three options of up to
 * eight foods, macros that add up, no option that breaks the diet (by food name). The main options are
 * scaled together to the daily calories, and each alternative to its main option. Returns [] when what is
 * left is not a usable day (fewer than three meals, or still far off the calories); the app then builds
 * the meals from its food database.
 */
export function normalizeMeals(raw: unknown, dailyCalories: number, proteinG: number, dietRestrictions: string): PlanMeal[] {
  const dietCodes = splitCodes(dietRestrictions).filter((code) => code !== "none");
  const seen = new Set<string>();
  const meals: PlanMeal[] = [];
  for (const item of (Array.isArray(raw) ? raw : []).slice(0, MAX_MEALS * 2)) {
    const m = asRecord(item);
    const slot = (MEAL_SLOTS as readonly string[]).includes(m.slot as string) ? m.slot as MealSlot : null;
    if (!slot || seen.has(slot)) continue;
    const options = (Array.isArray(m.options) ? m.options : [])
      .map((o): PlanMealOption => {
        const r = asRecord(o);
        const foods = (Array.isArray(r.foods) ? r.foods : []).map(normalizeFood)
          .filter((f): f is PlanFood => f != null).slice(0, MAX_FOODS);
        return { title: truncate(asString(r.title, "").trim(), 80), note: truncate(asString(r.note, "").trim(), 300), foods };
      })
      .filter((o) => o.foods.length > 0 && optionKcal(o) > 0 && !violatesDiet(o, dietCodes))
      .map((o) => ({ ...o, title: o.title || o.foods.map((f) => f.name).slice(0, 2).join(", ") }))
      .slice(0, OPTIONS_PER_MEAL);
    if (options.length === 0) continue;
    seen.add(slot);
    meals.push({ slot, time: normalizeTime(m.time, slot), options, selected: 0 });
  }
  meals.sort((a, b) => MEAL_SLOTS.indexOf(a.slot) - MEAL_SLOTS.indexOf(b.slot));
  if (meals.length < 3) return [];

  const mainKcal = meals.reduce((sum, meal) => sum + optionKcal(meal.options[0]), 0);
  if (mainKcal <= 0) return [];
  if (Math.abs(mainKcal - dailyCalories) > dailyCalories * 0.05) {
    const factor = clamp(dailyCalories / mainKcal, 0.7, 1.4);
    meals.forEach((meal) => { meal.options[0] = scaleOption(meal.options[0], factor); });
  }
  meals.forEach((meal) => {
    const target = optionKcal(meal.options[0]);
    meal.options = meal.options
      .map((option, index) => {
        if (index === 0) return option;
        const kcal = optionKcal(option);
        return Math.abs(kcal - target) > target * 0.10 ? scaleOption(option, clamp(target / kcal, 0.7, 1.4)) : option;
      })
      // An alternative that still can't stand in for the main option is dropped.
      .filter((option, index) => index === 0 || Math.abs(optionKcal(option) - target) <= target * 0.25);
  });
  const scaledKcal = meals.reduce((sum, meal) => sum + optionKcal(meal.options[0]), 0);
  if (Math.abs(scaledKcal - dailyCalories) > dailyCalories * 0.15) return [];
  // Protein way off the target would contradict the targets shown next to the plan.
  const protein = meals.reduce((sum, meal) => sum + optionProtein(meal.options[0]), 0);
  if (proteinG > 0 && Math.abs(protein - proteinG) > proteinG * 0.35) return [];
  return meals;
}
