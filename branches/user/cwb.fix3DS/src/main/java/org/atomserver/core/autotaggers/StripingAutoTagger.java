package org.atomserver.core.autotaggers;

import org.atomserver.core.EntryMetaData;
import org.atomserver.core.EntryCategory;
import org.atomserver.utils.ShardedPathGenerator;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * StripingAutoTagger - auto tagger to be used for grouping entries into "stripes".
 *
 * This auto tagger can be used to self-organize entries into a number of "stripes" -- arbitrary
 * groups of data that are useful for doing distributed processing of feeds - this tagger will
 * apply a "stripe" category to each entry as it is written, and then feed processors can pull
 * categorized feeds by those stripes to partition work amongst several processors.
 */
public class StripingAutoTagger extends BaseAutoTagger {

    private String stripeScheme = "urn:stripe";
    private String label = null;
    private int numStripes = 8;
    private int radix = 10;

    public void setStripeScheme(String stripeScheme) {
        this.stripeScheme = stripeScheme;
    }

    public void setNumStripes(int numStripes) {
        this.numStripes = numStripes;
    }

    public void setRadix(int radix) {
        this.radix = radix;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    // TODO : ??????
    //public String getStripeBasis(EntryMetaData entry, String content) {
    public String getStripeBasis(EntryMetaData entry) {
        return entry.getEntryId();
    }

    private static final List<EntryCategory> EMPTY_CAT_LIST = new ArrayList<EntryCategory>();

    public boolean tag(EntryMetaData entry, Document doc) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            log.debug("StripingAutoTagger.tag");

            // compute what the stripe term SHOULD be
            String stripeTerm = ShardedPathGenerator.computeShard(getStripeBasis(entry), numStripes, radix);

            // select the current set of categories, and check anything in the stripe scheme to see
            // whether it matches the correct term.  if there is already a category with the proper
            // scheme AND term, set a flag so we don't insert it again.  if there are any with the
            // right scheme but a DIFFERENT term, add them to a list which we will delete in a batch
            // after.

            // TODO: TESTING >>>>>>>>>>>>>>>>>>>>
            //List<EntryCategory> list = getCategoriesHandler().selectEntryCategories(entry);
            List<EntryCategory> list = entry.getCategories();
            // TODO : ?????????
            if ( (list.size() == 1)
                 && ((list.get(0).getScheme() == null) && (list.get(0).getTerm() == null)) ) {
                list = EMPTY_CAT_LIST;
            }
            if (log.isDebugEnabled()) {
                for (EntryCategory entryCategory : list) {
                    log.trace("TAG-INITIAL:" + entryCategory);
                }
            }

            boolean alreadyExists = false;
            List<EntryCategory> toDelete = new ArrayList<EntryCategory>();
            for (EntryCategory entryCategory : list) {
                log.debug("entryCategory= " + entryCategory);
                if (entryCategory.getScheme().equals(stripeScheme)) {
                    if (entryCategory.getTerm().equals(stripeTerm)) {
                        alreadyExists = true;
                    } else {
                        toDelete.add(entryCategory);
                    }
                }
            }

            // if there were any to delete - delete them now.
            if (!toDelete.isEmpty()) {
                getCategoriesHandler().deleteEntryCategoryBatch(toDelete);
            }
            
            // if we didn't already have the proper category, set it now.
            if (!alreadyExists) {
                EntryCategory category = new EntryCategory();
                category.setEntryStoreId(entry.getEntryStoreId());
                category.setScheme(stripeScheme);
                category.setTerm(stripeTerm);
                category.setLabel(label);
                getCategoriesHandler().insertEntryCategoryBatch(Arrays.asList(category));
            }
            return (!alreadyExists || !toDelete.isEmpty());
        } finally {
            stopWatch.stop("AutoTagger.stripe", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
        }
    }
}
