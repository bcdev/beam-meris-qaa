package org.esa.beam.meris.qaa.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JOptionPane;

/**
 * Provides the user interface for the BEAM QAA IOP processor.
 *
 * @author Marco Peters
 * @since 1.1
 */
class QaaDialog extends SingleTargetProductDialog {

    private OperatorParameterSupport parameterSupport;
    private QaaForm form;
    private String opAlias;

    QaaDialog(String opAlias, AppContext appContext, String title, String helpId) {
        super(appContext, title, helpId);
        this.opAlias = opAlias;
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(opAlias);
        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();

        parameterSupport = new OperatorParameterSupport(operatorDescriptor);
        parameterSupport.getParameterMap().remove("invalidPixelExpression");
        final PropertySet propertySet = parameterSupport.getPropertySet();
        final Property invalidPixelExpressionProperty = propertySet.getProperty("invalidPixelExpression");
        if (invalidPixelExpressionProperty != null) {
            propertySet.removeProperty(invalidPixelExpressionProperty);
        }
        form = new QaaForm(appContext, operatorSpi, propertySet, getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorDescriptor,
                                                     parameterSupport,
                                                     appContext,
                                                     helpId);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Product sourceProduct = form.getSourceProduct();
        return GPF.createProduct(opAlias, parameterSupport.getParameterMap(), sourceProduct);
    }

    @Override
    protected void onApply() {
        if (isUserInputValid()) {
            super.onApply();
        }

    }

    private boolean isUserInputValid() {
        Product sourceProduct = form.getSourceProduct();
        if (sourceProduct == null) {
            JOptionPane.showMessageDialog(this.getContent(), "No source product selected.",
                                          "Invalid Settings", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }


    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

}
