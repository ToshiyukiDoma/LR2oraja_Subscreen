package lr2touch;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.ComponentUI;
import java.util.function.IntConsumer;

public final class TouchWidgets {
 static String caps(Component c,String value){if(value==null)return null;for(Component p=c;p!=null;p=p.getParent())if(p instanceof JComponent&&Boolean.TRUE.equals(((JComponent)p).getClientProperty("preserveCase")))return value;return value.toUpperCase(java.util.Locale.ROOT);}
 public static JPanel half(Component child){JPanel row=Touch.panel(new GridLayout(1,2,16,0));row.add(child);row.add(Touch.panel(new BorderLayout()));return row;}
 public static final int TARGET=64;
 public static void install(){
  UIManager.put("Label.font",new Font("Yu Gothic UI",0,24));UIManager.put("Button.font",new Font("Yu Gothic UI",0,24));UIManager.put("CheckBox.font",new Font("Yu Gothic UI",0,24));
  installDrag();UIManager.put("ScrollBar.width",0);UIManager.put("ComboBox.font",new Font("Yu Gothic UI",0,24));UIManager.put("OptionPane.buttonMinimumWidth",200);
 }
 public static <T> void combo(JComboBox<T> box){
  box.setFont(new Font("Yu Gothic UI",0,24));box.setPreferredSize(new Dimension(340,TARGET));box.setMaximumRowCount(5);
  box.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focused){JLabel l=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);l.setText(caps(box,value==null?"":value.toString()));l.setFont(box.getFont());l.setBackground(box.getBackground());l.setForeground(box.getForeground());if(selected)l.setBackground(box.getBackground().brighter());l.setPreferredSize(new Dimension(300,TARGET));l.setBorder(BorderFactory.createEmptyBorder(10,16,10,16));return l;}});
  box.setUI(new BasicComboBoxUI(){protected JButton createArrowButton(){JButton b=new JButton("▼");b.setFont(new Font("Dialog",0,22));b.setPreferredSize(new Dimension(0,TARGET));b.setVisible(false);return b;}public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean focus){g.setColor(comboBox.getBackground());g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);}
   public void paintCurrentValue(Graphics g,Rectangle bounds,boolean focus){Component c=comboBox.getRenderer().getListCellRendererComponent(listBox,comboBox.getSelectedItem(),-1,false,false);c.setBackground(comboBox.getBackground());c.setForeground(comboBox.isEnabled()?comboBox.getForeground():new Color(0x90A0B4));c.setFont(comboBox.getFont());currentValuePane.paintComponent(g,c,comboBox,bounds.x,bounds.y,bounds.width,bounds.height,true);}
   protected javax.swing.plaf.basic.ComboPopup createPopup(){BasicComboPopup popup=new BasicComboPopup(comboBox){protected MouseListener createListMouseListener(){return new MouseAdapter(){public void mousePressed(MouseEvent e){list.setSelectedIndex(list.locationToIndex(e.getPoint()));}public void mouseReleased(MouseEvent e){if(e.isConsumed())return;int n=list.locationToIndex(e.getPoint());if(n>=0&&list.getCellBounds(n,n).contains(e.getPoint())){comboBox.setSelectedIndex(n);comboBox.setPopupVisible(false);}}};}};popup.getList().setFixedCellHeight(TARGET);for(Component c:popup.getComponents())if(c instanceof JScrollPane)dragPane((JScrollPane)c);return popup;}});
 }
 public static void stack(JPanel panel){Component[] children=panel.getComponents();panel.removeAll();panel.setLayout(new GridBagLayout());int row=0;for(Component original:children){Component child=original instanceof JButton?half(original):original;GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=row++;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(0,0,18,0);panel.add(child,c);}}
 public static void check(Touch ui,JCheckBox box){box.setOpaque(false);box.setIconTextGap(16);box.setPreferredSize(new Dimension(Math.max(300,box.getPreferredSize().width),TARGET));box.setIcon(new Icon(){public int getIconWidth(){return 34;}public int getIconHeight(){return 34;}public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(box.isSelected()?ui.accent:ui.line);g.fillRoundRect(x,y,32,32,10,10);if(box.isSelected()){g.setColor(ui.card);g.setStroke(new BasicStroke(3));g.drawLine(x+8,y+16,x+14,y+23);g.drawLine(x+14,y+23,x+25,y+9);}g.dispose();}});}
 static JDialog shell(Touch ui,String title){JDialog d=new JDialog(ui.frame,title.toUpperCase(java.util.Locale.ROOT),true);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);return d;}
 static JPanel body(Touch ui){JPanel b=new JPanel(new BorderLayout(16,20));b.setBackground(ui.card);b.setBorder(BorderFactory.createEmptyBorder(24,26,24,26));return b;}
 public static boolean confirm(Touch ui,String title,String text){
  JDialog d=shell(ui,title);JPanel b=body(ui);JPanel words=Touch.panel(new GridLayout(0,1,0,16));words.add(Touch.label(title,30,Font.BOLD));for(String line:text.split("\n"))words.add(Touch.label(line,24,0));b.add(words);
  boolean[] answer={false};JPanel buttons=Touch.panel(new GridLayout(1,2,18,0));buttons.add(ui.button("Cancel",d::dispose));JButton yes=ui.button("Yes, continue",()->{answer[0]=true;d.dispose();});yes.putClientProperty("accent",true);buttons.add(yes);b.add(buttons,BorderLayout.SOUTH);
  ui.themeTree(b);d.setContentPane(b);d.setSize(Math.min(1000,ui.frame.getWidth()-30),Math.min(500,ui.frame.getHeight()-30));d.setLocationRelativeTo(ui.frame);d.setVisible(true);d.dispose();return answer[0];
 }
 public static void dialog(Touch ui,String title,JComponent content){
  JDialog d=shell(ui,title);JPanel b=body(ui);b.add(Touch.label(title,30,Font.BOLD),BorderLayout.NORTH);JScrollPane scroll=scroll(content);b.add(scroll);b.add(half(ui.button("Done",d::dispose)),BorderLayout.SOUTH);ui.themeTree(b);d.setContentPane(b);d.setSize(Math.min(1140,ui.frame.getWidth()-20),Math.min(1000,ui.frame.getHeight()-20));d.setLocationRelativeTo(ui.frame);d.setVisible(true);d.dispose();
 }
 public static JScrollPane scroll(Component c){JScrollPane s=new JScrollPane(new Page(c));s.setBorder(null);dragPane(s);return s;}
 static final class Page extends JPanel implements Scrollable {
  Page(Component content){super(new BorderLayout());setOpaque(false);add(content,BorderLayout.NORTH);}
  public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
  public boolean getScrollableTracksViewportWidth(){return true;}
  public boolean getScrollableTracksViewportHeight(){return false;}
  public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 48;}
  public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(48,r.height-64);}
 }
 static void dragPane(JScrollPane s){s.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);s.putClientProperty("touchDrag",true);s.getVerticalScrollBar().setUnitIncrement(48);}
 static boolean dragInstalled;static java.awt.event.AWTEventListener dragListener;
 static void installDrag(){if(dragInstalled)return;dragInstalled=true;dragListener=new java.awt.event.AWTEventListener(){Point start,origin;JScrollPane pane;Component pressed;boolean dragged;
 public void eventDispatched(AWTEvent event){if(!(event instanceof MouseEvent))return;MouseEvent e=(MouseEvent)event;
 if(e.getID()==MouseEvent.MOUSE_PRESSED){pane=null;pressed=e.getComponent();if(pressed instanceof JSlider||pressed instanceof JTextComponent)return;Component c=pressed;while(c!=null&&!(c instanceof JScrollPane))c=c.getParent();if(c instanceof JScrollPane&&Boolean.TRUE.equals(((JScrollPane)c).getClientProperty("touchDrag"))){pane=(JScrollPane)c;start=e.getLocationOnScreen();origin=pane.getViewport().getViewPosition();dragged=false;}}
 else if(e.getID()==MouseEvent.MOUSE_DRAGGED&&pane!=null){Point now=e.getLocationOnScreen();if(!dragged&&start.distance(now)<12)return;dragged=true;if(pressed instanceof JComboBox)((JComboBox<?>)pressed).setPopupVisible(false);if(pressed instanceof AbstractButton){((AbstractButton)pressed).getModel().setArmed(false);((AbstractButton)pressed).getModel().setPressed(false);}Dimension size=pane.getViewport().getViewSize(),extent=pane.getViewport().getExtentSize();pane.getViewport().setViewPosition(new Point(Math.max(0,Math.min(size.width-extent.width,origin.x+start.x-now.x)),Math.max(0,Math.min(size.height-extent.height,origin.y+start.y-now.y))));e.consume();}
 else if(e.getID()==MouseEvent.MOUSE_RELEASED&&pane!=null){if(dragged){if(pressed instanceof AbstractButton)((AbstractButton)pressed).getModel().setArmed(false);e.consume();}pane=null;pressed=null;}
 }};Toolkit.getDefaultToolkit().addAWTEventListener(dragListener,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK);}
 public static void uninstallDrag(){if(dragListener!=null)Toolkit.getDefaultToolkit().removeAWTEventListener(dragListener);dragListener=null;dragInstalled=false;}
 public static JPanel option(Touch ui,String title,String[] names,int current,IntConsumer action){JPanel p=Touch.panel(new BorderLayout(0,4));p.add(Touch.label(title,24,Font.BOLD),BorderLayout.NORTH);JComboBox<String> box=new JComboBox<>(names);combo(box);box.setPreferredSize(new Dimension(240,TARGET));if(names.length>0)box.setSelectedIndex(Math.max(0,Math.min(names.length-1,current)));box.addActionListener(e->action.accept(box.getSelectedIndex()));p.add(box);((JLabel)p.getComponent(0)).addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(box.isEnabled())box.setPopupVisible(!box.isPopupVisible());}});return p;}
 public static int choose(Touch ui,String title,String[] choices){int[] chosen={-1};JDialog d=shell(ui,title);JPanel b=body(ui);b.add(Touch.label(title,30,Font.BOLD),BorderLayout.NORTH);JPanel list=Touch.panel(new GridLayout(0,1,0,12));for(int i=0;i<choices.length;i++){final int n=i;JButton button=ui.button(choices[i],()->{chosen[0]=n;d.dispose();});list.add(button);}b.add(scroll(list));b.add(half(ui.button("Cancel",d::dispose)),BorderLayout.SOUTH);ui.themeTree(b);d.setContentPane(b);d.setSize(Math.min(950,ui.frame.getWidth()-20),Math.min(850,ui.frame.getHeight()-20));d.setLocationRelativeTo(ui.frame);d.setVisible(true);d.dispose();return chosen[0];}
 public static JPanel number(Touch ui,String title,int value,int min,int max,String suffix,IntConsumer action){
  JPanel p=Touch.panel(new BorderLayout(12,8));JLabel label=Touch.label(title+": "+value+suffix,26,Font.BOLD);p.add(label,BorderLayout.NORTH);
  JSlider slider=new JSlider(min,max,Math.max(min,Math.min(max,value)));slider.setPreferredSize(new Dimension(260,TARGET));slider.setUI(new BasicSliderUI(slider){protected Dimension getThumbSize(){return new Dimension(34,46);}public void paintThumb(Graphics g){g.setColor(ui.accent);g.fillRoundRect(thumbRect.x,thumbRect.y,thumbRect.width,thumbRect.height,14,14);}protected void scrollDueToClickInTrack(int direction){int v=valueForXPosition(slider.getMousePosition()==null?thumbRect.x:slider.getMousePosition().x);slider.setValue(v);}});
  JPanel controls=Touch.panel(new BorderLayout(12,0));JPanel less=Touch.panel(new GridLayout(1,2,8,0)),more=Touch.panel(new GridLayout(1,2,8,0));
  less.add(ui.button("−10",()->slider.setValue(Math.max(min,slider.getValue()-10))));less.add(ui.button("−1",()->slider.setValue(Math.max(min,slider.getValue()-1))));more.add(ui.button("+1",()->slider.setValue(Math.min(max,slider.getValue()+1))));more.add(ui.button("+10",()->slider.setValue(Math.min(max,slider.getValue()+10))));less.setPreferredSize(new Dimension(136,TARGET));more.setPreferredSize(new Dimension(136,TARGET));controls.add(less,BorderLayout.WEST);controls.add(slider);controls.add(more,BorderLayout.EAST);p.add(controls);
  slider.addChangeListener(e->{label.setText(title+": "+slider.getValue()+suffix);if(!slider.getValueIsAdjusting())action.accept(slider.getValue());});return p;
 }
 public static void text(Touch ui,String title,JTextField destination){
  if(!destination.isEnabled())return;JDialog dialog=shell(ui,title);JPanel b=body(ui);JTextField input=destination instanceof JPasswordField?new JPasswordField():new JTextField();input.setText(destination.getText());input.setFont(new Font("Yu Gothic UI",0,30));input.setPreferredSize(new Dimension(600,72));b.add(input,BorderLayout.NORTH);
  JPanel keys=Touch.panel(new GridLayout(0,1,0,10));keys.putClientProperty("preserveCase",true);boolean[] shifted={false};java.util.List<JButton> letters=new java.util.ArrayList<>();String[] rows={"1 2 3 4 5 6 7 8 9 0 Backspace","q w e r t y u i o p","a s d f g h j k l","z x c v b n m . @ -"};
  for(String row:rows){JPanel r=Touch.panel(new GridLayout(1,0,6,0));for(String key:row.split(" ")){JButton button=ui.button(key,()->{if(key.equals("Backspace"))Touch.backspace(input);else input.replaceSelection(shifted[0]?key.toUpperCase(java.util.Locale.ROOT):key);});button.setFont(new Font("Yu Gothic UI",0,key.equals("Backspace")?20:28));if(key.matches("[a-z]"))letters.add(button);r.add(button);}keys.add(r);}
  JPanel options=Touch.panel(new GridLayout(1,4,10,0));options.add(ui.button("Shift",()->{shifted[0]=!shifted[0];for(JButton c:letters)c.setText(shifted[0]?c.getText().toUpperCase():c.getText().toLowerCase());}));options.add(ui.button("Space",()->input.replaceSelection(" ")));options.add(ui.button("Symbols",()->{String[] chars={"!","#","$","%","&","*","(",")","_","+","=","/","\\","?",":",";","'","\"","[","]","{","}","<",">","|","^","~","`",","};int selected=choose(ui,"Insert symbol",chars);if(selected>=0)input.replaceSelection(chars[selected]);}));options.add(ui.button("Clear",()->input.setText("")));keys.add(options);b.add(keys);
  JPanel actions=Touch.panel(new GridLayout(1,2,16,0));actions.add(ui.button("Cancel",dialog::dispose));actions.add(ui.button("Use this text",()->{destination.setText(input.getText());dialog.dispose();}));b.add(actions,BorderLayout.SOUTH);ui.themeTree(b);dialog.setContentPane(b);dialog.setSize(Math.min(1240,ui.frame.getWidth()-20),Math.min(900,ui.frame.getHeight()-20));dialog.setLocationRelativeTo(ui.frame);dialog.setVisible(true);input.setText("");dialog.dispose();
 }
 public static class OutlineUI extends BasicLabelUI {
  public static ComponentUI createUI(JComponent c){return new OutlineUI();}
  protected void paintEnabledText(JLabel label,Graphics graphics,String text,int x,int y){
   if(text==null||text.isEmpty())return;text=caps(label,text);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);TextLayout layout=new TextLayout(text,label.getFont(),g.getFontRenderContext());if(label.getHorizontalAlignment()==SwingConstants.CENTER)x=Math.round((label.getWidth()-layout.getAdvance())/2);else if(label.getHorizontalAlignment()==SwingConstants.RIGHT)x=Math.round(label.getWidth()-label.getInsets().right-layout.getAdvance());Shape path=layout.getOutline(AffineTransform.getTranslateInstance(x,y));Color fg=label.getForeground();double light=fg.getRed()*.2126+fg.getGreen()*.7152+fg.getBlue()*.0722;g.setColor(light>140?new Color(10,20,36,235):new Color(238,248,255,235));g.setStroke(new BasicStroke(3.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(path);g.setColor(fg);g.fill(path);g.dispose();
  }
 }
}




