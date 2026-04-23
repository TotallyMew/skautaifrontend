package lt.skautai.android.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavVisibilityTest {
    @Test
    fun `units tab is hidden with only members view`() {
        assertFalse(
            shouldShowBottomNavItem(
                BottomNavItem.Units,
                setOf("members.view")
            )
        )
    }

    @Test
    fun `units tab is visible with unit permissions`() {
        assertTrue(
            shouldShowBottomNavItem(
                BottomNavItem.Units,
                setOf("unit.members.manage:OWN_UNIT")
            )
        )
    }
}
