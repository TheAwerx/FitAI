#!/usr/bin/env python3
"""Builds the bundled food database: core/data/src/commonMain/composeResources/files/foods.json.

Edit the table below, then run from the repo root:
    python3 core/data/fooddb/build_foods.py
The script checks ids, servings and that kcal roughly matches 4/4/9 kcal per g of protein/carbs/fat
(FoodDatabaseTest checks the same). Values per 100 g of the edible part. See README.md for sources.

src: usda   = USDA FoodData Central (SR Legacy / FNDDS), public domain (CC0)
     recipe = estimate from a standard Turkish recipe, summed from USDA ingredient values
     label  = typical Turkish retail product label (category average), rounded
"""
import json
import sys

# (id, tr, en, cat, kcal, protein, carbs, fat, fiber, servings, tags, src, keywords)
F = []


def food(id, tr, en, cat, kcal, p, c, f, fiber, servings, tags="", src="usda", kw=""):
    F.append((id, tr, en, cat, kcal, p, c, f, fiber, servings, tags, src, kw))


# ---------- bread & bakery ----------
food("bread_white", "Beyaz ekmek", "White bread", "bread", 265, 8.8, 52.0, 1.6, 2.5, "slice:25", "gluten", "recipe", "somun,francala")
food("bread_toast", "Tost ekmeği", "Sandwich bread", "bread", 266, 7.6, 50.6, 3.3, 2.4, "slice:28", "gluten", "usda")
food("bread_whole_wheat", "Tam buğday ekmeği", "Whole wheat bread", "bread", 252, 12.5, 42.7, 3.5, 6.0, "slice:25", "gluten", "usda", "tam bugday")
food("bread_bran", "Kepekli ekmek", "Bran bread", "bread", 245, 9.5, 46.0, 2.5, 7.0, "slice:25", "gluten", "recipe")
food("bread_rye", "Çavdar ekmeği", "Rye bread", "bread", 259, 8.5, 48.3, 3.3, 5.8, "slice:25", "gluten", "usda")
food("bread_corn", "Mısır ekmeği", "Cornbread (Black Sea style)", "bread", 240, 5.8, 46.0, 3.4, 3.0, "slice:50", "gluten", "recipe")
food("bread_lavash", "Lavaş", "Lavash flatbread", "bread", 275, 9.0, 56.0, 1.2, 2.2, "piece:70", "gluten", "recipe", "durum ekmegi")
food("bread_pide", "Ramazan pidesi", "Turkish pide bread", "bread", 270, 9.0, 53.0, 2.5, 2.3, "slice:50", "gluten", "recipe", "sade pide")
food("bread_bazlama", "Bazlama", "Bazlama flatbread", "bread", 262, 8.0, 52.0, 2.5, 2.2, "piece:150,slice:50", "gluten", "recipe")
food("bread_tortilla", "Tortilla (buğday)", "Flour tortilla", "bread", 306, 8.2, 50.6, 8.0, 3.5, "piece:45", "gluten", "usda", "wrap")
food("simit", "Simit", "Simit (sesame bagel)", "bread", 330, 10.5, 57.0, 6.5, 3.0, "piece:110", "gluten", "recipe", "gevrek")
food("pogaca_cheese", "Poğaça (peynirli)", "Cheese pogaca", "bread", 340, 8.5, 40.0, 16.0, 1.5, "piece:75", "gluten,dairy,egg", "recipe")
food("acma", "Açma", "Acma (soft ring bun)", "bread", 370, 7.5, 45.0, 18.0, 1.5, "piece:80", "gluten,dairy,egg", "recipe")
food("pisi", "Pişi", "Pisi (fried dough)", "bread", 330, 7.0, 45.0, 13.5, 1.6, "piece:50", "gluten", "recipe")
food("croissant", "Kruvasan", "Croissant", "bread", 406, 8.2, 45.8, 21.0, 2.6, "piece:57", "gluten,dairy,egg", "usda")
food("grissini", "Galeta", "Breadsticks", "bread", 410, 11.0, 72.0, 8.5, 3.0, "piece:10,pack:50", "gluten", "label", "grissini")
food("cracker", "Kraker", "Crackers", "snack", 420, 9.5, 73.0, 9.5, 2.8, "piece:3,pack:40", "gluten", "usda", "tuzlu kraker")
food("rice_cake", "Pirinç patlağı", "Rice cake", "snack", 387, 8.2, 81.5, 2.8, 4.2, "piece:9", "", "usda")
food("borek_su", "Su böreği", "Su boregi (layered cheese pastry)", "bread", 250, 9.5, 21.0, 14.0, 0.8, "slice:150", "gluten,dairy,egg", "recipe")
food("borek_spinach", "Ispanaklı börek", "Spinach borek", "bread", 260, 7.0, 25.0, 14.5, 1.5, "slice:120", "gluten,dairy,egg", "recipe")
food("borek_sigara", "Sigara böreği", "Cheese rolls (sigara boregi)", "bread", 300, 9.0, 27.0, 17.5, 1.0, "piece:30", "gluten,dairy", "recipe")
food("gozleme_cheese", "Gözleme (peynirli)", "Cheese gozleme", "bread", 240, 9.0, 30.0, 9.5, 1.5, "piece:200", "gluten,dairy", "recipe")
food("gozleme_potato", "Gözleme (patatesli)", "Potato gozleme", "bread", 225, 5.5, 36.0, 6.5, 2.0, "piece:200", "gluten", "recipe")

# ---------- breakfast cereals ----------
food("oats", "Yulaf ezmesi", "Rolled oats", "grain", 379, 13.2, 67.7, 6.5, 10.1, "tbsp:8,glass:80,portion:40", "gluten", "usda", "yulaf")
food("oatmeal_milk", "Sütlü yulaf lapası", "Oatmeal with milk", "breakfast", 114, 4.8, 15.3, 3.8, 1.7, "bowl:250", "gluten,dairy", "recipe", "porridge")
food("granola", "Granola", "Granola", "grain", 450, 10.0, 64.0, 17.0, 7.0, "tbsp:12,portion:45", "gluten,nuts", "usda")
food("muesli", "Müsli", "Muesli", "grain", 365, 9.7, 66.0, 6.0, 7.5, "portion:45", "gluten,nuts", "usda")
food("corn_flakes", "Mısır gevreği", "Corn flakes", "grain", 357, 7.5, 84.0, 0.4, 3.3, "glass:30,portion:30", "gluten", "usda")
food("pancake", "Pankek", "Pancake", "breakfast", 227, 6.4, 28.3, 9.7, 1.0, "piece:40", "gluten,egg,dairy", "usda", "pancake")

# ---------- eggs ----------
food("egg_boiled", "Haşlanmış yumurta", "Boiled egg", "egg", 155, 12.6, 1.1, 10.6, 0, "piece:50", "egg", "usda", "yumurta")
food("egg_fried", "Sahanda yumurta", "Fried egg", "egg", 196, 13.6, 0.8, 15.3, 0, "piece:46", "egg", "usda", "yumurta")
food("egg_white", "Yumurta beyazı", "Egg white", "egg", 52, 10.9, 0.7, 0.2, 0, "piece:33", "egg", "usda", "yumurta aki")
food("omelette", "Omlet (sade)", "Plain omelette", "egg", 154, 10.6, 0.6, 11.7, 0, "portion:120", "egg", "usda", "omlet")
food("omelette_cheese", "Peynirli omlet", "Cheese omelette", "egg", 200, 13.0, 1.2, 16.0, 0, "portion:140", "egg,dairy", "recipe")
food("menemen", "Menemen", "Menemen (eggs with tomato & pepper)", "egg", 105, 5.5, 4.0, 7.5, 1.0, "portion:250", "egg", "recipe")
food("egg_sucuk", "Sucuklu yumurta", "Eggs with sucuk", "egg", 248, 14.2, 1.0, 20.5, 0, "portion:150", "egg,meat", "recipe")
food("shakshuka_veg", "Şakşuka", "Saksuka (fried vegetables in tomato)", "dish", 90, 1.3, 7.5, 6.3, 2.5, "portion:150", "", "recipe")

# ---------- breakfast spreads, cold cuts, olives ----------
food("olive_black", "Siyah zeytin", "Black olives", "breakfast", 230, 1.6, 5.5, 22.5, 3.5, "piece:4", "", "label", "gemlik")
food("olive_green", "Yeşil zeytin", "Green olives", "breakfast", 145, 1.0, 3.8, 15.3, 3.3, "piece:5", "", "usda")
food("honey", "Bal", "Honey", "breakfast", 304, 0.3, 82.4, 0, 0.2, "tsp:7,tbsp:21", "honey", "usda")
food("jam", "Reçel", "Jam", "breakfast", 278, 0.4, 68.9, 0.1, 1.1, "tsp:7,tbsp:20", "", "usda", "marmelat")
food("molasses_grape", "Üzüm pekmezi", "Grape molasses", "breakfast", 290, 0.8, 71.0, 0.1, 0, "tbsp:20", "", "label", "pekmez")
food("tahini", "Tahin", "Tahini", "breakfast", 595, 17.0, 21.2, 53.8, 9.3, "tbsp:15", "", "usda")
food("hazelnut_spread", "Kakaolu fındık kreması", "Chocolate hazelnut spread", "breakfast", 539, 6.3, 57.5, 30.9, 3.4, "tbsp:20", "dairy,nuts", "usda", "nutella")
food("peanut_butter", "Fıstık ezmesi", "Peanut butter", "breakfast", 588, 25.0, 20.0, 50.0, 6.0, "tbsp:16", "nuts", "usda")
food("butter", "Tereyağı", "Butter", "fat", 717, 0.9, 0.1, 81.1, 0, "tsp:5,tbsp:14", "dairy", "usda")
food("kaymak", "Kaymak", "Kaymak (clotted cream)", "breakfast", 420, 3.0, 3.0, 44.0, 0, "tbsp:15", "dairy", "label")
food("sucuk", "Sucuk", "Sucuk (spiced beef sausage)", "meat", 452, 21.0, 1.5, 40.0, 0, "slice:10", "meat", "label")
food("pastirma", "Pastırma", "Pastirma (cured beef)", "meat", 240, 30.0, 1.5, 12.5, 0, "slice:10", "meat", "label")
food("salami_beef", "Dana salam", "Beef salami", "meat", 275, 14.0, 2.5, 23.5, 0, "slice:10", "meat", "label", "salam")
food("sausage_chicken", "Piliç sosis", "Chicken sausage", "poultry", 225, 13.0, 4.5, 17.0, 0, "piece:40", "poultry", "label", "sosis")
food("turkey_smoked", "Hindi füme", "Smoked turkey breast", "poultry", 105, 18.0, 3.5, 2.0, 0, "slice:10", "poultry", "usda", "hindi")

# ---------- cheese ----------
food("cheese_white", "Beyaz peynir", "Turkish white cheese (feta)", "cheese", 264, 15.0, 2.5, 21.5, 0, "slice:30", "dairy", "usda", "feta,ezine,peynir")
food("cheese_white_light", "Yarım yağlı beyaz peynir", "Reduced-fat white cheese", "cheese", 190, 17.0, 2.5, 12.5, 0, "slice:30", "dairy", "label", "light peynir")
food("cheese_kashar", "Kaşar peyniri", "Kashar cheese", "cheese", 370, 25.0, 1.5, 29.5, 0, "slice:20", "dairy", "label", "kasar")
food("cheese_tulum", "Tulum peyniri", "Tulum cheese", "cheese", 388, 24.0, 1.0, 32.0, 0, "slice:30", "dairy", "label")
food("cheese_lor", "Lor peyniri", "Lor (whey cheese)", "cheese", 98, 12.0, 3.5, 4.0, 0, "tbsp:20", "dairy", "label", "lor")
food("cheese_cokelek", "Çökelek", "Cokelek (cottage-style cheese)", "cheese", 103, 15.0, 4.0, 3.0, 0, "tbsp:20", "dairy", "label")
food("cheese_cottage", "Süzme peynir (cottage)", "Cottage cheese", "cheese", 98, 11.1, 3.4, 4.3, 0, "tbsp:20,portion:100", "dairy", "usda", "cottage")
food("labneh", "Labne", "Labneh", "cheese", 198, 6.0, 4.0, 17.5, 0, "tbsp:20", "dairy", "label")
food("cream_cheese", "Krem peynir", "Cream cheese", "cheese", 342, 6.0, 4.0, 34.0, 0, "tbsp:15", "dairy", "usda")
food("cheese_mozzarella", "Mozzarella", "Mozzarella", "cheese", 300, 22.2, 2.2, 22.4, 0, "slice:20", "dairy", "usda")
food("cheese_cheddar", "Çedar peyniri", "Cheddar", "cheese", 404, 22.9, 3.1, 33.3, 0, "slice:20", "dairy", "usda", "cheddar")
food("cheese_parmesan", "Parmesan", "Parmesan", "cheese", 392, 35.8, 3.2, 25.8, 0, "tbsp:5", "dairy", "usda")
food("cheese_dil", "Dil peyniri", "Dil cheese (string cheese)", "cheese", 300, 22.0, 1.5, 23.0, 0, "slice:30", "dairy", "label")
food("cheese_halloumi", "Hellim", "Halloumi", "cheese", 318, 21.0, 2.2, 25.0, 0, "slice:30", "dairy", "label", "halloumi")

# ---------- dairy ----------
food("milk_whole", "Süt (tam yağlı)", "Whole milk", "dairy", 61, 3.2, 4.8, 3.3, 0, "glass:200,ml:1", "dairy", "usda", "sut")
food("milk_semi", "Süt (yarım yağlı)", "Semi-skimmed milk", "dairy", 50, 3.3, 4.8, 2.0, 0, "glass:200,ml:1", "dairy", "usda", "sut")
food("milk_skim", "Süt (yağsız)", "Skim milk", "dairy", 34, 3.4, 5.0, 0.1, 0, "glass:200,ml:1", "dairy", "usda", "sut")
food("yogurt_whole", "Yoğurt (tam yağlı)", "Plain yogurt (whole)", "dairy", 61, 3.5, 4.7, 3.3, 0, "bowl:200,tbsp:20", "dairy", "usda", "yogurt")
food("yogurt_light", "Yoğurt (az yağlı)", "Low-fat yogurt", "dairy", 47, 4.0, 4.5, 1.5, 0, "bowl:200,tbsp:20", "dairy", "label", "yogurt light")
food("yogurt_strained", "Süzme yoğurt", "Strained yogurt", "dairy", 97, 9.0, 4.0, 5.0, 0, "bowl:150,tbsp:25", "dairy", "usda", "greek yogurt")
food("yogurt_greek_nonfat", "Yağsız süzme yoğurt", "Nonfat Greek yogurt", "dairy", 59, 10.2, 3.6, 0.4, 0, "bowl:150,tbsp:25", "dairy", "usda", "greek yogurt")
food("ayran", "Ayran", "Ayran (yogurt drink)", "drink", 36, 1.7, 2.5, 1.8, 0, "glass:200,ml:1", "dairy", "label")
food("kefir", "Kefir", "Kefir", "drink", 52, 3.3, 4.5, 2.3, 0, "glass:200,ml:1", "dairy", "label")
food("cacik", "Cacık", "Cacik (yogurt & cucumber)", "dairy", 50, 1.9, 3.5, 3.3, 0.3, "bowl:200", "dairy", "recipe", "tzatziki")
food("haydari", "Haydari", "Haydari (thick yogurt dip)", "dairy", 150, 5.5, 4.5, 12.5, 0, "tbsp:25", "dairy", "recipe")
food("whey", "Whey protein tozu", "Whey protein powder", "dairy", 398, 78.0, 8.0, 6.0, 0, "scoop:30", "dairy", "label", "protein tozu")
food("milk_soy", "Soya sütü", "Soy milk (unsweetened)", "drink", 33, 2.9, 1.7, 1.6, 0.4, "glass:200,ml:1", "", "usda")
food("milk_almond", "Badem sütü", "Almond milk (unsweetened)", "drink", 15, 0.6, 0.6, 1.1, 0.3, "glass:200,ml:1", "nuts", "usda")
food("milk_oat", "Yulaf sütü", "Oat milk", "drink", 43, 0.8, 6.5, 1.5, 0.8, "glass:200,ml:1", "gluten", "label")
food("sahlep", "Sahlep", "Salep (hot milk drink)", "drink", 97, 3.0, 15.0, 2.8, 0, "mug:250", "dairy", "recipe", "salep")
food("tofu", "Tofu", "Tofu (firm)", "legume", 144, 17.3, 2.8, 8.7, 2.3, "portion:100,slice:50", "", "usda")
food("soy_mince", "Soya kıyması (pişmiş)", "Textured soy protein (cooked)", "legume", 110, 17.0, 10.0, 0.4, 4.0, "portion:100", "", "usda", "soya")
food("edamame", "Edamame", "Edamame", "legume", 121, 11.9, 8.9, 5.2, 5.2, "portion:100", "", "usda")

# ---------- poultry ----------
food("chicken_breast_grilled", "Izgara tavuk göğsü", "Grilled chicken breast", "poultry", 165, 31.0, 0, 3.6, 0, "portion:120,piece:170", "poultry", "usda", "tavuk gogus,chicken")
food("chicken_breast_boiled", "Haşlanmış tavuk göğsü", "Boiled chicken breast", "poultry", 151, 29.0, 0, 3.0, 0, "portion:120", "poultry", "usda", "tavuk")
food("chicken_thigh", "Tavuk but (derisiz, fırın)", "Roasted chicken thigh (skinless)", "poultry", 195, 24.5, 0, 10.7, 0, "piece:90,portion:150", "poultry", "usda", "baget,tavuk")
food("chicken_wings", "Tavuk kanat", "Chicken wings", "poultry", 254, 23.8, 0, 16.9, 0, "piece:25,portion:150", "poultry", "usda")
food("chicken_shish", "Tavuk şiş", "Chicken shish kebab", "poultry", 166, 26.0, 2.0, 6.0, 0, "portion:150", "poultry", "recipe", "sis")
food("chicken_doner", "Tavuk döner", "Chicken doner (meat)", "poultry", 209, 20.0, 3.0, 13.0, 0, "portion:120", "poultry", "recipe", "doner")
food("chicken_saute", "Tavuk sote", "Chicken saute with vegetables", "dish", 125, 15.0, 4.0, 5.5, 1.0, "portion:250", "poultry", "recipe")
food("chicken_oven_potato", "Fırında patatesli tavuk", "Oven chicken with potatoes", "dish", 140, 13.0, 8.0, 6.2, 1.0, "portion:300", "poultry", "recipe")
food("chicken_pilaf", "Tavuklu pilav", "Chicken and rice (nohutlu pilav)", "dish", 175, 9.0, 22.0, 5.5, 1.0, "portion:300", "poultry,dairy", "recipe", "nohutlu pilav")
food("chicken_nugget", "Tavuk nugget", "Chicken nuggets", "poultry", 296, 15.3, 16.0, 19.0, 1.0, "piece:18", "poultry,gluten", "usda")
food("turkey_breast_roast", "Fırın hindi göğsü", "Roast turkey breast", "poultry", 135, 30.0, 0, 1.7, 0, "portion:120", "poultry", "usda")

# ---------- red meat ----------
food("kofte_grilled", "Izgara köfte", "Grilled meatballs (kofte)", "meat", 235, 18.0, 6.0, 15.5, 0.3, "piece:30,portion:150", "meat,gluten", "recipe", "kofte,inegol")
food("adana_kebab", "Adana kebap", "Adana kebab", "meat", 282, 18.0, 2.0, 22.5, 0, "portion:150", "meat", "recipe", "urfa kebap,kebap")
food("beef_cubes", "Kuşbaşı et (sote)", "Sauteed beef cubes", "meat", 220, 26.0, 2.0, 12.0, 0, "portion:150", "meat", "recipe", "et sote")
food("beef_tenderloin", "Dana bonfile (ızgara)", "Grilled beef tenderloin", "meat", 203, 29.0, 0, 9.7, 0, "portion:150", "meat", "usda", "biftek,steak")
food("beef_mince_cooked", "Dana kıyma (pişmiş)", "Ground beef (cooked)", "meat", 250, 26.0, 0, 16.2, 0, "tbsp:15,portion:100", "meat", "usda", "kiyma")
food("lamb_chop", "Kuzu pirzola", "Lamb chops", "meat", 291, 24.5, 0, 21.5, 0, "piece:50", "meat", "usda")
food("lamb_tandir", "Kuzu tandır", "Slow-roasted lamb (tandir)", "meat", 258, 25.0, 0, 17.6, 0, "portion:200", "meat", "recipe")
food("kavurma", "Kavurma", "Kavurma (fried preserved meat)", "meat", 298, 25.0, 0, 22.0, 0, "portion:100", "meat", "recipe")
food("beef_liver", "Ciğer tava", "Pan-fried beef liver", "meat", 175, 26.5, 5.2, 4.7, 0, "portion:120", "meat", "usda", "ciger,arnavut cigeri")
food("meat_doner", "Et döner", "Beef doner (meat)", "meat", 263, 19.0, 3.0, 19.5, 0, "portion:120", "meat", "recipe", "doner")
food("doner_wrap_chicken", "Tavuk döner dürüm", "Chicken doner wrap", "street", 218, 12.5, 20.7, 9.2, 1.2, "piece:280", "poultry,gluten", "recipe", "durum")
food("doner_wrap_beef", "Et döner dürüm", "Beef doner wrap", "street", 240, 12.2, 21.0, 11.6, 1.2, "piece:280", "meat,gluten", "recipe", "durum")
food("iskender", "İskender kebap", "Iskender kebab", "dish", 200, 10.0, 13.7, 11.8, 0.8, "portion:350", "meat,gluten,dairy", "recipe", "iskender")
food("tantuni", "Tantuni", "Tantuni wrap", "street", 230, 12.0, 22.0, 10.4, 1.3, "piece:250", "meat,gluten", "recipe")
food("hamburger_wet", "Islak hamburger", "Wet burger (islak)", "street", 277, 11.0, 30.0, 12.5, 1.2, "piece:120", "meat,gluten", "recipe")
food("hamburger", "Hamburger", "Hamburger", "street", 250, 13.0, 27.5, 9.8, 1.5, "piece:220", "meat,gluten", "usda")
food("ali_nazik", "Ali nazik", "Ali nazik (lamb on smoky eggplant yogurt)", "dish", 149, 11.0, 6.0, 9.0, 1.5, "portion:300", "meat,dairy", "recipe")
food("hunkar_begendi", "Hünkar beğendi", "Hunkar begendi", "dish", 160, 10.0, 7.0, 10.3, 1.2, "portion:300", "meat,dairy,gluten", "recipe")
food("tas_kebab", "Tas kebabı", "Tas kebab (beef stew)", "dish", 131, 11.0, 5.0, 7.5, 1.0, "portion:250", "meat", "recipe")
food("karniyarik", "Karnıyarık", "Karniyarik (stuffed eggplant)", "dish", 137, 5.0, 6.0, 10.3, 2.5, "portion:250", "meat", "recipe")
food("musakka", "Patlıcan musakka", "Eggplant moussaka", "dish", 127, 6.0, 7.0, 8.3, 2.3, "portion:250", "meat", "recipe", "musakka")
food("stuffed_pepper_meat", "Etli biber dolması", "Stuffed peppers with meat", "dish", 120, 6.0, 11.0, 5.8, 1.5, "piece:120", "meat", "recipe", "dolma")
food("manti", "Mantı", "Manti (Turkish dumplings with yogurt)", "dish", 179, 8.0, 22.0, 6.5, 1.0, "portion:280", "meat,gluten,dairy,egg", "recipe")
food("pasta_mince", "Kıymalı makarna", "Pasta with minced beef", "dish", 166, 8.0, 20.0, 6.0, 1.3, "portion:300", "meat,gluten", "recipe")
food("spaghetti_bolognese", "Spagetti bolonez", "Spaghetti bolognese", "dish", 139, 7.5, 17.0, 4.6, 1.5, "portion:350", "meat,gluten", "recipe")
food("lasagna", "Lazanya", "Lasagna", "dish", 134, 8.0, 13.0, 5.6, 1.0, "portion:250", "meat,gluten,dairy,egg", "recipe")

# ---------- fish & seafood ----------
food("salmon_baked", "Fırın somon", "Baked salmon", "fish", 206, 22.1, 0, 12.4, 0, "portion:150", "fish", "usda", "somon")
food("sea_bass", "Izgara levrek", "Grilled sea bass", "fish", 124, 23.6, 0, 2.6, 0, "portion:180", "fish", "usda", "levrek")
food("sea_bream", "Izgara çipura", "Grilled sea bream", "fish", 134, 24.0, 0, 4.2, 0, "portion:180", "fish", "usda", "cipura")
food("anchovy_fried", "Hamsi tava", "Fried anchovies", "fish", 252, 19.0, 8.0, 16.0, 0.3, "portion:150", "fish,gluten", "recipe", "hamsi")
food("mackerel", "Izgara uskumru", "Grilled mackerel", "fish", 262, 23.9, 0, 17.8, 0, "portion:150", "fish", "usda", "uskumru,palamut")
food("tuna_water", "Ton balığı (suda)", "Tuna in water (drained)", "fish", 116, 25.5, 0, 0.8, 0, "can:80,tbsp:15", "fish", "usda", "ton")
food("tuna_oil", "Ton balığı (yağlı)", "Tuna in oil (drained)", "fish", 198, 29.1, 0, 8.2, 0, "can:80,tbsp:15", "fish", "usda", "ton")
food("shrimp", "Karides (pişmiş)", "Shrimp (cooked)", "fish", 99, 24.0, 0.2, 0.3, 0, "portion:100", "fish,shellfish", "usda", "karides")
food("mussels_stuffed", "Midye dolma", "Stuffed mussels", "street", 179, 8.0, 22.0, 6.5, 0.5, "piece:25", "fish,shellfish", "recipe", "midye")
food("fish_sandwich", "Balık ekmek", "Fish sandwich", "street", 226, 12.9, 27.9, 7.0, 1.4, "piece:290", "fish,gluten", "recipe")
food("sushi", "Suşi (karışık)", "Sushi (mixed)", "dish", 150, 6.0, 28.0, 1.6, 0.5, "piece:30", "fish", "usda", "sushi")
food("tuna_salad", "Ton balıklı salata", "Tuna salad", "salad", 121, 10.0, 5.0, 6.8, 1.5, "portion:250", "fish", "recipe")

# ---------- legumes ----------
food("lentil_soup", "Mercimek çorbası", "Red lentil soup", "soup", 68, 3.6, 9.5, 1.9, 1.8, "bowl:250,ladle:100", "", "recipe", "mercimek,corba")
food("ezogelin", "Ezogelin çorbası", "Ezogelin soup", "soup", 71, 3.2, 11.0, 1.6, 1.8, "bowl:250,ladle:100", "gluten", "recipe", "corba")
food("yayla_soup", "Yayla çorbası", "Yogurt soup (yayla)", "soup", 70, 2.8, 8.0, 3.0, 0.3, "bowl:250,ladle:100", "dairy,egg,gluten", "recipe", "corba")
food("chicken_soup", "Tavuk suyu çorbası", "Chicken noodle soup", "soup", 45, 3.0, 6.0, 1.0, 0.3, "bowl:250,ladle:100", "poultry,gluten", "recipe", "sehriye corbasi")
food("tomato_soup", "Domates çorbası", "Tomato soup", "soup", 55, 1.4, 7.0, 2.4, 0.8, "bowl:250,ladle:100", "gluten,dairy", "recipe", "corba")
food("vegetable_soup", "Sebze çorbası", "Vegetable soup", "soup", 40, 1.2, 6.0, 1.3, 1.5, "bowl:250,ladle:100", "", "recipe", "corba")
food("tarhana_soup", "Tarhana çorbası", "Tarhana soup", "soup", 55, 2.2, 8.0, 1.6, 0.8, "bowl:250,ladle:100", "gluten,dairy", "recipe", "corba")
food("tripe_soup", "İşkembe çorbası", "Tripe soup", "soup", 74, 6.0, 3.5, 4.0, 0, "bowl:250,ladle:100", "meat,gluten", "recipe", "corba")
food("beans_meat", "Etli kuru fasulye", "White bean stew with meat", "dish", 110, 6.5, 13.0, 3.5, 4.5, "portion:250", "meat", "recipe", "kuru fasulye")
food("beans_olive_oil", "Zeytinyağlı kuru fasulye", "White bean stew (olive oil)", "dish", 105, 5.5, 14.0, 3.0, 4.5, "portion:250", "", "recipe", "kuru fasulye")
food("chickpea_stew", "Nohut yemeği", "Chickpea stew", "dish", 120, 6.0, 15.5, 3.8, 4.5, "portion:250", "", "recipe", "nohut")
food("chickpeas_boiled", "Haşlanmış nohut", "Boiled chickpeas", "legume", 164, 8.9, 27.4, 2.6, 7.6, "glass:160,tbsp:15", "", "usda", "nohut")
food("lentils_boiled", "Haşlanmış yeşil mercimek", "Boiled green lentils", "legume", 116, 9.0, 20.1, 0.4, 7.9, "glass:190,tbsp:15", "", "usda", "mercimek")
food("kidney_beans_boiled", "Haşlanmış barbunya", "Boiled kidney beans", "legume", 127, 8.7, 22.8, 0.5, 6.4, "glass:180", "", "usda", "barbunya,kirmizi fasulye")
food("barbunya_olive_oil", "Zeytinyağlı barbunya", "Borlotti beans in olive oil", "dish", 120, 5.5, 15.0, 4.2, 5.0, "portion:200", "", "recipe")
food("hummus", "Humus", "Hummus", "legume", 166, 7.9, 14.3, 9.6, 6.0, "tbsp:15,portion:100", "", "usda", "humus")
food("falafel", "Falafel", "Falafel", "legume", 333, 13.3, 31.8, 17.8, 4.9, "piece:17", "", "usda")
food("fava", "Fava", "Fava (broad bean puree)", "legume", 132, 7.0, 16.0, 4.4, 5.0, "portion:100,tbsp:20", "", "recipe")
food("lentil_kofte", "Mercimek köftesi", "Lentil kofte", "legume", 164, 6.5, 26.0, 3.8, 4.5, "piece:30", "gluten", "recipe", "kofte")
food("cig_kofte", "Çiğ köfte (etsiz)", "Cig kofte (meatless)", "street", 175, 4.6, 32.0, 3.2, 4.0, "piece:25,portion:150", "gluten", "recipe", "cigkofte")
food("piyaz", "Piyaz", "Piyaz (bean salad)", "salad", 117, 5.0, 13.5, 4.8, 4.0, "portion:150", "", "recipe")
food("peas_boiled", "Haşlanmış bezelye", "Boiled green peas", "vegetable", 84, 5.4, 15.6, 0.2, 5.5, "portion:150", "", "usda", "bezelye")

# ---------- grains & sides ----------
food("rice_pilaf", "Pirinç pilavı", "Rice pilaf (with butter)", "grain", 161, 2.8, 28.0, 4.2, 0.4, "portion:150,tbsp:15", "dairy", "recipe", "pilav")
food("rice_boiled", "Haşlanmış pirinç", "Boiled white rice", "grain", 130, 2.7, 28.2, 0.3, 0.4, "portion:150,tbsp:15", "", "usda", "pirinc")
food("rice_brown", "Esmer pirinç (haşlanmış)", "Brown rice (cooked)", "grain", 123, 2.7, 25.6, 1.0, 1.6, "portion:150,tbsp:15", "", "usda")
food("bulgur_pilaf", "Bulgur pilavı", "Bulgur pilaf", "grain", 132, 3.4, 22.0, 3.4, 4.0, "portion:150,tbsp:15", "gluten", "recipe", "bulgur")
food("bulgur_boiled", "Haşlanmış bulgur", "Boiled bulgur", "grain", 83, 3.1, 18.6, 0.2, 4.5, "portion:150", "gluten", "usda", "bulgur")
food("quinoa", "Kinoa (haşlanmış)", "Quinoa (cooked)", "grain", 120, 4.4, 21.3, 1.9, 2.8, "portion:150,tbsp:15", "", "usda")
food("pasta_boiled", "Makarna (haşlanmış)", "Pasta (cooked)", "grain", 158, 5.8, 30.9, 0.9, 1.8, "portion:200", "gluten", "usda", "spagetti")
food("pasta_whole_boiled", "Tam buğday makarna", "Whole wheat pasta (cooked)", "grain", 124, 5.3, 26.5, 0.5, 4.5, "portion:200", "gluten", "usda")
food("noodles_eriste", "Erişte", "Erişte (egg noodles, cooked)", "grain", 144, 5.5, 27.0, 1.5, 1.2, "portion:200", "gluten,egg", "recipe")
food("couscous", "Kuskus (haşlanmış)", "Couscous (cooked)", "grain", 112, 3.8, 23.2, 0.2, 1.4, "portion:150", "gluten", "usda")
food("potato_boiled", "Haşlanmış patates", "Boiled potato", "vegetable", 87, 1.9, 20.1, 0.1, 1.8, "piece:150", "", "usda", "patates")
food("potato_baked", "Fırın patates", "Baked potato", "vegetable", 93, 2.5, 21.2, 0.1, 2.2, "piece:170", "", "usda", "patates")
food("potato_mashed", "Patates püresi", "Mashed potatoes", "vegetable", 113, 1.9, 16.8, 4.2, 1.5, "portion:200", "dairy", "usda", "pure")
food("french_fries", "Patates kızartması", "French fries", "street", 312, 3.4, 41.4, 14.7, 3.8, "portion:120", "", "usda", "patates")
food("sweet_potato", "Fırın tatlı patates", "Baked sweet potato", "vegetable", 90, 2.0, 20.7, 0.2, 3.3, "piece:130", "", "usda", "tatli patates")
food("corn_cob", "Haşlanmış mısır", "Boiled corn on the cob", "vegetable", 96, 3.4, 21.0, 1.5, 2.4, "piece:100", "", "usda", "misir")
food("kumpir", "Kumpir", "Kumpir (loaded baked potato)", "street", 148, 4.5, 17.0, 7.0, 1.8, "piece:500", "dairy", "recipe")

# ---------- home dishes (vegetables) ----------
food("green_beans_olive_oil", "Zeytinyağlı taze fasulye", "Green beans in olive oil", "dish", 71, 1.8, 7.0, 4.0, 3.0, "portion:200", "", "recipe", "taze fasulye")
food("spinach_dish", "Ispanak yemeği", "Spinach stew", "dish", 66, 2.5, 6.0, 3.6, 2.2, "portion:200", "", "recipe", "ispanak")
food("imam_bayildi", "İmam bayıldı", "Imam bayildi", "dish", 122, 1.5, 8.0, 9.3, 3.0, "portion:200", "", "recipe")
food("turlu", "Sebze türlü", "Mixed vegetable stew (turlu)", "dish", 71, 1.8, 7.5, 3.8, 2.5, "portion:250", "", "recipe", "turlu")
food("leeks_olive_oil", "Zeytinyağlı pırasa", "Leeks in olive oil", "dish", 76, 1.5, 9.0, 3.8, 1.8, "portion:200", "", "recipe", "pirasa")
food("peas_dish", "Bezelye yemeği", "Pea stew", "dish", 81, 3.5, 10.0, 3.0, 3.5, "portion:200", "", "recipe", "bezelye")
food("dolma_grape_leaves", "Zeytinyağlı yaprak sarma", "Stuffed vine leaves", "dish", 151, 2.8, 21.0, 6.2, 2.0, "piece:15,portion:150", "", "recipe", "sarma,dolma")
food("mucver", "Mücver", "Zucchini fritters (mucver)", "dish", 182, 6.5, 12.0, 12.0, 1.2, "piece:50", "egg,gluten,dairy", "recipe")
food("roasted_vegetables", "Fırın sebze", "Roasted vegetables", "vegetable", 85, 1.8, 9.0, 4.6, 2.8, "portion:200", "", "recipe")
food("kisir", "Kısır", "Kisir (bulgur salad)", "salad", 152, 3.5, 22.0, 5.5, 4.0, "portion:150", "gluten", "recipe")
food("babaganoush", "Babagannuş", "Baba ganoush", "salad", 110, 2.5, 8.0, 7.6, 3.0, "tbsp:25,portion:100", "", "recipe")
food("ezme", "Acılı ezme", "Spicy tomato ezme", "salad", 63, 1.5, 6.5, 3.5, 1.5, "tbsp:20", "", "recipe", "ezme")

# ---------- salads & vegetables ----------
food("shepherd_salad", "Çoban salatası", "Shepherd salad", "salad", 60, 0.9, 4.2, 4.4, 1.2, "portion:200", "", "recipe", "salata")
food("green_salad", "Mevsim salata", "Green salad (dressed)", "salad", 50, 1.2, 4.0, 3.3, 1.5, "portion:150", "", "recipe", "salata")
food("chicken_salad", "Tavuklu salata", "Chicken salad", "salad", 110, 11.0, 4.0, 5.5, 1.3, "portion:250", "poultry", "recipe", "salata")
food("tomato", "Domates", "Tomato", "vegetable", 18, 0.9, 3.9, 0.2, 1.2, "piece:120,slice:20", "", "usda")
food("cucumber", "Salatalık", "Cucumber", "vegetable", 15, 0.7, 3.6, 0.1, 0.5, "piece:120,slice:10", "", "usda", "hiyar")
food("green_pepper", "Yeşil biber", "Green pepper", "vegetable", 20, 0.9, 4.6, 0.2, 1.7, "piece:15", "", "usda", "sivri biber,carliston")
food("roasted_pepper", "Közlenmiş biber", "Roasted pepper", "vegetable", 30, 1.0, 6.0, 0.3, 1.5, "piece:30", "", "usda")
food("lettuce", "Marul", "Lettuce", "vegetable", 17, 1.2, 3.3, 0.3, 2.1, "handful:30,portion:100", "", "usda")
food("carrot", "Havuç", "Carrot", "vegetable", 41, 0.9, 9.6, 0.2, 2.8, "piece:60", "", "usda")
food("broccoli", "Haşlanmış brokoli", "Steamed broccoli", "vegetable", 35, 2.4, 7.2, 0.4, 3.3, "portion:150", "", "usda", "brokoli")
food("spinach_raw", "Ispanak (çiğ)", "Spinach (raw)", "vegetable", 23, 2.9, 3.6, 0.4, 2.2, "handful:30", "", "usda", "ispanak")
food("zucchini_grilled", "Izgara kabak", "Grilled zucchini", "vegetable", 17, 1.2, 3.1, 0.3, 1.0, "portion:150", "", "usda", "kabak")
food("eggplant_roasted", "Közlenmiş patlıcan", "Roasted eggplant", "vegetable", 35, 0.8, 8.7, 0.2, 2.5, "portion:150", "", "usda", "patlican")
food("mushroom_saute", "Mantar sote", "Sauteed mushrooms", "vegetable", 64, 3.0, 4.0, 4.0, 1.5, "portion:150", "", "recipe", "mantar")
food("onion", "Soğan", "Onion", "vegetable", 40, 1.1, 9.3, 0.1, 1.7, "piece:110", "", "usda")
food("pickles", "Turşu", "Pickles", "vegetable", 12, 0.3, 2.3, 0.2, 1.0, "portion:50", "", "usda")
food("avocado", "Avokado", "Avocado", "vegetable", 160, 2.0, 8.5, 14.7, 6.7, "piece:150,tbsp:15", "", "usda")
food("arugula", "Roka", "Arugula", "vegetable", 25, 2.6, 3.7, 0.7, 1.6, "handful:20", "", "usda")
food("parsley", "Maydanoz", "Parsley", "vegetable", 36, 3.0, 6.3, 0.8, 3.3, "handful:15", "", "usda")

# ---------- fruit ----------
food("apple", "Elma", "Apple", "fruit", 52, 0.3, 13.8, 0.2, 2.4, "piece:180", "", "usda")
food("banana", "Muz", "Banana", "fruit", 89, 1.1, 22.8, 0.3, 2.6, "piece:120", "", "usda")
food("orange", "Portakal", "Orange", "fruit", 47, 0.9, 11.8, 0.1, 2.4, "piece:150", "", "usda")
food("tangerine", "Mandalina", "Tangerine", "fruit", 53, 0.8, 13.3, 0.3, 1.8, "piece:80", "", "usda")
food("pear", "Armut", "Pear", "fruit", 57, 0.4, 15.2, 0.1, 3.1, "piece:170", "", "usda")
food("strawberry", "Çilek", "Strawberries", "fruit", 32, 0.7, 7.7, 0.3, 2.0, "glass:150,piece:12", "", "usda")
food("grapes", "Üzüm", "Grapes", "fruit", 69, 0.7, 18.1, 0.2, 0.9, "glass:150", "", "usda")
food("watermelon", "Karpuz", "Watermelon", "fruit", 30, 0.6, 7.6, 0.2, 0.4, "slice:280", "", "usda")
food("melon", "Kavun", "Melon", "fruit", 34, 0.8, 8.2, 0.2, 0.9, "slice:200", "", "usda")
food("apricot", "Kayısı", "Apricot", "fruit", 48, 1.4, 11.1, 0.4, 2.0, "piece:35", "", "usda")
food("apricot_dried", "Kuru kayısı", "Dried apricots", "fruit", 241, 3.4, 62.6, 0.5, 7.3, "piece:8,handful:40", "", "usda")
food("raisins", "Kuru üzüm", "Raisins", "fruit", 299, 3.1, 79.2, 0.5, 3.7, "tbsp:10,handful:30", "", "usda")
food("fig_dried", "Kuru incir", "Dried figs", "fruit", 249, 3.3, 63.9, 0.9, 9.8, "piece:20", "", "usda")
food("fig_fresh", "İncir", "Fresh figs", "fruit", 74, 0.8, 19.2, 0.3, 2.9, "piece:50", "", "usda")
food("dates", "Hurma", "Dates", "fruit", 277, 1.8, 75.0, 0.2, 6.7, "piece:20", "", "usda")
food("peach", "Şeftali", "Peach", "fruit", 39, 0.9, 9.5, 0.3, 1.5, "piece:150", "", "usda")
food("cherries", "Kiraz", "Cherries", "fruit", 63, 1.1, 16.0, 0.2, 2.1, "glass:140,piece:8", "", "usda")
food("pomegranate", "Nar", "Pomegranate", "fruit", 83, 1.7, 18.7, 1.2, 4.0, "glass:170", "", "usda")
food("kiwi", "Kivi", "Kiwi", "fruit", 61, 1.1, 14.7, 0.5, 3.0, "piece:75", "", "usda")
food("pineapple", "Ananas", "Pineapple", "fruit", 50, 0.5, 13.1, 0.1, 1.4, "slice:80", "", "usda")
food("plum", "Erik", "Plum", "fruit", 46, 0.7, 11.4, 0.3, 1.4, "piece:65", "", "usda")
food("blueberries", "Yaban mersini", "Blueberries", "fruit", 57, 0.7, 14.5, 0.3, 2.4, "glass:150,handful:50", "", "usda")
food("grapefruit", "Greyfurt", "Grapefruit", "fruit", 42, 0.8, 10.7, 0.1, 1.6, "piece:250", "", "usda")
food("quince", "Ayva", "Quince", "fruit", 57, 0.4, 15.3, 0.1, 1.9, "piece:200", "", "usda")
food("mulberries_dried", "Kuru dut", "Dried mulberries", "fruit", 330, 10.0, 70.0, 2.0, 12.0, "handful:30", "", "label", "dut")
food("chestnuts_roasted", "Kestane kebap", "Roasted chestnuts", "nut", 245, 3.2, 53.0, 2.2, 5.1, "piece:8", "", "usda", "kestane")

# ---------- nuts & seeds ----------
food("walnuts", "Ceviz", "Walnuts", "nut", 654, 15.2, 13.7, 65.2, 6.7, "piece:4,handful:30", "nuts", "usda")
food("hazelnuts", "Fındık", "Hazelnuts", "nut", 628, 15.0, 16.7, 60.8, 9.7, "piece:1.5,handful:30", "nuts", "usda")
food("almonds", "Badem", "Almonds", "nut", 579, 21.2, 21.6, 49.9, 12.5, "piece:1.2,handful:30", "nuts", "usda")
food("pistachios", "Antep fıstığı", "Pistachios", "nut", 560, 20.2, 27.2, 45.3, 10.6, "handful:30", "nuts", "usda", "fistik")
food("cashews", "Kaju", "Cashews", "nut", 553, 18.2, 30.2, 43.9, 3.3, "handful:30", "nuts", "usda")
food("peanuts", "Yer fıstığı", "Peanuts (roasted)", "nut", 587, 24.4, 21.3, 49.7, 8.4, "handful:30", "nuts", "usda", "fistik")
food("leblebi", "Leblebi", "Roasted chickpeas (leblebi)", "nut", 368, 20.0, 58.0, 5.6, 12.0, "handful:30", "", "label")
food("sunflower_seeds", "Ay çekirdeği (iç)", "Sunflower seeds", "nut", 582, 19.3, 24.1, 49.8, 11.1, "handful:30", "", "usda", "cekirdek")
food("pumpkin_seeds", "Kabak çekirdeği (iç)", "Pumpkin seeds", "nut", 574, 29.8, 14.7, 49.0, 6.5, "handful:30", "", "usda", "cekirdek")
food("mixed_nuts", "Karışık kuruyemiş", "Mixed nuts", "nut", 600, 17.0, 22.0, 52.0, 7.0, "handful:30", "nuts", "usda", "kuruyemis")
food("chia", "Chia tohumu", "Chia seeds", "nut", 486, 16.5, 42.1, 30.7, 34.4, "tbsp:12", "", "usda")
food("flaxseed", "Keten tohumu", "Flaxseed", "nut", 534, 18.3, 28.9, 42.2, 27.3, "tbsp:10", "", "usda")
food("sesame", "Susam", "Sesame seeds", "nut", 573, 17.7, 23.4, 49.7, 11.8, "tbsp:9", "", "usda")

# ---------- street food & fast food ----------
food("lahmacun", "Lahmacun", "Lahmacun", "street", 234, 11.0, 30.0, 7.8, 2.0, "piece:170", "meat,gluten", "recipe")
food("pide_mince", "Kıymalı pide", "Pide with minced beef", "street", 256, 12.0, 30.0, 9.8, 1.6, "portion:300,slice:60", "meat,gluten", "recipe", "pide")
food("pide_cheese", "Kaşarlı pide", "Cheese pide", "street", 280, 12.5, 31.0, 11.8, 1.4, "portion:280,slice:60", "dairy,gluten", "recipe", "pide")
food("pide_cubes", "Kuşbaşılı pide", "Pide with beef cubes", "street", 251, 13.5, 29.0, 9.0, 1.4, "portion:300,slice:60", "meat,gluten", "recipe", "pide")
food("toast_cheese", "Kaşarlı tost", "Cheese toastie", "street", 327, 13.9, 29.5, 17.0, 1.5, "piece:105", "gluten,dairy", "recipe", "tost")
food("toast_mixed", "Karışık tost", "Mixed toastie (cheese & sucuk)", "street", 342, 14.5, 26.0, 20.0, 1.3, "piece:120", "gluten,dairy,meat", "recipe", "tost")
food("chicken_sandwich", "Tavuklu sandviç", "Chicken sandwich", "street", 228, 14.0, 26.0, 7.5, 1.8, "piece:180", "gluten,poultry", "recipe", "sandvic")
food("pizza_mixed", "Karışık pizza", "Pizza (mixed toppings)", "street", 266, 11.0, 33.0, 10.0, 2.3, "slice:110", "gluten,dairy,meat", "usda", "pizza")
food("pizza_margherita", "Margarita pizza", "Margherita pizza", "street", 251, 10.5, 32.0, 9.0, 2.0, "slice:100", "gluten,dairy", "usda", "pizza")

# ---------- desserts ----------
food("baklava", "Baklava", "Baklava", "dessert", 428, 6.7, 37.6, 29.0, 2.5, "piece:40", "gluten,dairy,nuts", "usda")
food("kunefe", "Künefe", "Kunefe", "dessert", 363, 8.0, 40.0, 19.0, 0.8, "portion:150", "gluten,dairy", "recipe")
food("kadayif", "Tel kadayıf", "Kadayif", "dessert", 382, 5.0, 50.0, 18.0, 1.2, "piece:60", "gluten,dairy,nuts", "recipe")
food("revani", "Revani", "Revani (semolina cake)", "dessert", 330, 5.0, 55.0, 10.0, 0.8, "piece:80", "gluten,egg,dairy", "recipe")
food("sekerpare", "Şekerpare", "Sekerpare", "dessert", 400, 5.0, 60.0, 15.5, 0.8, "piece:40", "gluten,egg,dairy", "recipe")
food("tulumba", "Tulumba tatlısı", "Tulumba", "dessert", 342, 3.0, 50.0, 14.5, 0.6, "piece:25", "gluten,egg", "recipe")
food("sutlac", "Sütlaç", "Rice pudding (sutlac)", "dessert", 125, 3.5, 21.0, 3.0, 0.1, "bowl:170", "dairy", "recipe")
food("muhallebi", "Muhallebi", "Milk pudding (muhallebi)", "dessert", 124, 3.4, 20.0, 3.4, 0.1, "bowl:150", "dairy", "recipe")
food("asure", "Aşure", "Asure (Noah's pudding)", "dessert", 143, 2.5, 30.0, 1.4, 2.5, "bowl:200", "gluten", "recipe")
food("lokum", "Lokum", "Turkish delight", "dessert", 382, 0.2, 94.0, 0.6, 0.2, "piece:15", "", "label")
food("halva_tahini", "Tahin helvası", "Tahini halva", "dessert", 529, 12.0, 55.0, 29.0, 3.5, "slice:30", "", "label", "helva")
food("halva_semolina", "İrmik helvası", "Semolina halva", "dessert", 324, 4.5, 45.0, 14.0, 1.0, "portion:100", "gluten,dairy", "recipe", "helva")
food("profiterole", "Profiterol", "Profiteroles", "dessert", 284, 5.0, 30.0, 16.0, 0.8, "portion:150", "gluten,dairy,egg", "recipe")
food("cake_plain", "Kek", "Plain cake", "dessert", 371, 6.0, 52.0, 15.5, 1.0, "slice:50", "gluten,egg,dairy", "recipe")
food("cookies", "Kurabiye", "Butter cookies", "dessert", 501, 6.0, 63.0, 25.0, 1.5, "piece:15", "gluten,dairy,egg", "usda")
food("biscuits", "Bisküvi (petibör)", "Tea biscuits", "snack", 448, 7.5, 72.0, 14.5, 2.5, "piece:8,pack:80", "gluten,dairy", "label", "biskuvi")
food("wafer", "Gofret", "Wafer bar", "snack", 519, 6.0, 63.0, 27.0, 1.5, "piece:35", "gluten,dairy", "label")
food("chocolate_milk", "Sütlü çikolata", "Milk chocolate", "dessert", 535, 7.7, 59.4, 29.7, 3.4, "piece:5,pack:80", "dairy", "usda", "cikolata")
food("chocolate_dark", "Bitter çikolata (%70)", "Dark chocolate (70%)", "dessert", 598, 7.8, 45.9, 42.6, 10.9, "piece:5", "", "usda", "cikolata")
food("ice_cream", "Dondurma", "Ice cream (vanilla)", "dessert", 207, 3.5, 23.6, 11.0, 0.7, "scoop:50", "dairy", "usda", "dondurma")

# ---------- snacks ----------
food("chips_potato", "Patates cipsi", "Potato chips", "snack", 536, 7.0, 53.0, 34.6, 4.4, "pack:40,handful:25", "", "usda", "cips")
food("chips_corn", "Mısır cipsi", "Tortilla chips", "snack", 498, 7.0, 63.0, 24.5, 5.0, "pack:40,handful:25", "", "usda", "cips")
food("popcorn", "Patlamış mısır", "Popcorn (air-popped)", "snack", 387, 12.9, 77.8, 4.5, 14.5, "glass:8,portion:30", "", "usda", "misir")
food("protein_bar", "Protein bar", "Protein bar", "snack", 388, 30.0, 40.0, 12.0, 5.0, "piece:50", "dairy", "label")

# ---------- drinks ----------
food("tea", "Çay (şekersiz)", "Black tea (unsweetened)", "drink", 1, 0, 0.3, 0, 0, "tea_glass:100,ml:1", "", "usda", "cay")
food("turkish_coffee", "Türk kahvesi (şekersiz)", "Turkish coffee (unsweetened)", "drink", 6, 0.3, 0.8, 0.2, 0, "cup:65", "", "label", "kahve")
food("filter_coffee", "Filtre kahve", "Brewed coffee", "drink", 1, 0.1, 0, 0, 0, "mug:250,ml:1", "", "usda", "kahve,americano")
food("latte", "Latte", "Caffe latte", "drink", 57, 3.0, 4.5, 3.0, 0, "mug:300,ml:1", "dairy", "usda", "kahve")
food("orange_juice", "Portakal suyu (taze)", "Orange juice (fresh)", "drink", 45, 0.7, 10.4, 0.2, 0.2, "glass:200,ml:1", "", "usda")
food("fruit_nectar", "Meyve suyu (nektar)", "Fruit nectar", "drink", 50, 0.2, 12.4, 0.1, 0.2, "glass:200,ml:1", "", "label", "meyve suyu")
food("cola", "Kola", "Cola", "drink", 42, 0, 10.6, 0, 0, "can:330,glass:200,ml:1", "", "usda", "gazli icecek")
food("cola_zero", "Kola (şekersiz)", "Diet cola", "drink", 0.3, 0, 0.1, 0, 0, "can:330,glass:200,ml:1", "", "usda", "zero,light")
food("salgam", "Şalgam suyu", "Salgam (turnip juice)", "drink", 6, 0.2, 1.3, 0, 0, "glass:200,ml:1", "", "label", "salgam")
food("lemonade", "Limonata", "Lemonade", "drink", 45, 0, 11.5, 0, 0, "glass:200,ml:1", "", "usda")
food("mineral_water", "Maden suyu", "Mineral water", "drink", 0, 0, 0, 0, 0, "glass:200,can:200,ml:1", "", "usda", "soda")
food("boza", "Boza", "Boza", "drink", 99, 1.0, 23.0, 0.3, 0.5, "glass:200,ml:1", "gluten", "label")
food("beer", "Bira", "Beer", "drink", 43, 0.5, 3.6, 0, 0, "can:500,glass:330,ml:1", "alcohol,gluten", "usda")
food("wine_red", "Kırmızı şarap", "Red wine", "drink", 85, 0.1, 2.6, 0, 0, "glass:150,ml:1", "alcohol", "usda", "sarap")

# ---------- fats, sauces, condiments ----------
food("olive_oil", "Zeytinyağı", "Olive oil", "fat", 884, 0, 0, 100, 0, "tsp:5,tbsp:13", "", "usda", "zeytinyagi")
food("sunflower_oil", "Ayçiçek yağı", "Sunflower oil", "fat", 884, 0, 0, 100, 0, "tsp:5,tbsp:13", "", "usda", "yag")
food("mayonnaise", "Mayonez", "Mayonnaise", "sauce", 680, 1.0, 0.6, 75.0, 0, "tbsp:14", "egg", "usda")
food("ketchup", "Ketçap", "Ketchup", "sauce", 101, 1.0, 27.4, 0.1, 0.3, "tbsp:15", "", "usda")
food("mustard", "Hardal", "Mustard", "sauce", 60, 3.7, 5.8, 3.3, 4.0, "tsp:5", "", "usda")
food("tomato_paste", "Domates salçası", "Tomato paste", "sauce", 82, 4.3, 18.9, 0.5, 4.1, "tbsp:16", "", "usda", "salca")
food("pepper_paste", "Biber salçası", "Pepper paste", "sauce", 70, 2.5, 13.0, 0.9, 3.0, "tbsp:16", "", "label", "salca")
food("sugar", "Şeker", "Sugar", "sauce", 387, 0, 100.0, 0, 0, "piece:4,tsp:4,tbsp:12", "", "usda", "kup seker")


def kcal_of(p, c, f):
    return 4 * p + 4 * c + 9 * f


def main(out_path):
    ids = set()
    problems = []
    foods = []
    for (id, tr, en, cat, kcal, p, c, f, fiber, servings, tags, src, kw) in F:
        if id in ids:
            problems.append(f"duplicate id {id}")
        ids.add(id)
        tag_list = [t for t in tags.split(",") if t]
        if "alcohol" not in tag_list and kcal >= 20:
            est = kcal_of(p, c, f)
            # USDA kcal uses specific Atwater factors and counts fibre lower, so allow some drift.
            if abs(est - kcal) / kcal > 0.20 and abs(est - kcal) > 10:
                problems.append(f"{id}: kcal {kcal} vs 4/4/9 {est:.0f}")
        serv = []
        for s in servings.split(","):
            unit, grams = s.split(":")
            grams = float(grams)
            if grams <= 0:
                problems.append(f"{id}: serving {s}")
            serv.append({"u": unit, "g": grams if grams % 1 else int(grams)})
        item = {
            "id": id, "tr": tr, "en": en, "cat": cat,
            "kcal": kcal, "p": p, "c": c, "f": f,
        }
        if fiber:
            item["fiber"] = fiber
        item["s"] = serv
        if tag_list:
            item["tags"] = tag_list
        if kw:
            item["kw"] = [k for k in kw.split(",") if k]
        item["src"] = src
        foods.append(item)
    if problems:
        print("\n".join(problems))
        sys.exit(1)
    lines = ",\n".join("    " + json.dumps(item, ensure_ascii=False, separators=(",", ":")) for item in foods)
    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write('{\n  "version": 1,\n  "foods": [\n' + lines + "\n  ]\n}\n")
    cats = {}
    for item in foods:
        cats[item["cat"]] = cats.get(item["cat"], 0) + 1
    print(len(foods), "foods", cats)


if __name__ == "__main__":
    import os
    default = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "commonMain", "composeResources", "files", "foods.json")
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.normpath(default))
