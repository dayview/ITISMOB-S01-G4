package com.purrfolio.mco1;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
public class CareCaptureTest {
    @Test public void extractsNaturalVoiceDetails(){CareCapture.Draft d=CareCapture.parse("Fed Miso one cup at 7:30 PM",Arrays.asList("Miso","Bailey"));
        assertEquals("Miso",d.getPet());assertEquals("Feeding",d.getActivity());assertEquals("one cup",d.getAmount());assertEquals("7:30 PM",d.getTime());}
    @Test public void missingDetailsStayMissing(){CareCapture.Draft d=CareCapture.parse("fed",Arrays.asList("Miso"));
        assertNull(d.getPet());assertEquals("Feeding",d.getActivity());assertNull(d.getAmount());assertNull(d.getTime());}
    @Test public void ambiguousPetMustBeConfirmed(){assertNull(CareCapture.parse("fed Miso and Bailey",Arrays.asList("Miso","Bailey")).getPet());}
    @Test public void namesUseWordBoundaries(){assertNull(CareCapture.parse("Fed Hannah one cup",Arrays.asList("Ann")).getPet());}
    @Test public void supportsFractionAndMilitaryTime(){CareCapture.Draft d=CareCapture.parse("gave Miso 1/2 tablet at 19:30 yesterday",Arrays.asList("Miso"));
        assertEquals("1/2 tablet",d.getAmount());assertEquals("19:30",d.getTime());assertEquals(-1,d.getDayOffset());}
    @Test public void retainsTranscriptForConfirmation(){assertEquals("Miso drank water",CareCapture.parse(" Miso drank water ",Arrays.asList("Miso")).getTranscript());}
}
