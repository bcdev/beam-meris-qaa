package org.esa.beam.meris.qaa.ui;

import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.meris.qaa.QaaOp;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * This action is invokes the dialog of the BEAM QAA IOP processor.
 *
 * @author Marco Peters
 * @since 1.1
 */
public class MerisQaaAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final OperatorMetadata opMetadata = QaaOp.class.getAnnotation(OperatorMetadata.class);
        Command command = event.getCommand();
        final QaaDialog operatorDialog = new QaaDialog(opMetadata.alias(), getAppContext(),
                                                        "QAA for IOP", command.getHelpId());
        operatorDialog.getJDialog().pack();
        operatorDialog.show();


    }


}

