package org.esa.beam.meris.qaa.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultIOParametersPanel;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.io.FileUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Holds the UI components for the QAA dialog..
 *
 * @author Marco Peters
 * @since 1.1
 */
public class QaaForm extends JTabbedPane {

    private DefaultIOParametersPanel ioParametersPanel;
    private TargetProductSelector targetProductSelector;

    public QaaForm(AppContext appContext, OperatorSpi operatorSpi, PropertySet propertySet,
                   TargetProductSelector targetProductSelector) {
        this.targetProductSelector = targetProductSelector;
        ioParametersPanel = createIOParameterPanel(appContext, operatorSpi, this.targetProductSelector);

        JScrollPane parametersPanel = createParametersPanel(appContext, propertySet);
        addTab("I/O Parameters", ioParametersPanel);
        addTab("Processing Parameters", parametersPanel);

        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();
        if (!sourceProductSelectorList.isEmpty()) {
            final SourceProductSelector sourceProductSelector = sourceProductSelectorList.get(0);
            sourceProductSelector.addSelectionChangeListener(new SourceProductChangeListener());
        }
    }

    private DefaultIOParametersPanel createIOParameterPanel(AppContext appContext, OperatorSpi operatorSpi,
                                                            TargetProductSelector targetProductSelector) {
        return new DefaultIOParametersPanel(appContext, operatorSpi, targetProductSelector);
    }

    private JScrollPane createParametersPanel(final AppContext appContext, PropertySet propertySet) {
        PropertyDescriptor validExpressionDescriptor = propertySet.getDescriptor("validPixelExpression");
        validExpressionDescriptor.setAttribute("propertyEditor", new ExpressionPropertyEditor(appContext));
        PropertyPane parametersPane = new PropertyPane(propertySet);
        final JPanel parametersPanel = parametersPane.createPanel();
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        return new JScrollPane(parametersPanel);
    }

    private class SourceProductChangeListener extends AbstractSelectionChangeListener {

        private static final String TARGET_PRODUCT_NAME_SUFFIX = "_qaa";

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
            String productName = "";
            if (selectedProduct != null) {
                productName = FileUtils.getFilenameWithoutExtension(selectedProduct.getName());
            }
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            targetProductSelectorModel.setProductName(productName + TARGET_PRODUCT_NAME_SUFFIX);

        }
    }

    public Product getSourceProduct() {
        SourceProductSelector sourceProductSelector = ioParametersPanel.getSourceProductSelectorList().get(0);
        return sourceProductSelector.getSelectedProduct();
    }

    public void prepareShow() {
        ioParametersPanel.initSourceProductSelectors();
    }

    public void prepareHide() {
        ioParametersPanel.releaseSourceProductSelectors();
    }

    private class ExpressionPropertyEditor extends PropertyEditor {

        private final AppContext appContext;

        ExpressionPropertyEditor(AppContext appContext) {
            this.appContext = appContext;
        }

        @Override
        public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
            JTextField textField = new JTextField();
            ComponentAdapter adapter = new TextComponentAdapter(textField);
            final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
            final JPanel subPanel = new JPanel(new BorderLayout(2, 2));
            subPanel.add(textField, BorderLayout.CENTER);
            JButton etcButton = new JButton("...");
            etcButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    invokeExpressionEditor(binding);
                }
            });
            subPanel.add(etcButton, BorderLayout.EAST);
            return subPanel;

        }

        private void invokeExpressionEditor(Binding binding) {
            ArrayList<SourceProductSelector> srcProductList = ioParametersPanel.getSourceProductSelectorList();
            if (isSourceProductSelected(srcProductList)) {
                Product[] products = new Product[]{srcProductList.get(0).getSelectedProduct()};
                ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                        products, products[0], appContext.getPreferences());
                expressionPane.setCode((String) binding.getPropertyValue());
                if (expressionPane.showModalDialog(null, "Expression Editor") == ModalDialog.ID_OK) {
                    binding.setPropertyValue(expressionPane.getCode());
                }
            } else {
                JOptionPane.showMessageDialog(QaaForm.this,
                                              "Please select a source product before editing the expression.",
                                              "Edit Expression", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private boolean isSourceProductSelected(ArrayList<SourceProductSelector> srcProductList) {
            return !srcProductList.isEmpty() && srcProductList.get(0).getSelectedProduct() != null;
        }
    }
}
