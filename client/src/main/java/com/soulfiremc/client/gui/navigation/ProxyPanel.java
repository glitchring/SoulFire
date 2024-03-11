/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.client.gui.navigation;

import com.soulfiremc.client.gui.GUIFrame;
import com.soulfiremc.client.gui.GUIManager;
import com.soulfiremc.client.gui.libs.JEnumComboBox;
import com.soulfiremc.client.gui.popups.ImportTextDialog;
import com.soulfiremc.settings.proxy.ProxyType;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.BuiltinSettingsConstants;
import com.soulfiremc.util.EnabledWrapper;
import com.soulfiremc.util.SFPathConstants;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.lenni0451.commons.swing.GBC;

public class ProxyPanel extends NavigationItem {
  @Inject
  public ProxyPanel(GUIManager guiManager, GUIFrame parent, CardsContainer cardsContainer) {
    setLayout(new GridBagLayout());

    var proxySettingsPanel = new JPanel();
    proxySettingsPanel.setLayout(new GridBagLayout());

    GeneratedPanel.addComponents(
        proxySettingsPanel,
        cardsContainer.getByNamespace(BuiltinSettingsConstants.PROXY_SETTINGS_ID),
        guiManager.clientSettingsManager());

    GBC.create(this).grid(0, 0).fill(GBC.HORIZONTAL).weightx(1).add(proxySettingsPanel);

    var toolBar = new JToolBar();

    GBC.create(this).grid(0, 1).insets(10, 4, -5, 4).fill(GBC.HORIZONTAL).weightx(0).add(toolBar);

    var columnNames = new String[] {"IP", "Port", "Username", "Password", "Type", "Enabled"};
    var model =
        new DefaultTableModel(columnNames, 0) {
          final Class<?>[] columnTypes =
              new Class<?>[] {
                Object.class,
                Integer.class,
                Object.class,
                Object.class,
                ProxyType.class,
                Boolean.class
              };

          @Override
          public Class<?> getColumnClass(int columnIndex) {
            return columnTypes[columnIndex];
          }
        };

    var proxyList = new JTable(model);

    var proxyRegistry = guiManager.clientSettingsManager().proxyRegistry();
    proxyRegistry.addLoadHook(
        () -> {
          model.getDataVector().removeAllElements();

          var proxies = proxyRegistry.getProxies();
          var registrySize = proxies.size();
          var dataVector = new Object[registrySize][];
          for (var i = 0; i < registrySize; i++) {
            var proxy = proxies.get(i);

            dataVector[i] =
                new Object[] {
                  proxy.value().host(),
                  proxy.value().port(),
                  proxy.value().username(),
                  proxy.value().password(),
                  proxy.value().type(),
                  proxy.enabled()
                };
          }

          model.setDataVector(dataVector, columnNames);

          proxyList
              .getColumnModel()
              .getColumn(4)
              .setCellEditor(new DefaultCellEditor(new JEnumComboBox<>(ProxyType.class)));

          model.fireTableDataChanged();
        });

    Runnable reconstructFromTable =
        () -> {
          var proxies = new ArrayList<EnabledWrapper<SFProxy>>();

          for (var i = 0; i < proxyList.getRowCount(); i++) {
            var row = new Object[proxyList.getColumnCount()];
            for (var j = 0; j < proxyList.getColumnCount(); j++) {
              row[j] = proxyList.getValueAt(i, j);
            }

            var host = (String) row[0];
            var port = (int) row[1];
            var username = (String) row[2];
            var password = (String) row[3];
            var type = (ProxyType) row[4];
            var enabled = (boolean) row[5];

            proxies.add(
                new EnabledWrapper<>(enabled, new SFProxy(type, host, port, username, password)));
          }

          proxyRegistry.setProxies(proxies);
        };
    proxyList.addPropertyChangeListener(
        evt -> {
          if ("tableCellEditor".equals(evt.getPropertyName()) && !proxyList.isEditing()) {
            reconstructFromTable.run();
          }
        });

    var scrollPane = new JScrollPane(proxyList);

    GBC.create(this).grid(0, 2).fill(GBC.BOTH).weight(1, 1).add(scrollPane);

    toolBar.setFloatable(false);
    var addButton = new JButton("+");
    addButton.setToolTipText("Add proxies to the list");
    addButton.addMouseListener(
        new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            var menu = new JPopupMenu();
            menu.add(createProxyLoadButton(guiManager, parent, ProxyType.HTTP));
            menu.add(createProxyLoadButton(guiManager, parent, ProxyType.SOCKS4));
            menu.add(createProxyLoadButton(guiManager, parent, ProxyType.SOCKS5));
            menu.show(e.getComponent(), e.getX(), e.getY());
          }
        });
    var removeButton = new JButton("-");
    removeButton.setToolTipText("Remove selected proxies from the list");
    removeButton.addActionListener(
        e -> {
          var selectedRows = proxyList.getSelectedRows();
          for (var i = selectedRows.length - 1; i >= 0; i--) {
            model.removeRow(selectedRows[i]);
          }
          reconstructFromTable.run();
        });

    toolBar.add(addButton);
    toolBar.add(removeButton);
    toolBar.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
    toolBar.setBackground(UIManager.getColor("Table.background"));
  }

  private static JMenuItem createProxyLoadButton(
      GUIManager guiManager, GUIFrame parent, ProxyType type) {
    var button = new JMenuItem(type.toString());

    button.addActionListener(
        e ->
            new ImportTextDialog(
                SFPathConstants.WORKING_DIRECTORY,
                String.format("Load %s proxies", type),
                String.format("%s list file", type),
                guiManager,
                parent,
                text ->
                    guiManager.clientSettingsManager().proxyRegistry().loadFromString(text, type)));

    return button;
  }

  @Override
  public String getNavigationName() {
    return "Proxies";
  }

  @Override
  public String getNavigationId() {
    return "proxy-menu";
  }
}
