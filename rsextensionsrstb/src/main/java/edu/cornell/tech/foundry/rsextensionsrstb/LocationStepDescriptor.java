package edu.cornell.tech.foundry.rsextensionsrstb;

import org.researchstack.backbone.answerformat.TextAnswerFormat;

import edu.cornell.tech.foundry.researchsuitetaskbuilder.DefaultStepGenerators.descriptors.RSTBQuestionStepDescriptor;

/**
 * Created by Christina on 6/20/17.
 */

public class LocationStepDescriptor extends RSTBQuestionStepDescriptor{


    public int maximumLength = TextAnswerFormat.UNLIMITED_LENGTH;
    public boolean multipleLines = false;


    LocationStepDescriptor() {

    }
}
