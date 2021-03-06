/*
 * This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 *  Foundation.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this
 *  program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  or from the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  Copyright (c) 2006 - 2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.ui.datasources.kettle.embedded;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.trans.step.BaseStepGenericXulDialog;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.designtime.DesignTimeContext;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.EmbeddedKettleDataFactoryMetaData;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.swing.tags.SwingDialog;

public class XulDialogHelper
{
  private static final Log logger = LogFactory.getLog(XulDialogHelper.class);

  private class ChangeHandler implements PropertyChangeListener
  {
    public void propertyChange(final PropertyChangeEvent evt)
    {

    }
  }

  private BaseStepGenericXulDialog dialog;
  private DesignTimeContext context;
  private TransMeta transformation;

  public XulDialogHelper(final DesignTimeContext context,
                         final TransMeta transformation)
  {
    this.context = context;
    this.transformation = transformation;
  }

  public JComponent createEditor() throws ReportDataFactoryException
  {
    dialog = createDialog();
    if (dialog == null)
    {
      return new JPanel();
    }

    dialog.addPropertyChangeListener(new ChangeHandler());
    dialog.validate();

    XulComponent root = dialog.getXulDomContainer().getDocumentRoot().getElementById("root");

    // Without the following two lines of code, message boxes and prompts will freeze the dialog...
    // There must be a better way.
    SwingDialog parent = (SwingDialog) root.getParent();
    JComponent panel = parent.getContainer();
    dialog.setModalParent(panel);
    return panel;
  }


  private BaseStepGenericXulDialog createDialog()
      throws ReportDataFactoryException
  {
    final StepMeta step = findInputStep();
    if (step == null)
    {
      return null;
    }

    // Render datasource specific dialog for editing step details...
    try
    {
      final String dlgClassName = step.getStepMetaInterface().getDialogClassName().replace("Dialog", "XulDialog");

      ClassLoader pluginClassLoader = step.getStepMetaInterface().getClass().getClassLoader();
      final Class<BaseStepGenericXulDialog> dialog =
          (Class<BaseStepGenericXulDialog>) Class.forName(dlgClassName, true, pluginClassLoader);
      final Constructor<BaseStepGenericXulDialog> constructor =
          dialog.getDeclaredConstructor(Object.class, BaseStepMeta.class, TransMeta.class, String.class);
      return constructor.newInstance(null, step.getStepMetaInterface(),
          step.getParentTransMeta(), EmbeddedKettleDataFactoryMetaData.DATA_CONFIGURATION_STEP);
    }
    catch (Exception e)
    {

      logger.error("Critical error attempting to dynamically create dialog. This datasource will not be available.", e);
      throw new ReportDataFactoryException("Error attempting to dynamically create dialog. Abort dialog rendering.");
    }
  }

  private StepMeta findInputStep()
  {
    return transformation.findStep(EmbeddedKettleDataFactoryMetaData.DATA_CONFIGURATION_STEP);
  }

  public boolean validate()
  {
    if (dialog != null)
    {
      if (!dialog.validate())
      {
        int val = dialog.showPromptMessage("One or more queries are missing required information. \n\r"
            + "This can cause these queries to fail when executing. \n\r"
            + "Select OK to continue, or Cancel to return and correct the query.", "MongoDb Datasource Warning");
        return (val == 0);
      }
    }
    return true;
  }

  public void accept()
  {
    if (dialog != null)
    {
      dialog.onAccept();
    }
  }

  public byte[] getRawData() throws KettleException
  {
    accept();
    try
    {
      return transformation.getXML().getBytes("UTF8");
    }
    catch (UnsupportedEncodingException e)
    {
      throw new KettleException(e);
    }
  }

  public void clear()
  {
    if (dialog != null)
    {
      dialog.clear();
    }
  }
}
