import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.tags.Tags;

public class enrichment_OUTBOUND_GENIUM_TCR_SetTags {

    private static final String SENDERSUBID_GENIUM_TCR = System.getProperty("O_DMA_INET_GENIUM_TCR.session.tag50");
  

    public void doEnrichment(ULMessage ulm) {
        ulm.add("SENDERSUBID", SENDERSUBID_GENIUM_TCR);

        correctNoPartyIds(ulm);
    }

    private void correctNoPartyIds(ULMessage ulm) {
        int iterator = 0;
        int nbNoPartyIds = Integer.parseInt(ulm.getString(Tags.NOPARTYIDS));

        while (iterator<nbNoPartyIds){
            ULMessage currentRptGroup = ULMessage.valueOf(ulm.getString(Tags.NOPARTYIDS, iterator));
            iterator++;
        }
    }
}