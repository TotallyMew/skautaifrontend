package lt.skautai.android.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavVisibilityTest {
    @Test
    fun `members tab is hidden without members view`() {
        assertFalse(
            shouldShowBottomNavItem(
                BottomNavItem.Members,
                setOf("items.view")
            )
        )
    }

    @Test
    fun `members tab is visible with members view`() {
        assertTrue(
            shouldShowBottomNavItem(
                BottomNavItem.Members,
                setOf("members.view:OWN_UNIT")
            )
        )
    }
}
