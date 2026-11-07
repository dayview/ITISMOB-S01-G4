package com.purrfolio.mco1.care;
import org.junit.Test;
import static org.junit.Assert.*;
public class CareTimeTest {
    @Test public void timeIsAppliedToConfirmedDate(){long at=CareTime.parse("2026-11-07","7:30 PM");
        assertEquals("2026-11-07",CareTime.date(at));assertEquals("7:30 PM",CareTime.time(at));}
    @Test public void supportsMilitaryTime(){assertEquals(CareTime.parse("2026-11-07","7:30 PM"),CareTime.parse("2026-11-07","19:30"));}
    @Test(expected=IllegalArgumentException.class) public void rejectsAmbiguousHour(){CareTime.parse("2026-11-07","7:30");}
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidDate(){CareTime.parse("2026-02-30","7:30 AM");}
    @Test(expected=IllegalArgumentException.class) public void rejectsTrailingGarbage(){CareTime.parse("2026-11-07","7:30 PM xyz");}
}
