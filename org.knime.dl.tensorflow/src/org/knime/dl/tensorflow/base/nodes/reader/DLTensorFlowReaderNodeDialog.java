/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.tensorflow.base.nodes.reader;

import java.awt.Color;
import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.tensorflow.base.nodes.reader.config.DialogComponentColoredLabel;
import org.knime.dl.tensorflow.base.nodes.reader.config.DialogComponentFileOrDirChooser;
import org.knime.dl.tensorflow.core.DLTensorFlowSavedModelUtil;
import org.tensorflow.framework.SavedModel;

/**
 * Dialog for the TensorFlow SavedModel Reader node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLTensorFlowReaderNodeDialog extends DefaultNodeSettingsPane {

	private static final Collection<String> EMPTY_COLLECTION = Collections.singleton("                        ");

	private static final String FILE_HISTORY_ID = "org.knime.dl.tensorflow,base.nodes.reader";

	private final SettingsModelString m_smFilePath = DLTensorFlowReaderNodeModel.createFilePathSettingsModel();

	private final SettingsModelBoolean m_smCopyNetwork = DLTensorFlowReaderNodeModel.createCopyNetworkSettingsModel();

	private final SettingsModelStringArray m_smTags = DLTensorFlowReaderNodeModel.createTagsSettingsModel();

	private final SettingsModelStringArray m_smSignatures = DLTensorFlowReaderNodeModel.createSignaturesSettingsModel();

	private final DialogComponentFileOrDirChooser m_dcFiles;

	private final DialogComponentBoolean m_dcCopyNetwork;

	private final DialogComponentStringListSelection m_dcTags;

	private final DialogComponentStringListSelection m_dcSignatures;

	private final DialogComponentColoredLabel m_dcErrorLabel;

	private SavedModel m_savedModel;

	private String m_previousFilePath;

	public DLTensorFlowReaderNodeDialog() {
		super();

		// Create the dialog components
		m_dcFiles = new DialogComponentFileOrDirChooser(m_smFilePath, FILE_HISTORY_ID, JFileChooser.OPEN_DIALOG, null,
				".zip");
		m_dcCopyNetwork = new DialogComponentBoolean(m_smCopyNetwork,
				"Copy deep learning network into KNIME workflow?");
		m_dcTags = new DialogComponentStringListSelection(m_smTags, "Tags", EMPTY_COLLECTION, true, 5);
		m_dcSignatures = new DialogComponentStringListSelection(m_smSignatures, "Signatures", EMPTY_COLLECTION, true,
				5);
		m_dcErrorLabel = new DialogComponentColoredLabel("", Color.RED);

		// Add the dialog components
		createNewGroup("Input Location");
		addDialogComponent(m_dcFiles);
		addDialogComponent(m_dcTags);
		addDialogComponent(m_dcSignatures);
		addDialogComponent(m_dcCopyNetwork);
		addDialogComponent(m_dcErrorLabel);
		closeCurrentGroup();

		// Add change listeners
		m_smFilePath.addChangeListener((e) -> {
			readSavedModel();
			updateTags();
		});
		m_smTags.addChangeListener((e) -> updateSignatures());
	}

	private void readSavedModel() {
		m_dcErrorLabel.setText("");
		try {
			String filePath = m_smFilePath.getStringValue();
			if (!filePath.equals(m_previousFilePath)) {
				m_savedModel = DLTensorFlowSavedModelUtil.readSavedModelProtoBuf(FileUtil.toURL(filePath));
			}
			return;
		} catch (DLInvalidSourceException e) {
			m_dcErrorLabel.setText(e.getMessage());
		} catch (InvalidPathException | MalformedURLException e) {
			m_dcErrorLabel.setText("The filepath is not valid.");
		}
		m_savedModel = null;
	}

	/**
	 * Updates the tags shown for selection in {@link #m_dcTags}. If
	 * {@link #m_savedModel} is <code>null</code> the list is set to
	 * {@link #EMPTY_COLLECTION}.
	 */
	private void updateTags() {
		Collection<String> newTagList;
		if (m_savedModel != null) {
			newTagList = DLTensorFlowSavedModelUtil.getContainedTags(m_savedModel);
			if (newTagList.isEmpty()) {
				newTagList = EMPTY_COLLECTION;
				m_dcErrorLabel.setText("The SavedModel doesn't contain tags.");
			}
		} else {
			newTagList = EMPTY_COLLECTION;
		}
		m_dcTags.replaceListItems(newTagList);
	}

	/**
	 * Updates the signatures shown for selection in {@link #m_dcSignatures}.
	 */
	private void updateSignatures() {
		Collection<String> newSignatureList;
		List<String> tags = Arrays.asList(m_smTags.getStringArrayValue());
		if (m_savedModel != null && !tags.isEmpty()) {
			newSignatureList = DLTensorFlowSavedModelUtil.getSignatureDefs(m_savedModel, tags);
			if (newSignatureList.isEmpty()) {
				newSignatureList = EMPTY_COLLECTION;
				m_dcErrorLabel.setText("The SavedModel doesn't contain signatures with the selected tag");
			}
		} else {
			newSignatureList = EMPTY_COLLECTION;
		}
		m_dcSignatures.replaceListItems(newSignatureList);
	}
}