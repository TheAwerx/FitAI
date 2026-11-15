// Consistency check of normalizeDietPlan on a sample answer (npm run check:diet builds the functions first).
const assert = require("node:assert/strict");
const path = require("node:path");
const lib = process.argv[2] ?? path.join(__dirname, "..", "lib");
const { dietReference, normalizeDietPlan, dietPlanPrompt } = require(path.join(lib, "plans.js"));
const { violatesDiet } = require(path.join(lib, "dietMeals.js"));

const profile = {
  name: "Ada", age: 31, heightCm: 168, weightKg: 64, goal: "lose_fat", activityLevel: "moderate",
  equipment: "", dietRestrictions: "vegetarian", gender: "female",
};
const reference = dietReference(profile);

const food = (name, amount, unit, grams, calories, p, c, f) => ({ name, amount, unit, grams, calories, proteinG: p, carbsG: c, fatG: f });
const sample = {
  dailyCalories: reference.calories,
  proteinG: reference.proteinG,
  carbsG: reference.carbsG,
  fatG: reference.fatG,
  meals: [
    { slot: "dinner", time: "19:30", options: [
      { title: "Mercimek köftesi", note: "Marula sar.", foods: [food("Mercimek köftesi", 6, "piece", 180, 295, 12, 47, 7), food("Ayran", 1, "glass", 200, 72, 3.4, 5, 3.6), food("Yağsız süzme yoğurt", 2, "bowl", 300, 177, 30.6, 10.8, 1.2)] },
      { title: "Izgara tavuk", note: "", foods: [food("Izgara tavuk göğsü", 150, "g", 150, 248, 46, 0, 5)] },
      { title: "Etli nohut", note: "", foods: [food("Etli nohut", 1, "portion", 250, 300, 15, 30, 12)] },
      { title: "Peynirli omlet", note: "Az yağda.", foods: [food("Peynirli omlet", 1, "portion", 160, 320, 21, 2, 26)] },
    ] },
    { slot: "breakfast", time: "8:05", options: [
      { title: "Kahvaltı", note: "Çay şekersiz.", foods: [food("Haşlanmış yumurta", 2, "piece", 100, 155, 12.6, 1.1, 10.6), food("Beyaz peynir", 1, "slice", 30, 79, 4.5, 0.8, 6.5), food("Tam buğday ekmeği", 2, "slice", 50, 999, 6.2, 21.4, 1.8)] },
      { title: "Yoğurt", note: "", foods: [food("Süzme yoğurt", 1, "bowl", 150, 146, 13.5, 6, 7.5), food("Bal", 1, "tsp", 7, 21, 0, 5.8, 0)] },
    ] },
    { slot: "lunch", time: "13:00", options: [
      { title: "Kuru fasulye", note: "", foods: [food("Zeytinyağlı kuru fasulye", 1, "portion", 250, 263, 14, 35, 7.5), food("Bulgur pilavı", 1, "portion", 150, 198, 5, 33, 5), food("Cacık", 1, "bowl", 200, 100, 4, 7, 6.6)] },
    ] },
    { slot: "lunch", time: "14:00", options: [{ title: "dup", note: "", foods: [food("Elma", 1, "piece", 180, 94, 0.5, 25, 0.3)] }] },
    { slot: "afternoon_snack", time: "16:30", options: [
      { title: "Meyve", note: "", foods: [food("Elma", 1, "piece", 180, 94, 0.5, 25, 0.3), food("Ceviz", 4, "bucket", 16, 105, 2.4, 2.2, 10.4), food("Whey protein tozu", 1, "scoop", 30, 119, 23.4, 2.4, 1.8)] },
    ] },
    { slot: "brunch", time: "11:00", options: [] },
  ],
};

const out = normalizeDietPlan(sample, reference, profile);
console.log("targets", out.dailyCalories, out.proteinG, out.carbsG, out.fatG, "reference", reference.calories);
assert.equal(out.dailyCalories * 1, Math.round(out.dailyCalories));
assert.deepEqual(out.meals.map((m) => m.slot), ["breakfast", "lunch", "afternoon_snack", "dinner"]);
assert.equal(out.meals[0].time, "08:05");
const dinner = out.meals[3];
assert.deepEqual(dinner.options.map((o) => o.title), ["Mercimek köftesi", "Peynirli omlet"], "vegetarian guard");
const bread = out.meals[0].options[0].foods[2];
assert.ok(Math.abs(bread.calories - (bread.proteinG * 4 + bread.carbsG * 4 + bread.fatG * 9)) < 20, "bread kcal from macros");
assert.equal(out.meals[2].options[0].foods[1].unit, "g", "unknown unit falls back to grams");
for (const meal of out.meals) {
  assert.equal(meal.selected, 0);
  for (const option of meal.options) {
    for (const f of option.foods) {
      const fromMacros = f.proteinG * 4 + f.carbsG * 4 + f.fatG * 9;
      assert.ok(Math.abs(f.calories - fromMacros) <= Math.max(15, fromMacros * 0.15) + 1, `${f.name} macros add up`);
      assert.ok(f.amount > 0 && f.grams > 0, f.name);
    }
  }
}
const mainKcal = out.meals.reduce((sum, m) => sum + m.options[0].foods.reduce((s, f) => s + f.calories, 0), 0);
console.log("main kcal", mainKcal, "target", out.dailyCalories);
assert.ok(Math.abs(mainKcal - out.dailyCalories) <= out.dailyCalories * 0.08, "main options scaled to the target");
for (const meal of out.meals) {
  const main = meal.options[0].foods.reduce((s, f) => s + f.calories, 0);
  for (const alt of meal.options.slice(1)) {
    const kcal = alt.foods.reduce((s, f) => s + f.calories, 0);
    assert.ok(Math.abs(kcal - main) <= main * 0.25, `${meal.slot} alternative close to main: ${kcal} vs ${main}`);
  }
}

// Unusable meals fall back to [] so the app builds them from the food database.
assert.deepEqual(normalizeDietPlan({ ...sample, meals: sample.meals.slice(0, 2) }, reference, profile).meals, []);
assert.deepEqual(normalizeDietPlan({ dailyCalories: 2000, proteinG: 120, carbsG: 200, fatG: 60 }, reference, profile).meals, []);
// Without a profile (old call) the targets behave as before.
assert.deepEqual(Object.keys(normalizeDietPlan(sample, reference)).sort(), ["carbsG", "dailyCalories", "fatG", "meals", "proteinG"]);

// Restriction guard by name.
const opt = (...names) => ({ title: "", note: "", foods: names.map((n) => food(n, 1, "piece", 100, 100, 5, 10, 4)) });
assert.equal(violatesDiet(opt("Mercimek köftesi"), ["vegan"]), false);
assert.equal(violatesDiet(opt("Etli nohut"), ["vegetarian"]), true);
assert.equal(violatesDiet(opt("Soya sütü", "Yulaf ezmesi"), ["vegan"]), false);
assert.equal(violatesDiet(opt("Sütlü yulaf"), ["vegan"]), true);
assert.equal(violatesDiet(opt("Balkabağı çorbası"), ["vegan"]), false);
assert.equal(violatesDiet(opt("Ballı yoğurt"), ["vegan"]), true);
assert.equal(violatesDiet(opt("Ton balıklı salata"), ["vegetarian"]), true);
assert.equal(violatesDiet(opt("Tavuk sote", "Cacık"), ["kosher"]), true);
assert.equal(violatesDiet(opt("Tavuk sote", "Pilav"), ["kosher"]), false);
assert.equal(violatesDiet(opt("Glutensiz ekmek"), ["gluten-free"]), false);
assert.equal(violatesDiet(opt("Bulgur pilavı"), ["gluten-free"]), true);
assert.equal(violatesDiet(opt("Karides güveç"), ["halal"]), false);
assert.equal(violatesDiet(opt("Karides güveç"), ["kosher"]), true);

const prompt = dietPlanPrompt({ ...profile, dietRestrictions: "vegan,gluten-free" }, reference, "tr");
assert.ok(prompt.includes("breakfast ~") && prompt.includes("vegan:") && prompt.includes("gluten-free:"));
console.log("normalizeDietPlan checks passed");
