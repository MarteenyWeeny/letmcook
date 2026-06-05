package com.letmcook.letmcook.services

import com.letmcook.letmcook.models.*
import com.letmcook.letmcook.utils.*
import org.json.JSONArray
import org.json.JSONObject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

const val TODO_MAPPING_REQUIRED = "TODO_MAPPING_REQUIRED"

enum class SyncState(val code: Int) {
    PENDING(0),
    SYNCED(1),
    CONFLICT(2)
}

data class ColumnDef(
    val name: String,
    val sqlType: String
)

data class ExternalSyncSpec(
    val recordRoots: List<List<String>> = emptyList(),
    val fieldAliases: Map<String, List<String>> = emptyMap(),
    val allowPartialRecords: Boolean = true,
    val overrideLocalChanges: Boolean = false
)

data class SyncIssue(
    val recordIndex: Int,
    val message: String
)

data class SyncResult(
    val tableName: String,
    val processedCount: Int,
    val insertedCount: Int,
    val updatedCount: Int,
    val softDeletedCount: Int,
    val conflictCount: Int,
    val skippedCount: Int,
    val issues: List<SyncIssue>
)

data class CrashRecoveryResult(
    val schemaEnsured: Boolean,
    val integrityCheckPassed: Boolean,
    val issues: List<String>
)

private data class RowState<T : Any>(
    val model: T,
    val syncState: SyncState,
    val deleted: Boolean
)

private data class ParsedRecord(
    val index: Int,
    val values: Map<String, Any?>
)

private abstract class ModelAdapter<T : Any>(
    val tableName: String,
    val columns: List<ColumnDef>,
    val requiredColumns: Set<String>
) {
    abstract fun toRow(model: T, syncState: SyncState): Map<String, Any?>
    abstract fun fromRow(row: Map<String, Any?>): T?
    abstract fun idOf(model: T): String
    abstract fun withSyncState(model: T, syncState: SyncState): T
    abstract fun withDeleted(model: T, deleted: Boolean): T

    val columnNames: List<String> = columns.map { it.name }

    fun rowFromExternal(source: Map<String, Any?>, spec: ExternalSyncSpec): Map<String, Any?>? {
        val issues = mutableListOf<String>()
        val row = mutableMapOf<String, Any?>()

        for (column in columns) {
            if (column.name == "is_synchronized" || column.name == "sync_state") {
                continue
            }

            val aliases = spec.fieldAliases[column.name].orEmpty()
            val extracted = extractMappedValue(source, aliases)

            if (extracted != null && extracted.toString().startsWith("java.lang.Object")) {
                if (column.name in requiredColumns) {
                    issues += "$TODO_MAPPING_REQUIRED:${tableName}.${column.name}"
                }
                continue
            }

            row[column.name] = extracted
        }

        if (issues.isNotEmpty()) {
            return null
        }

        if (!row.containsKey("id") || row["id"].toString().isBlank()) {
            return null
        }

        return row
    }

    fun mergeRows(
        existing: Map<String, Any?>?,
        external: Map<String, Any?>,
        syncState: SyncState
    ): Map<String, Any?>? {
        val merged = mutableMapOf<String, Any?>()

        for (column in columns) {
            if (column.name == "is_synchronized") {
                merged[column.name] = syncState == SyncState.SYNCED
                continue
            }

            if (column.name == "sync_state") {
                merged[column.name] = syncState.code
                continue
            }

            val externalHasValue = external.containsKey(column.name)
            val externalValue = external[column.name]
            val existingValue = existing?.get(column.name)

            merged[column.name] = when {
                externalHasValue -> externalValue
                existing != null -> existingValue
                else -> null
            }
        }

        for (requiredColumn in requiredColumns) {
            if (merged[requiredColumn] == null) {
                return null
            }
        }

        return merged
    }

    fun rowToModel(row: Map<String, Any?>): T? = fromRow(row)

    fun syncStateFromRow(row: Map<String, Any?>): SyncState {
        val explicit = row["sync_state"].asInt()
        if (explicit != null) {
            return when (explicit) {
                1 -> SyncState.SYNCED
                2 -> SyncState.CONFLICT
                else -> SyncState.PENDING
            }
        }

        val legacy = row["is_synchronized"].asBoolean()
        return if (legacy) SyncState.SYNCED else SyncState.PENDING
    }

    fun isDeletedFromRow(row: Map<String, Any?>): Boolean = row["is_deleted"].asBoolean()
}

private object UserAdapter : ModelAdapter<UserModel>(
    tableName = "User",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("email", "TEXT"),
        ColumnDef("display_name", "TEXT"),
        ColumnDef("full_name", "TEXT"),
        ColumnDef("role", "INTEGER"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "email", "created_at")
) {
    override fun toRow(model: UserModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "email" to model.email,
        "display_name" to model.displayName,
        "full_name" to model.fullName,
        "role" to model.role,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): UserModel? {
        val id = row["id"]?.toString() ?: return null
        val email = row["email"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return UserModel(
            id = id,
            email = email,
            displayName = row["display_name"]?.toString(),
            fullName = row["full_name"]?.toString(),
            role = row["role"].asInt(),
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: UserModel): String = model.id
    override fun withSyncState(model: UserModel, syncState: SyncState): UserModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: UserModel, deleted: Boolean): UserModel = model.copy(isDeleted = deleted)
}

private object RecipeAdapter : ModelAdapter<RecipeModel>(
    tableName = "Recipe",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("owner_id", "TEXT"),
        ColumnDef("title", "TEXT"),
        ColumnDef("instructions", "TEXT"),
        ColumnDef("total_calories", "INTEGER"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "owner_id", "title", "created_at")
) {
    override fun toRow(model: RecipeModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "owner_id" to model.ownerId,
        "title" to model.title,
        "instructions" to model.instructions,
        "total_calories" to model.totalCalories,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): RecipeModel? {
        val id = row["id"]?.toString() ?: return null
        val ownerId = row["owner_id"]?.toString() ?: return null
        val title = row["title"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return RecipeModel(
            id = id,
            ownerId = ownerId,
            title = title,
            instructions = row["instructions"]?.toString(),
            totalCalories = row["total_calories"].asInt(),
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: RecipeModel): String = model.id
    override fun withSyncState(model: RecipeModel, syncState: SyncState): RecipeModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: RecipeModel, deleted: Boolean): RecipeModel = model.copy(isDeleted = deleted)
}

private object IngredientAdapter : ModelAdapter<IngredientModel>(
    tableName = "Ingredient",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("owner_id", "TEXT"),
        ColumnDef("name", "TEXT"),
        ColumnDef("category", "TEXT"),
        ColumnDef("unit_of_measure", "TEXT"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "owner_id", "name", "created_at")
) {
    override fun toRow(model: IngredientModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "owner_id" to model.ownerId,
        "name" to model.name,
        "category" to model.category,
        "unit_of_measure" to model.unitOfMeasure,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): IngredientModel? {
        val id = row["id"]?.toString() ?: return null
        val ownerId = row["owner_id"]?.toString() ?: return null
        val name = row["name"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return IngredientModel(
            id = id,
            ownerId = ownerId,
            name = name,
            category = row["category"]?.toString(),
            unitOfMeasure = row["unit_of_measure"]?.toString(),
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: IngredientModel): String = model.id
    override fun withSyncState(model: IngredientModel, syncState: SyncState): IngredientModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: IngredientModel, deleted: Boolean): IngredientModel = model.copy(isDeleted = deleted)
}

private object MealPlanAdapter : ModelAdapter<MealPlanModel>(
    tableName = "MealPlan",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("owner_id", "TEXT"),
        ColumnDef("recipe_id", "TEXT"),
        ColumnDef("planned_date", "TEXT"),
        ColumnDef("meal_type", "TEXT"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "owner_id", "recipe_id", "planned_date", "created_at")
) {
    override fun toRow(model: MealPlanModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "owner_id" to model.ownerId,
        "recipe_id" to model.recipeId,
        "planned_date" to model.plannedDate,
        "meal_type" to model.mealType,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): MealPlanModel? {
        val id = row["id"]?.toString() ?: return null
        val ownerId = row["owner_id"]?.toString() ?: return null
        val recipeId = row["recipe_id"]?.toString() ?: return null
        val plannedDate = row["planned_date"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return MealPlanModel(
            id = id,
            ownerId = ownerId,
            recipeId = recipeId,
            plannedDate = plannedDate,
            mealType = row["meal_type"]?.toString(),
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: MealPlanModel): String = model.id
    override fun withSyncState(model: MealPlanModel, syncState: SyncState): MealPlanModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: MealPlanModel, deleted: Boolean): MealPlanModel = model.copy(isDeleted = deleted)
}

private object NutritionGoalAdapter : ModelAdapter<NutritionGoalModel>(
    tableName = "NutritionGoal",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("owner_id", "TEXT"),
        ColumnDef("daily_calorie_target", "INTEGER"),
        ColumnDef("protein_target_grams", "INTEGER"),
        ColumnDef("carb_target_grams", "INTEGER"),
        ColumnDef("fat_target_grams", "INTEGER"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "owner_id", "created_at")
) {
    override fun toRow(model: NutritionGoalModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "owner_id" to model.ownerId,
        "daily_calorie_target" to model.dailyCalorieTarget,
        "protein_target_grams" to model.proteinTargetGrams,
        "carb_target_grams" to model.carbTargetGrams,
        "fat_target_grams" to model.fatTargetGrams,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): NutritionGoalModel? {
        val id = row["id"]?.toString() ?: return null
        val ownerId = row["owner_id"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return NutritionGoalModel(
            id = id,
            ownerId = ownerId,
            dailyCalorieTarget = row["daily_calorie_target"].asInt(),
            proteinTargetGrams = row["protein_target_grams"].asInt(),
            carbTargetGrams = row["carb_target_grams"].asInt(),
            fatTargetGrams = row["fat_target_grams"].asInt(),
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: NutritionGoalModel): String = model.id
    override fun withSyncState(model: NutritionGoalModel, syncState: SyncState): NutritionGoalModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: NutritionGoalModel, deleted: Boolean): NutritionGoalModel = model.copy(isDeleted = deleted)
}

private object PantryItemAdapter : ModelAdapter<PantryItemModel>(
    tableName = "PantryItem",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("owner_id", "TEXT"),
        ColumnDef("ingredient_id", "TEXT"),
        ColumnDef("current_quantity", "REAL"),
        ColumnDef("expiration_date", "TEXT"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "owner_id", "ingredient_id", "created_at")
) {
    override fun toRow(model: PantryItemModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "owner_id" to model.ownerId,
        "ingredient_id" to model.ingredientId,
        "current_quantity" to model.currentQuantity,
        "expiration_date" to model.expirationDate,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): PantryItemModel? {
        val id = row["id"]?.toString() ?: return null
        val ownerId = row["owner_id"]?.toString() ?: return null
        val ingredientId = row["ingredient_id"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return PantryItemModel(
            id = id,
            ownerId = ownerId,
            ingredientId = ingredientId,
            currentQuantity = row["current_quantity"].asDouble() ?: 0.0,
            expirationDate = row["expiration_date"]?.toString(),
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: PantryItemModel): String = model.id
    override fun withSyncState(model: PantryItemModel, syncState: SyncState): PantryItemModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: PantryItemModel, deleted: Boolean): PantryItemModel = model.copy(isDeleted = deleted)
}

private object RecipeIngredientAdapter : ModelAdapter<RecipeIngredientModel>(
    tableName = "RecipeIngredient",
    columns = listOf(
        ColumnDef("id", "TEXT"),
        ColumnDef("owner_id", "TEXT"),
        ColumnDef("recipe_id", "TEXT"),
        ColumnDef("ingredient_id", "TEXT"),
        ColumnDef("required_quantity", "REAL"),
        ColumnDef("is_deleted", "INTEGER"),
        ColumnDef("is_synchronized", "INTEGER"),
        ColumnDef("sync_state", "INTEGER"),
        ColumnDef("created_at", "TEXT")
    ),
    requiredColumns = setOf("id", "owner_id", "recipe_id", "ingredient_id", "created_at")
) {
    override fun toRow(model: RecipeIngredientModel, syncState: SyncState): Map<String, Any?> = mapOf(
        "id" to model.id,
        "owner_id" to model.ownerId,
        "recipe_id" to model.recipeId,
        "ingredient_id" to model.ingredientId,
        "required_quantity" to model.requiredQuantity,
        "is_deleted" to model.isDeleted,
        "is_synchronized" to (syncState == SyncState.SYNCED),
        "sync_state" to syncState.code,
        "created_at" to model.createdAt
    )

    override fun fromRow(row: Map<String, Any?>): RecipeIngredientModel? {
        val id = row["id"]?.toString() ?: return null
        val ownerId = row["owner_id"]?.toString() ?: return null
        val recipeId = row["recipe_id"]?.toString() ?: return null
        val ingredientId = row["ingredient_id"]?.toString() ?: return null
        val createdAt = row["created_at"]?.toString() ?: return null
        return RecipeIngredientModel(
            id = id,
            ownerId = ownerId,
            recipeId = recipeId,
            ingredientId = ingredientId,
            requiredQuantity = row["required_quantity"].asDouble() ?: 0.0,
            isDeleted = row["is_deleted"].asBoolean(),
            isSynchronized = row["sync_state"].asSyncState(row["is_synchronized"]) == SyncState.SYNCED,
            createdAt = createdAt
        )
    }

    override fun idOf(model: RecipeIngredientModel): String = model.id
    override fun withSyncState(model: RecipeIngredientModel, syncState: SyncState): RecipeIngredientModel = model.copy(isSynchronized = syncState == SyncState.SYNCED)
    override fun withDeleted(model: RecipeIngredientModel, deleted: Boolean): RecipeIngredientModel = model.copy(isDeleted = deleted)
}

class DatabaseService(
    private val databaseUrl: String = "jdbc:sqlite:calmmove.db"
) {
    private val adapters: List<ModelAdapter<*>> = listOf(
        UserAdapter,
        RecipeAdapter,
        IngredientAdapter,
        MealPlanAdapter,
        NutritionGoalAdapter,
        PantryItemAdapter,
        RecipeIngredientAdapter
    )

    init {
        initializeSchema()
    }

    fun recoverDatabase(): CrashRecoveryResult {
        val issues = mutableListOf<String>()
        var integrityCheckPassed = false

        withConnection { connection ->
            ensureAllSchema(connection)
            try {
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA integrity_check").use { resultSet ->
                        while (resultSet.next()) {
                            val value = resultSet.getString(1)
                            if (value.equals("ok", ignoreCase = true)) {
                                integrityCheckPassed = true
                            } else {
                                issues += value
                            }
                        }
                    }
                }
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA wal_checkpoint(PASSIVE)")
                }
            } catch (error: SQLException) {
                issues += error.message ?: error.javaClass.simpleName
            }
        }

        return CrashRecoveryResult(
            schemaEnsured = true,
            integrityCheckPassed = integrityCheckPassed,
            issues = issues
        )
    }

    fun upsertUser(model: UserModel): UserModel = upsert(UserAdapter, model)
    fun upsertRecipe(model: RecipeModel): RecipeModel = upsert(RecipeAdapter, model)
    fun upsertIngredient(model: IngredientModel): IngredientModel = upsert(IngredientAdapter, model)
    fun upsertMealPlan(model: MealPlanModel): MealPlanModel = upsert(MealPlanAdapter, model)
    fun upsertNutritionGoal(model: NutritionGoalModel): NutritionGoalModel = upsert(NutritionGoalAdapter, model)
    fun upsertPantryItem(model: PantryItemModel): PantryItemModel = upsert(PantryItemAdapter, model)
    fun upsertRecipeIngredient(model: RecipeIngredientModel): RecipeIngredientModel = upsert(RecipeIngredientAdapter, model)

    fun softDeleteUser(id: String): Boolean = softDeleteById(UserAdapter, id)
    fun softDeleteRecipe(id: String): Boolean = softDeleteById(RecipeAdapter, id)
    fun softDeleteIngredient(id: String): Boolean = softDeleteById(IngredientAdapter, id)
    fun softDeleteMealPlan(id: String): Boolean = softDeleteById(MealPlanAdapter, id)
    fun softDeleteNutritionGoal(id: String): Boolean = softDeleteById(NutritionGoalAdapter, id)
    fun softDeletePantryItem(id: String): Boolean = softDeleteById(PantryItemAdapter, id)
    fun softDeleteRecipeIngredient(id: String): Boolean = softDeleteById(RecipeIngredientAdapter, id)

    fun getUserById(id: String, includeDeleted: Boolean = false): UserModel? = getById(UserAdapter, id, includeDeleted)
    fun getRecipeById(id: String, includeDeleted: Boolean = false): RecipeModel? = getById(RecipeAdapter, id, includeDeleted)
    fun getIngredientById(id: String, includeDeleted: Boolean = false): IngredientModel? = getById(IngredientAdapter, id, includeDeleted)
    fun getMealPlanById(id: String, includeDeleted: Boolean = false): MealPlanModel? = getById(MealPlanAdapter, id, includeDeleted)
    fun getNutritionGoalById(id: String, includeDeleted: Boolean = false): NutritionGoalModel? = getById(NutritionGoalAdapter, id, includeDeleted)
    fun getPantryItemById(id: String, includeDeleted: Boolean = false): PantryItemModel? = getById(PantryItemAdapter, id, includeDeleted)
    fun getRecipeIngredientById(id: String, includeDeleted: Boolean = false): RecipeIngredientModel? = getById(RecipeIngredientAdapter, id, includeDeleted)

    fun getUsers(includeDeleted: Boolean = false): List<UserModel> = listAll(UserAdapter, includeDeleted)
    fun getRecipes(includeDeleted: Boolean = false): List<RecipeModel> = listAll(RecipeAdapter, includeDeleted)
    fun getIngredients(includeDeleted: Boolean = false): List<IngredientModel> = listAll(IngredientAdapter, includeDeleted)
    fun getMealPlans(includeDeleted: Boolean = false): List<MealPlanModel> = listAll(MealPlanAdapter, includeDeleted)
    fun getNutritionGoals(includeDeleted: Boolean = false): List<NutritionGoalModel> = listAll(NutritionGoalAdapter, includeDeleted)
    fun getPantryItems(includeDeleted: Boolean = false): List<PantryItemModel> = listAll(PantryItemAdapter, includeDeleted)
    fun getRecipeIngredients(includeDeleted: Boolean = false): List<RecipeIngredientModel> = listAll(RecipeIngredientAdapter, includeDeleted)

    fun syncUsers(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(UserAdapter, rawData, spec)
    fun syncRecipes(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(RecipeAdapter, rawData, spec)
    fun syncIngredients(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(IngredientAdapter, rawData, spec)
    fun syncMealPlans(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(MealPlanAdapter, rawData, spec)
    fun syncNutritionGoals(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(NutritionGoalAdapter, rawData, spec)
    fun syncPantryItems(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(PantryItemAdapter, rawData, spec)
    fun syncRecipeIngredients(rawData: Any?, spec: ExternalSyncSpec = ExternalSyncSpec()): SyncResult = syncTable(RecipeIngredientAdapter, rawData, spec)

    fun syncAll(rawDataByTable: Map<String, Any?>, specByTable: Map<String, ExternalSyncSpec> = emptyMap()): List<SyncResult> {
        return transaction { connection ->
            listOf(
                syncTable(connection, UserAdapter, rawDataByTable[UserAdapter.tableName], specByTable[UserAdapter.tableName] ?: ExternalSyncSpec()),
                syncTable(connection, RecipeAdapter, rawDataByTable[RecipeAdapter.tableName], specByTable[RecipeAdapter.tableName] ?: ExternalSyncSpec()),
                syncTable(connection, IngredientAdapter, rawDataByTable[IngredientAdapter.tableName], specByTable[IngredientAdapter.tableName] ?: ExternalSyncSpec()),
                syncTable(connection, MealPlanAdapter, rawDataByTable[MealPlanAdapter.tableName], specByTable[MealPlanAdapter.tableName] ?: ExternalSyncSpec()),
                syncTable(connection, NutritionGoalAdapter, rawDataByTable[NutritionGoalAdapter.tableName], specByTable[NutritionGoalAdapter.tableName] ?: ExternalSyncSpec()),
                syncTable(connection, PantryItemAdapter, rawDataByTable[PantryItemAdapter.tableName], specByTable[PantryItemAdapter.tableName] ?: ExternalSyncSpec()),
                syncTable(connection, RecipeIngredientAdapter, rawDataByTable[RecipeIngredientAdapter.tableName], specByTable[RecipeIngredientAdapter.tableName] ?: ExternalSyncSpec())
            )
        }
    }

    private fun initializeSchema() {
        withConnection { connection ->
            ensureAllSchema(connection)
        }
    }

    private fun ensureAllSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA journal_mode = WAL")
            statement.execute("PRAGMA synchronous = FULL")
            statement.execute("PRAGMA busy_timeout = 5000")
        }

        for (adapter in adapters) {
            ensureTable(connection, adapter)
        }
    }

    private fun ensureTable(connection: Connection, adapter: ModelAdapter<*>) {
        val createSql = buildCreateTableSql(adapter)
        connection.createStatement().use { statement ->
            statement.execute(createSql)
        }

        val existingColumns = existingColumns(connection, adapter.tableName)
        for (column in adapter.columns) {
            if (!existingColumns.contains(column.name.lowercase())) {
                connection.createStatement().use { statement ->
                    statement.execute("ALTER TABLE ${quoted(adapter.tableName)} ADD COLUMN ${quoted(column.name)} ${column.sqlType}")
                }
            }
        }
    }

    private fun buildCreateTableSql(adapter: ModelAdapter<*>): String {
        val definitions = adapter.columns.joinToString(", ") { column -> "${quoted(column.name)} ${column.sqlType}" }
        return "CREATE TABLE IF NOT EXISTS ${quoted(adapter.tableName)} ($definitions)"
    }

    private fun existingColumns(connection: Connection, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(${quoted(tableName)})").use { resultSet ->
                while (resultSet.next()) {
                    columns += resultSet.getString("name")?.lowercase().orEmpty()
                }
            }
        }
        return columns
    }

    private fun <T : Any> getById(adapter: ModelAdapter<T>, id: String, includeDeleted: Boolean): T? {
        return withConnection { connection ->
            ensureTable(connection, adapter)
            querySingle(connection, adapter, id, includeDeleted)?.model
        }
    }

    private fun <T : Any> listAll(adapter: ModelAdapter<T>, includeDeleted: Boolean): List<T> {
        return withConnection { connection ->
            ensureTable(connection, adapter)
            queryAll(connection, adapter, includeDeleted).mapNotNull { it.model }
        }
    }

    private fun <T : Any> upsert(adapter: ModelAdapter<T>, model: T): T {
        return transaction { connection ->
            ensureTable(connection, adapter)
            val row = adapter.toRow(model, SyncState.PENDING)
            upsertRow(connection, adapter, row)
            adapter.withSyncState(model, SyncState.PENDING)
        }
    }

    private fun <T : Any> softDeleteById(adapter: ModelAdapter<T>, id: String): Boolean {
        return transaction { connection ->
            ensureTable(connection, adapter)
            val existing = querySingle(connection, adapter, id, includeDeleted = true)
            if (existing == null) {
                return@transaction false
            }
            val deletedModel = adapter.withDeleted(existing.model, true)
            val row = adapter.toRow(deletedModel, SyncState.PENDING)
            upsertRow(connection, adapter, row)
            true
        }
    }

    private fun <T : Any> querySingle(
        connection: Connection,
        adapter: ModelAdapter<T>,
        id: String,
        includeDeleted: Boolean
    ): RowState<T>? {
        val sql = buildSelectSql(adapter, includeDeleted, includeIdFilter = true)
        connection.prepareStatement(sql).use { statement ->
            statement.bind(1, id)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    return null
                }
                return readRow(resultSet, adapter)
            }
        }
    }

    private fun <T : Any> queryAll(
        connection: Connection,
        adapter: ModelAdapter<T>,
        includeDeleted: Boolean
    ): List<RowState<T>> {
        val sql = buildSelectSql(adapter, includeDeleted, includeIdFilter = false)
        val rows = mutableListOf<RowState<T>>()
        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    readRow(resultSet, adapter)?.let(rows::add)
                }
            }
        }
        return rows
    }

    private fun <T : Any> readRow(resultSet: ResultSet, adapter: ModelAdapter<T>): RowState<T>? {
        val row = mutableMapOf<String, Any?>()
        for (column in adapter.columns) {
            row[column.name] = try {
                resultSet.getObject(column.name)
            } catch (_: SQLException) {
                null
            }
        }

        val model = adapter.rowToModel(row) ?: return null
        return RowState(
            model = model,
            syncState = adapter.syncStateFromRow(row),
            deleted = adapter.isDeletedFromRow(row)
        )
    }

    private fun buildSelectSql(
        adapter: ModelAdapter<*>,
        includeDeleted: Boolean,
        includeIdFilter: Boolean
    ): String {
        val selectedColumns = adapter.columns.joinToString(", ") { quoted(it.name) }
        val whereParts = mutableListOf<String>()
        if (!includeDeleted) {
            whereParts += "COALESCE(${quoted("is_deleted")}, 0) = 0"
        }
        if (includeIdFilter) {
            whereParts += "${quoted("id")} = ?"
        }
        val whereClause = if (whereParts.isEmpty()) "" else " WHERE ${whereParts.joinToString(" AND ")}" 
        return "SELECT $selectedColumns FROM ${quoted(adapter.tableName)}$whereClause"
    }

    private fun <T : Any> upsertRow(connection: Connection, adapter: ModelAdapter<T>, row: Map<String, Any?>) {
        val columns = adapter.columns.map { it.name }
        val placeholders = columns.joinToString(", ") { "?" }
        val conflictUpdates = columns
            .filterNot { it == "id" }
            .joinToString(", ") { column -> "${quoted(column)} = excluded.${quoted(column)}" }

        val sql = buildString {
            append("INSERT INTO ")
            append(quoted(adapter.tableName))
            append(" (")
            append(columns.joinToString(", ") { quoted(it) })
            append(") VALUES (")
            append(placeholders)
            append(") ON CONFLICT(")
            append(quoted("id"))
            append(") DO UPDATE SET ")
            append(conflictUpdates)
        }

        connection.prepareStatement(sql).use { statement ->
            columns.forEachIndexed { index, column ->
                statement.bind(index + 1, row[column])
            }
            statement.executeUpdate()
        }
    }

    private fun <T : Any> syncTable(adapter: ModelAdapter<T>, rawData: Any?, spec: ExternalSyncSpec): SyncResult {
        return transaction { connection ->
            syncTable(connection, adapter, rawData, spec)
        }
    }

    private fun <T : Any> syncTable(
        connection: Connection,
        adapter: ModelAdapter<T>,
        rawData: Any?,
        spec: ExternalSyncSpec
    ): SyncResult {
        ensureTable(connection, adapter)

        val issues = mutableListOf<SyncIssue>()
        val records = collectRecords(rawData, spec.recordRoots)
        if (records.isEmpty()) {
            if (spec.fieldAliases.isEmpty()) {
                issues += SyncIssue(0, TODO_MAPPING_REQUIRED)
            }
            return SyncResult(
                tableName = adapter.tableName,
                processedCount = 0,
                insertedCount = 0,
                updatedCount = 0,
                softDeletedCount = 0,
                conflictCount = 0,
                skippedCount = if (spec.fieldAliases.isEmpty()) 1 else 0,
                issues = issues
            )
        }

        var processed = 0
        var inserted = 0
        var updated = 0
        var softDeleted = 0
        var conflicts = 0
        var skipped = 0

        for (record in records) {
            processed += 1

            val externalRow = adapter.rowFromExternal(record.values, spec)
            if (externalRow == null) {
                skipped += 1
                issues += SyncIssue(record.index, TODO_MAPPING_REQUIRED)
                continue
            }

            val id = externalRow["id"]?.toString()
            if (id.isNullOrBlank()) {
                skipped += 1
                issues += SyncIssue(record.index, "Missing id")
                continue
            }

            val existing = querySingle(connection, adapter, id, includeDeleted = true)
            val localRow = existing?.let { snapshotToRow(adapter, it) }

            val remoteDeleted = externalRow["is_deleted"].asBoolean()
            val merged = adapter.mergeRows(localRow, externalRow, SyncState.SYNCED)
            if (merged == null) {
                skipped += 1
                issues += SyncIssue(record.index, "Required fields missing after merge")
                continue
            }

            when {
                existing == null -> {
                    upsertRow(connection, adapter, merged)
                    inserted += 1
                    if (remoteDeleted) {
                        softDeleted += 1
                    }
                }

                existing.syncState == SyncState.PENDING && !spec.overrideLocalChanges && !rowsEqualIgnoringMeta(localRow, merged) -> {
                    val conflictRow = adapter.mergeRows(localRow, emptyMap(), SyncState.CONFLICT) ?: localRow!!
                    upsertRow(connection, adapter, conflictRow)
                    conflicts += 1
                }

                else -> {
                    upsertRow(connection, adapter, merged)
                    updated += 1
                    if (remoteDeleted) {
                        softDeleted += 1
                    }
                }
            }
        }

        return SyncResult(
            tableName = adapter.tableName,
            processedCount = processed,
            insertedCount = inserted,
            updatedCount = updated,
            softDeletedCount = softDeleted,
            conflictCount = conflicts,
            skippedCount = skipped,
            issues = issues
        )
    }

    private fun <T : Any> snapshotToRow(adapter: ModelAdapter<T>, state: RowState<T>): Map<String, Any?> {
        val modelRow = adapter.toRow(state.model, state.syncState)
        return modelRow.toMutableMap().apply {
            put("is_deleted", state.deleted)
            put("is_synchronized", state.syncState == SyncState.SYNCED)
            put("sync_state", state.syncState.code)
        }
    }

    private fun rowsEqualIgnoringMeta(left: Map<String, Any?>?, right: Map<String, Any?>): Boolean {
        if (left == null) {
            return false
        }

        val metaColumns = setOf("is_deleted", "is_synchronized", "sync_state")
        val leftComparable = left.filterKeys { it !in metaColumns }
        val rightComparable = right.filterKeys { it !in metaColumns }
        return leftComparable == rightComparable
    }

    private fun collectRecords(rawData: Any?, roots: List<List<String>>): List<ParsedRecord> {
        val records = mutableListOf<ParsedRecord>()

        fun addCandidate(candidate: Any?) {
            when (candidate) {
                null -> Unit
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    records += ParsedRecord(records.size, candidate as Map<String, Any?>)
                }
                is JSONObject -> records += ParsedRecord(records.size, candidate.toMap())
                is String -> parseLooseJson(candidate)?.let { addCandidate(it) }
                is Collection<*> -> candidate.forEach { addCandidate(it) }
                is JSONArray -> candidate.toList().forEach { addCandidate(it) }
                else -> Unit
            }
        }

        addCandidate(rawData)
        for (root in roots) {
            resolvePath(rawData, root)?.let { addCandidate(it) }
        }

        return records.distinctBy { it.index to it.values["id"].toString() }
    }

    private fun resolvePath(source: Any?, path: List<String>): Any? {
        var current: Any? = source
        for (segment in path) {
            current = when (current) {
                is String -> parseLooseJson(current)
                is JSONObject -> current.toMap()
                is JSONArray -> current.toList()
                else -> current
            }

            current = when (current) {
                is Map<*, *> -> current[segment]
                is List<*> -> segment.toIntOrNull()?.let { index -> current.getOrNull(index) }
                else -> null
            }

            if (current == null) {
                return null
            }
        }
        return current
    }

    private fun parseLooseJson(source: String): Any? {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        return try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toMap()
                trimmed.startsWith("[") -> JSONArray(trimmed).toList()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun quoted(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""

    private fun <T> withConnection(block: (Connection) -> T): T {
        DriverManager.getConnection(databaseUrl).use { connection ->
            configureConnection(connection)
            return block(connection)
        }
    }

    private fun <T> transaction(block: (Connection) -> T): T {
        return withConnection { connection ->
            val previousAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (error: Throwable) {
                try {
                    connection.rollback()
                } catch (_: Throwable) {
                    Unit
                }
                throw error
            } finally {
                connection.autoCommit = previousAutoCommit
            }
        }
    }

    private fun configureConnection(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA journal_mode = WAL")
            statement.execute("PRAGMA synchronous = FULL")
            statement.execute("PRAGMA busy_timeout = 5000")
        }
    }

    private fun PreparedStatement.bind(parameterIndex: Int, value: Any?) {
        when (value) {
            null -> setObject(parameterIndex, null)
            is Boolean -> setInt(parameterIndex, if (value) 1 else 0)
            is Int -> setInt(parameterIndex, value)
            is Long -> setLong(parameterIndex, value)
            is Double -> setDouble(parameterIndex, value)
            is Float -> setFloat(parameterIndex, value)
            is Number -> setObject(parameterIndex, value)
            else -> setObject(parameterIndex, value)
        }
    }
}
