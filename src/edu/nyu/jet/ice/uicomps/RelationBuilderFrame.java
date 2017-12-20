package edu.nyu.jet.ice.uicomps;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.relation.ActiveLearner;
import edu.nyu.jet.ice.relation.RelationOracle;
import edu.nyu.jet.ice.utils.SwingProgressMonitor;
import edu.nyu.jet.ice.views.swing.SwingRelationsPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * A frame which enables user review of paths generated by bootstrapping as part of the process of defining a relation.
 * The paths are displayed in a scrollable list; individual entries may be assigned the label "Yes" or "No" by key
 * bindings or buttons.
 *
 * @author yhe
 * @version 1.0
 */

public class RelationBuilderFrame extends JFrame {
	private ActiveLearner bootstrap;
	public JScrollPane listPane;
	public JList rankedList;
	public DefaultListModel rankedListModel = new DefaultListModel();
	public JRadioButton yesButton;
	public JRadioButton noButton;
	public JRadioButton undecidedButton;
	public RelationBuilder relationBuilder;
	public SwingRelationsPanel swingRelationsPanel;

	public static Set<String> usedPath = new HashSet<String>();

	public RelationBuilderFrame(String title, final RelationBuilder relationBuilder, final ActiveLearner bootstrap,
			final SwingRelationsPanel swingRelationsPanel, final String seedsAndArgs) {
		super(title);
		this.bootstrap = bootstrap;
		this.relationBuilder = relationBuilder;
		this.swingRelationsPanel = swingRelationsPanel;
		JPanel entitySetPanel = new JPanel(new MigLayout());
		entitySetPanel.setSize(400, 700);

		JLabel rankedListLabel = new JLabel("Bootstrapped patterns");
		entitySetPanel.add(rankedListLabel, "wrap");

		rankedList = new JList(rankedListModel) {
			@Override
			public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
				return -1;
			}
		};

		rankedList.setCellRenderer(new MyListCellRenderer()); // set labeled pattern color

		listPane = new JScrollPane(rankedList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		listPane.setSize(new Dimension(380, 380));
		listPane.setPreferredSize(new Dimension(380, 380));
		listPane.setMinimumSize(new Dimension(380, 380));
		listPane.setMaximumSize(new Dimension(380, 380));
		JPanel decisionPanel = new JPanel(new MigLayout());
		TitledBorder border = new TitledBorder("Decision");
		decisionPanel.setBorder(border);
		decisionPanel.setSize(new Dimension(380, 100));
		decisionPanel.setPreferredSize(new Dimension(380, 100));
		decisionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		yesButton = new JRadioButton("Yes");
		yesButton.setActionCommand("YES");
		noButton = new JRadioButton("No");
		noButton.setActionCommand("NO");
		undecidedButton = new JRadioButton("Undecided");
		undecidedButton.setActionCommand("UNDECIDED");

		ButtonGroup group = new ButtonGroup();
		group.add(yesButton);
		group.add(noButton);
		group.add(undecidedButton);
		decisionPanel.add(yesButton);
		decisionPanel.add(noButton);
		decisionPanel.add(undecidedButton);
		ActionListener decisionActionListener = new BootstrappingActionListener(this);
		yesButton.addActionListener(decisionActionListener);
		noButton.addActionListener(decisionActionListener);
		undecidedButton.addActionListener(decisionActionListener);

		entitySetPanel.add(listPane, "wrap");
		entitySetPanel.add(decisionPanel, "wrap");

		JPanel actionButtonsPanel = new JPanel(new MigLayout());
		JButton iterateButton = new JButton("Iterate");
		JButton saveButton = new JButton("Save");
		JButton exitButton = new JButton("Exit");
		actionButtonsPanel.add(iterateButton);
		actionButtonsPanel.add(saveButton);
		actionButtonsPanel.add(exitButton);
		entitySetPanel.add(actionButtonsPanel);
		this.add(entitySetPanel);

		// listeners...
		rankedList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent listSelectionEvent) {
				int idx = rankedList.getSelectedIndex();
				if (idx < 0)
					return;
				IcePath e = (IcePath) rankedListModel.getElementAt(idx);
				if (e.getChoice() == IcePath.IcePathChoice.YES) {
					yesButton.setSelected(true);
				}
				if (e.getChoice() == IcePath.IcePathChoice.NO) {
					noButton.setSelected(true);
				}
				if (e.getChoice() == IcePath.IcePathChoice.UNDECIDED) {
					undecidedButton.setSelected(true);
				}
			}
		});

		//
		// when Iterate button is pressed, collect pattern which have been
		// labeled YES or NO and start new thread for next bootstrapping
		// iteration
		//
		iterateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				java.util.List<IcePath> approvedPaths = new ArrayList<IcePath>();
				java.util.List<IcePath> rejectedPaths = new ArrayList<IcePath>();
				for (Object o : rankedListModel.toArray()) {
					IcePath e = (IcePath) o;
					if (e.getChoice() == IcePath.IcePathChoice.YES) {
						approvedPaths.add(e);
					} else {
						if (e.getChoice() == IcePath.IcePathChoice.NO) {
							rejectedPaths.add(e);
						}
					}
				}

				bootstrap.setProgressMonitor(
						new SwingProgressMonitor(RelationBuilderFrame.this, "Bootstrapping", "Collecting seeds...", 0, 5));

				BootstrapIterateThread thread = new BootstrapIterateThread(bootstrap, approvedPaths, rejectedPaths,
						RelationBuilderFrame.this);
				thread.start();
			}
		});

		// saveButton:
		//
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				java.util.List<IcePath> approvedPaths = new ArrayList<IcePath>();
				java.util.List<IcePath> rejectedPaths = new ArrayList<IcePath>();

				for (Object o : rankedListModel.toArray()) {
					IcePath e = (IcePath) o;

					if (e.getChoice() == IcePath.IcePathChoice.YES) {
						approvedPaths.add(e);
					}

					if (e.getChoice() == IcePath.IcePathChoice.NO) {
						rejectedPaths.add(e);
					}
				}

				String relationName = bootstrap.relationName;
				IceRelation iceRelation = Ice.getRelation(relationName);

				bootstrap.addPathsToSeedSet(approvedPaths, bootstrap.seedPaths);
				bootstrap.addPathsToSeedSet(rejectedPaths, bootstrap.rejects);
				DepPathMap depPathMap = DepPathMap.getInstance();
				StringBuilder text = new StringBuilder();
				Set<String> usedRepr = new HashSet<String>();

				// write to relation oracle
				if (seedsAndArgs.split("=").length == 2) {
					String seeds = seedsAndArgs.split("=")[0].trim();
					String[] seedsArr = seeds.split(":::"); // pass in seeds

					String args = seedsAndArgs.split("=")[1].trim();
					String invArg1 = args.split(":")[0].trim();
					String invArg2 = args.split(":")[1].trim();
					String arg1 = iceRelation.getArg1type().trim();
					String arg2 = iceRelation.getArg2type().trim();
					boolean inv = false;

					if (invArg1.equals(arg2) && invArg2.equals(arg1) && !arg1.equals(arg2)) { // check for inverted arguments
						inv = true;
					}

					// update relation oracle
					if (RelationOracle.existsRepr()) {
						RelationOracle.addRepr(approvedPaths, seedsArr, rejectedPaths, relationName + (inv ? "-1" : ""));

						RelationOracle.alterChoice(); // record changes in choice (YES/NO)

						RelationOracle.addLDP(depPathMap); // convert repr to LDP in relation oracle LDP
					}
				}

				for (String path : bootstrap.getSeedPaths()) {
					AnchoredPath ap = new AnchoredPath(bootstrap.getArg1Type(), path, bootstrap.getArg2Type());
					if (iceRelation != null) {
						iceRelation.addPath(ap.toString());
					}

					String repr = depPathMap.findRepr(ap);

					if (repr != null && !usedRepr.contains(repr)) {
						text.append(repr).append("\n");
						usedRepr.add(repr);
					}
				}

				if (relationBuilder != null) {
					relationBuilder.textArea.setText(text.toString());
				}
				if (swingRelationsPanel != null) {
					String[] reprs = text.toString().trim().split("\n");
					java.util.List<String> paths = Arrays.asList(reprs);
					swingRelationsPanel.updateEntriesListModel(paths);
				}

				// swingRelationsPanel.negPaths.clear();
				java.util.List<String> paths = new ArrayList<String>();
				for (String negPath : bootstrap.getRejects()) {
					// swingRelationsPanel.negPaths
					paths.add(bootstrap.getArg1Type() + " -- " + negPath + " -- " + bootstrap.getArg2Type());
				}
				swingRelationsPanel.negPaths.put(bootstrap.getRelationName(), paths);

				RelationBuilderFrame.this.dispose();
			}
		});

		// handle the click of [Exit]
		exitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				RelationBuilderFrame.this.dispose();
			}
		});

		// handle the click of [x]
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				RelationBuilderFrame.this.dispose();

			}
		});

		// adapters

		rankedList.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				JList l = (JList) e.getSource();
				ListModel m = l.getModel();
				int index = l.locationToIndex(e.getPoint());
				if (index > -1) {
					l.setToolTipText(((IcePath) m.getElementAt(index)).getExample());
				}
			}
		});

		// Key bindings
		rankedList.getInputMap().put(KeyStroke.getKeyStroke("Y"), "YES");
		rankedList.getInputMap().put(KeyStroke.getKeyStroke("y"), "YES");
		rankedList.getInputMap().put(KeyStroke.getKeyStroke("N"), "NO");
		rankedList.getInputMap().put(KeyStroke.getKeyStroke("n"), "NO");
		rankedList.getInputMap().put(KeyStroke.getKeyStroke("U"), "UNDECIDED");
		rankedList.getInputMap().put(KeyStroke.getKeyStroke("u"), "UNDECIDED");
		rankedList.getActionMap().put("YES", new AbstractAction() {
			public void actionPerformed(ActionEvent actionEvent) {
				yesButton.doClick();
			}
		});
		rankedList.getActionMap().put("NO", new AbstractAction() {
			public void actionPerformed(ActionEvent actionEvent) {
				noButton.doClick();
			}
		});
		rankedList.getActionMap().put("UNDECIDED", new AbstractAction() {
			public void actionPerformed(ActionEvent actionEvent) {
				undecidedButton.doClick();
			}
		});
	}

	// private void saveEntitySetToAuxFile(String typeName) {
	// try {
	// Properties props = new Properties();
	// props.load(new FileReader("parseprops"));
	// String fileName = props.getProperty("Jet.dataPath")
	// + "/"
	// + props.getProperty("Ace.EDTtype.auxFileName");
	// PrintWriter pw = new PrintWriter(new BufferedWriter(new
	// FileWriter(fileName, true)));
	//
	// for (Object o : rankedListModel.toArray()) {
	// RankChoiceEntity e = (RankChoiceEntity) o;
	// if (e.getDecision() == RankChoiceEntity.EntityDecision.YES) {
	// pw.println(e.getText().trim() + " | " + typeName + ":" + typeName + "
	// 1");
	// }
	// }
	// pw.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	public void updateList() {
		DefaultListModel newListModel = new DefaultListModel();
		for (IcePath s : bootstrap.foundPatterns) {
			newListModel.addElement(s);
		}
		rankedListModel = newListModel;
		rankedList.setModel(rankedListModel);
	}

}

// highlight labeled items (YES/NO) on the list
class MyListCellRenderer extends DefaultListCellRenderer {
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		Color myGreen = Color.decode("#00CC00");
		Color myRed = Color.decode("#FF6666");

		if (value.toString().contains("YES")) {
			c.setForeground(myGreen);
		} else if (value.toString().contains("NO")) {
			c.setForeground(Color.RED);
		}

		return c;
	}
}

class BootstrappingActionListener implements ActionListener {
	RelationBuilderFrame frame;

	BootstrappingActionListener(RelationBuilderFrame frame) {
		this.frame = frame;
	}

	public void actionPerformed(ActionEvent actionEvent) {
		int idx = frame.rankedList.getSelectedIndex();
		if (idx < 0)
			return;
		IcePath e = (IcePath) frame.rankedListModel.getElementAt(idx);
		e.setChoice(IcePath.IcePathChoice.valueOf(actionEvent.getActionCommand()));
		frame.rankedList.revalidate();
		frame.rankedList.repaint();
	}
}

class BootstrapIterateThread extends Thread {
	ActiveLearner bootstrap;
	java.util.List<IcePath> approvedPaths;
	java.util.List<IcePath> rejectedPaths;
	RelationBuilderFrame frame;

	BootstrapIterateThread(ActiveLearner bootstrap, java.util.List<IcePath> approvedPaths,
			java.util.List<IcePath> rejectedPaths, RelationBuilderFrame frame) {
		this.bootstrap = bootstrap;
		this.approvedPaths = approvedPaths;
		this.rejectedPaths = rejectedPaths;
		this.frame = frame;
	}

	public void run() {
		bootstrap.iterate(approvedPaths, rejectedPaths);
		frame.updateList();
		frame.listPane.validate();
		frame.listPane.repaint();
	}
}