/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;


/**
 * Read a simple phoneme string and generate default acoustic parameters.
 *
 * @author Marc Schr&ouml;der
 */

public class TargetFeatureLister extends InternalModule
{
    protected TargetFeatureComputer featureComputer;

    public TargetFeatureLister()
    {
        super("TargetFeatureLister",
                MaryDataType.get("FREETTS_ACOUSTPARAMS"), 
                MaryDataType.get("TARGETFEATURES"));
    }

    public void startup() throws Exception
    {
        super.startup();
        String managerClass = MaryProperties.needProperty("targetfeaturelister.featuremanager");
        FeatureProcessorManager manager = (FeatureProcessorManager) Class.forName(managerClass).newInstance();
        String features = MaryProperties.needProperty("targetfeaturelister.features");
        this.featureComputer = new TargetFeatureComputer(manager, features);
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        List uttList = d.getUtterances();
        String header = featureComputer.getAllFeatureProcessorNamesAndValues();
        StringBuffer text = new StringBuffer();
        StringBuffer bin = new StringBuffer();
        for (int i=0, len=uttList.size(); i<len; i++) {
            Utterance utt = (Utterance)uttList.get(i);
            // Create target chain for the utterance
            Relation segs = utt.getRelation(Relation.SEGMENT);
            List targets = new ArrayList();
            for (Item s = segs.getHead(); s != null; s = s.getNext()) {
                String segName = s.getFeatures().getString("name");
                // Not sure if this is needed:
                s.getFeatures().setString("clunit_name", segName);
                targets.add(new Target(segName, s, -999)); //-999: value for unit size is not used
            }
            // create target feature string for the target chain
            for (int j=0, nTargets = targets.size(); j<nTargets; j++) {
                Target target = (Target) targets.get(j);
                FeatureVector features = featureComputer.computeFeatureVector(target);
                text.append(featureComputer.toStringValues(features));
                text.append("\n");
                bin.append(features.toString());
                bin.append("\n");
            }
        }
        // Leave an empty line between sections:
        String out = header + "\n" + text + "\n" + bin;
        MaryData result = new MaryData(outputType());
        result.setPlainText(out);
        return result;
    }
}
