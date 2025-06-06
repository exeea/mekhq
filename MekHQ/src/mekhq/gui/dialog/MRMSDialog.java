/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static megamek.client.ui.WrapLayout.wordWrap;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.models.XTableColumnModel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.service.enums.MRMSMode;
import mekhq.service.mrms.MRMSConfiguredOptions;
import mekhq.service.mrms.MRMSOption;
import mekhq.service.mrms.MRMSService;
import mekhq.service.mrms.MRMSService.MRMSPartSet;

/**
 * @author Kipsta
 */
public class MRMSDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(MRMSDialog.class);

    // region Variable Declarations
    private final JFrame frame;
    private CampaignGUI campaignGUI;
    private CampaignOptions campaignOptions;

    private MRMSMode mode;

    private Unit selectedUnit;
    private UnitTableModel unitTableModel;
    private JTable unitTable;
    private TableRowSorter<UnitTableModel> unitSorter;
    private JPanel pnlUnits;
    private JScrollPane scrollUnitList;
    private JButton btnSelectNone;
    private JButton btnSelectAssigned;
    private JButton btnSelectUnassigned;

    private PartsTableModel partsTableModel;
    private JTable partsTable;
    private JPanel pnlParts;
    private JScrollPane scrollPartsTable;
    private JButton btnSelectAllParts;

    private JCheckBox useRepairBox;
    private JCheckBox useSalvageBox;
    private JCheckBox useExtraTimeBox;
    private JCheckBox useRushJobBox;
    private JCheckBox allowCarryoverBox;
    private JCheckBox optimizeToCompleteTodayBox;
    private JCheckBox scrapImpossibleBox;
    private JCheckBox useAssignedTechsFirstBox;
    private JCheckBox replacePodPartsBox;

    private Map<PartRepairType, MRMSOptionControl> mrmsOptionControls = null;

    private List<Unit> unitList = null;
    private List<Part> completePartsList = null;
    private List<Part> filteredPartsList = null;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MRMS",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Constructors
    public MRMSDialog(final JFrame frame, final boolean modal, final CampaignGUI campaignGUI, final MRMSMode mode) {
        this(frame, modal, campaignGUI, null, mode);
    }

    public MRMSDialog(final JFrame frame, final boolean modal, final CampaignGUI campaignGUI, final Unit selectedUnit,
          final MRMSMode mode) {
        super(frame, modal);
        this.frame = frame;
        this.campaignGUI = campaignGUI;
        this.selectedUnit = selectedUnit;
        this.mode = mode;

        campaignOptions = campaignGUI.getCampaign().getCampaignOptions();

        initComponents();
        refreshOptions();

        if (getMode().isUnits()) {
            filterUnits(new MRMSConfiguredOptions(this));

            if (selectedUnit != null) {
                int unitCount = unitTable.getRowCount();

                for (int i = 0; i < unitCount; i++) {
                    int rowIdx = unitTable.convertRowIndexToModel(i);
                    Unit unit = unitTableModel.getUnit(rowIdx);

                    if (unit == null) {
                        continue;
                    }

                    if (unit.getId().toString().equals(selectedUnit.getId().toString())) {
                        unitTable.addRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
        } else if (getMode().isWarehouse()) {
            filterCompletePartsList(true);
        }

        setLocationRelativeTo(frame);
        setUserPreferences();
    }
    // endregion Constructors

    // region Initialization
    // endregion Initialization

    // region Getters and Setters
    public MRMSMode getMode() {
        return mode;
    }

    public Map<PartRepairType, MRMSOptionControl> getMRMSOptionControls() {
        return mrmsOptionControls;
    }
    // endregion Getters and Setters

    private void filterUnits(MRMSConfiguredOptions configuredOptions) {
        // Store selections so after the table is refreshed we can re-select them
        Map<String, Unit> selectedUnitMap = new HashMap<>();

        int[] selectedRows = unitTable.getSelectedRows();

        for (int selectedRow : selectedRows) {
            int rowIdx = unitTable.convertRowIndexToModel(selectedRow);
            Unit unit = unitTableModel.getUnit(rowIdx);

            if (unit == null) {
                continue;
            }

            selectedUnitMap.put(unit.getId().toString(), unit);
        }

        int activeCount = 0;
        int inactiveCount = 0;

        unitList = new ArrayList<>();

        for (Unit unit : campaignGUI.getCampaign().getServiceableUnits()) {
            if (!MRMSService.isValidMRMSUnit(unit, configuredOptions)) {
                continue;
            }

            unitList.add(unit);

            if ((unit.getActiveCrew() == null) || unit.getActiveCrew().isEmpty()) {
                inactiveCount++;
            } else {
                activeCount++;
            }
        }

        btnSelectAssigned.setText(MessageFormat.format(resources.getString("btnSelectAssigned.format"), activeCount));
        btnSelectUnassigned.setText(MessageFormat.format(resources.getString("btnSelectUnassigned.format"),
              inactiveCount));

        unitTableModel.setData(unitList);

        int unitCount = unitTable.getRowCount();

        for (int i = 0; i < unitCount; i++) {
            int rowIdx = unitTable.convertRowIndexToModel(i);
            Unit unit = unitTableModel.getUnit(rowIdx);

            if (!selectedUnitMap.containsKey(unit.getId().toString())) {
                continue;
            }

            unitTable.addRowSelectionInterval(i, i);
        }
    }

    private void filterCompletePartsList(boolean refreshCompleteList) {
        Map<PartRepairType, PartRepairType> activeMROMap = new HashMap<>();

        for (final PartRepairType partRepairType : PartRepairType.getMRMSValidTypes()) {
            final MRMSOptionControl mrmsOptionControl = mrmsOptionControls.get(partRepairType);
            if ((mrmsOptionControl == null) || !mrmsOptionControl.getActiveBox().isSelected()) {
                continue;
            }
            activeMROMap.put(partRepairType, partRepairType);
        }

        if (refreshCompleteList) {
            completePartsList = new ArrayList<>();

            campaignGUI.getCampaign().getWarehouse().forEachSparePart(part -> {
                if (!part.isBeingWorkedOn() &&
                          part.needsFixing() &&
                          !(part instanceof AmmoBin) &&
                          (part.getSkillMin() <= SkillType.EXP_ELITE)) {
                    completePartsList.add(part);
                }
            });
        }

        filteredPartsList = new ArrayList<>();
        int quantity = 0;

        for (Part part : completePartsList) {
            PartRepairType partType = IPartWork.findCorrectMRMSType(part);

            if (activeMROMap.containsKey(partType)) {
                filteredPartsList.add(part);

                quantity += part.getQuantity();
            }
        }

        btnSelectAllParts.setText(MessageFormat.format(resources.getString("btnSelectAllParts.format"), quantity));
        partsTableModel.setData(filteredPartsList);

        int count = partsTable.getRowCount();

        if (count > 0) {
            partsTable.addRowSelectionInterval(0, count - 1);
        }
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resources.getString(getMode().isUnits() ? "MRMSDialog.title" : "MassRepair.title"));

        final Container content = getContentPane();
        content.setLayout(new BorderLayout());

        AbstractMHQScrollablePanel pnlMain = new DefaultMHQScrollablePanel(frame, "pnlMain", new GridBagLayout());

        if (getMode().isUnits()) {
            pnlMain.add(createUnitsPanel(), createBaseConstraints(0));
            pnlMain.add(createUnitActionButtons(), createBaseConstraints(1));
        } else if (getMode().isWarehouse()) {
            pnlMain.add(createPartsPanel(), createBaseConstraints(0));
            pnlMain.add(createPartsActionButtons(), createBaseConstraints(1));
        }

        pnlMain.add(createOptionsPanel(), createBaseConstraints(2));

        content.add(new JScrollPaneWithSpeed(pnlMain), BorderLayout.CENTER);
        content.add(createActionButtons(), BorderLayout.SOUTH);

        pack();
    }

    private GridBagConstraints createBaseConstraints(int rowIdx) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = rowIdx;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        return gridBagConstraints;
    }

    private JPanel createUnitsPanel() {
        pnlUnits = new JPanel(new GridBagLayout());
        pnlUnits.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resources.getString(
              "UnitsPanel.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        unitTableModel = new UnitTableModel(campaignGUI.getCampaign());

        unitSorter = new TableRowSorter<>(unitTableModel);
        unitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
        unitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());
        unitSorter.setComparator(UnitTableModel.COL_RSTATUS, Comparator.naturalOrder());

        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_STATUS, SortOrder.DESCENDING));
        unitSorter.setSortKeys(sortKeys);

        unitTable = new JTable(unitTableModel);
        unitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        unitTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        unitTable.setColumnModel(new XTableColumnModel());
        unitTable.createDefaultColumnsFromModel();
        unitTable.setRowSorter(unitSorter);

        TableColumn column;

        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = ((XTableColumnModel) unitTable.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(unitTableModel.getColumnWidth(i));
            column.setCellRenderer(unitTableModel.getRenderer(false));

            if ((i != UnitTableModel.COL_NAME) &&
                      (i != UnitTableModel.COL_TYPE) &&
                      (i != UnitTableModel.COL_STATUS) &&
                      (i != UnitTableModel.COL_RSTATUS)) {
                ((XTableColumnModel) unitTable.getColumnModel()).setColumnVisible(column, false);
            }
        }

        unitTable.setIntercellSpacing(new Dimension(0, 0));
        unitTable.setShowGrid(false);

        scrollUnitList = new JScrollPaneWithSpeed(unitTable);
        scrollUnitList.setMinimumSize(new Dimension(350, 200));
        scrollUnitList.setPreferredSize(new Dimension(350, 200));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        pnlUnits.add(scrollUnitList, gridBagConstraints);

        return pnlUnits;
    }

    private JPanel createPartsPanel() {
        pnlParts = new JPanel(new GridBagLayout());
        pnlParts.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resources.getString(
              "PartsPanel.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        partsTableModel = new PartsTableModel();
        partsTableModel.setData(new ArrayList<>());
        partsTable = new JTable(partsTableModel);
        partsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        partsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        partsTable.setColumnModel(new XTableColumnModel());
        partsTable.createDefaultColumnsFromModel();
        TableRowSorter<PartsTableModel> partsSorter = new TableRowSorter<>(partsTableModel);
        partsSorter.setComparator(PartsTableModel.COL_DETAIL, new PartsDetailSorter());
        partsTable.setRowSorter(partsSorter);

        TableColumn column;

        for (int i = 0; i < PartsTableModel.N_COL; i++) {
            column = ((XTableColumnModel) partsTable.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(partsTableModel.getColumnWidth(i));
            column.setCellRenderer(partsTableModel.getRenderer());

            if ((i != PartsTableModel.COL_QUANTITY) &&
                      (i != PartsTableModel.COL_NAME) &&
                      (i != PartsTableModel.COL_DETAIL) &&
                      (i != PartsTableModel.COL_TECH_BASE)) {
                ((XTableColumnModel) partsTable.getColumnModel()).setColumnVisible(column, false);
            }
        }

        partsTable.setIntercellSpacing(new Dimension(0, 0));
        partsTable.setShowGrid(false);

        scrollPartsTable = new JScrollPaneWithSpeed(partsTable);
        scrollPartsTable.setMinimumSize(new Dimension(350, 200));
        scrollPartsTable.setPreferredSize(new Dimension(350, 200));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlParts.add(scrollPartsTable, gridBagConstraints);

        return pnlParts;
    }

    private JPanel createOptionsPanel() {
        JPanel pnlOptions = new JPanel(new GridBagLayout());
        pnlOptions.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resources.getString(
              "OptionsPanel.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        int gridRowIdx = 0;

        GridBagConstraints gridBagConstraints;

        useRepairBox = new JCheckBox(resources.getString("useRepairBox.text"));
        useRepairBox.setToolTipText(wordWrap(resources.getString("useRepairBox.toolTipText")));
        useRepairBox.setName("useRepairBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridRowIdx++;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlOptions.add(useRepairBox, gridBagConstraints);

        useSalvageBox = new JCheckBox(resources.getString("useSalvageBox.text"));
        useSalvageBox.setToolTipText(wordWrap(resources.getString("useSalvageBox.toolTipText")));
        useSalvageBox.setName("useSalvageBox");
        gridBagConstraints.gridy = gridRowIdx++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        pnlOptions.add(useSalvageBox, gridBagConstraints);

        useExtraTimeBox = new JCheckBox(resources.getString("useExtraTimeBox.text"));
        useExtraTimeBox.setToolTipText(wordWrap(resources.getString("useExtraTimeBox.toolTipText")));
        useExtraTimeBox.setName("useExtraTimeBox");
        gridBagConstraints.gridy = gridRowIdx++;
        pnlOptions.add(useExtraTimeBox, gridBagConstraints);

        useRushJobBox = new JCheckBox(resources.getString("useRushJobBox.text"));
        useRushJobBox.setToolTipText(wordWrap(resources.getString("useRushJobBox.toolTipText")));
        useRushJobBox.setName("useRushJobBox");
        gridBagConstraints.gridy = gridRowIdx++;
        pnlOptions.add(useRushJobBox, gridBagConstraints);

        allowCarryoverBox = new JCheckBox(resources.getString("allowCarryoverBox.text"));
        allowCarryoverBox.setToolTipText(wordWrap(resources.getString("allowCarryoverBox.toolTipText")));
        allowCarryoverBox.setName("allowCarryoverBox");
        allowCarryoverBox.addActionListener(e -> optimizeToCompleteTodayBox.setEnabled(allowCarryoverBox.isSelected()));
        gridBagConstraints.gridy = gridRowIdx++;
        pnlOptions.add(allowCarryoverBox, gridBagConstraints);

        optimizeToCompleteTodayBox = new JCheckBox(resources.getString("optimizeToCompleteTodayBox.text"));
        optimizeToCompleteTodayBox.setToolTipText(wordWrap(resources.getString("optimizeToCompleteTodayBox.toolTipText")));
        optimizeToCompleteTodayBox.setName("optimizeToCompleteTodayBox");
        gridBagConstraints.gridy = gridRowIdx++;
        pnlOptions.add(optimizeToCompleteTodayBox, gridBagConstraints);

        if (!getMode().isWarehouse()) {
            useAssignedTechsFirstBox = new JCheckBox(resources.getString("useAssignedTechsFirstBox.text"));
            useAssignedTechsFirstBox.setToolTipText(wordWrap(resources.getString("useAssignedTechsFirstBox.toolTipText")));
            useAssignedTechsFirstBox.setName("useAssignedTechsFirstBox");
            gridBagConstraints.gridy = gridRowIdx++;
            pnlOptions.add(useAssignedTechsFirstBox, gridBagConstraints);

            scrapImpossibleBox = new JCheckBox(resources.getString("scrapImpossibleBox.text"));
            scrapImpossibleBox.setToolTipText(wordWrap(resources.getString("scrapImpossibleBox.toolTipText")));
            scrapImpossibleBox.setName("scrapImpossibleBox");
            gridBagConstraints.gridy = gridRowIdx++;
            pnlOptions.add(scrapImpossibleBox, gridBagConstraints);

            replacePodPartsBox = new JCheckBox(resources.getString("replacePodPartsBox.text"));
            replacePodPartsBox.setToolTipText(wordWrap(resources.getString("replacePodPartsBox.toolTipText")));
            replacePodPartsBox.setName("replacePodPartsBox");
            gridBagConstraints.gridy = gridRowIdx++;
            pnlOptions.add(replacePodPartsBox, gridBagConstraints);
        }

        JPanel pnlItems = new JPanel(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridRowIdx++;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        pnlOptions.add(pnlItems, gridBagConstraints);

        gridRowIdx = 0;

        JLabel itemLabel = new JLabel(resources.getString("itemLabel.text"));
        itemLabel.setName("itemLabel");
        Font boldFont = new Font(itemLabel.getFont().getFontName(), Font.BOLD, itemLabel.getFont().getSize());
        itemLabel.setFont(boldFont);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridRowIdx++;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlItems.add(itemLabel, gridBagConstraints);

        JLabel minSkillLabel = new JLabel(resources.getString("minSkillLabel.text"));
        minSkillLabel.setToolTipText(wordWrap(resources.getString("minSkillLabel.toolTipText")));
        minSkillLabel.setName("minSkillLabel");
        minSkillLabel.setFont(boldFont);
        gridBagConstraints.gridx = gridRowIdx++;
        pnlItems.add(minSkillLabel, gridBagConstraints);

        JLabel maxSkillLabel = new JLabel(resources.getString("maxSkillLabel.text"));
        maxSkillLabel.setToolTipText(wordWrap(resources.getString("maxSkillLabel.toolTipText")));
        maxSkillLabel.setName("maxSkillLabel");
        maxSkillLabel.setFont(boldFont);
        gridBagConstraints.gridx = gridRowIdx++;
        pnlItems.add(maxSkillLabel, gridBagConstraints);

        JLabel minBTHLabel = new JLabel(resources.getString("minBTHLabel.text"));
        minBTHLabel.setToolTipText(wordWrap(resources.getString("minBTHLabel.toolTipText")));
        minBTHLabel.setName("minBTHLabel");
        minBTHLabel.setFont(boldFont);
        gridBagConstraints.gridx = gridRowIdx++;
        pnlItems.add(minBTHLabel, gridBagConstraints);

        JLabel maxBTHLabel = new JLabel(resources.getString("maxBTHLabel.text"));
        maxBTHLabel.setToolTipText(wordWrap(resources.getString("maxBTHLabel.toolTipText")));
        maxBTHLabel.setName("maxBTHLabel");
        maxBTHLabel.setFont(boldFont);
        gridBagConstraints.gridx = gridRowIdx++;
        pnlItems.add(maxBTHLabel, gridBagConstraints);

        JLabel minDailyTimeLabel = new JLabel(resources.getString("minDailyTimeLabel.text"));
        minDailyTimeLabel.setToolTipText(wordWrap(resources.getString("minDailyTimeLabel.toolTipText")));
        minDailyTimeLabel.setName("minDailyTimeLabel");
        minDailyTimeLabel.setFont(boldFont);
        gridBagConstraints.gridx = gridRowIdx++;
        pnlItems.add(minDailyTimeLabel, gridBagConstraints);

        gridRowIdx = 1;

        mrmsOptionControls = new HashMap<>();

        if (!getMode().isWarehouse()) {
            mrmsOptionControls.put(PartRepairType.ARMOUR,
                  createMRMSOptionControls(PartRepairType.ARMOUR,
                        "mrmsItemArmor.text",
                        "mrmsItemArmor.toolTipText",
                        "mrmsItemArmor",
                        pnlItems,
                        gridRowIdx++));

            mrmsOptionControls.put(PartRepairType.AMMUNITION,
                  createMRMSOptionControls(PartRepairType.AMMUNITION,
                        "mrmsItemAmmo.text",
                        "mrmsItemAmmo.toolTipText",
                        "mrmsItemAmmo",
                        pnlItems,
                        gridRowIdx++));
        }

        mrmsOptionControls.put(PartRepairType.WEAPON,
              createMRMSOptionControls(PartRepairType.WEAPON,
                    "mrmsItemWeapons.text",
                    "mrmsItemWeapons.toolTipText",
                    "mrmsItemWeapons",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.GENERAL_LOCATION,
              createMRMSOptionControls(PartRepairType.GENERAL_LOCATION,
                    "mrmsItemLocations.text",
                    "mrmsItemLocations.toolTipText",
                    "mrmsItemLocations",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.ENGINE,
              createMRMSOptionControls(PartRepairType.ENGINE,
                    "mrmsItemEngines.text",
                    "mrmsItemEngines.toolTipText",
                    "mrmsItemEngines",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.GYRO,
              createMRMSOptionControls(PartRepairType.GYRO,
                    "mrmsItemGyros.text",
                    "mrmsItemGyros.toolTipText",
                    "mrmsItemGyros",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.ACTUATOR,
              createMRMSOptionControls(PartRepairType.ACTUATOR,
                    "mrmsItemActuators.text",
                    "mrmsItemActuators.toolTipText",
                    "mrmsItemActuators",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.ELECTRONICS,
              createMRMSOptionControls(PartRepairType.ELECTRONICS,
                    "mrmsItemHead.text",
                    "mrmsItemHead.toolTipText",
                    "mrmsItemHead",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.GENERAL,
              createMRMSOptionControls(PartRepairType.GENERAL,
                    "mrmsItemOther.text",
                    "mrmsItemOther.toolTipText",
                    "mrmsItemOther",
                    pnlItems,
                    gridRowIdx++));
        mrmsOptionControls.put(PartRepairType.POD_SPACE,
              createMRMSOptionControls(PartRepairType.POD_SPACE,
                    "mrmsItemPod.text",
                    "mrmsItemPod.toolTipText",
                    "mrmsItemPod",
                    pnlItems,
                    gridRowIdx++));

        return pnlOptions;
    }

    private MRMSOptionControl createMRMSOptionControls(PartRepairType type, String text, String tooltipText,
          String activeBoxName, JPanel pnlItems, int rowIdx) {
        MRMSOption mrmsOption = campaignOptions.getMRMSOptions()
                                      .stream()
                                      .filter(option -> option.getType() == type)
                                      .findFirst()
                                      .orElse(new MRMSOption(type));

        int columnIdx = 0;

        MRMSOptionControl mrmsOptionControl = new MRMSOptionControl();
        mrmsOptionControl.setActiveBox(createMRMSOptionItemBox(text,
              tooltipText,
              activeBoxName,
              mrmsOption.isActive(),
              pnlItems,
              rowIdx,
              columnIdx++));
        mrmsOptionControl.setMinSkillCBox(createMRMSSkillCBox(mrmsOption.getSkillMin(),
              mrmsOption.isActive(),
              pnlItems,
              rowIdx,
              columnIdx++));
        mrmsOptionControl.setMaxSkillCBox(createMRMSSkillCBox(mrmsOption.getSkillMax(),
              mrmsOption.isActive(),
              pnlItems,
              rowIdx,
              columnIdx++));
        mrmsOptionControl.setMinBTHSpn(createMRMSSkillBTHSpinner(mrmsOption.getBthMin(),
              mrmsOption.isActive(),
              pnlItems,
              rowIdx,
              columnIdx++));
        mrmsOptionControl.setMaxBTHSpn(createMRMSSkillBTHSpinner(mrmsOption.getBthMax(),
              mrmsOption.isActive(),
              pnlItems,
              rowIdx,
              columnIdx++));
        mrmsOptionControl.setMinDailyTimeSpn(createMRMSDailyTimeSpinner(mrmsOption.getDailyTimeMin(),
              mrmsOption.isActive(),
              pnlItems,
              rowIdx,
              columnIdx++));

        mrmsOptionControl.getActiveBox().addActionListener(evt -> {
            if (mrmsOptionControl.getActiveBox().isSelected()) {
                mrmsOptionControl.getMinSkillCBox().setEnabled(true);
                mrmsOptionControl.getMaxSkillCBox().setEnabled(true);
                mrmsOptionControl.getMinBTHSpn().setEnabled(true);
                mrmsOptionControl.getMaxBTHSpn().setEnabled(true);
                mrmsOptionControl.getMinDailyTimeSpn().setEnabled(true);
            } else {
                mrmsOptionControl.getMinSkillCBox().setEnabled(false);
                mrmsOptionControl.getMaxSkillCBox().setEnabled(false);
                mrmsOptionControl.getMinBTHSpn().setEnabled(false);
                mrmsOptionControl.getMaxBTHSpn().setEnabled(false);
                mrmsOptionControl.getMinDailyTimeSpn().setEnabled(false);
            }
        });

        return mrmsOptionControl;
    }

    private JSpinner createMRMSSkillBTHSpinner(int selectedValue, boolean enabled, JPanel pnlItems, int rowIdx,
          int columnIdx) {
        JSpinner skillBTHSpn = new JSpinner(new SpinnerNumberModel(selectedValue, 1, 12, 1));
        ((DefaultEditor) skillBTHSpn.getEditor()).getTextField().setEditable(false);
        skillBTHSpn.setEnabled(enabled);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = columnIdx;
        gridBagConstraints.gridy = rowIdx;
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        pnlItems.add(skillBTHSpn, gridBagConstraints);

        return skillBTHSpn;
    }

    private JSpinner createMRMSDailyTimeSpinner(int selectedValue, boolean enabled, JPanel pnlItems, int rowIdx,
          int columnIdx) {
        JSpinner dailyTimeSpn = new JSpinner(new SpinnerNumberModel(selectedValue, 0, 480, 30));
        ((DefaultEditor) dailyTimeSpn.getEditor()).getTextField().setEditable(true);
        dailyTimeSpn.setEnabled(enabled);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = columnIdx;
        gridBagConstraints.gridy = rowIdx;
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        pnlItems.add(dailyTimeSpn, gridBagConstraints);

        return dailyTimeSpn;
    }

    private JComboBox<String> createMRMSSkillCBox(int selectedValue, boolean enabled, JPanel pnlItems, int rowIdx,
          int columnIdx) {
        DefaultComboBoxModel<String> skillModel = new DefaultComboBoxModel<>();
        skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_ULTRA_GREEN));
        skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_GREEN));
        skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_REGULAR));
        skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_VETERAN));
        skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_ELITE));
        skillModel.setSelectedItem(SkillType.getExperienceLevelName(selectedValue));
        JComboBox<String> skillCBox = new JComboBox<>(skillModel);
        skillCBox.setEnabled(enabled);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = columnIdx;
        gridBagConstraints.gridy = rowIdx;
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints.fill = GridBagConstraints.NONE;

        pnlItems.add(skillCBox, gridBagConstraints);

        return skillCBox;
    }

    private JCheckBox createMRMSOptionItemBox(String text, String toolTipText, String name, boolean selected,
          JPanel pnlItems, int rowIdx, int columnIdx) {
        JCheckBox optionItemBox = new JCheckBox();
        optionItemBox.setText(resources.getString(text));
        optionItemBox.setToolTipText(wordWrap(resources.getString(toolTipText)));
        optionItemBox.setName(name);
        optionItemBox.setSelected(selected);
        if (name.equals("mrmsItemPod") && !getMode().isWarehouse()) {
            replacePodPartsBox.setEnabled(selected);
        }
        optionItemBox.addActionListener(evt -> {
            mroOptionChecked();
            if ("mrmsItemPod".equals(((JCheckBox) evt.getSource()).getName()) && !getMode().isWarehouse()) {
                replacePodPartsBox.setEnabled(((JCheckBox) evt.getSource()).isSelected());
            }
        });

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = columnIdx;
        gridBagConstraints.gridy = rowIdx;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        pnlItems.add(optionItemBox, gridBagConstraints);

        return optionItemBox;
    }

    private void mroOptionChecked() {
        if (getMode().isWarehouse()) {
            filterCompletePartsList(false);
        }
    }

    private JPanel createUnitActionButtons() {
        JPanel pnlButtons = new JPanel();

        int btnIdx = 0;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = btnIdx++;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        btnSelectNone = new JButton(resources.getString("btnSelectNone.text"));
        btnSelectNone.setToolTipText(wordWrap(resources.getString("btnSelectNone.toolTipText")));
        btnSelectNone.setName("btnSelectNone");
        btnSelectNone.addActionListener(this::btnUnitsSelectNoneActionPerformed);
        pnlButtons.add(btnSelectNone, gridBagConstraints);

        btnSelectAssigned = new JButton(resources.getString("btnSelectAssigned.text"));
        btnSelectAssigned.setToolTipText(wordWrap(resources.getString("btnSelectAssigned.toolTipText")));
        btnSelectAssigned.setName("btnSelectAssigned");
        btnSelectAssigned.addActionListener(this::btnUnitsSelectAssignedActionPerformed);
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnSelectAssigned, gridBagConstraints);

        btnSelectUnassigned = new JButton(resources.getString("btnSelectUnassigned.text"));
        btnSelectUnassigned.setToolTipText(wordWrap(resources.getString("btnSelectUnassigned.toolTipText")));
        btnSelectUnassigned.setName("btnSelectUnassigned");
        btnSelectUnassigned.addActionListener(this::btnUnitsSelectUnassignedActionPerformed);
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnSelectUnassigned, gridBagConstraints);

        JButton btnHideUnits = new JButton(resources.getString(pnlUnits.isVisible() ?
                                                                     "btnHideUnits.Hide.text" :
                                                                     "btnHideUnits.Show.text"));
        btnHideUnits.setToolTipText(wordWrap(resources.getString(pnlUnits.isVisible() ?
                                                                       "btnHideUnits.Hide.toolTipText" :
                                                                       "btnHideUnits.Show.toolTipText")));
        btnHideUnits.setName("btnHideUnits");
        btnHideUnits.addActionListener(evt -> {
            pnlUnits.setVisible(!pnlUnits.isVisible());
            btnHideUnits.setText(resources.getString(pnlUnits.isVisible() ?
                                                           "btnHideUnits.Hide.text" :
                                                           "btnHideUnits.Show.text"));
            btnHideUnits.setToolTipText(wordWrap(resources.getString(pnlUnits.isVisible() ?
                                                                           "btnHideUnits.Hide.toolTipText" :
                                                                           "btnHideUnits.Show.toolTipText")));
            this.pack();
        });
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnHideUnits, gridBagConstraints);

        return pnlButtons;
    }

    private void btnUnitsSelectNoneActionPerformed(ActionEvent evt) {
        unitTable.removeRowSelectionInterval(0, unitTable.getRowCount() - 1);
    }

    private void btnUnitsSelectAssignedActionPerformed(ActionEvent evt) {
        int unitCount = unitTable.getRowCount();

        for (int i = 0; i < unitCount; i++) {
            int rowIdx = unitTable.convertRowIndexToModel(i);
            Unit unit = unitTableModel.getUnit(rowIdx);

            if ((unit == null) || (unit.getActiveCrew() == null) || unit.getActiveCrew().isEmpty()) {
                continue;
            }

            unitTable.addRowSelectionInterval(i, i);
        }
    }

    private void btnUnitsSelectUnassignedActionPerformed(ActionEvent evt) {
        int unitCount = unitTable.getRowCount();

        for (int i = 0; i < unitCount; i++) {
            int rowIdx = unitTable.convertRowIndexToModel(i);
            Unit unit = unitTableModel.getUnit(rowIdx);

            if (unit == null) {
                continue;
            }

            if ((unit.getActiveCrew() == null) || unit.getActiveCrew().isEmpty()) {
                unitTable.addRowSelectionInterval(i, i);
            }
        }
    }

    private JPanel createPartsActionButtons() {
        JPanel pnlButtons = new JPanel();

        int btnIdx = 0;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = btnIdx++;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        JButton btnDeselectParts = new JButton(resources.getString("btnDeselectParts.text"));
        btnDeselectParts.setToolTipText(wordWrap(resources.getString("btnDeselectParts.toolTipText")));
        btnDeselectParts.setName("btnDeselectParts");
        btnDeselectParts.addActionListener(this::btnUnselectPartsActionPerformed);
        pnlButtons.add(btnDeselectParts, gridBagConstraints);

        btnSelectAllParts = new JButton(resources.getString("btnSelectAllParts.text"));
        btnSelectAllParts.setToolTipText(wordWrap(resources.getString("btnSelectAllParts.toolTipText")));
        btnSelectAllParts.setName("btnSelectAllParts");
        btnSelectAllParts.addActionListener(this::btnSelectAllPartsActionPerformed);
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnSelectAllParts, gridBagConstraints);

        JButton btnHideParts = new JButton(resources.getString(pnlParts.isVisible() ?
                                                                     "btnHideParts.Hide.text" :
                                                                     "btnHideParts.Show.text"));
        btnHideParts.setToolTipText(wordWrap(resources.getString(pnlParts.isVisible() ?
                                                                       "btnHideParts.Hide.toolTipText" :
                                                                       "btnHideParts.Show.toolTipText")));
        btnHideParts.setName("btnHideParts");
        btnHideParts.addActionListener(evt -> {
            pnlParts.setVisible(!pnlParts.isVisible());
            btnHideParts.setText(resources.getString(pnlParts.isVisible() ?
                                                           "btnHideParts.Hide.text" :
                                                           "btnHideParts.Show.text"));
            btnHideParts.setToolTipText(wordWrap(resources.getString(pnlParts.isVisible() ?
                                                                           "btnHideParts.Hide.toolTipText" :
                                                                           "btnHideParts.Show.toolTipText")));
            this.pack();
        });
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnHideParts, gridBagConstraints);

        return pnlButtons;
    }

    private void btnUnselectPartsActionPerformed(ActionEvent evt) {
        partsTable.removeRowSelectionInterval(0, partsTable.getRowCount() - 1);
    }

    private void btnSelectAllPartsActionPerformed(ActionEvent evt) {
        partsTable.addRowSelectionInterval(0, partsTable.getRowCount() - 1);
    }

    private JPanel createActionButtons() {
        JPanel pnlButtons = new JPanel();

        int btnIdx = 0;

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = btnIdx++;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        JButton btnStart = new JButton(resources.getString(getMode().isUnits() ?
                                                                 "btnStart.MRMS.text" :
                                                                 "btnStart.MR.text"));
        btnStart.setName("btnStart");
        btnStart.addActionListener(this::btnStartMRMSActionPerformed);
        pnlButtons.add(btnStart, gridBagConstraints);

        JButton btnSaveAsDefault = new JButton(resources.getString("btnSaveAsDefault.text"));
        btnSaveAsDefault.setName("btnSaveAsDefault");
        btnSaveAsDefault.addActionListener(this::btnSaveAsDefaultActionPerformed);
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnSaveAsDefault, gridBagConstraints);

        JButton btnClose = new JButton(resources.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCancelActionPerformed);
        gridBagConstraints.gridx = btnIdx++;
        pnlButtons.add(btnClose, gridBagConstraints);

        return pnlButtons;
    }

    private void btnStartMRMSActionPerformed(ActionEvent evt) {
        // Not enough Astechs to run the tech teams
        if (campaignGUI.getCampaign().requiresAdditionalAstechs()) {
            int savePrompt = JOptionPane.showConfirmDialog(null,
                  resources.getString("NotEnoughAstechs.error"),
                  resources.getString("NotEnoughAstechs.errorTitle"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.ERROR_MESSAGE);
            if (savePrompt != JOptionPane.YES_OPTION) {
                return;
            } else {
                campaignGUI.getCampaign().fillAstechPool();
            }
        }

        if (getMode().isUnits()) {
            int[] selectedRows = unitTable.getSelectedRows();

            if ((selectedRows == null) || (selectedRows.length == 0)) {
                JOptionPane.showMessageDialog(this,
                      resources.getString("NoSelectedUnit.error"),
                      resources.getString("NoSelectedUnit.errorTitle"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Unit> units = new ArrayList<>();

            for (int selectedRow : selectedRows) {
                int rowIdx = unitTable.convertRowIndexToModel(selectedRow);
                Unit unit = unitTableModel.getUnit(rowIdx);

                if (unit == null) {
                    continue;
                }

                units.add(unit);
            }

            if (units.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                      resources.getString("NoUnits.error"),
                      resources.getString("NoUnits.errorTitle"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }

            MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(this);

            if (!configuredOptions.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                      resources.getString("MRMSDisabled.error"),
                      resources.getString("MRMSDisabled.errorTitle"),
                      JOptionPane.ERROR_MESSAGE);
            } else if (!configuredOptions.isHActiveMRMSOption()) {
                JOptionPane.showMessageDialog(this,
                      resources.getString("NoEnabledRepairOptions.error"),
                      resources.getString("NoEnabledRepairOptions.errorTitle"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }

            MRMSService.mrmsUnits(campaignGUI.getCampaign(), units, configuredOptions);

            filterUnits(configuredOptions);
        } else if (getMode().isWarehouse()) {
            int[] selectedRows = partsTable.getSelectedRows();

            if ((selectedRows == null) || (selectedRows.length == 0)) {
                JOptionPane.showMessageDialog(this,
                      resources.getString("NoSelectedParts.error"),
                      resources.getString("NoSelectedParts.errorTitle"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<IPartWork> parts = new ArrayList<>();

            for (int selectedRow : selectedRows) {
                int rowIdx = partsTable.convertRowIndexToModel(selectedRow);
                Part part = partsTableModel.getPartAt(rowIdx);

                if (part == null) {
                    continue;
                }

                parts.add(part);
            }

            if (parts.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                      resources.getString("NoParts.error"),
                      resources.getString("NoParts.errorTitle"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }

            MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(this);
            configuredOptions.setScrapImpossible(false);

            MRMSPartSet partSet = MRMSService.performWarehouseMRMS(parts, configuredOptions, campaignGUI.getCampaign());

            String msg = resources.getString("Completed.text");

            if (partSet.isHasRepairs()) {
                int count = partSet.countRepairs();
                msg += MessageFormat.format(resources.getString((count == 1) ?
                                                                      "Completed.repairCount.text" :
                                                                      "Completed.repairCountPlural.text"), count);
            }

            filterCompletePartsList(true);

            campaignGUI.getCampaign().addReport(msg);

            JOptionPane.showMessageDialog(this,
                  msg,
                  resources.getString("Completed.title"),
                  JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void btnSaveAsDefaultActionPerformed(ActionEvent evt) {
        updateOptions();
    }

    private void btnCancelActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    // region Campaign Options
    private void refreshOptions() {
        getUseRepairBox().setSelected(campaignOptions.isMRMSUseRepair());
        getUseSalvageBox().setSelected(campaignOptions.isMRMSUseSalvage());
        getUseExtraTimeBox().setSelected(campaignOptions.isMRMSUseExtraTime());
        getUseRushJobBox().setSelected(campaignOptions.isMRMSUseRushJob());
        getAllowCarryoverBox().setSelected(campaignOptions.isMRMSAllowCarryover());
        getOptimizeToCompleteTodayBox().setSelected(campaignOptions.isMRMSOptimizeToCompleteToday());
        getOptimizeToCompleteTodayBox().setEnabled(campaignOptions.isMRMSAllowCarryover());

        if (!getMode().isWarehouse()) {
            getScrapImpossibleBox().setSelected(campaignOptions.isMRMSScrapImpossible());
            getUseAssignedTechsFirstBox().setSelected(campaignOptions.isMRMSUseAssignedTechsFirst());
            getReplacePodPartsBox().setSelected(campaignOptions.isMRMSReplacePod());
        }
    }

    private void updateOptions() {
        campaignOptions.setMRMSUseRepair(getUseRepairBox().isSelected());
        campaignOptions.setMRMSUseSalvage(getUseSalvageBox().isSelected());
        campaignOptions.setMRMSUseExtraTime(getUseExtraTimeBox().isSelected());
        campaignOptions.setMRMSUseRushJob(getUseRushJobBox().isSelected());
        campaignOptions.setMRMSAllowCarryover(getAllowCarryoverBox().isSelected());
        campaignOptions.setMRMSOptimizeToCompleteToday(getOptimizeToCompleteTodayBox().isSelected());

        if (!getMode().isWarehouse()) {
            campaignOptions.setMRMSScrapImpossible(scrapImpossibleBox.isSelected());
            campaignOptions.setMRMSUseAssignedTechsFirst(useAssignedTechsFirstBox.isSelected());
            campaignOptions.setMRMSReplacePod(replacePodPartsBox.isSelected());
        }

        for (PartRepairType partRepairType : PartRepairType.getMRMSValidTypes()) {
            MRMSOptionControl mrmsOptionControl = mrmsOptionControls.get(partRepairType);
            if (mrmsOptionControl == null) {
                continue;
            }
            MRMSOption mrmsOption = new MRMSOption(partRepairType,
                  mrmsOptionControl.getActiveBox().isSelected(),
                  mrmsOptionControl.getMinSkillCBox().getSelectedIndex(),
                  mrmsOptionControl.getMaxSkillCBox().getSelectedIndex(),
                  (Integer) mrmsOptionControl.getMinBTHSpn().getValue(),
                  (Integer) mrmsOptionControl.getMaxBTHSpn().getValue(),
                  (Integer) mrmsOptionControl.getMinDailyTimeSpn().getValue());

            campaignOptions.addMRMSOption(mrmsOption);
        }

        MekHQ.triggerEvent(new OptionsChangedEvent(campaignGUI.getCampaign(), campaignOptions));

        JOptionPane.showMessageDialog(this,
              resources.getString("DefaultOptionsSaved.text"),
              resources.getString("DefaultOptionsSaved.title"),
              JOptionPane.INFORMATION_MESSAGE);
    }
    // endregion Campaign Options

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MRMSDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    public JCheckBox getUseRepairBox() {
        return useRepairBox;
    }

    public JCheckBox getUseSalvageBox() {
        return useSalvageBox;
    }

    public JCheckBox getUseExtraTimeBox() {
        return useExtraTimeBox;
    }

    public JCheckBox getUseRushJobBox() {
        return useRushJobBox;
    }

    public JCheckBox getAllowCarryoverBox() {
        return allowCarryoverBox;
    }

    public JCheckBox getOptimizeToCompleteTodayBox() {
        return optimizeToCompleteTodayBox;
    }

    public JCheckBox getScrapImpossibleBox() {
        return scrapImpossibleBox;
    }

    public JCheckBox getUseAssignedTechsFirstBox() {
        return useAssignedTechsFirstBox;
    }

    public JCheckBox getReplacePodPartsBox() {
        return replacePodPartsBox;
    }

    public static class MRMSOptionControl {
        private JCheckBox activeBox = null;
        private JComboBox<String> minSkillCBox = null;
        private JComboBox<String> maxSkillCBox = null;
        private JSpinner minBTHSpn = null;
        private JSpinner maxBTHSpn = null;
        private JSpinner minDailyTimeSpn = null;

        public JCheckBox getActiveBox() {
            return activeBox;
        }

        public void setActiveBox(JCheckBox activeBox) {
            this.activeBox = activeBox;
        }

        public JComboBox<String> getMinSkillCBox() {
            return minSkillCBox;
        }

        public void setMinSkillCBox(JComboBox<String> minSkillCBox) {
            this.minSkillCBox = minSkillCBox;
        }

        public JComboBox<String> getMaxSkillCBox() {
            return maxSkillCBox;
        }

        public void setMaxSkillCBox(JComboBox<String> maxSkillCBox) {
            this.maxSkillCBox = maxSkillCBox;
        }

        public JSpinner getMinBTHSpn() {
            return minBTHSpn;
        }

        public void setMinBTHSpn(JSpinner minBTHSpn) {
            this.minBTHSpn = minBTHSpn;
        }

        public JSpinner getMaxBTHSpn() {
            return maxBTHSpn;
        }

        public void setMaxBTHSpn(JSpinner maxBTHSpn) {
            this.maxBTHSpn = maxBTHSpn;
        }

        public JSpinner getMinDailyTimeSpn() {
            return minDailyTimeSpn;
        }

        public void setMinDailyTimeSpn(JSpinner minDailyTimeSpn) {
            this.minDailyTimeSpn = minDailyTimeSpn;
        }
    }
}
