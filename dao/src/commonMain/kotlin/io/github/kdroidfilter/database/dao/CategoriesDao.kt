package io.github.kdroidfilter.database.dao

import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.localization.LocalizedAppCategory
import io.github.kdroidfilter.database.store.App_categories
import io.github.kdroidfilter.database.store.Applications
import io.github.kdroidfilter.database.store.Developers
import io.github.kdroidfilter.storekit.gplay.core.model.GooglePlayApplicationInfo

/**
 * Data Access Object for Categories
 * Contains functions for database operations related to categories and retrieving applications by category
 */
object CategoriesDao {

    /**
     * Gets all categories from the database
     * @param database The database instance
     * @param deviceLanguage The device language for localized category names
     * @return A list of pairs containing the category enum and its localized name
     */
    fun getAllCategories(
        database: Database,
        deviceLanguage: String
    ): List<Pair<AppCategory, String>> {
        val categoriesQueries = database.app_categoriesQueries
        
        return categoriesQueries.getAllCategories().executeAsList().map { category ->
            val categoryEnum = AppCategory.valueOf(category.category_name)
            val localizedName = LocalizedAppCategory.getLocalizedName(categoryEnum, deviceLanguage)
            
            Pair(categoryEnum, localizedName)
        }
    }

    /**
     * Gets applications by category ID
     * @param database The database instance
     * @param categoryId The category ID
     * @param deviceLanguage The device language for localized category names
     * @param creator A function to create the return type from the application data
     * @return A list of applications in the specified category
     */
    fun <T> getApplicationsByCategoryId(
        database: Database,
        categoryId: Long,
        deviceLanguage: String,
        creator: (Long, String, GooglePlayApplicationInfo) -> T
    ): List<T> {
        val applicationsQueries = database.applicationsQueries
        val developersQueries = database.developersQueries
        val categoriesQueries = database.app_categoriesQueries
        
        return applicationsQueries.getApplicationsByCategory(categoryId).executeAsList().map { app ->
            val developer = developersQueries.getDeveloperById(app.developer_id).executeAsOne()
            val category = categoriesQueries.getCategoryById(app.app_category_id).executeAsOne()
            
            ApplicationsDao.createAppInfoWithExtras(app, developer, category, deviceLanguage, creator)
        }
    }

    /**
     * Gets applications by category name
     * @param database The database instance
     * @param categoryName The category name (must match an AppCategory enum value)
     * @param deviceLanguage The device language for localized category names
     * @param creator A function to create the return type from the application data
     * @return A list of applications in the specified category
     */
    fun <T> getApplicationsByCategoryName(
        database: Database,
        categoryName: String,
        deviceLanguage: String,
        creator: (Long, String, GooglePlayApplicationInfo) -> T
    ): List<T> {
        val applicationsQueries = database.applicationsQueries
        val developersQueries = database.developersQueries
        val categoriesQueries = database.app_categoriesQueries
        
        return applicationsQueries.getApplicationsByCategoryName(categoryName).executeAsList().map { app ->
            val developer = developersQueries.getDeveloperById(app.developer_id).executeAsOne()
            val category = categoriesQueries.getCategoryById(app.app_category_id).executeAsOne()
            
            ApplicationsDao.createAppInfoWithExtras(app, developer, category, deviceLanguage, creator)
        }
    }

    /**
     * Gets applications by category enum
     * @param database The database instance
     * @param category The AppCategory enum
     * @param deviceLanguage The device language for localized category names
     * @param creator A function to create the return type from the application data
     * @return A list of applications in the specified category
     */
    fun <T> getApplicationsByCategory(
        database: Database,
        category: AppCategory,
        deviceLanguage: String,
        creator: (Long, String, GooglePlayApplicationInfo) -> T
    ): List<T> {
        return getApplicationsByCategoryName(database, category.name, deviceLanguage, creator)
    }
}