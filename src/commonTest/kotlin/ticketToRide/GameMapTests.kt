package ticketToRide

import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testTicketPoints() {
        assertEquals(20, GameMap.longTickets.count())
        assertEquals(452, GameMap.shortTickets.count())
    }
}