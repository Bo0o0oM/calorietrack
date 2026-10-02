# CalorieTrack â€” Local Food Data Strategy

This document details how CalorieTrack manages its offline food catalogue: how foods are packaged, seeded into SQLite, identified, and scaled over time without requiring cloud APIs or schema redesigns.

---

## 1. Packaging Strategy: The Asset-Seeded Database

To guarantee 100% offline functionality on the very first app launch, the initial food catalogue is pre-packaged directly inside the APK as an embedded asset.

### How Room Pre-populates the Database
Android Room natively supports database pre-population from an asset file via the `createFromAsset()` API:

```kotlin
Room.databaseBuilder(context, CalorieTrackDatabase::class.java, "calorietrack.db")
    .createFromAsset("database/calorietrack.db")
    .addMigrations(CalorieTrackDatabase.Migration1To2(context))
    .build()
```

### Benefits of Pre-Bundling:
1. **Zero First-Run Download**: The app doesn't need to ask the user to wait while downloading hundreds of megabytes on first launch.
2. **Instant Search Readiness**: The moment the app opens, the SQLite search indexes are already compiled and queryable.
3. **Low Storage Impact**: A compressed initial database of 104 foundational foods occupies only **~60 KB** in the APK assets.

---

## 2. Food Identification & Differentiation

Every food in the database is uniquely identified by an integer Primary Key (`id`), but distinguished by an `is_custom` flag:

```
[ foods Table ]
 â”œâ”€â”€ id: 101, name: "Egg, whole, boiled", is_custom: 0  <-- Built-in reference food
 â””â”€â”€ id: 102, name: "Protein Shake",     is_custom: 1  <-- User-created custom food
```

### Built-in Foods (`is_custom = 0`):
- Read-only reference foods bundled with the app.
- Protected from accidental deletion so the baseline catalogue is never corrupted.
- Sourced from standardized public nutrition datasets (USDA FoodData Central).
- Deterministic IDs assigned sequentially from 1 to 104.

### Custom Foods (`is_custom = 1`):
- Created locally by the user when an item is not found in the reference catalogue.
- Can be freely edited, updated, or deleted by the user at any time.
- Filterable in search results with a special badge (e.g., *"Custom"* or *"My Food"*).
- IDs automatically generated starting at 1000 to prevent collisions with built-in foods.

---

## 3. Data Source Attribution & Catalogue Evolution

### Initial Foundation (Milestone 2B):
Bundled 104 baseline foods (eggs, oats, chicken breast, milk, rice, etc.).

### Expanded 500-Item Indian-Focused Catalogue (Milestone 2D):
To make CalorieTrack practical and delightful for real-world daily logging, Milestone 2D expanded the built-in offline catalogue from 104 to **500 curated food items**, strongly prioritizing an **Indian-focused diet**:
- **Grains, Flours, Breads & Breakfast Staples (45 items)**: Roti/Phulka, Parathas (plain, aloo, gobi, paneer, methi, mooli), Naan, Poori, Bhatura, Kulcha, Missi Roti, Makki ki Roti, Bajra/Jowar/Ragi Roti, Thepla, Idli, Dosa (plain, masala, rava), Uttapam, Upma, Poha, Khichdi, Pongal, Appam, Puttu, Sabudana Khichdi, Besan/Moong Chilla, Dalia, Basmati rice, Jeera rice, Curd rice, Lemon rice.
- **Dals, Lentils, Pulses, Legumes & Soy (45 items)**: Toor/Arhar Dal, Moong Dal (yellow, split chilka, sabut green), Masoor Dal (red split, whole brown), Urad Dal (split chilka, sabut black, dhuli white), Chana Dal, Bhuna Chana (with & without skin), Sattu, Kabuli Chana/Chole, Kala Chana, Rajma (red, white/cannellini), Lobia/Cowpeas, Moth/Matki, Kulthi/Horse Gram, Green Peas, Safed Vatana, Soya Chunks/Granules, Tofu (firm, silken), Tempeh, Sprouted lentils.
- **Dairy, Curds, Cheeses & Traditional Milk Delicacies (45 items)**: Buffalo milk, Cow milk (skimmed, toned, double toned), Desi cow ghee, Buffalo ghee, White butter/makhan, Low-fat & Malai Paneer, Chenna, Hung curd/chakka, Sweet & Salted Lassi, Masala Chaas, Condensed/Evaporated milk, Khoya/Mawa, Whole/Skim milk powder, Fresh Malai, Mozzarella, Processed cheese, Shrikhand, Mishti Doi, Kulfi, Rabri, Rasgulla, Gulab Jamun, Rasmalai, Sandesh, Kalakand, Peda, Milk Cake, Paneer Bhurji.
- **Vegetables, Gourds, Greens & Roots (75 items)**: Bhindi/Okra, Baingan/Eggplant, Baingan Bharta, Lauki/Bottle Gourd, Turai/Ridge Gourd, Karela/Bitter Gourd, Tinda, Parwal, Kundru/Tindora, Arbi/Taro Root, Jimikand/Suran, Drumstick/Moringa pods & leaves, Methi leaves, Sarson Saag, Bathua, Chaulai, Palak/Spinach, Coriander, Mint, Curry leaves, Green chili, Bell peppers (green, red, yellow), Cabbage, Cauliflower, Beetroot, Radish/Mooli, Red Indian winter carrot, Kheera/Cucumber, English Kakdi, Raw mango (Kaccha Aam), Raw banana, Raw jackfruit (Kathal), Lotus stem (Kamal Kakdi), Button/Oyster mushroom, Pumpkin/Kaddu, Ash gourd/Petha, Snake gourd, Sponge gourd/Nenua, Turnip/Shalgam, Knol Khol, French beans, Cluster beans/Gawar, Broad beans/Sem, Onions, Garlic, Ginger, Boiled potatoes.
- **Fruits & Fresh Produce (45 items)**: Alphonso/Kesar Mango, Guava/Amrood, Papaya/Papita, Pomegranate/Anar, Watermelon/Tarbooj, Muskmelon/Kharbooja, Sweet lime/Mosambi, Orange/Santra, Kinnow, Chiku/Sapodilla, Custard apple/Sitaphal, Jamun/Black plum, Lychee, Pineapple, Amla/Indian gooseberry, Tender coconut water, Coconut meat/copra, Dry coconut/khopra, Robusta & Yelakki bananas, Apple/Seb, Pear/Nashpati, Grapes (green, black), Fresh & Dried figs/Anjeer, Fresh & Dried dates (Medjool, Chhuhara), Plum, Peach, Apricot, Kiwi, Strawberries, Blueberries, Blackberries, Raspberries, Cherries, Mulberry/Shahtoot, Dragonfruit, Passion fruit, Tamarind/Imli, Starfruit, Bel fruit, Ber/Jujube, Rasbhari, Amchur slices.
- **Nuts, Seeds, Dried Fruits & Healthy Snacks (35 items)**: Almonds (raw, roasted), Walnuts, Cashews (raw, roasted salted), Pistachios (raw, roasted), Raisins (golden, black munakka), Makhana/Foxnuts (raw, roasted in ghee), Chia/Sabja seeds (raw, soaked), Flaxseeds/Alsi, Sesame seeds (white, black til), Pumpkin seeds, Sunflower seeds, Watermelon seeds (magaz), Pine nuts/Chilgoza, Brazil nuts, Hazelnuts, Pecans, Macadamia, Murmura/Puffed rice, Chutney dal, Plain & Methi Khakhra, Poha chivda, Makhana chivda, Diet roasted namkeen, Marie biscuit, Coconut chips.
- **Spices, Seasonings, Condiments & Cooking Oils (40 items)**: Turmeric/Haldi, Jeera (whole, roasted powder), Dhania powder, Lal mirch powder, Kashmiri chili powder, Garam masala, Chhoti & Badi Elaichi, Cloves/Laung, Dalchini/Cinnamon, Kali mirch (whole, powder), Saunf, Methi dana, Ajwain, Rai & Yellow mustard seeds, Hing, Tej patta, Star anise, Jaiphal, Javitri, Amchur powder, Chaat masala, Sambar powder, Rasam powder, Kitchen King, Biryani masala, Kasuri methi, Mustard oil, Peanut oil, Sesame oil, Coconut oil, Sunflower oil, Soybean oil, Rice bran oil, Jaggery/Gur, Shakkar, Pure honey.
- **Popular Prepared Indian Dishes, Curries & Rice (40 items)**: Dal Tadka, Dal Makhani, Chole Masala, Rajma Masala, Sambar, Rasam, Kadhi Pakora, Gujarati Kadhi, Palak Paneer, Paneer Butter Masala, Shahi Paneer, Matar Paneer, Kadai Paneer, Aloo Gobi, Aloo Matar, Aloo Palak, Bhindi Masala, Lauki Chana Dal, Mix Veg Curry, Veg Biryani, Veg Pulao, Dal Khichdi, Pav Bhaji, Misal Pav Gravy, Chana Chaat, Bhelpuri, Sev Puri, Pani Puri/Golgappa, Dahi Vada/Bhalla, Veg Samosa, Pakora, Khaman Dhokla, Medu Vada, Masala Omelette, Egg Bhurji, Egg Curry, Chicken Curry, Butter Chicken, Chicken Tikka, Chicken Biryani.
- **Poultry, Meat, Fish, Traditional Desserts & Drinks (26 items)**: Tandoori Chicken, Chicken Keema, Mutton Curry/Rogan Josh, Mutton Keema, Indian Fish Curry, Fish Fry, Prawn Masala, Katla, Hilsa/Ilish, Pomfret Fry, Crab Masala, Masala Chai, Ginger Tea/Adrak Chai, Filter Coffee, Green Tea, Nimbu Pani/Shikanji, Aam Panna, Badam Milk, Gajar ka Halwa, Moong Dal Halwa, Sooji Halwa/Sheera, Rice Kheer, Besan Ladoo, Kaju Katli, Motichoor Ladoo, Jalebi.

### Bilingual Search Keywords
To allow frictionless search regardless of whether users search in English or Indian transliteration, every food item includes a comprehensive `search_keywords` field indexed with a SQLite B-Tree index:
- Example: Searching `"chole"` matches Chickpeas (Cooked), White Chickpeas (Raw), and Chole Masala.
- Example: Searching `"roti"` or `"chapati"` matches Tawa Roti, Butter Roti, Rumali Roti, and Missi Roti.
- Example: Searching `"dahi"` matches Plain Curd / Dahi, Greek Yogurt, Curd Rice, Hung Curd, and Sweet Lassi.
- Example: Searching `"bhindi"` matches Bhindi Raw, Bhindi Sautéed, and Bhindi Masala.

### Source Attribution
The nutritional values for all 500 built-in foods are sourced exclusively from public-domain, authoritative nutritional databases:
- **USDA FoodData Central (FDC)**: Standard Reference Legacy, Foundation Foods, and Survey (FNDDS) food composition datasets.
- Every built-in item has a verified `source_id` matching its USDA FoodData Central identifier.
- Normalized to standard metric units (`100g` base serving) alongside intuitive household portion descriptions.
- Both raw reference JSON (`app/src/main/assets/source/food_catalogue.json`) and compiled SQLite binary (`app/src/main/assets/database/calorietrack.db`) are tracked in assets.


---

## 4. Scaling to Thousands of Foods (Without Redesigning the Schema)

The schema designed in `docs/database-design.md` was intentionally crafted so that expanding from 100 foods to 50,000 foods requires **zero database migrations or architectural redesigns**:

1. **Normalized Nutrition Columns**: Calories, protein, carbs, and fat are clean numeric columns (`REAL`), which query at the same speed whether the table has 100 rows or 100,000 rows.
2. **SQLite B-Tree Indexes**: The index on `foods(name)` allows SQLite to find matching text prefixes in logarithmic time ($O(\log N)$), with a target search execution time of under 15 milliseconds on a mobile CPU.
3. **Future Full-Text Search (FTS5)**: When scaling to a large catalogue of 20,000+ items, SQLite's built-in `FTS5` (Full-Text Search) module can be enabled on the `foods` table with a one-line Room annotation (`@Fts4` or `@Fts5`). This unlocks fuzzy search and multi-word matching (e.g., finding *"Greek yogurt low fat"* from *"yogurt greek"*) without changing the rest of the application.
4. **Offline Update Packs**: In future versions, updated or expanded food catalogues can be distributed as compressed local database patches or SQLite files during app updates without clearing the user's historical meal logs.
