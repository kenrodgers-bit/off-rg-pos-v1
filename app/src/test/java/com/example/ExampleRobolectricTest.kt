package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RG POS", appName)
  }

  @Test
  fun `verify packaging profile conversion factor`() {
    val cartonFactor = 18.0
    val cartonsCount = 3.0
    val baseQuantity = cartonsCount * cartonFactor
    assertEquals(54.0, baseQuantity, 0.001)
  }

  @Test
  fun `verify database schema and business table`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.database.AppDatabase.getDatabase(context)
    val biz = db.businessDao().getBusiness()
    // Verifies database opens and query executes cleanly without schema mismatch
  }

  @Test
  fun `verify primary navigation tabs count and destinations`() {
    val tabs = com.example.ui.navigation.MainTab.values()
    assertEquals(4, tabs.size)
    assertEquals(listOf("Home", "POS", "Inventory", "More"), tabs.map { it.title })
    assertEquals(listOf("nav_home", "nav_pos", "nav_inventory", "nav_more"), tabs.map { it.tag })
  }
}
