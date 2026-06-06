package com.letmcook.letmcook.services

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.letmcook.letmcook.models.*
import java.text.SimpleDateFormat
import java.util.*

class DatabaseService(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "letmcook_integrated.db"
        private const val DATABASE_VERSION = 7

        // Table Names (Matching SQL Schema)
        private const val TABLE_USER = "User"
        private const val TABLE_NUTRITION_GOAL = "NutritionGoal"
        private const val TABLE_INGREDIENT = "Ingredient"
        private const val TABLE_RECIPE = "Recipe"
        private const val TABLE_PANTRY_ITEM = "PantryItem"
        private const val TABLE_RECIPE_INGREDIENT = "RecipeIngredient"
        private const val TABLE_MEAL_PLAN = "MealPlan"
        
        // Local-only/Extension Tables
        private const val TABLE_SETTINGS = "AppSettings"
        private const val TABLE_INTAKE = "Intake"
        private const val TABLE_GROCERY = "GroceryItem"
        private const val TABLE_WORKOUT = "Workout"
        private const val TABLE_WATER = "WaterIntake"

        // Common Column Names
        private const val COL_ID = "id"
        private const val COL_OWNER_ID = "owner_id"
        private const val COL_IS_DELETED = "is_deleted"
        private const val COL_CREATED_AT = "created_at"

        // User Table Columns
        private const val COL_USER_EMAIL = "email"
        private const val COL_USER_PASSWORD_HASH = "password_hash"
        private const val COL_USER_DISPLAY_NAME = "display_name"
        private const val COL_USER_FULL_NAME = "full_name"
        private const val COL_USER_ROLE = "role"

        // AppSettings Table Columns
        private const val COL_SETTINGS_LARGE_TEXT = "use_large_text"
        private const val COL_SETTINGS_KEEP_ON = "keep_screen_on"

        // NutritionGoal Table Columns
        private const val COL_GOAL_CALORIES = "daily_calorie_target"
        private const val COL_GOAL_PROTEIN = "protein_target_grams"
        private const val COL_GOAL_CARBS = "carb_target_grams"
        private const val COL_GOAL_FAT = "fat_target_grams"

        // PantryItem Table Columns
        private const val COL_PANTRY_INGREDIENT_ID = "ingredient_id"
        private const val COL_PANTRY_QUANTITY = "current_quantity"
        private const val COL_PANTRY_EXP_DATE = "expiration_date"

        // Ingredient Table Columns
        private const val COL_ING_NAME = "name"
        private const val COL_ING_CATEGORY = "category"
        private const val COL_ING_UNIT = "unit_of_measure"
        private const val COL_ING_CALORIES = "calories" // Extension for local calc
        private const val COL_ING_PROTEIN = "protein" // Extension for local calc
        private const val COL_ING_CARBS = "carbs" // Extension for local calc
        private const val COL_ING_FAT = "fat" // Extension for local calc

        // Recipe Table Columns
        private const val COL_RECIPE_TITLE = "title"
        private const val COL_RECIPE_INSTRUCTIONS = "instructions"
        private const val COL_RECIPE_CALORIES = "total_calories"
        private const val COL_RECIPE_PROTEIN = "total_protein" // Extension
        private const val COL_RECIPE_CARBS = "total_carbs" // Extension
        private const val COL_RECIPE_FAT = "total_fat" // Extension
        private const val COL_RECIPE_IMAGE = "image_url" // Extension

        // RecipeIngredient Table Columns
        private const val COL_RI_RECIPE_ID = "recipe_id"
        private const val COL_RI_INGREDIENT_ID = "ingredient_id"
        private const val COL_RI_QUANTITY = "required_quantity"

        // MealPlan Table Columns
        private const val COL_MP_RECIPE_ID = "recipe_id"
        private const val COL_MP_DATE = "planned_date"
        private const val COL_MP_TYPE = "meal_type"

        // Intake Table Columns
        private const val COL_INTAKE_RECIPE_ID = "recipe_id"
        private const val COL_INTAKE_INGREDIENT_ID = "ingredient_id"
        private const val COL_INTAKE_QUANTITY = "quantity"
        private const val COL_INTAKE_DATE = "date"
        private const val COL_INTAKE_CALORIES = "calories"
        private const val COL_INTAKE_PROTEIN = "protein"
        private const val COL_INTAKE_CARBS = "carbs"
        private const val COL_INTAKE_FAT = "fat"

        // Grocery Table Columns
        private const val COL_GROCERY_ING_ID = "ingredient_id"
        private const val COL_GROCERY_QUANTITY = "quantity"
        private const val COL_GROCERY_IS_BOUGHT = "is_bought"

        // Workout Table Columns
        private const val COL_WORKOUT_TITLE = "title"
        private const val COL_WORKOUT_TYPE = "type"
        private const val COL_WORKOUT_DURATION = "duration_minutes"
        private const val COL_WORKOUT_CALORIES = "calories_burned"
        private const val COL_WORKOUT_DESC = "description"
        private const val COL_WORKOUT_COMPLETED = "is_completed"
        private const val COL_WORKOUT_DATE = "scheduled_date"

        // Water Table Columns
        private const val COL_WATER_AMOUNT = "amount_ml"
        private const val COL_WATER_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USER (
                $COL_ID TEXT PRIMARY KEY,
                $COL_USER_EMAIL TEXT NOT NULL,
                $COL_USER_PASSWORD_HASH TEXT,
                $COL_USER_DISPLAY_NAME TEXT,
                $COL_USER_FULL_NAME TEXT,
                $COL_USER_ROLE INTEGER,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_SETTINGS (
                id INTEGER PRIMARY KEY CHECK (id = 0),
                $COL_SETTINGS_LARGE_TEXT INTEGER DEFAULT 0,
                $COL_SETTINGS_KEEP_ON INTEGER DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_NUTRITION_GOAL (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT NOT NULL,
                $COL_GOAL_CALORIES INTEGER,
                $COL_GOAL_PROTEIN INTEGER,
                $COL_GOAL_CARBS INTEGER,
                $COL_GOAL_FAT INTEGER,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_INGREDIENT (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT NOT NULL,
                $COL_ING_NAME TEXT NOT NULL,
                $COL_ING_CATEGORY TEXT,
                $COL_ING_UNIT TEXT,
                $COL_ING_CALORIES REAL,
                $COL_ING_PROTEIN REAL,
                $COL_ING_CARBS REAL,
                $COL_ING_FAT REAL,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_PANTRY_ITEM (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT NOT NULL,
                $COL_PANTRY_INGREDIENT_ID TEXT NOT NULL,
                $COL_PANTRY_QUANTITY REAL DEFAULT 0,
                $COL_PANTRY_EXP_DATE TEXT,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_RECIPE (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT NOT NULL,
                $COL_RECIPE_TITLE TEXT NOT NULL,
                $COL_RECIPE_INSTRUCTIONS TEXT,
                $COL_RECIPE_CALORIES REAL,
                $COL_RECIPE_PROTEIN REAL,
                $COL_RECIPE_CARBS REAL,
                $COL_RECIPE_FAT REAL,
                $COL_RECIPE_IMAGE TEXT,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_RECIPE_INGREDIENT (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT NOT NULL,
                $COL_RI_RECIPE_ID TEXT NOT NULL,
                $COL_RI_INGREDIENT_ID TEXT NOT NULL,
                $COL_RI_QUANTITY REAL NOT NULL,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_MEAL_PLAN (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT NOT NULL,
                $COL_MP_RECIPE_ID TEXT NOT NULL,
                $COL_MP_DATE TEXT NOT NULL,
                $COL_MP_TYPE TEXT,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_INTAKE (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT,
                $COL_INTAKE_RECIPE_ID TEXT,
                $COL_INTAKE_INGREDIENT_ID TEXT,
                $COL_INTAKE_QUANTITY REAL,
                $COL_INTAKE_DATE TEXT,
                $COL_INTAKE_CALORIES REAL,
                $COL_INTAKE_PROTEIN REAL,
                $COL_INTAKE_CARBS REAL,
                $COL_INTAKE_FAT REAL,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_GROCERY (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT,
                $COL_GROCERY_ING_ID TEXT,
                $COL_GROCERY_QUANTITY REAL,
                $COL_GROCERY_IS_BOUGHT INTEGER DEFAULT 0,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT
            )
        """)
        
        db.execSQL("""
            CREATE TABLE $TABLE_WORKOUT (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT,
                $COL_WORKOUT_TITLE TEXT,
                $COL_WORKOUT_TYPE TEXT,
                $COL_WORKOUT_DURATION INTEGER,
                $COL_WORKOUT_CALORIES INTEGER,
                $COL_WORKOUT_DESC TEXT,
                $COL_WORKOUT_COMPLETED INTEGER DEFAULT 0,
                $COL_WORKOUT_DATE TEXT,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_CREATED_AT TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_WATER (
                $COL_ID TEXT PRIMARY KEY,
                $COL_OWNER_ID TEXT,
                $COL_WATER_AMOUNT INTEGER,
                $COL_WATER_DATE TEXT,
                $COL_CREATED_AT TEXT
            )
        """)
        
        db.execSQL("INSERT INTO $TABLE_SETTINGS (id, $COL_SETTINGS_LARGE_TEXT, $COL_SETTINGS_KEEP_ON) VALUES (0, 0, 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NUTRITION_GOAL")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PANTRY_ITEM")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INGREDIENT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECIPE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECIPE_INGREDIENT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEAL_PLAN")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INTAKE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROCERY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WATER")
        onCreate(db)
    }

    // --- Operations ---

    fun upsertUser(user: UserModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, user.id)
            put(COL_USER_EMAIL, user.email)
            put(COL_USER_DISPLAY_NAME, user.displayName)
            put(COL_USER_FULL_NAME, user.fullName)
            put(COL_USER_ROLE, user.role)
            put(COL_IS_DELETED, if (user.isDeleted) 1 else 0)
            put(COL_CREATED_AT, user.createdAt)
        }
        db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUser(id: String): UserModel? {
        val db = readableDatabase
        val cursor = db.query(TABLE_USER, null, "$COL_ID = ?", arrayOf(id), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                UserModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    email = it.getString(it.getColumnIndexOrThrow(COL_USER_EMAIL)),
                    displayName = it.getString(it.getColumnIndexOrThrow(COL_USER_DISPLAY_NAME)),
                    fullName = it.getString(it.getColumnIndexOrThrow(COL_USER_FULL_NAME)),
                    role = it.getInt(it.getColumnIndexOrThrow(COL_USER_ROLE)),
                    isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_DELETED)) == 1,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                )
            } else null
        }
    }

    fun getAppSettings(): AppSettingsModel {
        val db = readableDatabase
        val cursor = db.query(TABLE_SETTINGS, null, "id = 0", null, null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                AppSettingsModel(
                    useLargeText = it.getInt(it.getColumnIndexOrThrow(COL_SETTINGS_LARGE_TEXT)) == 1,
                    keepScreenOn = it.getInt(it.getColumnIndexOrThrow(COL_SETTINGS_KEEP_ON)) == 1
                )
            } else AppSettingsModel()
        }
    }

    fun updateAppSettings(settings: AppSettingsModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SETTINGS_LARGE_TEXT, if (settings.useLargeText) 1 else 0)
            put(COL_SETTINGS_KEEP_ON, if (settings.keepScreenOn) 1 else 0)
        }
        db.update(TABLE_SETTINGS, values, "id = 0", null)
    }

    // Nutrition Goals
    fun upsertNutritionGoal(goal: NutritionGoalModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, goal.id)
            put(COL_OWNER_ID, goal.ownerId)
            put(COL_GOAL_CALORIES, goal.dailyCalorieTarget)
            put(COL_GOAL_PROTEIN, goal.proteinTargetGrams)
            put(COL_GOAL_CARBS, goal.carbTargetGrams)
            put(COL_GOAL_FAT, goal.fatTargetGrams)
            put(COL_IS_DELETED, if (goal.isDeleted) 1 else 0)
            put(COL_CREATED_AT, goal.createdAt)
        }
        db.insertWithOnConflict(TABLE_NUTRITION_GOAL, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getNutritionGoal(ownerId: String): NutritionGoalModel? {
        val db = readableDatabase
        val cursor = db.query(TABLE_NUTRITION_GOAL, null, "$COL_OWNER_ID = ? AND $COL_IS_DELETED = 0", arrayOf(ownerId), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                NutritionGoalModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    dailyCalorieTarget = it.getInt(it.getColumnIndexOrThrow(COL_GOAL_CALORIES)),
                    proteinTargetGrams = it.getInt(it.getColumnIndexOrThrow(COL_GOAL_PROTEIN)),
                    carbTargetGrams = it.getInt(it.getColumnIndexOrThrow(COL_GOAL_CARBS)),
                    fatTargetGrams = it.getInt(it.getColumnIndexOrThrow(COL_GOAL_FAT)),
                    isDeleted = false,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                )
            } else null
        }
    }

    // Ingredients
    fun getAllIngredients(): List<IngredientModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_INGREDIENT, null, "$COL_IS_DELETED = 0", null, null, null, "$COL_ING_NAME ASC")
        val list = mutableListOf<IngredientModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(IngredientModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COL_ING_NAME)),
                    category = it.getString(it.getColumnIndexOrThrow(COL_ING_CATEGORY)),
                    unitOfMeasure = it.getString(it.getColumnIndexOrThrow(COL_ING_UNIT)),
                    calories = it.getDouble(it.getColumnIndexOrThrow(COL_ING_CALORIES)),
                    protein = it.getDouble(it.getColumnIndexOrThrow(COL_ING_PROTEIN)),
                    carbs = it.getDouble(it.getColumnIndexOrThrow(COL_ING_CARBS)),
                    fat = it.getDouble(it.getColumnIndexOrThrow(COL_ING_FAT)),
                    isDeleted = false,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertIngredient(ingredient: IngredientModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, ingredient.id)
            put(COL_OWNER_ID, ingredient.ownerId)
            put(COL_ING_NAME, ingredient.name)
            put(COL_ING_CATEGORY, ingredient.category)
            put(COL_ING_UNIT, ingredient.unitOfMeasure)
            put(COL_ING_CALORIES, ingredient.calories)
            put(COL_ING_PROTEIN, ingredient.protein)
            put(COL_ING_CARBS, ingredient.carbs)
            put(COL_ING_FAT, ingredient.fat)
            put(COL_IS_DELETED, if (ingredient.isDeleted) 1 else 0)
            put(COL_CREATED_AT, ingredient.createdAt)
        }
        db.insertWithOnConflict(TABLE_INGREDIENT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Pantry
    fun getPantryItems(ownerId: String): List<PantryItemModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_PANTRY_ITEM, null, "$COL_OWNER_ID = ? AND $COL_IS_DELETED = 0", arrayOf(ownerId), null, null, null)
        val list = mutableListOf<PantryItemModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(PantryItemModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    ingredientId = it.getString(it.getColumnIndexOrThrow(COL_PANTRY_INGREDIENT_ID)),
                    currentQuantity = it.getDouble(it.getColumnIndexOrThrow(COL_PANTRY_QUANTITY)),
                    expirationDate = it.getString(it.getColumnIndexOrThrow(COL_PANTRY_EXP_DATE)),
                    isDeleted = false,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertPantryItem(item: PantryItemModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, item.id)
            put(COL_OWNER_ID, item.ownerId)
            put(COL_PANTRY_INGREDIENT_ID, item.ingredientId)
            put(COL_PANTRY_QUANTITY, item.currentQuantity)
            put(COL_PANTRY_EXP_DATE, item.expirationDate)
            put(COL_IS_DELETED, if (item.isDeleted) 1 else 0)
            put(COL_CREATED_AT, item.createdAt)
        }
        db.insertWithOnConflict(TABLE_PANTRY_ITEM, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Recipes
    fun getAllRecipes(): List<RecipeModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_RECIPE, null, "$COL_IS_DELETED = 0", null, null, null, "$COL_RECIPE_TITLE ASC")
        val list = mutableListOf<RecipeModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(RecipeModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_RECIPE_TITLE)),
                    instructions = it.getString(it.getColumnIndexOrThrow(COL_RECIPE_INSTRUCTIONS)),
                    totalCalories = it.getDouble(it.getColumnIndexOrThrow(COL_RECIPE_CALORIES)),
                    totalProtein = it.getDouble(it.getColumnIndexOrThrow(COL_RECIPE_PROTEIN)),
                    totalCarbs = it.getDouble(it.getColumnIndexOrThrow(COL_RECIPE_CARBS)),
                    totalFat = it.getDouble(it.getColumnIndexOrThrow(COL_RECIPE_FAT)),
                    imageUrl = it.getString(it.getColumnIndexOrThrow(COL_RECIPE_IMAGE)),
                    isDeleted = false,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertRecipe(recipe: RecipeModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, recipe.id)
            put(COL_OWNER_ID, recipe.ownerId)
            put(COL_RECIPE_TITLE, recipe.title)
            put(COL_RECIPE_INSTRUCTIONS, recipe.instructions)
            put(COL_RECIPE_CALORIES, recipe.totalCalories)
            put(COL_RECIPE_PROTEIN, recipe.totalProtein)
            put(COL_RECIPE_CARBS, recipe.totalCarbs)
            put(COL_RECIPE_FAT, recipe.totalFat)
            put(COL_RECIPE_IMAGE, recipe.imageUrl)
            put(COL_IS_DELETED, if (recipe.isDeleted) 1 else 0)
            put(COL_CREATED_AT, recipe.createdAt)
        }
        db.insertWithOnConflict(TABLE_RECIPE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteRecipe(recipeId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_DELETED, 1)
        }
        db.update(TABLE_RECIPE, values, "$COL_ID = ?", arrayOf(recipeId))
        db.update(TABLE_RECIPE_INGREDIENT, values, "$COL_RI_RECIPE_ID = ?", arrayOf(recipeId))
    }

    fun upsertRecipeWithIngredients(recipe: RecipeModel, ingredients: List<RecipeIngredientModel>) {
        if (ingredients.isEmpty()) {
            throw IllegalArgumentException("A recipe must have at least one ingredient.")
        }
        
        val db = writableDatabase
        db.beginTransaction()
        try {
            upsertRecipe(recipe)
            ingredients.forEach { upsertRecipeIngredient(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRecipeIngredients(recipeId: String): List<RecipeIngredientModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_RECIPE_INGREDIENT, null, "$COL_RI_RECIPE_ID = ? AND $COL_IS_DELETED = 0", arrayOf(recipeId), null, null, null)
        val list = mutableListOf<RecipeIngredientModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(RecipeIngredientModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    recipeId = it.getString(it.getColumnIndexOrThrow(COL_RI_RECIPE_ID)),
                    ingredientId = it.getString(it.getColumnIndexOrThrow(COL_RI_INGREDIENT_ID)),
                    requiredQuantity = it.getDouble(it.getColumnIndexOrThrow(COL_RI_QUANTITY)),
                    isDeleted = false,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertRecipeIngredient(ri: RecipeIngredientModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, ri.id)
            put(COL_OWNER_ID, ri.ownerId)
            put(COL_RI_RECIPE_ID, ri.recipeId)
            put(COL_RI_INGREDIENT_ID, ri.ingredientId)
            put(COL_RI_QUANTITY, ri.requiredQuantity)
            put(COL_IS_DELETED, if (ri.isDeleted) 1 else 0)
            put(COL_CREATED_AT, ri.createdAt)
        }
        db.insertWithOnConflict(TABLE_RECIPE_INGREDIENT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Intake
    fun getIntakeForDate(ownerId: String, date: String): List<IntakeModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_INTAKE, null, "$COL_OWNER_ID = ? AND $COL_INTAKE_DATE = ? AND $COL_IS_DELETED = 0", arrayOf(ownerId, date), null, null, null)
        val list = mutableListOf<IntakeModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(IntakeModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    recipeId = it.getString(it.getColumnIndexOrThrow(COL_INTAKE_RECIPE_ID)),
                    ingredientId = it.getString(it.getColumnIndexOrThrow(COL_INTAKE_INGREDIENT_ID)),
                    quantity = it.getDouble(it.getColumnIndexOrThrow(COL_INTAKE_QUANTITY)),
                    date = it.getString(it.getColumnIndexOrThrow(COL_INTAKE_DATE)),
                    calories = it.getDouble(it.getColumnIndexOrThrow(COL_INTAKE_CALORIES)),
                    protein = it.getDouble(it.getColumnIndexOrThrow(COL_INTAKE_PROTEIN)),
                    carbs = it.getDouble(it.getColumnIndexOrThrow(COL_INTAKE_CARBS)),
                    fat = it.getDouble(it.getColumnIndexOrThrow(COL_INTAKE_FAT)),
                    isDeleted = false,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertIntake(intake: IntakeModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, intake.id)
            put(COL_OWNER_ID, intake.ownerId)
            put(COL_INTAKE_RECIPE_ID, intake.recipeId)
            put(COL_INTAKE_INGREDIENT_ID, intake.ingredientId)
            put(COL_INTAKE_QUANTITY, intake.quantity)
            put(COL_INTAKE_DATE, intake.date)
            put(COL_INTAKE_CALORIES, intake.calories)
            put(COL_INTAKE_PROTEIN, intake.protein)
            put(COL_INTAKE_CARBS, intake.carbs)
            put(COL_INTAKE_FAT, intake.fat)
            put(COL_IS_DELETED, if (intake.isDeleted) 1 else 0)
            put(COL_CREATED_AT, intake.createdAt)
        }
        db.insertWithOnConflict(TABLE_INTAKE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Grocery
    fun getGroceryItems(ownerId: String): List<GroceryItemModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_GROCERY, null, "$COL_OWNER_ID = ? AND $COL_GROCERY_IS_BOUGHT = 0 AND $COL_IS_DELETED = 0", arrayOf(ownerId), null, null, null)
        val list = mutableListOf<GroceryItemModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(GroceryItemModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    ingredientId = it.getString(it.getColumnIndexOrThrow(COL_GROCERY_ING_ID)),
                    quantity = it.getDouble(it.getColumnIndexOrThrow(COL_GROCERY_QUANTITY)),
                    isBought = it.getInt(it.getColumnIndexOrThrow(COL_GROCERY_IS_BOUGHT)) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_DELETED)) == 1,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertGroceryItem(item: GroceryItemModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, item.id)
            put(COL_OWNER_ID, item.ownerId)
            put(COL_GROCERY_ING_ID, item.ingredientId)
            put(COL_GROCERY_QUANTITY, item.quantity)
            put(COL_GROCERY_IS_BOUGHT, if (item.isBought) 1 else 0)
            put(COL_IS_DELETED, if (item.isDeleted) 1 else 0)
            put(COL_CREATED_AT, item.createdAt)
        }
        db.insertWithOnConflict(TABLE_GROCERY, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun addOrUpdateGroceryItem(ownerId: String, ingredientId: String, quantity: Double) {
        val db = writableDatabase
        val cursor = db.query(TABLE_GROCERY, null, "$COL_OWNER_ID = ? AND $COL_GROCERY_ING_ID = ? AND $COL_GROCERY_IS_BOUGHT = 0 AND $COL_IS_DELETED = 0", arrayOf(ownerId, ingredientId), null, null, null)
        
        val existingItems = mutableListOf<GroceryItemModel>()
        cursor.use {
            while (it.moveToNext()) {
                existingItems.add(GroceryItemModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    ingredientId = it.getString(it.getColumnIndexOrThrow(COL_GROCERY_ING_ID)),
                    quantity = it.getDouble(it.getColumnIndexOrThrow(COL_GROCERY_QUANTITY)),
                    isBought = it.getInt(it.getColumnIndexOrThrow(COL_GROCERY_IS_BOUGHT)) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_IS_DELETED)) == 1,
                    isSynchronized = true,
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }

        if (existingItems.isNotEmpty()) {
            val target = existingItems.first()
            target.quantity = existingItems.sumOf { it.quantity } + quantity
            upsertGroceryItem(target)
            
            for (i in 1 until existingItems.size) {
                deleteGroceryItem(existingItems[i].id)
            }
        } else {
            val newItem = GroceryItemModel(
                id = UUID.randomUUID().toString(),
                ownerId = ownerId,
                ingredientId = ingredientId,
                quantity = quantity,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            upsertGroceryItem(newItem)
        }
    }

    fun deleteGroceryItem(id: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_DELETED, 1)
        }
        db.update(TABLE_GROCERY, values, "$COL_ID = ?", arrayOf(id))
    }

    fun deleteGroceryItemByIngredient(ownerId: String, ingredientId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_DELETED, 1)
        }
        db.update(TABLE_GROCERY, values, "$COL_OWNER_ID = ? AND $COL_GROCERY_ING_ID = ?", arrayOf(ownerId, ingredientId))
    }

    // Workouts
    fun getWorkoutsForDate(ownerId: String, date: String): List<WorkoutModel> {
        val db = readableDatabase
        val cursor = db.query(TABLE_WORKOUT, null, "$COL_OWNER_ID = ? AND $COL_WORKOUT_DATE = ? AND $COL_IS_DELETED = 0", arrayOf(ownerId, date), null, null, null)
        val list = mutableListOf<WorkoutModel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(WorkoutModel(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    ownerId = it.getString(it.getColumnIndexOrThrow(COL_OWNER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_WORKOUT_TITLE)),
                    type = it.getString(it.getColumnIndexOrThrow(COL_WORKOUT_TYPE)),
                    durationMinutes = it.getInt(it.getColumnIndexOrThrow(COL_WORKOUT_DURATION)),
                    caloriesBurned = it.getInt(it.getColumnIndexOrThrow(COL_WORKOUT_CALORIES)),
                    description = it.getString(it.getColumnIndexOrThrow(COL_WORKOUT_DESC)),
                    isCompleted = it.getInt(it.getColumnIndexOrThrow(COL_WORKOUT_COMPLETED)) == 1,
                    scheduledDate = it.getString(it.getColumnIndexOrThrow(COL_WORKOUT_DATE)),
                    createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                ))
            }
        }
        return list
    }

    fun upsertWorkout(workout: WorkoutModel) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, workout.id)
            put(COL_OWNER_ID, workout.ownerId)
            put(COL_WORKOUT_TITLE, workout.title)
            put(COL_WORKOUT_TYPE, workout.type)
            put(COL_WORKOUT_DURATION, workout.durationMinutes)
            put(COL_WORKOUT_CALORIES, workout.caloriesBurned)
            put(COL_WORKOUT_DESC, workout.description)
            put(COL_WORKOUT_COMPLETED, if (workout.isCompleted) 1 else 0)
            put(COL_WORKOUT_DATE, workout.scheduledDate)
            put(COL_CREATED_AT, workout.createdAt)
        }
        db.insertWithOnConflict(TABLE_WORKOUT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Water
    fun getWaterIntakeForDate(ownerId: String, date: String): Int {
        val db = readableDatabase
        val cursor = db.query(TABLE_WATER, arrayOf("SUM($COL_WATER_AMOUNT)"), "$COL_OWNER_ID = ? AND $COL_WATER_DATE = ?", arrayOf(ownerId, date), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun addWaterIntake(ownerId: String, date: String, amountMl: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, UUID.randomUUID().toString())
            put(COL_OWNER_ID, ownerId)
            put(COL_WATER_AMOUNT, amountMl)
            put(COL_WATER_DATE, date)
            put(COL_CREATED_AT, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        db.insert(TABLE_WATER, null, values)
    }
}
